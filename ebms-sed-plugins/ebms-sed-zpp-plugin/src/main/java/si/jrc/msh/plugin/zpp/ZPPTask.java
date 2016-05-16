/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.plugin.zpp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import org.apache.xmlgraphics.util.MimeConstants;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.msh.ebms.outbox.payload.MSHOutPayload;
import org.sed.ebms.cert.SEDCertStore;
import si.jrc.msh.plugin.zpp.doc.DocumentSodBuilder;
import si.jrc.msh.plugin.zpp.utils.FOPUtils;
import si.jrc.msh.sec.SEDCrypto;
import si.sed.commons.MimeValues;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.exception.FOPException;
import si.sed.commons.exception.HashException;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.JMSManagerInterface;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.interfaces.TaskExecutionInterface;
import si.sed.commons.interfaces.exception.TaskException;
import si.sed.commons.utils.HashUtils;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.StringFormater;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.sec.KeystoreUtils;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(TaskExecutionInterface.class)
public class ZPPTask implements TaskExecutionInterface {

private static final SEDLogger LOG = new SEDLogger(ZPPTask.class);

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
    
    // TODO externalize
    String singDAAlias = "msh.e-box-a.si";

    @Override
    public String executeTask(Properties p) throws TaskException {

        long l = LOG.logStart();
        StringWriter sw = new StringWriter();
        sw.append("Start zpp plugin task: \n");

        MSHInMail mi = new MSHInMail();
        mi.setStatus(ZPPConstants.LOCK_STATUS);
        List<MSHInMail> lst=  mDB.getDataList(MSHInMail.class,  -1, -1, "Id", "ASC", mi);
        sw.append("got " + lst.size() + " do deliver");
        for (MSHInMail m: lst){
            try {
                processInZPPDelivery(m);                
            } catch (FOPException | HashException ex) {
                LOG.logError(l, ex);
                sw.append("Error occurred processing: " + m.getId() + " err: " + ex.getMessage());
            }
        }
        
        sw.append("Endzpp plugin task");
        return sw.toString();
    }

    @Override
    public String getType() {
        return "zpp-plugin";
    }

    @Override
    public String getName() {
        return "ZPP plugin";
    }

    @Override
    public String getDesc() {
        return "Sign deliveryadvice for incomming mail ";
    }

    @Override
    public Properties getProperties() {
        Properties p = new Properties();
        

        return p;
    }
    
    
    public void processInZPPDelivery(MSHInMail mInMail) throws FOPException, HashException {
        long l = LOG.logStart();
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

            mDB.serializeOutMail(mout, "", "ZPPDeliveryPlugin", "");

            mInMail.setStatus(SEDInboxMailStatus.PROCESS.getValue());
            mInMail.setStatusDate(Calendar.getInstance().getTime());
//           
            mDB.updateInMail(mInMail, "DeliveryAdviceGenerated and submited to sender");

        } catch (IOException | SEDSecurityException ex) {
            LOG.logError(l, ex);
        }
        LOG.logEnd(l);
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

}
