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
package si.jrc.msh.interceptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.msh.sed.pmode.PMode;
import org.msh.sed.pmode.PartyIdentitySet;
import org.msh.sed.pmode.PartyIdentitySetType;
import org.msh.sed.pmode.References;
import org.msh.sed.pmode.Security;
import org.msh.sed.pmode.X509;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cert.SEDCertificate;
import si.jrc.msh.client.sec.MSHKeyPasswordCallback;
import si.jrc.msh.client.sec.SecurityUtils;
import si.jrc.msh.exception.EBMSError;
import si.jrc.msh.exception.EBMSErrorCode;
import si.jrc.msh.utils.EBMSBuilder;
import si.jrc.msh.utils.SoapUtils;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.GZIPUtil;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.sec.KeystoreUtils;
import si.sed.commons.pmode.EBMSMessageContext;

/**
 * Sets up the outgoing chain to build a ebms 3.0 (AS4) form message. First it will create Messaging
 * object according pmode configuratin added as "PMode.class" param in message context. For user
 * message attachments are added (and compressed according to pmode settings ) In the end encryption
 * and security interceptors are configured.
 *
 * @author Jože Rihtaršič
 */
public class EBMSOutInterceptor extends AbstractEBMSInterceptor {

  /**
   * Logger for EBMSOutInterceptor class
   */
  protected final static SEDLogger LOG = new SEDLogger(EBMSOutInterceptor.class);

  /**
   * ebms message tools for converting between ebms and ebms-sed message entity
   */
  protected final EBMSBuilder mEBMSUtil = new EBMSBuilder();

  /**
   * GZIP utils
   */
  protected final GZIPUtil mGZIPUtils = new GZIPUtil();

  /**
   * Keystore tools
   */
  private final KeystoreUtils mKSUtis = new KeystoreUtils();

  /**
   * Contstructor EBMSOutInterceptor for setting instance in a phase Phase.PRE_PROTOCOL
   */
  public EBMSOutInterceptor() {
    super(Phase.PRE_PROTOCOL);
  }

  /**
   * Method transforms message to ebMS 3.0 (AS4) message form and sets signature and encryption
   * interceptors.
   *
   * @param msg: SoapMessage handled in CXF bus
   */
  @Override
  public void handleMessage(SoapMessage msg) {
    long l = LOG.logStart(msg);
    SoapVersion version = msg.getVersion();
    // is out mail request or response
    boolean isRequest = MessageUtils.isRequestor(msg);
    QName qnFault = (isRequest ? SoapFault.FAULT_CODE_CLIENT : SoapFault.FAULT_CODE_SERVER);

    if (msg.getContent(SOAPMessage.class) == null) {
      String errmsg = "Internal error missing SOAPMessage!";
      LOG.logError(l, errmsg, null);
      throw new EBMSError(EBMSErrorCode.InvalidSoapRequest, null, errmsg, qnFault);
    }

    // get context variables
    EBMSMessageContext ectx = SoapUtils.getEBMSMessageOutContext(msg);
    MSHOutMail outMail = SoapUtils.getMSHOutMail(msg);

    String msgId = outMail != null ? outMail.getMessageId() : null;

    LOG.log("Prepare to submit message: " + msgId);
    // get pmode data
    PartyIdentitySet sPID  = ectx.getSenderPartyIdentitySet();
    PartyIdentitySet rPID = ectx.getReceiverPartyIdentitySet();
    PMode pMode = ectx.getPMode();
    

    // create message 
    Messaging msgHeader = mEBMSUtil.createMessaging(version);
    // create usermessageunit for out mail 
    if (outMail != null) {
      // add user message
      outMail.setSentDate(Calendar.getInstance().getTime()); // reset sent  to new value 
      UserMessage um;
      try {

        // add attachments
        try {
          // set attachment for wss signature!
          LOG.log("Set attachmetns for message: " + msgId);
          setAttachments(msg, outMail, sPID.getDomain(),
              ectx.getTransportProtocol().getGzipCompress());
        } catch (StorageException ex) {
          String msgError = "Error adding attachments to soap" + ex.getMessage();
          LOG.logError(l, msgError, ex);
          throw new EBMSError(EBMSErrorCode.PModeConfigurationError, msgId,
              msgError, ex,
              SoapFault.FAULT_CODE_CLIENT);
        }
        // create user message
        LOG.log("Create userMessage unit for  message: " + msgId);
        um = mEBMSUtil.createUserMessage(ectx, outMail, outMail.getSentDate(), qnFault);
        msgHeader.getUserMessages().add(um);
      } catch (EBMSError ex) {

        LOG.logError(l, ex.getSubMessage(), ex);
        throw new EBMSError(EBMSErrorCode.PModeConfigurationError, msgId, ex.getMessage(), ex,
            SoapFault.FAULT_CODE_CLIENT);
      }

    }

    SignalMessage signal = msg.getExchange().get(SignalMessage.class);
    if (signal != null) {
      msgHeader.getSignalMessages().add(signal);
    }

    // add error signal
    EBMSError err = msg.getExchange().get(EBMSError.class);

    if (err != null) {
      SignalMessage sm =
          mEBMSUtil.generateErrorSignal(err, getSettings().getDomain(), Calendar.getInstance()
              .getTime());

      msgHeader.getSignalMessages().add(sm);
    }
    // add svev signal
    // add error
    /*
     * if (msgSvevKey != null) { SignalMessage sm = mEBMSUtil.generateSVEVKeySignal(msgSvevKey,
     * mSettings.getDomain());
     * sm.getMessageInfo().setRefToMessageId(mgsInboundMessage.getUserMessages
     * ().get(0).getMessageInfo().getMessageId()); msgHeader.getSignalMessages().add(sm); }
     */
    try {
      SOAPMessage request = msg.getContent(SOAPMessage.class);
      SOAPHeader sh = request.getSOAPHeader();
      Marshaller marshaller = JAXBContext.newInstance(Messaging.class).createMarshaller();
      marshaller.marshal(msgHeader, sh);
      request.saveChanges();
    } catch (JAXBException | SOAPException ex) {
      String errMsg = "Error adding ebms header to soap: " + ex.getMessage();
      LOG.logError(l, errMsg, ex);
      throw new EBMSError(EBMSErrorCode.ApplicationError, msgId, errMsg, ex,
          SoapFault.FAULT_CODE_CLIENT);
    }

    // if out mail add security / f
    if (ectx.getSecurity() != null) {

      WSS4JOutInterceptor sc =
          configureSecurityInterceptors(ectx.getSecurity(), sPID.getLocalPartySecurity(),
              rPID.getExchangePartySecurity(), msgId,
              SoapFault.FAULT_CODE_CLIENT);

      sc.handleMessage(msg);

    } else {
      LOG.logWarn("No Security policy for message: '" + msgId +
          "' pmode: " + pMode.getId() + "!", null);
    }
    LOG.logEnd(l);
  }

  

  public WSS4JOutInterceptor configureSecurityInterceptors(Security sc,
      PartyIdentitySetType.LocalPartySecurity lps, PartyIdentitySetType.ExchangePartySecurity epx,
      String msgId, QName sv)
      throws EBMSError {
    long l = LOG.logStart();
    WSS4JOutInterceptor sec = null;
    Map<String, Object> outProps = null;

    if (sc.getX509() == null) {
      LOG.logWarn(l,
          "Sending not message with not security policy. No security configuration (pmode) for message:" +
          msgId, null);
      return null;
    }

    if (sc.getX509().getSignature() != null && sc.getX509().getSignature().getReference() != null) {
      X509.Signature sig = sc.getX509().getSignature();
      
      
      SEDCertStore cs = getLookups().getSEDCertStoreByName(lps.getKeystoreName());
    if (cs == null) {
      String msg = "Keystore for name '" + lps.getKeystoreName() + "' do not exists - check configuration!";
      LOG.logError(l, msg, null);
      throw new EBMSError(EBMSErrorCode.PModeConfigurationError, msgId, msg, sv);
    }

    SEDCertificate aliasCrt = getLookups().getSEDCertificatForAlias( lps.getSignatureKeyAlias(), cs, true);
    if (aliasCrt == null) {
      String msg = "Key for alias '" +  lps.getSignatureKeyAlias() + "' do not exists!";
      LOG.logError(l, msg, null);
      throw new EBMSError(EBMSErrorCode.PModeConfigurationError, msgId, msg, sv);
    }
    
      outProps =
          SecurityUtils.createSignatureConfiguration(sig, cs, aliasCrt);
      if (outProps == null) {
        LOG.logWarn(l,
            "Sending not signed message. Incomplete configuration: X509/Signature for message:  " +
            msgId, null);
      }
    } else {
      LOG.logWarn(l,
          "Sending not signed message. No configuration: X509/Signature/Sign for message:  " + msgId,
          null);
    }

    if (sc.getX509().getEncryption() != null && sc.getX509().getEncryption().getReference() != null) {
      X509.Encryption enc = sc.getX509().getEncryption();
      
      SEDCertStore cs = getLookups().getSEDCertStoreByName(epx.getTrustoreName());
    if (cs == null) {
      String msg = "Trustore for name '" + epx.getTrustoreName() + "' do not exists - check configuration!";
      LOG.logError(l, msg, null);
      throw new EBMSError(EBMSErrorCode.PModeConfigurationError, msgId, msg, sv);
    }
    SEDCertificate aliasCrt = getLookups().getSEDCertificatForAlias(epx.getEncryptionCertAlias(), cs, false);
    if (aliasCrt == null) {
      String msg = "Ecryptiong cert for alias '" + epx.getEncryptionCertAlias() + "' do not exists!";
      LOG.logError(l, msg, null);
      throw new EBMSError(EBMSErrorCode.PModeConfigurationError, msgId, msg, sv);
    }
      
      Map<String, Object> penc =SecurityUtils.createEncryptionConfiguration(enc, cs,aliasCrt);
      if (enc == null) {
        LOG.logWarn(l,
            "Sending not encrypted message. Incomplete configuration: X509/Encryption/Encryp for message:  " +
            msgId, null);
      } else if (outProps == null) {
        outProps = penc;
      } else {
        String action = (String) outProps.get(WSHandlerConstants.ACTION);
        action += " " + (String) penc.get(WSHandlerConstants.ACTION);
        outProps.putAll(penc);
        outProps.put(WSHandlerConstants.ACTION, action);
      }
    } else {
      LOG.logWarn(l,
          "Sending not encypted message. No configuration: X509/Encryption/Encrypt for message:  " +
          msgId, null);
    }

    if (outProps != null) {
      LOG.log("Set security parameters");
      for (Iterator<String> it = outProps.keySet().iterator(); it.hasNext();) {
        String key = it.next();
        LOG.log(key + ": " + outProps.get(key));
      }
      sec = new WSS4JOutInterceptor(outProps);
    } else {
      LOG.logWarn(l,
          "Sending not message with not security policy. Bad/incomplete security configuration (pmode) for message:" +
          msgId, null);
    }
    LOG.logEnd(l);
    return sec;
  }

  /**
   * Method sets attachments to outgoing ebmsUserMessage.
   *
   * @param msg - SAOP message
   * @param mail - MSH out mail
   * @throws StorageException
   */
  private void setAttachments(SoapMessage msg, MSHOutMail mail, String domain, boolean compress)
      throws StorageException {
    long l = LOG.logStart();
    if (mail != null && mail.getMSHOutPayload() != null &&
        !mail.getMSHOutPayload().getMSHOutParts().isEmpty()) {

      msg.setAttachments(new ArrayList<>(mail.getMSHOutPayload().getMSHOutParts().size()));
      for (MSHOutPart p : mail.getMSHOutPayload().getMSHOutParts()) {
        String id = UUID.randomUUID().toString() + "@" + domain;
        p.setEbmsId(id);

        AttachmentImpl att = new AttachmentImpl(p.getEbmsId());
        att.setHeader("id", id);
        File fatt = StorageUtils.getFile(p.getFilepath());
        if (compress) {
          File fattCmp = StorageUtils.getNewStorageFile("gzip", fatt.getName());
          try {
            mGZIPUtils.compressGZIP(fatt, fattCmp);
            fatt = fattCmp;
          } catch (IOException ex) {
            String msgErr =
                "Error compressing attachment: " + fatt.getAbsolutePath() + " for mail: " +
                p.getId();
            LOG.logError(l, msgErr, ex);
            throw new StorageException(msgErr, ex);
          }
        }

        DataHandler dh = new DataHandler(new FileDataSource(fatt));
        att.setDataHandler(dh);
        msg.getAttachments().add(att);
      }
    }

  }

  /**
   *
   * @param message
   */
  @Override
  public void handleFault(SoapMessage message) {
    super.handleFault(message); // To change body of generated methods, choose Tools | Templates.
    LOG.log("handleFault 1");
  }

}
