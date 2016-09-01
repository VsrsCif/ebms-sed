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

import com.google.common.base.Objects;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageChecker;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.inbox.payload.MSHInPart;
import org.msh.sed.pmode.PartyIdentitySet;
import org.msh.sed.pmode.PartyIdentitySetType;
import org.msh.sed.pmode.Security;
import org.msh.sed.pmode.X509;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cert.SEDCertificate;
import org.sed.ebms.ebox.SEDBox;
import si.jrc.msh.client.sec.SecurityUtils;
import si.jrc.msh.exception.EBMSError;
import si.jrc.msh.exception.EBMSErrorCode;
import si.jrc.msh.exception.EBMSErrorMessage;
import si.jrc.msh.utils.EBMSBuilder;
import si.jrc.msh.utils.EBMSValidation;
import si.sed.commons.cxf.EBMSConstants;
import si.jrc.msh.utils.EBMSParser;
import si.sed.commons.MimeValues;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.cxf.SoapUtils;
import si.sed.commons.exception.HashException;
import si.sed.commons.exception.PModeException;
import si.sed.commons.exception.StorageException;
import si.sed.commons.pmode.EBMSMessageContext;
import si.sed.commons.utils.GZIPUtil;
import si.sed.commons.utils.HashUtils;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.sec.KeystoreUtils;

/**
 *
 * @author sluzba
 */
public class EBMSInInterceptor extends AbstractEBMSInterceptor {

  static final Set<QName> HEADERS = new HashSet<>();
  static final SEDLogger LOG = new SEDLogger(EBMSInInterceptor.class);

  static {
    HEADERS.add(new QName(EBMSConstants.EBMS_NS, EBMSConstants.EBMS_ROOT_ELEMENT_NAME));
    HEADERS.addAll(new WSS4JInInterceptor().getUnderstoodHeaders());
  }

  final StorageUtils msuStorageUtils = new StorageUtils();
  final EBMSValidation mebmsValidation = new EBMSValidation();
  final EBMSBuilder mebmsUtils = new EBMSBuilder();
  final HashUtils mpHU = new HashUtils();
  final GZIPUtil mGZIPUtils = new GZIPUtil();
  final EBMSParser mebmsParser = new EBMSParser();
  final CryptoCoverageChecker checker = new CryptoCoverageChecker();
  /**
   * Keystore tools
   */
  private final KeystoreUtils mKSUtis = new KeystoreUtils();

  /**
   *
   */
  public EBMSInInterceptor() {
    super(Phase.PRE_PROTOCOL); // user preprotocol for generating receipt
    getAfter().add(WSS4JInInterceptor.class.getName());
  }

  /**
   *
   * @return
   */
  @Override
  public Set<QName> getUnderstoodHeaders() {
    return HEADERS;
  }

  /**
   *
   * @param msg
   */
  @Override
  public void handleMessage(SoapMessage msg) {
    long l = LOG.logStart();

    SoapVersion version = msg.getVersion();
    boolean isBackChannel = SoapUtils.isRequestMessage(msg);

    // check soap version
    if (version.getVersion() != 1.2) {
      LOG.logError(l, EBMSErrorMessage.INVALID_SOAP_VERSION, null);
      throw new EBMSError(EBMSErrorCode.ValueInconsistent, null,
          EBMSErrorMessage.INVALID_SOAP_VERSION, SoapFault.FAULT_CODE_CLIENT);
    }

    // get Soap content
    SOAPMessage request = msg.getContent(SOAPMessage.class);
    if (request == null) {
      LOG.logError(l, "Message is not a SOAP message! Check log file: '" +
          SoapUtils.getInLogFilePath(msg) + "'", null);
      throw new EBMSError(EBMSErrorCode.InvalidSoapRequest, null,
          "Not a soap message", SoapFault.FAULT_CODE_CLIENT);
    }

    // validate soap request and retrieve messaging
    Messaging msgHeader = mebmsValidation.vaildateHeader_Messaging(request,
        SoapFault.FAULT_CODE_CLIENT);

    // if user message get context from user message
    EBMSMessageContext inmctx = null;
    UserMessage um = null;
    String messageId = null;
    if (!msgHeader.getUserMessages().isEmpty()) {
      // vaildateHeader_Messaging already checked if count is 1
      um = msgHeader.getUserMessages().get(0);
      mebmsValidation.vaildateUserMessage(msg, um, SoapFault.FAULT_CODE_CLIENT);
      inmctx = EBMSParser.createEBMSContextFromUserMessage(msg, um, getPModeManager());
      messageId = um.getMessageInfo().getMessageId();
    }
    // validate signals
    for (SignalMessage sm : msgHeader.getSignalMessages()) {
      mebmsValidation.vaildateSignalMessage(msg, sm, SoapFault.FAULT_CODE_CLIENT);
      
    }
    

    Security sec = null;

    // if backchannel out EBMSMessageContext must be  registred
    if (isBackChannel) {
      EBMSMessageContext outmctx = SoapUtils.getEBMSMessageOutContext(msg);
      if (outmctx == null) {
        String msgERr = "Out message context is not setted!";
        LOG.logError(l, msgERr, null);
        throw new EBMSError(EBMSErrorCode.ApplicationError, messageId,
            msgERr, SoapFault.FAULT_CODE_CLIENT);
      }

      if (inmctx != null && Objects.equal(inmctx.getPMode().getId(), outmctx.getPMode().getId())) {
        String msgERr = String.format("In pmode id: '%s' out pmode id: '%s'!",
            inmctx.getPMode().getId(), outmctx.getPMode().getId());
        LOG.logError(l, msgERr, null);
        throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, messageId,
            msgERr, SoapFault.FAULT_CODE_CLIENT);

      }

      if (inmctx == null) {
        inmctx = new EBMSMessageContext();
        inmctx.setPMode(outmctx.getPMode());
        inmctx.setService(outmctx.getService());
        inmctx.setMEPType(outmctx.getMEPType());
        inmctx.setMEPLegType(outmctx.getMEPLegType());
        inmctx.setSendingRole(outmctx.getReceivingRole());
        inmctx.setReceivingRole(outmctx.getSendingRole());
        inmctx.setReceiverPartyIdentitySet(outmctx.getSenderPartyIdentitySet());
        inmctx.setSenderPartyIdentitySet(outmctx.getReceiverPartyIdentitySet());
        inmctx.setPushTransfrer(inmctx.isPushTransfrer());
        if (outmctx.getMEPLegType().getTransport() != null) { // transport binding is BackChannel
          inmctx.setTransportChannelType(
              outmctx.getMEPLegType().getTransport().getBackChannel());
        } else {
          inmctx.setSecurity(outmctx.getSecurity());
        }
        if (inmctx.getTransportChannelType() != null) {
          if (!Utils.isEmptyString(inmctx.getTransportChannelType().getSecurityIdRef())) {
            String secId = inmctx.getTransportChannelType().getSecurityIdRef();
            try {
              inmctx.setSecurity(getPModeManager().getSecurityById(secId));
            } catch (PModeException ex) {
              String msgERr = String.format(
                  "Error occured while retrieving securitypatteren for '%s'!", secId);
              LOG.logError(l, msgERr, null);
              throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, messageId,
                  msgERr, SoapFault.FAULT_CODE_CLIENT);
            }
          } else {
            inmctx.setSecurity(outmctx.getSecurity());
          }
        }

      }
    }
    if (inmctx == null) {
      LOG.log("IS isBackChannel: " + isBackChannel);
      String msgERr = String.format(
          "Could not find PMode parameters for message %s", messageId);
      LOG.logError(l, msgERr, null);
      throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, messageId,
          msgERr, SoapFault.FAULT_CODE_CLIENT);
    }

    SoapUtils.setEBMSMessageInContext(inmctx, msg);

    if (!isBackChannel && SoapUtils.getEBMSMessageOutContext(msg) == null) {
      EBMSMessageContext outmctx = new EBMSMessageContext();
      outmctx.setPMode(inmctx.getPMode());
      outmctx.setService(inmctx.getService());
      outmctx.setMEPType(inmctx.getMEPType());
      outmctx.setMEPLegType(inmctx.getMEPLegType());
      outmctx.setSendingRole(inmctx.getReceivingRole());
      outmctx.setReceivingRole(inmctx.getSendingRole());
      outmctx.setReceiverPartyIdentitySet(inmctx.getSenderPartyIdentitySet());
      outmctx.setSenderPartyIdentitySet(inmctx.getReceiverPartyIdentitySet());
      outmctx.setPushTransfrer(inmctx.isPushTransfrer());
      outmctx.setSecurity(inmctx.getSecurity());
      if (inmctx.getMEPLegType().getTransport() != null &&
          inmctx.getMEPLegType().getTransport().getBackChannel() != null) { // transport binding is BackChannel
        outmctx.setTransportChannelType(
            inmctx.getMEPLegType().getTransport().getBackChannel());
      } else {
        outmctx.setSecurity(outmctx.getSecurity());
      }

      if (outmctx.getTransportChannelType() != null) {
        if (!Utils.isEmptyString(outmctx.getTransportChannelType().getSecurityIdRef())) {
          String secId = outmctx.getTransportChannelType().getSecurityIdRef();
          try {
            outmctx.setSecurity(getPModeManager().getSecurityById(secId));
          } catch (PModeException ex) {
            String msgERr = String.format(
                "Error occured while retrieving securitypatteren for '%s'!", secId);
            LOG.logError(l, msgERr, null);
            throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, messageId,
                msgERr, SoapFault.FAULT_CODE_CLIENT);
          }
        } else {
          outmctx.setSecurity(inmctx.getSecurity());
        }
      }
      SoapUtils.setEBMSMessageOutContext(outmctx, msg);
    }

    if (inmctx.getSecurity() != null) {
      handleMessageSecurity(msg, inmctx, messageId);
    } else {
      LOG.formatedWarning("No Security policy for message: '%s', pmode '%s'!'", messageId,
          inmctx.getPMode().getId());
    }

    if (um != null) {
      processUserMessageUnit(msg, um, inmctx);
    }

    // validate signals
    for (SignalMessage sm : msgHeader.getSignalMessages()) {
      // process signal
    }

    LOG.logEnd(l);
  }

  public void processUserMessageUnit(SoapMessage msg, UserMessage um, EBMSMessageContext ectx) {
    long l = LOG.logStart();

    MSHInMail mMail = mebmsParser.parseUserMessage(um, ectx, SoapFault.FAULT_CODE_CLIENT);
    SoapUtils.setMSHInMail(mMail, msg);
    String receiverBox = mMail.getReceiverEBox();
    if (receiverBox == null || receiverBox.trim().isEmpty()) {
      String errmsg = "Missing receiver box!";
      LOG.logError(l, errmsg, null);
      throw new EBMSError(EBMSErrorCode.Other, mMail.getMessageId(), errmsg,
          SoapFault.FAULT_CODE_CLIENT);
    }

    SEDBox inSb = getSedBoxByName(mMail.getReceiverEBox());
    mMail.setReceiverEBox(inSb.getBoxName());
    if (inSb.getActiveToDate() != null && inSb.getActiveToDate().before(
        Calendar.getInstance().getTime())) {
      String errmsg =
          "Receiver box: '" + mMail.getReceiverEBox() + "' not exists or is not active.";
      LOG.logError(l, errmsg, null);
      throw new EBMSError(EBMSErrorCode.Other, mMail.getMessageId(), errmsg,
          SoapFault.FAULT_CODE_CLIENT);
    }
    // set inbox to message context
    SoapUtils.setMSHInMailReceiverBox(inSb, msg);

    // serialize attachments
    if (mMail.getMSHInPayload() != null && !mMail.getMSHInPayload().getMSHInParts().isEmpty()) {
      for (MSHInPart p : mMail.getMSHInPayload().getMSHInParts()) {
        boolean isCmpr = false;
        for (MSHInPart.Property prp: p.getProperties()){
          if (prp.getName()!= null && prp.getValue()!= null 
              && prp.getName().equalsIgnoreCase(EBMSConstants.EBMS_PAYLOAD_COMPRESSION_TYPE) 
              && prp.getValue().equalsIgnoreCase(MimeValues.MIME_GZIP.getMimeType()) ) {
            // found property EBMS_PAYLOAD_COMPRESSION_TYPE 
            isCmpr = true;
            // remove property because is no longer needed
            p.getProperties().remove(prp);
            break;
          }
        }
        
        
        try {
          serializeAttachments(p, msg.getAttachments(),
              isCmpr);
        } catch (StorageException | IOException | HashException ex) {
          String errmsg = "Error reading attachments .";
          LOG.logError(l, errmsg, ex);
          throw new EBMSError(EBMSErrorCode.ExternalPayloadError, mMail.getMessageId(), errmsg,
              SoapFault.FAULT_CODE_CLIENT);
        }
      }

    }

    // serializa data DB
    // prepare mail to persist
    Date dt = Calendar.getInstance().getTime();
    // set current status
    mMail.setStatus(SEDInboxMailStatus.RECEIVE.getValue());
    mMail.setStatusDate(dt);
    mMail.setReceivedDate(dt);
    try {
      getDAO().serializeInMail(mMail, "ebms-msh-ws");
    } catch (StorageException ex) {
      String errmsg = "Internal error occured while serializing incomming mail.";
      LOG.logError(l, errmsg, ex);
      throw new EBMSError(EBMSErrorCode.ExternalPayloadError, mMail.getMessageId(), errmsg,
          SoapFault.FAULT_CODE_CLIENT);
    }

    msg.getExchange().put(MSHInMail.class, mMail);

    SOAPMessage request = msg.getContent(SOAPMessage.class);
    LOG.log("Generate AS4Receipt");
    SignalMessage as4Receipt =
        mebmsUtils.generateAS4ReceiptSignal(mMail.getMessageId(),
            ectx.getReceiverPartyIdentitySet().getDomain(), request.getSOAPPart()
            .getDocumentElement(), dt);
    msg.getExchange().put(SignalMessage.class, as4Receipt);

  }

  private SEDBox getSedBoxByName(String sbox) {
    return getLookups().getSEDBoxByName(sbox, true);

  }
  

  /*
  // receive
  private void processResponseSignals(List<SignalMessage> lstSignals, PMode pm, SoapMessage msg)
      throws EBMSError {
    SoapVersion version = msg.getVersion();
    long l = LOG.logStart();

    for (SignalMessage sm : lstSignals) {
      MessageInfo mi = sm.getMessageInfo();
      if (mi == null) {
        String errmsg = "Missing MessageInfo in SignalMessage";
        LOG.logError(l, errmsg, null);
        throw new SoapFault(errmsg, version.getReceiver());
      }

      if (sm.getPullRequest() != null) {
        String errmsg = "Pull MEP is not supported! Pull signal is ignored";
        LOG.logError(l, errmsg, null);

      }

      if (mi.getRefToMessageId() == null || mi.getRefToMessageId().trim().isEmpty()) {
        String errmsg = "Missing missing RefToMessageId";
        LOG.logError(l, errmsg, null);
        throw new SoapFault(errmsg, version.getReceiver());
      }

      MSHOutMail outmsg = msg.getExchange().get(MSHOutMail.class);
      String strOutMsg = outmsg.getMessageId() + "@" + getSettings().getDomain();
      if (strOutMsg != null && !strOutMsg.equals(mi.getRefToMessageId())) {
        String errmsg =
            "Outgoing msg ID '" + strOutMsg +
            "' not equals to received response signal RefToMessageId: '" +
            mi.getRefToMessageId() + "' ";
        LOG.logError(l, errmsg, null);
        // throw new SoapFault(errmsg, version.getReceiver());
      }

      if (sm.getErrors() != null && !sm.getErrors().isEmpty()) {
        String desc = "";
        for (org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Error er : sm.getErrors()) {
          desc =
              er.getOrigin() + "" + er.getSeverity() + " " + er.getErrorCode() + " " +
              er.getErrorDetail();
          break;
        }

        try {
          getDAO().setStatusToOutMail(outmsg, SEDOutboxMailStatus.EBMSERROR, desc);
        } catch (StorageException ex) {
          String msgErr =
              "Error occured when setting MSHOutMail (id" + outmsg.getId() +
              ") status to EBMSERROR";
          LOG.logError(l, msgErr, ex);
        }

      } else if (sm.getReceipt() != null) {
        outmsg.setReceivedDate(mi.getTimestamp());
        try {
          getDAO().setStatusToOutMail(outmsg, SEDOutboxMailStatus.SENT,
              "Mail received to receiver MSH");
        } catch (StorageException ex) {
          String msgErr =
              "Error occured when setting MSHOutMail (id" + outmsg.getId() + ") status to SENT";
          LOG.logError(l, msgErr, ex);
        }

      }

      msg.getExchange().put("SIGNAL_ELEMENTS", sm.getAnies());

      for (Element e : sm.getAnies()) {
        super.A_LOG.log("Got elements in signal: " + e.getLocalName());
        /*
         * if (e.getLocalName().equals("SVEVEncryptionKey")) {
         * System.out.println("********************** got encryptionKey"); try { SVEVEncryptionKey
         * se = (SVEVEncryptionKey) XMLUtils.deserialize(e, SVEVEncryptionKey.class);
         * MshIncomingMail mm = mSHDB.getIncomingMailByActionAndByConversationId(SVEVConstants.
         * SVEV_ACTION_DeliveryNotification, se.getId()); if (mm == null) { String errmsg =
         * "Incoming mail with message ID: " + se.getId() + " not exists!"; LOG.error(errmsg); throw
         * new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg); }
         * System.out.println("SET enc key"); mm.getMmshMail().setSVEVEncryptionKey(se);
         * System.out.println("update incoming mail!!!!"); mSHDB.update(mm);
         * 
         * } catch (JAXBException ex) { String errmsg = "Error parsing  '" + e.getNamespaceURI() +
         * "', tagname: '" + e.getLocalName() + "'! Error: " + ex.getMessage(); LOG.error(errmsg,
         * ex); throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg, ex); } }
         * else { String errmsg = "Error parsing  '" + e.getNamespaceURI() + "', tagname: '" +
         * e.getLocalName() + "'!"; LOG.logError(l, errmsg); throw new
         * EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg); }
         * /
      }

    }
  }*/
  private void handleMessageSecurity(SoapMessage msg, EBMSMessageContext ectx, String messageId) {
    PartyIdentitySet rPID = ectx.getReceiverPartyIdentitySet();
    PartyIdentitySet sPID = ectx.getSenderPartyIdentitySet();
    long l = LOG.logStart();
    try {
      WSS4JInInterceptor sc =
          configureSecurityInterceptors(ectx.getSecurity(), rPID.getLocalPartySecurity(),
              sPID.getExchangePartySecurity(), messageId,
              SoapFault.FAULT_CODE_CLIENT);
      sc.handleMessage(msg);

    } catch (Throwable tg) {
      LOG.logError(l, "Error validatig security: '" +
          SoapUtils.getInLogFilePath(msg) + "'", tg);
      throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch,
          messageId,
          "Error occured validatig security: " + tg.getMessage(), tg, SoapFault.FAULT_CODE_CLIENT);
    }

    try {
      CryptoCoverageChecker cc = SecurityUtils.configureCryptoCoverageCheckerInterceptors(
          ectx.getSecurity());
      cc.handleMessage(msg);

    } catch (Throwable tg) {
      LOG.logError(l, "Error validatig security: '" +
          SoapUtils.getInLogFilePath(msg) + "'", tg);
      throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch,
          messageId,
          "Security coverage mishatch! Error: " + tg.getMessage(), tg, SoapFault.FAULT_CODE_CLIENT);
    }
    LOG.logEnd(l);
  }

  private WSS4JInInterceptor configureSecurityInterceptors(Security sc,
      PartyIdentitySetType.LocalPartySecurity lps, PartyIdentitySetType.ExchangePartySecurity eps,
      String msgId, QName sv)
      throws EBMSError {

    long l = LOG.logStart();
    WSS4JInInterceptor sec = null;
    Map<String, Object> outProps = null;

    if (sc.getX509() == null) {
      LOG.logWarn(l,
          "Sending not message with not security policy. No security configuration (pmode) for message:" +
          msgId, null);
      return null;
    }
    if (sc.getX509().getSignature() != null && sc.getX509().getSignature().getReference() != null) {
      X509.Signature sig = sc.getX509().getSignature();

      // create signature priperties
      SEDCertStore cs = getLookups().getSEDCertStoreByName(eps.getTrustoreName());
      if (cs == null) {
        String msg = "Truststore for name '" + eps.getTrustoreName() +
            "' do not exists - check configuration!";
        LOG.logError(l, msg, null);
        throw new EBMSError(EBMSErrorCode.PModeConfigurationError, msgId, msg, sv);
      }
      SEDCertificate aliasCrt = getLookups().getSEDCertificatForAlias(eps.getSignatureCertAlias(),
          cs, false);
      if (aliasCrt == null) {
        String msg = "Certificate for alias '" + eps.getSignatureCertAlias() +
            "' do not exists in keystore:" + eps.getTrustoreName();
        LOG.logError(l, msg, null);
        throw new EBMSError(EBMSErrorCode.PModeConfigurationError, msgId, msg, sv);
      }
      outProps = SecurityUtils.createSignatureValidationConfiguration(sig, cs, aliasCrt);
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
      SEDCertStore cs = getLookups().getSEDCertStoreByName(lps.getKeystoreName());
      if (cs == null) {
        String msg = "Keystore for name '" + lps.getKeystoreName() +
            "' do not exists - check configuration!";
        LOG.logError(l, msg, null);
        throw new EBMSError(EBMSErrorCode.PModeConfigurationError, msgId, msg, sv);
      }
      SEDCertificate aliasCrt = getLookups().getSEDCertificatForAlias(lps.getDecryptionKeyAlias(),
          cs, true);
      if (aliasCrt == null) {
        String msg = "Decryptiong key for alias '" + lps.getDecryptionKeyAlias() +
            "' do not exist in keystore '" +
            lps.getKeystoreName() + "' !";
        LOG.logError(l, msg, null);
        throw new EBMSError(EBMSErrorCode.PModeConfigurationError, msgId, msg, sv);
      }

      Map<String, Object> penc =
          SecurityUtils.createDecryptionConfiguration(enc, cs, aliasCrt);
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

      sec = new WSS4JInInterceptor(outProps);
    } else {
      LOG.logWarn(l,
          "Sending not message with not security policy. Bad/incomplete security configuration (pmode) for message:" +
          msgId, null);
    }
    LOG.logEnd(l);
    return sec;
  }

  /**
   *
   * @param message
   */
  @Override
  public void handleFault(SoapMessage message) {
    super.handleFault(message);
    LOG.log("handle fault interceptor");

    LOG.log("SoapMessage: ********************************************************");
    message.entrySet().stream().forEach((entry) -> {
      LOG.formatedlog("Key: %s, val: %s", entry.getKey(), entry.getValue());
    });
    LOG.log("Exchange: ********************************************************");
    Exchange map = message.getExchange();
    map.entrySet().stream().forEach((entry) -> {
      LOG.formatedlog("Key: %s, val: %s", entry.getKey(), entry.getValue());
    });
  }

  private void serializeAttachments(MSHInPart p, Collection<Attachment> lstAttch, boolean compressed)
      throws StorageException, IOException, HashException {
    DataHandler dh = null;
    for (Attachment a : lstAttch) {
      if (a.getId().equals(p.getEbmsId())) {
        dh = a.getDataHandler();
        break;
      }
    }

    File fout = null;
    if (dh != null) {
      if (compressed) {
        fout = msuStorageUtils.getCreateEmptyInFile(p.getMimeType());
        mGZIPUtils.decompressGZIP(dh.getInputStream(), fout);
      } else {
        // if not compressed
        fout = msuStorageUtils.storeInFile(p.getMimeType(), dh.getInputStream());
      }
    }
    // set MD5 and relative path;
    if (fout != null) {
      String strMD5 = mpHU.getMD5Hash(fout);
      String relPath = StorageUtils.getRelativePath(fout);
      p.setFilepath(relPath);
      p.setMd5(strMD5);

      if (Utils.isEmptyString(p.getFilename())) {
        p.setFilename(fout.getName());
      }
      if (Utils.isEmptyString(p.getName())) {
        p.setName(p.getFilename().substring(p.getFilename().lastIndexOf(".")));
      }
    }

  }

}
