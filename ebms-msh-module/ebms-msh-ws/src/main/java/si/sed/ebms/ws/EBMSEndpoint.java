package si.sed.ebms.ws;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Date;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.xml.bind.JAXBException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.soap.SOAPBinding;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.inbox.payload.MSHInPart;
import org.sed.ebms.ebox.Export;
import org.sed.ebms.ebox.SEDBox;
import si.sed.commons.MimeValues;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.cxf.SoapUtils;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.utils.HashUtils;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.StringFormater;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author sluzba
 */
@WebServiceProvider(serviceName = "ebms")
@ServiceMode(value = Service.Mode.MESSAGE)
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
@org.apache.cxf.interceptor.InInterceptors(interceptors = {
    "si.jrc.msh.interceptor.EBMSLogInInterceptor", "si.jrc.msh.interceptor.EBMSInInterceptor",
    "si.jrc.msh.interceptor.MSHPluginInInterceptor"})
@org.apache.cxf.interceptor.OutInterceptors(interceptors = {
    "si.jrc.msh.interceptor.EBMSLogOutInterceptor", "si.jrc.msh.interceptor.EBMSOutInterceptor",
    "si.jrc.msh.interceptor.MSHPluginOutInterceptor"})
@org.apache.cxf.interceptor.OutFaultInterceptors(interceptors = {
    "si.jrc.msh.interceptor.EBMSOutFaultInterceptor"})
@org.apache.cxf.interceptor.InFaultInterceptors(interceptors = {
    "si.jrc.msh.interceptor.EBMSInFaultInterceptor"})

public class EBMSEndpoint implements Provider<SOAPMessage> {

  private static final SEDLogger LOG = new SEDLogger(EBMSEndpoint.class);

  @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
  SEDDaoInterface mDB;
  HashUtils mpHU = new HashUtils();
  StringFormater msfFormat = new StringFormater();
  StorageUtils msuStorageUtils = new StorageUtils();
  @Resource
  WebServiceContext wsContext;

  /**
     *
     */
  public EBMSEndpoint() {

  }

  private String getJNDIPrefix() {

    return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/jboss/");
  }

  @Override
  public SOAPMessage invoke(SOAPMessage request) {
    long l = LOG.logStart();
    SOAPMessage response = null;
    try {
      // create empty response
      MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
      response = mf.createMessage();

      // Using this cxf specific code you can access the CXF Message and Exchange objects
      WrappedMessageContext wmc = (WrappedMessageContext) wsContext.getMessageContext();
      Message msg = wmc.getWrappedMessage();      
      MSHInMail inmail = SoapUtils.getMSHInMail(msg);
      if (inmail == null){
        
        LOG.logWarn("No inbox message", null);
        // todo application error
        return null;
      }
      
      SEDBox sb = SoapUtils.getMSHInMailReceiverBox(msg);

      if (sb == null) {
        LOG.formatedWarning("Inbox message %s but no inbox found  for message: %s", inmail.getId(), inmail.getReceiverEBox());
        // return error
      } else if (inmail.getStatus().equals(SEDInboxMailStatus.RECEIVE.getValue())) {
        serializeMail(inmail, msg.getAttachments(), sb);
      }

    } catch (SOAPException ex) {
      LOG.logError(l, ex);
    }
    LOG.logEnd(l);
    return response;
  }

  private void serializeMail(MSHInMail mail, Collection<Attachment> lstAttch, SEDBox sb) {
    long l = LOG.logStart();
    // prepare mail to persist
    Date dt = new Date();
    // set current status
    mail.setStatus(SEDInboxMailStatus.RECEIVED.getValue());
    mail.setStatusDate(dt);
    mail.setReceivedDate(dt);

    try {
      // --------------------
      // serialize data to db

      mDB.setStatusToInMail(mail, SEDInboxMailStatus.RECEIVED, null);
    } catch (StorageException ex) {
      LOG.logError(l, "Error setting status ERROR to MSHInMail :'" + mail.getId() + "'!", ex);
    }


    // serialize to file
    Export e = sb.getExport();
    if (e != null && e.getActive()) {

      String val = msfFormat.format(e.getFileMask(), mail);
      int i = 1;
      try {
        String folder = StringFormater.replaceProperties(e.getFolder());
        File fld = new File(folder);
        if (!fld.exists()) {
          fld.mkdirs();
        }
        String filPrefix = fld.getAbsolutePath() + File.separator + val;
        if (e.getExportMetaData()) {
          String fileMetaData = filPrefix + "." + MimeValues.MIME_XML.getSuffix();
          try {

            XMLUtils.serialize(mail, fld.getAbsolutePath() + File.separator + val + "."
                + MimeValues.MIME_XML.getSuffix());
          } catch (JAXBException | FileNotFoundException ex) {
            LOG.logError(l, "Export metadata ERROR. Export file:" + fileMetaData + ".", ex);
          }
        }
        for (MSHInPart mp : mail.getMSHInPayload().getMSHInParts()) {
          msuStorageUtils
              .copyFileToFolder(
                  mp.getFilepath(),
                  new File(filPrefix + "_" + i + "."
                      + MimeValues.getSuffixBYMimeType(mp.getMimeType())));
        }
      } catch ( StorageException ex) {
        LOG.logError(l, "Export ERROR", ex);
      }
    }

    LOG.logEnd(l);
  }

}
