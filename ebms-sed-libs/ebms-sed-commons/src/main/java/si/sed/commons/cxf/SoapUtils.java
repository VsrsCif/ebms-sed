/*
 * Copyright 2015, Supreme Court Republic of Slovenia
 * 
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package si.sed.commons.cxf;

import si.sed.commons.cxf.EBMSConstants;
import java.io.File;
import java.net.URI;
import org.apache.cxf.message.Message;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.sed.ebms.ebox.SEDBox;
import si.sed.commons.pmode.EBMSMessageContext;

/**
 *
 * @author Jože Rihtaršič
 */
public class SoapUtils {

  public static boolean isRequestMessage(Message message) {
    Boolean requestor = (Boolean) message.get(Message.REQUESTOR_ROLE);
    return requestor != null && requestor;
  }

  public static String getInLogFilePath(Message message) {
    File f = (File) message.getExchange().get(EBMSConstants.EBMS_CP_IN_LOG_SOAP_MESSAGE_FILE);
    return f != null ? f.getAbsolutePath() : "";
  }

  public static MSHOutMail getMSHOutMail(Message message) {
    return (MSHOutMail) message.getExchange().get(EBMSConstants.EBMS_CP_OUTMAIL);
  }

  public static void setMSHOutnMail(MSHOutMail omail, Message message) {
    message.getExchange().put(EBMSConstants.EBMS_CP_OUTMAIL, omail);
  }

  public static void setMSHOutnMail(MSHOutMail omail, javax.xml.ws.Dispatch client) {
    client.getRequestContext().put(EBMSConstants.EBMS_CP_OUTMAIL, omail);
  }

  public static MSHInMail getMSHInMail(Message message) {
    return (MSHInMail) message.getExchange().get(EBMSConstants.EBMS_CP_INMAIL);
  }

  public static void setMSHInMail(MSHInMail imail, Message message) {
    message.getExchange().put(EBMSConstants.EBMS_CP_INMAIL, imail);
  }
  
   public static SEDBox getMSHInMailReceiverBox(Message message) {
    return (SEDBox) message.getExchange().get(EBMSConstants.EBMS_CP_INMAIL_RECEIVER);
  }

  public static void setMSHInMailReceiverBox(SEDBox imail, Message message) {
    message.getExchange().put(EBMSConstants.EBMS_CP_INMAIL_RECEIVER, imail);
  }

  public static EBMSMessageContext getEBMSMessageOutContext(Message message) {
    return (EBMSMessageContext) message.getExchange().get(EBMSConstants.EBMS_CP_OUT_CONTEXT);
  }
  
  public static EBMSMessageContext getEBMSMessageInContext(Message message) {
    return (EBMSMessageContext) message.getExchange().get(EBMSConstants.EBMS_CP_IN_CONTEXT);
  }

  public static void setEBMSMessageOutContext(EBMSMessageContext emc, Message message) {
    message.getExchange().put(EBMSConstants.EBMS_CP_OUT_CONTEXT, emc);
  }
  public static void setEBMSMessageInContext(EBMSMessageContext emc, Message message) {
    message.getExchange().put(EBMSConstants.EBMS_CP_IN_CONTEXT, emc);
  }

  public static void setEBMSMessageOutContext(EBMSMessageContext emc, javax.xml.ws.Dispatch client) {
    client.getRequestContext().put(EBMSConstants.EBMS_CP_OUT_CONTEXT, emc);
  }

  public static boolean isValidURI(final String validateUri) {
    if (validateUri == null) {
      return false;
    }
    try {
      final URI uri = new URI(validateUri.trim());
      return true;
    } catch (Exception e1) {
      return false;
    }
  }

}
