/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.plugin.zpp;

import si.sed.commons.exception.SEDSecurityException;
import si.jrc.msh.sec.SEDCrypto;
import si.jrc.msh.sec.SEDKey;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.UserTransaction;
import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.xmlgraphics.util.MimeConstants;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.inbox.payload.MSHInPart;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.msh.ebms.outbox.payload.MSHOutPayload;
import org.msh.svev.pmode.PMode;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.ebox.Execute;
import org.sed.ebms.ebox.Export;
import org.sed.ebms.ebox.SEDBox;
import org.w3c.dom.Element;

import si.jrc.msh.plugin.zpp.doc.DocumentSodBuilder;
import si.sed.commons.MimeValues;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.exception.FOPException;
import si.sed.commons.exception.HashException;
import si.sed.commons.exception.StorageException;
import si.jrc.msh.plugin.zpp.utils.FOPUtils;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.JMSManagerInterface;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.interfaces.SoapInterceptorInterface;
import si.sed.commons.utils.HashUtils;

import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StringFormater;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.sec.KeystoreUtils;
//import si.sed.commons.utils.sec.CertificateUtils;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(SoapInterceptorInterface.class)
@TransactionManagement(TransactionManagementType.BEAN)
public class ZPPInInterceptor implements SoapInterceptorInterface {

    protected final SEDLogger mlog = new SEDLogger(ZPPOutInterceptor.class);
    SEDCrypto mSedCrypto = new SEDCrypto();
    HashUtils mpHU = new HashUtils();
    DocumentSodBuilder dsbSodBuilder = new DocumentSodBuilder();
    KeystoreUtils mkeyUtils = new KeystoreUtils();

    FOPUtils mfpFop = null;
    StringFormater msfFormat = new StringFormater();

    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mDB;

    @EJB(mappedName = SEDJNDI.JNDI_JMSMANAGER)
    JMSManagerInterface mJMS;
    
    @EJB(mappedName = SEDJNDI.JNDI_SEDLOOKUPS)
    SEDLookupsInterface msedLookup;

    @Resource
    public UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_ZPP_PU", name = "ebMS_ZPP_PU")
    public EntityManager memEManager;
    
    // TODO externalize
    String singDAAlias = "msh.e-box-a.si";

    public ZPPInInterceptor() {

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

            mlog.log("ZPPInInterceptor 1");
            if (sigAnies != null) {
                mlog.log("ZPPInInterceptor 2");
                List<Element> lst = (List<Element>) sigAnies;
                Key k = null;
                for (Element e : lst) {
                    if (e.getLocalName().equals("EncryptedKey")) {
                        mlog.log("ZPPInInterceptor 4");
                        SEDCertStore sc =  msedLookup.getSEDCertStoreByCertAlias(singDAAlias, true);
                        
                        k = mSedCrypto.decryptEncryptedKey(e, mkeyUtils.getPrivateKeyEntryForAlias(singDAAlias, sc).getPrivateKey(), SEDCrypto.SymEncAlgorithms.AES128_CBC);
                        break;

                    }
                }
                mlog.log("ZPPInInterceptor 3, key: " + k);
                if (moutMail != null && k != null) {
                    mlog.log("ZPPInInterceptor 4, key: " + k);
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
                mlog.log("processInZPPAdviceoFDelivery - get key" + key);
                Element elKey = mSedCrypto.encryptedKeyWithReceiverPublicKey(key, xc, mInMail.getSenderEBox(), mInMail.getConversationId());
                mlog.log("processInZPPAdviceoFDelivery - get encrypted key" + elKey);
                // got signal message:
                SignalMessage signal = msg.getExchange().get(SignalMessage.class);
                signal.getAnies().add(elKey);

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

            
            
            SEDCertStore cs =  msedLookup.getSEDCertStoreByCertAlias(singDAAlias, true);
            
            
            // create signed delivery advice
            dsbSodBuilder.createMail(mout, fos, mkeyUtils.getPrivateKeyEntryForAlias(singDAAlias, cs));
            mp.setDescription("DeliveryAdvice");

            mp.setFilepath(StorageUtils.getRelativePath(fDA));
            mp.setMd5(mpHU.getMD5Hash(fDA));
            mp.setFilename(fDA.getName());
            mp.setName(mp.getFilename().substring(mp.getFilename().lastIndexOf(".")));

            mDB.serializeOutMail(mout, "", "ZPPDeliveryPlugin", pmd.getId());

            mInMail.setStatus(SEDInboxMailStatus.PROCESS.getValue());
            mInMail.setStatusDate(Calendar.getInstance().getTime());
//           
            mDB.updateInMail(mInMail, "DeliveryAdviceGenerated and submited to sender");

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

//Key k = getSecretKey(mi.getConversationId());
        TypedQuery<SEDKey> q = memEManager.createNamedQuery("si.jrc.msh.sec.SEDKey.getById", SEDKey.class);
        q.setParameter("id", new BigInteger(mi.getConversationId()));
        return q.getSingleResult();

        /*List<Object[]> lst = q.getResultList();
            String keyStr = (String) lst.get(0)[0];
            String alg = (String) lst.get(0)[1];

            k = new SEDKey(bi, Base64.getDecoder().decode(keyStr), alg, "");
         */
    }


    public void decryptMail(Key key, String convID, SEDBox sb) {
        long l = mlog.logStart();

        try {

            List<MSHInMail> lst = mDB.getInMailConvIdAndAction("DeliveryNotification", convID);;

            for (MSHInMail mi : lst) {

                if (sb == null) {
                    sb = mDB.getSedBoxByName(mi.getReceiverEBox());
                }

                String exporFileName = null;
                File exportFolder = null;
                if (sb != null && sb.getExport() != null && sb.getExport().getActive() && sb.getExport().getFileMask() != null) {
                    Export e = sb.getExport();
                    exporFileName = msfFormat.format(e.getFileMask(), mi);
                    String folder = Utils.replaceProperties(e.getFolder());
                    exportFolder = new File(folder);
                    if (!exportFolder.exists()) {
                        exportFolder.mkdirs();
                    }
                }

                List<MSHInPart> lstDec = new ArrayList<>();
                int i = 0;
                List<String> listFiles = new  ArrayList<>();
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
                                String filPrefix = exportFolder.getAbsolutePath() + File.separator + exporFileName;
                                System.out.println("Export files prefix: " + filPrefix);
                                if (sb.getExport().getExportMetaData()) {
                                    System.out.println("Export metadata: " + filPrefix);
                                    try {
                                        String fn = filPrefix + "." + MimeValues.MIME_XML.getSuffix();
                                        listFiles.add(fn);
                                        XMLUtils.serialize(mi, fn);
                                    } catch (JAXBException | FileNotFoundException ex) {
                                        // LOG.logError(l, "Export metadata ERROR. Export file:" + fileMetaData + ".", ex);
                                    }
                                }
                                System.out.println("Export file: : " + fNew);
                                String fn = filPrefix + "_" + i + "." + MimeValues.getSuffixBYMimeType(miDec.getMimeType());
                                listFiles.add(fn);
                                StorageUtils.copyFile(fNew, new File(fn));
                            }

                        } catch (IOException | StorageException | SEDSecurityException | HashException ex) {
                            Logger.getLogger(ZPPInInterceptor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                mi.getMSHInPayload().getMSHInParts().addAll(lstDec);
                mi.setStatus(SEDInboxMailStatus.RECEIVED.getValue());
                mi.setStatusDate(Calendar.getInstance().getTime());
                mDB.updateInMail(mi, "Received secred key and decript payloads");

                if (sb != null && sb.getExecute() != null && sb.getExecute().getActive() && sb.getExecute().getCommand() != null) {
                    Execute e = sb.getExecute();
                    String params = msfFormat.format(e.getParameters(), mi);
                    try {
                        mJMS.executeProcessOnInMail(mi.getId().longValue(), sb.getExecute().getCommand(), String.join(File.pathSeparator, listFiles) + " " +   params);
                    } catch (NamingException | JMSException ex) {
                        Logger.getLogger(ZPPInInterceptor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                }
                
            }
        } finally {

        }
        mlog.logEnd(l);
    }

    @Override
    public void handleFault(SoapMessage t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
