/*
 * Copyright 2016, Supreme Court Republic of Slovenia
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
package si.jrc.msh.utils;

import si.sed.commons.cxf.EBMSConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.cxf.binding.soap.SoapVersion;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.msh.ebms.outbox.property.MSHOutProperty;
import org.msh.sed.pmode.PMode;
import org.msh.sed.pmode.PartyIdentitySet;
import org.msh.sed.pmode.PartyIdentitySetType;

import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.AgreementRef;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Description;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.From;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageProperties;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartProperties;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyId;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PayloadInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Property;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Receipt;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.To;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Error;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import si.jrc.msh.exception.EBMSError;
import si.sed.commons.MimeValues;
import si.sed.commons.PModeConstants;
import si.sed.commons.pmode.EBMSMessageContext;

import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author Jože Rihtaršič
 */
public class EBMSBuilder {

  private static final String ID_PREFIX = "SED-";

  /**
   *
   */
  protected final SEDLogger LOG = new SEDLogger(EBMSBuilder.class);

  private MessageInfo createMessageInfo(String senderDomain, String refToMessage, Date timestamp) {
    return createMessageInfo(UUID.randomUUID().toString(), senderDomain, refToMessage, timestamp);
  }

  private MessageInfo createMessageInfo(String msgId, String senderDomain, String refToMessage,
      Date timestamp) {
    if (Utils.isEmptyString(msgId)) {
      msgId = Utils.getUUIDWithDomain(senderDomain);
    }
    MessageInfo mi = new MessageInfo();
    mi.setMessageId(msgId);
    mi.setTimestamp(timestamp);
    mi.setRefToMessageId(refToMessage);
    return mi;
  }

  /**
   *
   * @param version
   * @return
   */
  public Messaging createMessaging(SoapVersion version) {
    Messaging msg = new Messaging();
    // ID must be an NCName. This means that it must start with a letter or underscore,
    // and can only contain letters, digits, underscores, hyphens, and periods.
    msg.setId(ID_PREFIX + UUID.randomUUID().toString()); // generate unique id
    if (version.getVersion() != 1.1) {
      msg.setMustUnderstand(Boolean.TRUE);
    } else {
      msg.setS11MustUnderstand(Boolean.TRUE);
    }
    return msg;

  }

  /**
   * Method creates ebms 3.0 list of PartyIDs
   *
   * @param pis
   * @param address
   * @param name
   * @return
   */
  public List<PartyId> createPartyIdList(PartyIdentitySet pis, String address, String name) {
    List<PartyId> pilst = new ArrayList<>();
    for (PartyIdentitySetType.PartyId pisPi : pis.getPartyIds()) {
      PartyId pi = new PartyId();
      pi.setType(pisPi.getType());

      if (!Utils.isEmptyString(pisPi.getFixValue())) {
        pi.setValue(pisPi.getFixValue());

      } else if (!pisPi.getValueSource().equals(PModeConstants.PARTY_ID_SOURCE_TYPE_IGNORE)) {
        switch (pisPi.getValueSource()) {
          case PModeConstants.PARTY_ID_SOURCE_TYPE_ADDRESS:
            pi.setValue(address);
            break;
          case PModeConstants.PARTY_ID_SOURCE_TYPE_NAME:
            pi.setValue(name);
            break;
          case PModeConstants.PARTY_ID_SOURCE_TYPE_IDENTIFIER:
            String identifier = address.substring(0, address.indexOf('@'));
            pi.setValue(identifier);
            break;
          default:
            pi.setValue(address);

        }
      }
      pilst.add(pi);
    }
    return pilst;
  }

  public UserMessage createUserMessage(
      EBMSMessageContext ctx,
      MSHOutMail mo,
      Date timestamp,
      QName sv)
      throws EBMSError {

    PMode pm = ctx.getPMode();
    PartyIdentitySet pisSender = ctx.getSenderPartyIdentitySet();
    PartyIdentitySet pisReceiver = ctx.getReceiverPartyIdentitySet();

    UserMessage usgMsg = new UserMessage();

    // UserMessage usgMsg = new UserMessage();
    // --------------------------------------
    // generate MessageInfo
    MessageInfo mi =
        createMessageInfo(mo.getMessageId(), pisSender.getDomain(), mo.getRefToMessageId(),
            timestamp);
    usgMsg.setMessageInfo(mi);

    // generate from
    usgMsg.setPartyInfo(new PartyInfo());
    usgMsg.getPartyInfo().setFrom(new From());
    // sender ids
    usgMsg.getPartyInfo().getFrom().setRole(ctx.getSendingRole()); // get from p-mode
    List<PartyId> plstSender = createPartyIdList(pisSender, mo.getSenderEBox(), mo.getSenderName());
    usgMsg.getPartyInfo().getFrom().getPartyIds().addAll(plstSender);

    // generate to
    usgMsg.getPartyInfo().setTo(new To());
    usgMsg.getPartyInfo().getTo().setRole(ctx.getReceivingRole());
    List<PartyId> plstReceiver = createPartyIdList(pisReceiver, mo.getReceiverEBox(),
        mo.getReceiverName());
    usgMsg.getPartyInfo().getTo().getPartyIds().addAll(plstReceiver);

    // set colloboration info
    // BusinessInfo bi = pm.getLegs().get(0).getBusinessInfo();
    usgMsg.setCollaborationInfo(new CollaborationInfo());
    usgMsg.getCollaborationInfo().setService(
        new org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service());
    usgMsg.getCollaborationInfo().getService().setValue(ctx.getService().getServiceName());
    usgMsg.getCollaborationInfo().getService().setType(ctx.getService().getServiceType());
    usgMsg.getCollaborationInfo().setAction(mo.getAction());
    usgMsg.getCollaborationInfo().setConversationId(mo.getConversationId());

    org.msh.sed.pmode.AgreementRef ar = ctx.getExchangeAgreementRef();
    if (ar != null) {
      usgMsg.getCollaborationInfo().setAgreementRef(new AgreementRef());
      usgMsg.getCollaborationInfo().getAgreementRef().setPmode(ar.getPmode());
      usgMsg.getCollaborationInfo().getAgreementRef().setValue(ar.getValue());
      usgMsg.getCollaborationInfo().getAgreementRef().setType(ar.getType());
    }

    List<Property> lstProperties = new ArrayList<>();
    if (ctx.getService().getUseSEDProperties()) {

      if (mo.getSubject() != null) {
        Property p = new Property();
        p.setName(EBMSConstants.EBMS_PROPERTY_DESC);
        p.setValue(mo.getSubject());
        lstProperties.add(p);

      }
      // add submit date
      if (mo.getSubmittedDate() != null) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(mo.getSubmittedDate());
        Property p = new Property();
        p.setName(EBMSConstants.EBMS_PROPERTY_SUBMIT_DATE);
        p.setValue(DatatypeConverter.printDateTime(cal));
        lstProperties.add(p);
      }
    }
    // add aditional properties
    if (mo.getMSHOutProperties() != null &&
        !mo.getMSHOutProperties().getMSHOutProperties().isEmpty()) {
      for (MSHOutProperty moutProp : mo.getMSHOutProperties().getMSHOutProperties()) {
        Property p = new Property();
        p.setName(moutProp.getName());
        p.setValue(moutProp.getValue());
        lstProperties.add(p);
      }
    }
    if (!lstProperties.isEmpty()) {
      MessageProperties mp = new MessageProperties();
      mp.getProperties().addAll(lstProperties);
      usgMsg.setMessageProperties(mp);
    }

    // add payload info
    usgMsg.setPayloadInfo(new PayloadInfo());
    if (mo.getMSHOutPayload() != null && mo.getMSHOutPayload().getMSHOutParts() != null) {
      for (MSHOutPart mp : mo.getMSHOutPayload().getMSHOutParts()) {
        String attachentId = mp.getEbmsId();
        if (Utils.isEmptyString(attachentId)) {
          LOG.formatedWarning("NULL ID for attachment for out message %s", mo.getMessageId());
        }
        PartInfo pl = new PartInfo();

        pl.setHref(EBMSConstants.ATT_CID_PREFIX + attachentId); // all parts are attachments!
        if (mp.getDescription() != null && !mp.getDescription().isEmpty()) {
          pl.setDescription(new Description());
          pl.getDescription().setLang(Locale.getDefault().getLanguage());
          pl.getDescription().setValue(mp.getDescription());
        }
        List<Property> fileProp = new ArrayList<>();
        if (ctx.getTransportProtocol().getGzipCompress()) {
          Property fp = new Property();
          fp.setName(EBMSConstants.EBMS_PAYLOAD_COMPRESSION_TYPE);
          fp.setValue(MimeValues.MIME_GZIP.getMimeType());
          fileProp.add(fp);

        }

        if (!Utils.isEmptyString(mp.getEncoding())) {
          Property fp = new Property();
          fp.setName(EBMSConstants.EBMS_PAYLOAD_PROPERTY_ENCODING);
          fp.setValue(mp.getEncoding());
          fileProp.add(fp);
        }

        if (!Utils.isEmptyString(mp.getMimeType())) {
          Property fp = new Property();
          fp.setName(EBMSConstants.EBMS_PAYLOAD_PROPERTY_MIME);
          fp.setValue(mp.getMimeType());
          fileProp.add(fp);
        }
        if (ctx.getService().getUseSEDProperties()) {
          if (!Utils.isEmptyString(mp.getName())) {
            Property fp = new Property();
            fp.setName(EBMSConstants.EBMS_PAYLOAD_PROPERTY_NAME);
            fp.setValue(mp.getName());
            fileProp.add(fp);
          }

          if (!Utils.isEmptyString(mp.getFilename())) {
            Property fp = new Property();
            fp.setName(EBMSConstants.EBMS_PAYLOAD_PROPERTY_FILENAME);
            fp.setValue(mp.getFilename());
            fileProp.add(fp);
          }
          if (!Utils.isEmptyString(mp.getType())) {
            Property fp = new Property();
            fp.setName(EBMSConstants.EBMS_PAYLOAD_PROPERTY_TYPE);
            fp.setValue(mp.getType());
            fileProp.add(fp);
          }
          Property fp = new Property();
          fp.setName(EBMSConstants.EBMS_PAYLOAD_PROPERTY_IS_ENCRYPTED);
          fp.setValue(mp.getIsEncrypted() ? "true" : "false");
          fileProp.add(fp);
        }

        if (!fileProp.isEmpty()) {
          pl.setPartProperties(new PartProperties());
          pl.getPartProperties().getProperties().addAll(fileProp);
        }
        usgMsg.getPayloadInfo().getPartInfos().add(pl);
      }
    }

    return usgMsg;
  }

  /**
   *
   * @param userMessage
   * @param senderDomain
   * @param inboundMail
   * @param timestamp
   * @return
   */
  public SignalMessage generateAS4ReceiptSignal(UserMessage userMessage, String senderDomain,
      File inboundMail, Date timestamp) {
    SignalMessage sigMsg = new SignalMessage();
    try (FileInputStream fos = new FileInputStream(inboundMail);
        InputStream isXSLT = getClass().getResourceAsStream("/xslt/soap2AS4Receipt.xsl")) {

      // add message infof
      sigMsg.setMessageInfo(createMessageInfo(senderDomain, userMessage.getMessageInfo()
          .getMessageId(), timestamp));
      // generate receipt
      Receipt rcp = new Receipt();
      // generate as4 receipt from xslt
      Document doc = XMLUtils.deserializeToDom(fos, isXSLT);
      rcp.getAnies().add(doc.getDocumentElement());
      sigMsg.setReceipt(rcp);

    } catch (JAXBException | TransformerException | ParserConfigurationException | SAXException |
        IOException ex) {
      LOG.logError(0, ex);
    }
    return sigMsg;
  }

  /**
   *
   * @param refMessageId
   * @param senderDomain
   * @param inboundMail
   * @param timestamp
   * @return
   */
  public SignalMessage generateAS4ReceiptSignal(String refMessageId, String senderDomain,
      Element inboundMail, Date timestamp) {
    SignalMessage sigMsg = null;
    try (InputStream isXSLT = getClass().getResourceAsStream("/xslt/as4receipt-jmsh.xsl")) {

      // add message infof
      //sigMsg.setMessageInfo(createMessageInfo(senderDomain, refMessageId, timestamp));
      // generate receipt
      //Receipt rcp = new Receipt();
      // generate as4 receipt from xslt
      Messaging m = (Messaging) XMLUtils.deserialize(inboundMail, isXSLT, Messaging.class);
      if (m != null && m.getSignalMessages().size() == 1) {
        sigMsg = m.getSignalMessages().get(0);
        sigMsg.getMessageInfo().setMessageId(UUID.randomUUID().toString() + "@" + senderDomain);
      }

    } catch (JAXBException | TransformerException |
        IOException ex) {
      LOG.logError(0, ex);
    }
    return sigMsg;
  }

  /**
   *
   * @param ebError
   * @param senderDomain
   * @param timestamp
   * @return
   */
  public SignalMessage generateErrorSignal(EBMSError ebError, String senderDomain, Date timestamp) {
    SignalMessage sigMsg = new SignalMessage();
    // generate MessageInfo
    sigMsg.setMessageInfo(createMessageInfo(senderDomain, ebError.getRefToMessage(), timestamp));

    // get references
    org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Error er =
        new org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Error();
    er.setCategory(ebError.getEbmsErrorCode().getCategory());
    // er.setDescription(ebError.getEbmsErrorCode().getDescription());
    er.setOrigin(ebError.getEbmsErrorCode().getOrigin());
    er.setErrorCode(ebError.getEbmsErrorCode().getCode());
    er.setErrorDetail(ebError.getSubMessage());
    er.setRefToMessageInError(ebError.getRefToMessage());
    er.setSeverity(ebError.getEbmsErrorCode().getSeverity());
    er.setShortDescription(ebError.getEbmsErrorCode().getName());
    sigMsg.getErrors().add(er);
    return sigMsg;
  }

  /**
   *
   * @param um user message
   * @return
   */
  public static String getUserMessageId(UserMessage um) {
    return um != null && um.getMessageInfo() != null ? um.getMessageInfo().getMessageId() : null;
  }

  /**
   *
   * @param sm Signal message
   * @return
   */
  public static String getSignalMessageId(SignalMessage sm) {
    return sm != null && sm.getMessageInfo() != null ? sm.getMessageInfo().getMessageId() : null;
  }

  /**
   * Method returns message ID, if exists User message: usermessage id is returned else if signal
   * message than first message id is returned
   *
   * @param mi
   * @return Message id
   */
  public static String getFirstMessageId(Messaging mi) {
    if (mi == null) {
      return null;
    } else if (!mi.getUserMessages().isEmpty()) {
      return getUserMessageId(mi.getUserMessages().get(0));
    } else if (!mi.getSignalMessages().isEmpty()) {
      return getSignalMessageId(mi.getSignalMessages().get(0));
    }
    return null;
  }
  
  
  public static Error createErrorSignal(EBMSError err){
      Error er = new Error();
      er.setDescription(new Description());
      er.getDescription().setLang("en");
      er.getDescription().setValue(err.getEbmsErrorCode().getDescription());
      er.setErrorDetail(err.getSubMessage());
      er.setCategory(err.getEbmsErrorCode().getCategory());
      er.setRefToMessageInError(err.getRefToMessage());
      er.setErrorCode(err.getEbmsErrorCode().getCode());
      er.setOrigin(err.getEbmsErrorCode().getOrigin());
      er.setSeverity(err.getEbmsErrorCode().getSeverity());
      er.setShortDescription(err.getEbmsErrorCode().getName());
      
      return er;
  }

}
