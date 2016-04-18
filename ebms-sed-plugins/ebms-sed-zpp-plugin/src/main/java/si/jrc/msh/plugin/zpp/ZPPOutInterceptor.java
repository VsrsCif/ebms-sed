/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.plugin.zpp;

import si.sed.commons.exception.SEDSecurityException;
import si.jrc.msh.sec.SEDCrypto;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javax.crypto.SecretKey;
import org.apache.cxf.binding.soap.SoapMessage;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.msh.ebms.outbox.payload.MSHOutPayload;

import si.sed.commons.MimeValues;
import si.sed.commons.exception.StorageException;

import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.apache.xmlgraphics.util.MimeConstants;
import si.jrc.msh.plugin.zpp.doc.DocumentSodBuilder;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.exception.FOPException;
import si.sed.commons.exception.HashException;
import si.jrc.msh.plugin.zpp.utils.FOPUtils;
import si.jrc.msh.sec.SEDKey;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.interfaces.SoapInterceptorInterface;
import si.sed.commons.utils.HashUtils;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(SoapInterceptorInterface.class)
@TransactionManagement(TransactionManagementType.BEAN)
public class ZPPOutInterceptor implements SoapInterceptorInterface {

    public static final String ENCODING_UTF8 = "UTF-8";
    public static final String ENCODING_BASE64 = "base64";
    public static final String SECRET_KEY_FILE = "secret-key.dat";

    protected final SEDCrypto.SymEncAlgorithms mAlgorithem = SEDCrypto.SymEncAlgorithms.AES128_CBC;

    protected final SEDLogger mlog = new SEDLogger(ZPPOutInterceptor.class);

      @Resource
    public UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_ZPP_PU", name = "ebMS_ZPP_PU")
    public EntityManager memEManager;

    SEDCrypto mscCrypto = new SEDCrypto();
    HashUtils mpHU = new HashUtils();
    FOPUtils mfpFop = null;
    DocumentSodBuilder dsbSodBuilder = new DocumentSodBuilder();

    public ZPPOutInterceptor() {

    }

    @Override
    public void handleMessage(SoapMessage msg) {
        long l = mlog.logStart(msg);

        // get outgoing mail (is requesto
        MSHOutMail outMail = msg.getExchange().get(MSHOutMail.class);
        // if  service ZPP delivery,  action delivery 
        // set Notification, ecrypt attachemts and store enc key.
        if (outMail != null && ZPPConstants.S_ZPP_SERVICE.equals(outMail.getService())
                && ZPPConstants.S_ZPP_ACTION_DELIVERY_NOTIFICATION.equals(outMail.getAction())) {
            //if (outMail != null) {
            try {
                prepareToZPPDelivery(outMail);
            } catch (HashException | SEDSecurityException | StorageException | FOPException | IOException ex) {
                mlog.logError(l, ex);
            }
        }
        mlog.logEnd(l);
    }

    private void prepareToZPPDelivery(MSHOutMail outMail) throws SEDSecurityException, StorageException, FOPException, IOException, HashException {

        long l = mlog.logStart(outMail);

        //  EntityManagerFactory emf = null;
        //  EntityManager em = null;
        //    try {
        //    emf = getSEDEntityManagerFactory();
        //   em = emf.createEntityManager();
        // todo check if key is already in db
        //SecretKey sk = new SecretKeySpec(bytes, ENCODING_UTF8)
        // generate key
        SecretKey skey = mscCrypto.getKey(mAlgorithem);
        //create nofitication
        File fDNViz = StorageUtils.getNewStorageFile("pdf", "DeliveryNoification");

        getFOP().generateVisualization(outMail, fDNViz, FOPUtils.FopTransformations.DeliveryNotification, MimeConstants.MIME_PDF);

        // encrypt payloads 
        MSHOutPayload pl = getEncryptedPayloads(skey, outMail);

        String fPDFVizualization = StorageUtils.getRelativePath(fDNViz);
        //add vizualization as pdf
        MSHOutPart ptNew = new MSHOutPart();
        ptNew.setEncoding(ENCODING_UTF8);
        ptNew.setMimeType(MimeValues.MIME_PDF.getMimeType());
        ptNew.setDescription("DeliveryNoification");
        ptNew.setType("DeliveryNoification");
        ptNew.setFilepath(fPDFVizualization);
        ptNew.setFilename("DeliveryNoification.pdf");
        pl.getMSHOutParts().add(0, ptNew);

        outMail.setMSHOutPayload(pl);
        outMail.setConversationId(outMail.getId().toString()); // conversation id is id of fist mail

        storeSecretKey(outMail.getId(), skey);
        /*           
         
            ZPPKey zk = new ZPPKey();
            zk.setId(outMail.getId());
            zk.setSecretKey(Base64.getEncoder().encodeToString(skey.getEncoded()));
            zk.setAlgorithm(mAlgorithem.getURI());
             em.getTransaction().begin();
            em.persist(zk);
            em.getTransaction().commit(); 
             
            Query q = em.createNativeQuery("insert into zpp_plugin_out_keys  (mail_id, secret_key,algorithm) values (:mailId, :secretKey, :algorithm )");
            q.setParameter("mailId", outMail.getId());
            q.setParameter("secretKey", Base64.getEncoder().encodeToString(skey.getEncoded()));
            q.setParameter("algorithm", mAlgorithem.getURI());
            // if key not exists
            // store key to db
            //   System.out.println("format:" + skey.getFormat());
            System.out.println("**************************");
            System.out.println("format:" + skey.getFormat());
            em.getTransaction().begin();
            q.executeUpdate();
            em.getTransaction().commit();

        } finally {
            if (em != null) {
                em.close();
            }
            if (emf != null) {
                emf.close();
            }
        }*/

    }

    private MSHOutPayload getEncryptedPayloads(SecretKey skey, MSHOutMail mail) throws SEDSecurityException, StorageException, HashException {
        long l = mlog.logStart();

        MSHOutPayload op = new MSHOutPayload();

        int i = 0;
        for (MSHOutPart pt : mail.getMSHOutPayload().getMSHOutParts()) {
            File fIn = StorageUtils.getFile(pt.getFilepath());
            File fOut = new File(fIn.getAbsoluteFile() + ".enc");
            mscCrypto.encryptFile(fIn, fOut, skey);
            //ed.getKeyInfo().setId(mail.getConversationId());
            //pt.setName("EncryptedData_" + (++i));
            MSHOutPart ptNew = new MSHOutPart();

            ptNew.setMimeType(pt.getMimeType());
            //ptNew.setMimeType(MimeValues.MIME_BIN.getMimeType());
            ptNew.setFilepath(StorageUtils.getRelativePath(fOut));
            ptNew.setMd5(mpHU.getMD5Hash(fOut));
            ptNew.setFilename(fOut.getName());
            op.getMSHOutParts().add(ptNew);
        }
        mlog.logEnd(l);
        return op;
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

    private void storeSecretKey(BigInteger bi, SecretKey skey) throws IOException {
        SEDKey sk = new SEDKey(bi, skey.getEncoded() , skey.getAlgorithm(),skey.getFormat());
        
        try {
            mutUTransaction.begin();
            memEManager.persist(sk);
            mutUTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            Logger.getLogger(ZPPOutInterceptor.class.getName()).log(Level.SEVERE, null, ex);
        }
        /*Path filePath = Paths.get(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + "/SVEV/" + SECRET_KEY_FILE);
        if (!Files.exists(filePath)) {
        Files.createFile(filePath);
        }
        Files.write(filePath, String.format("%d %s %s\n", bi, Base64.getEncoder().encodeToString(skey.getEncoded()), skey.getFormat()).getBytes(), StandardOpenOption.APPEND);
         */ 
        
        
        
        /*Path filePath = Paths.get(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + "/SVEV/" + SECRET_KEY_FILE);
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
        Files.write(filePath, String.format("%d %s %s\n", bi, Base64.getEncoder().encodeToString(skey.getEncoded()), skey.getFormat()).getBytes(), StandardOpenOption.APPEND);
*/
    }

    @Override
    public void handleFault(SoapMessage t) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
