/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
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
import org.sed.ebms.cron.SEDTaskType;
import org.sed.ebms.cron.SEDTaskTypeProperty;
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
  private static final String SIGN_ALIAS = "zpp.sign.key.alias";
  private static final String REC_SEDBOX = "zpp.sedbox";

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
  /**
   *
   * @param p
   * @return
   * @throws TaskException
   */
  @Override
  public String executeTask(Properties p)
      throws TaskException {

    long l = LOG.logStart();
    StringWriter sw = new StringWriter();
    sw.append("Start zpp plugin task: \n");

    String singDAAlias = "";
    if (!p.containsKey(SIGN_ALIAS)) {
      throw new TaskException(TaskException.TaskExceptionCode.InitException,
          "Missing parameter:  '" + SIGN_ALIAS + "'!");
    } else {
      singDAAlias = p.getProperty(SIGN_ALIAS);
    }

    String sedBox = "";
    if (!p.containsKey(REC_SEDBOX)) {
      throw new TaskException(TaskException.TaskExceptionCode.InitException,
          "Missing parameter:  '" + REC_SEDBOX + "'!");
    } else {
      sedBox = p.getProperty(REC_SEDBOX);
    }

    MSHInMail mi = new MSHInMail();
    mi.setStatus(SEDInboxMailStatus.PLUGINLOCKED.getValue());
    mi.setReceiverEBox(sedBox);

    List<MSHInMail> lst = mDB.getDataList(MSHInMail.class, -1, -1, "Id", "ASC", mi);
    sw.append("got " + lst.size() + " mails for sedbox: '" + sedBox + "'!");
    for (MSHInMail m : lst) {
      try {
        processInZPPDelivery(m, singDAAlias);
      } catch (FOPException | HashException ex) {
        LOG.logError(l, ex);
        sw.append("Error occurred processing: " + m.getId() + " err: " + ex.getMessage());
      }
    }

    sw.append("Endzpp plugin task");
    return sw.toString();
  }

  /**
   *
   * @return
   */
  @Override
  public SEDTaskType getTaskDefinition() {
    SEDTaskType tt = new SEDTaskType();
    tt.setType("zpp-plugin");
    tt.setName("ZPP plugin");
    tt.setDescription("Sign deliveryadvice for incomming mail");
    tt.getSEDTaskTypeProperties().add(createTTProperty(REC_SEDBOX, "Receiver sedbox."));
    tt.getSEDTaskTypeProperties().add(createTTProperty(SIGN_ALIAS, "Signature key alias."));

    return tt;
  }

  private SEDTaskTypeProperty createTTProperty(String key, String desc, boolean mandatory,
      String type, String valFormat, String valList) {
    SEDTaskTypeProperty ttp = new SEDTaskTypeProperty();
    ttp.setKey(key);
    ttp.setDescription(desc);
    ttp.setMandatory(mandatory);
    ttp.setType(type);
    ttp.setValueFormat(valFormat);
    ttp.setValueList(valList);
    return ttp;
  }

  private SEDTaskTypeProperty createTTProperty(String key, String desc) {
    return createTTProperty(key, desc, true, "string", null, null);
  }

  /**
   *
   * @param mInMail
   * @param singDAAlias
   * @throws FOPException
   * @throws HashException
   */
  public void processInZPPDelivery(MSHInMail mInMail, String singDAAlias)
      throws FOPException,
      HashException {
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

    getFOP().generateVisualization(mInMail, fDNViz, FOPUtils.FopTransformations.AdviceOfDelivery,
        MimeConstants.MIME_PDF);
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

    try (FileOutputStream fos = new FileOutputStream(fDA)) {
      MSHOutPart mp = new MSHOutPart();
      mp.setDescription("DeliveryAdvice");
      mp.setFilepath(StorageUtils.getRelativePath(fDNViz));
      mp.setMimeType(MimeValues.MIME_XML.getMimeType());
      mout.getMSHOutPayload().getMSHOutParts().add(mp);

      SEDCertStore cs = msedLookup.getSEDCertStoreByCertAlias(singDAAlias, true);

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
      mDB.updateInMail(mInMail, "DeliveryAdviceGenerated and submited to sender", null);

    } catch (IOException | SEDSecurityException | StorageException ex) {
      LOG.logError(l, ex);
    }

    LOG.logEnd(l);
  }

  /**
   *
   * @return
   */
  public FOPUtils getFOP() {
    if (mfpFop == null) {
      File fconf =
          new File(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator +
               ZPPConstants.SVEV_FOLDER + File.separator + ZPPConstants.FOP_CONFIG_FILENAME);

      mfpFop =
          new FOPUtils(fconf, System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) +
               File.separator + ZPPConstants.SVEV_FOLDER + File.separator +
               ZPPConstants.XSLT_FOLDER);
    }
    return mfpFop;
  }

}
