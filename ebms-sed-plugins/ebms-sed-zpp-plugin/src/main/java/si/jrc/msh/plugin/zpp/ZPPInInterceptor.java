/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.plugin.zpp;

import java.io.BufferedReader;
import si.sed.commons.exception.SEDSecurityException;
import si.jrc.msh.sec.SEDCrypto;
import si.jrc.msh.sec.SEDKey;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.phase.Phase;
import org.apache.xmlgraphics.util.MimeConstants;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.inbox.payload.MSHInPart;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.msh.ebms.outbox.payload.MSHOutPayload;
import org.msh.svev.pmode.PMode;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import org.sed.ebms.ebox.Export;
import org.sed.ebms.ebox.SEDBox;
import org.w3c.dom.Element;
import static si.jrc.msh.plugin.zpp.ZPPOutInterceptor.SECRET_KEY_FILE;

import si.jrc.msh.plugin.zpp.doc.DocumentSodBuilder;
import si.sed.commons.MimeValues;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.exception.FOPException;
import si.sed.commons.exception.HashException;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.FOPUtils;
import si.sed.commons.utils.HashUtils;

import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StringFormater;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.sec.CertificateUtils;
import si.sed.commons.utils.xml.XMLUtils;
import si.sed.msh.plugin.AbstractPluginInterceptor;

/**
 *
 * @author sluzba
 */
public class ZPPInInterceptor extends AbstractPluginInterceptor {

    protected final SEDLogger mlog = new SEDLogger(ZPPOutInterceptor.class);
    SEDCrypto mSedCrypto = new SEDCrypto();
    HashUtils mpHU = new HashUtils();
    DocumentSodBuilder dsbSodBuilder = new DocumentSodBuilder();

    FOPUtils mfpFop = null;
    StringFormater msfFormat = new StringFormater();

    public ZPPInInterceptor() {
        super(Phase.PRE_INVOKE);

    }

    public ZPPInInterceptor(String phase) {
        super(phase);
    }

    @Override
    public void handleMessage(SoapMessage msg) {
        long l = mlog.logStart();
        PMode pmd = msg.getExchange().get(PMode.class);
        SEDBox sb = msg.getExchange().get(SEDBox.class);

        MSHInMail mInMail = msg.getExchange().get(MSHInMail.class);
        MSHOutMail moutMail = msg.getExchange().get(MSHOutMail.class);
        Object sigAnies = msg.getExchange().get("SIGNAL_ELEMENTS");
        try {
            if (mInMail != null && ZPPConstants.S_ZPP_SERVICE.equals(mInMail.getService())
                    && ZPPConstants.S_ZPP_ACTION_DELIVERY_NOTIFICATION.equals(mInMail.getAction())) {
                processInZPPDelivery(mInMail, pmd);
            }

            if (mInMail != null && ZPPConstants.S_ZPP_SERVICE.equals(mInMail.getService())
                    && ZPPConstants.S_ZPP_ACTION_ADVICE_OF_DELIVERY.equals(mInMail.getAction())) {
                processInZPPAdviceoFDelivery(mInMail, pmd, msg);
            }

            if (sigAnies != null) {
                List<Element> lst = (List<Element>) sigAnies;
                Key k = null;
                for (Element e : lst) {
                    if (e.getLocalName().equals("EncryptedKey")) {
                        k = mSedCrypto.decryptEncryptedKey(e, CertificateUtils.getInstance().getKeyStore(), SEDCrypto.SymEncAlgorithms.AES128_CBC);
                        break;

                    }
                }
                if (moutMail != null && k != null) {
                    decryptMail(k, moutMail.getConversationId(), sb);

                }
            }

        } catch (FOPException | HashException | SEDSecurityException ex) {
            mlog.logError(l, ex);
        }
        mlog.logEnd(l);
    }

    public void processInZPPAdviceoFDelivery(MSHInMail mInMail, PMode pmd, SoapMessage msg) throws FOPException, HashException {
        long l = mlog.logStart();
        try {
            // get x509 keys

            File docFile = StorageUtils.getFile(mInMail.getMSHInPayload().getMSHInParts().get(0).getFilepath());

            String x509 = XMLUtils.getElementValue(new FileInputStream(docFile), ZPPInInterceptor.class.getResourceAsStream("/xslt/getX509CertFromDocument.xsl"));
            if (x509 != null) {
                X509Certificate xc = mSedCrypto.getCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(x509)));
                // get key 
                Key key = getEncryptionKeyForDeliveryAdvice(mInMail);
                String encrypteKey = mSedCrypto.encryptKeyWithReceiverPublicKey(key, xc, mInMail.getSenderEBox(), mInMail.getConversationId());
                // got signal message:
                SignalMessage signal = msg.getExchange().get(SignalMessage.class);
                signal.getAnies().add(mSedCrypto.encryptedKeyWithReceiverPublicKey(key, xc, mInMail.getSenderEBox(), mInMail.getConversationId()));

            }

        } catch (StorageException | JAXBException | TransformerException | IOException | SEDSecurityException ex) {
            mlog.logError(l, ex);
        }
        mlog.logEnd(l);
    }

    public void processInZPPDelivery(MSHInMail mInMail, PMode pmd) throws FOPException, HashException {
        long l = mlog.logStart();
        // create delivery advice 
        File fDNViz = null;
        File fDA = null;
        try {
            fDNViz = StorageUtils.getNewStorageFile("pdf", "AdviceOfDelivery");
            fDA = new File(fDNViz.getAbsoluteFile() + ".xml"); // create deliveryadvice
        } catch (StorageException ex) {
            Logger.getLogger(ZPPInInterceptor.class.getName()).log(Level.SEVERE, null, ex);
        }

        getFOP().generateVisualization(mInMail, fDNViz, FOPUtils.FopTransformations.AdviceOfDelivery, MimeConstants.MIME_PDF);
        MSHOutMail mout = new MSHOutMail();
        mout.setMessageId(Utils.getInstance().getGuidString());
        mout.setService(ZPPConstants.S_ZPP_SERVICE);
        mout.setAction(ZPPConstants.S_ZPP_ACTION_ADVICE_OF_DELIVERY);
        mout.setConversationId(mInMail.getConversationId());
        mout.setSenderEBox(mInMail.getReceiverEBox());
        mout.setSenderName(mInMail.getReceiverName());
        mout.setRefToMessageId(mInMail.getMessageId());
        mout.setReceiverEBox(mInMail.getSenderEBox());
        mout.setReceiverName(mInMail.getSenderName());
        mout.setSubject(ZPPConstants.S_ZPP_ACTION_ADVICE_OF_DELIVERY);
        // prepare mail to persist 
        Date dt = new Date();
        // set current status
        mout.setStatus(SEDOutboxMailStatus.SUBMITTED.getValue());
        mout.setSubmittedDate(dt);
        mout.setStatusDate(dt);

        mout.setMSHOutPayload(new MSHOutPayload());
        MSHOutPart mp = new MSHOutPart();
        mp.setDescription("DeliveryAdvice");
        mp.setFilepath(StorageUtils.getRelativePath(fDNViz));
        mp.setMimeType(MimeValues.MIME_XML.getMimeType());
        mout.getMSHOutPayload().getMSHOutParts().add(mp);

        try (FileOutputStream fos = new FileOutputStream(fDA)) {

            // TODO externalize
            String singDAAlias = "msh.e-box-a.si";
            // create signed delivery advice
            dsbSodBuilder.createMail(mout, singDAAlias, fos);
            mp.setDescription("DeliveryAdvice");

            mp.setFilepath(StorageUtils.getRelativePath(fDA));
            mp.setMd5(mpHU.getMD5Hash(fDA));
            mp.setFilename(fDA.getName());
            mp.setName(mp.getFilename().substring(mp.getFilename().lastIndexOf(".")));

            serializeMail(mout, "", "ZPPDeliveryPlugin", pmd.getId());

            mInMail.setStatus(SEDInboxMailStatus.PROCESS.getValue());
            mInMail.setStatusDate(Calendar.getInstance().getTime());
            updateInMail(mInMail, "DeliveryAdviceGenerated and submited to sender");

        } catch (IOException | SEDSecurityException ex) {
            mlog.logError(l, ex);
        }
        mlog.logEnd(l);
    }

    public FOPUtils getFOP() {
        if (mfpFop == null) {
            File fconf = new File(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator
                    + ZPPConstants.SVEV_FOLDER + File.separator + ZPPConstants.FOP_CONFIG_FILENAME);

            mfpFop = new FOPUtils(fconf, System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator
                    + ZPPConstants.SVEV_FOLDER + File.separator + ZPPConstants.XSLT_FOLDER);
        }
        return mfpFop;
    }

    public Key getEncryptionKeyForDeliveryAdvice(MSHInMail mi) throws IOException {
        Key k = getSecretKey(mi.getConversationId());
        /*
        EntityManagerFactory emf = null;
        EntityManager em = null;
        try {
            emf = getSEDEntityManagerFactory();
            em = emf.createEntityManager();
            BigInteger bi = new BigInteger(mi.getConversationId());
            Query q = em.createNativeQuery("select secret_key, algorithm from zpp_plugin_out_keys  where mail_id = :mailId");
            q.setParameter("mailId", bi);

            List<Object[]> lst = q.getResultList();
            String keyStr = (String) lst.get(0)[0];
            String alg = (String) lst.get(0)[1];

            k = new SEDKey(bi, Base64.getDecoder().decode(keyStr), alg, "");

        } finally {
            if (em != null) {
                em.close();
            }
            if (emf != null) {
                emf.close();
            }
        }*/

        return k;

    }

    private Key getSecretKey(String bi) throws IOException {
        Key sk = null;
        Path filePath = Paths.get(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + "/SVEV/" + SECRET_KEY_FILE);
        try (BufferedReader in = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line = null;
            while ((line = in.readLine()) != null) {
                String[] str = line.split(" ");
                if (str[0].equals(bi)) {
                    sk = new SEDKey(new BigInteger(str[0]), Base64.getDecoder().decode(str[1]), str[2], "");

                }
            }
        };
        return sk;

    }

    public void decryptMail(Key key, String convID, SEDBox sb) {
        long l = mlog.logStart();

        EntityManagerFactory emf = null;
        EntityManager em = null;
        try {
            //emf = getSEDEntityManagerFactory();
            em = getEntityManager();

            Query q = em.createNamedQuery("MSHInMail.getByConvIdAndAction", MSHInMail.class);
            q.setParameter("convId", convID);
            q.setParameter("action", "DeliveryNotification");
            List<MSHInMail> lst = q.getResultList();

            for (MSHInMail mi : lst) {

                if (sb == null) {
                    sb = getSedBoxByName(mi.getReceiverEBox());
                }
                
                
                String exporFileName = null;
                File exportFolder = null;
                if (sb!= null && sb.getExport() != null && sb.getExport().getActive() && sb.getExport().getFileMask() != null) {
                    Export e = sb.getExport();
                    exporFileName = msfFormat.format(e.getFileMask(), mi);
                    String folder = Utils.replaceProperties(e.getFolder());
                    exportFolder = new File(folder);
                    if (!exportFolder.exists()) {
                        exportFolder.mkdirs();
                    }
                    System.out.println("Export files to: " +folder + " mask: " + exporFileName );
                } else {
                    System.out.println("Do not export files! ") ;
                }

                List<MSHInPart> lstDec = new ArrayList<>();
                int i =0; 
                for (MSHInPart mip : mi.getMSHInPayload().getMSHInParts()) {
                    String oldFileName = mip.getFilename();
                    i++;
                    if (oldFileName.endsWith(".enc")) {
                        String newFileName = oldFileName.substring(0, oldFileName.lastIndexOf(".enc"));
                        File fNew;
                        try (FileInputStream fis = new FileInputStream(StorageUtils.getFile(mip.getFilepath()));
                                FileOutputStream bos = new FileOutputStream(fNew = StorageUtils.getFile(newFileName))) {
                            mSedCrypto.decryptStream(fis, bos, key);

                            MSHInPart miDec = new MSHInPart();
                            miDec.setDescription(mip.getDescription());
                            miDec.setEbmsId(mip.getEbmsId() + "-dec");
                            miDec.setEncoding(mip.getEncoding());
                            miDec.setFilename(newFileName);
                            miDec.setMimeType(mip.getMimeType());
                            miDec.setName(mip.getName());
                            miDec.setType(mip.getType());
                            miDec.setMd5(mpHU.getMD5Hash(fNew));
                            miDec.setFilepath(StorageUtils.getRelativePath(fNew));
                            lstDec.add(miDec);
                            if (sb.getExport() != null && exportFolder != null && exporFileName != null) {
                                String filPrefix = exportFolder.getAbsolutePath() + File.separator + exporFileName ;
                                System.out.println("Export files prefix: " + filPrefix );
                                if (sb.getExport().getExportMetaData()) {
                                    System.out.println("Export metadata: " + filPrefix );
                                    try {

                                        XMLUtils.serialize(mi, filPrefix+ "." + MimeValues.MIME_XML.getSuffix());
                                    } catch (JAXBException | FileNotFoundException ex) {
                                       // LOG.logError(l, "Export metadata ERROR. Export file:" + fileMetaData + ".", ex);
                                    }
                                }
                                System.out.println("Export file: : " + fNew );
                                StorageUtils.copyFile(fNew,  new File(filPrefix + "_" +i +"." +  MimeValues.getSuffixBYMimeType(miDec.getMimeType())));
                            }

                        } catch (IOException | StorageException | SEDSecurityException | HashException ex) {
                            Logger.getLogger(ZPPInInterceptor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                mi.getMSHInPayload().getMSHInParts().addAll(lstDec);
                mi.setStatus(SEDInboxMailStatus.RECEIVED.getValue());
                mi.setStatusDate(Calendar.getInstance().getTime());
                updateInMail(mi, "Received secred key and decript payloads");

            }
        } finally {

        }
        mlog.logEnd(l);
    }
    
     private SEDBox getSedBoxByName(String sbox) {
        TypedQuery<SEDBox> sq = getEntityManager().createNamedQuery("org.sed.ebms.ebox.SEDBox.getByName", SEDBox.class);
        sq.setParameter("BoxName", sbox);
        try {
            return sq.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

}
