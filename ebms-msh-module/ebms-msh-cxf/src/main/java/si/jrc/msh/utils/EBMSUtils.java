/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.rmi.CORBA.Util;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.message.Attachment;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.inbox.payload.MSHInPart;
import org.msh.ebms.inbox.payload.MSHInPayload;
import org.msh.ebms.inbox.property.MSHInProperties;
import org.msh.ebms.inbox.property.MSHInProperty;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.msh.ebms.outbox.property.MSHOutProperties;
import org.msh.ebms.outbox.property.MSHOutProperty;
import org.msh.svev.pmode.PMode;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import si.jrc.msh.client.MshClient;

import si.jrc.msh.exception.EBMSError;
import si.jrc.msh.exception.EBMSErrorCode;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author sluzba
 */
public class EBMSUtils {

    protected final SEDLogger mlog = new SEDLogger(MshClient.class);
    private static final String ID_PREFIX_ = "SED-";

    public SignalMessage generateErrorSignal(EBMSError ebError, String senderDomain) {
        SignalMessage sigMsg = new SignalMessage();
        // generate  MessageInfo
        sigMsg.setMessageInfo(createMessageInfo(senderDomain, ebError.getRefToMessage()));

        // get references
        org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Error er = new org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Error();
        er.setCategory(ebError.getEbmsErrorCode().getCategory());
        //er.setDescription(ebError.getEbmsErrorCode().getDescription());
        er.setOrigin(ebError.getEbmsErrorCode().getOrigin());
        er.setErrorCode(ebError.getEbmsErrorCode().getCode());
        er.setErrorDetail(ebError.getSubMessage());
        er.setRefToMessageInError(ebError.getRefToMessage());
        er.setSeverity(ebError.getEbmsErrorCode().getSeverity());
        er.setShortDescription(ebError.getEbmsErrorCode().getName());
        sigMsg.getErrors().add(er);
        return sigMsg;
    }

    public SignalMessage generateSVEVKeySignal(/*SVEVEncryptionKey skey,*/String senderDomain) {
        SignalMessage sigMsg = new SignalMessage();
        // generate  MessageInfo
        sigMsg.setMessageInfo(createMessageInfo(senderDomain, null));
        Document doc;
        /*try {
         doc = XMLUtils.jaxbToDocument(skey);
         sigMsg.getAnies().add(doc.getDocumentElement());
         } catch (JAXBException | ParserConfigurationException ex) {
         Logger.getLogger(EBMSUtils.class.getName()).log(Level.SEVERE, null, ex);
         }*/

        return sigMsg;
    }

    public SignalMessage generateAS4ReceiptSignal(UserMessage userMessage, String senderDomain, File inboundMail) {
        SignalMessage sigMsg = new SignalMessage();
        try (FileInputStream fos = new FileInputStream(inboundMail);
                InputStream isXSLT = getClass().getResourceAsStream("/xslt/soap2AS4Receipt.xsl")) {

            // add message infof
            sigMsg.setMessageInfo(createMessageInfo(senderDomain, userMessage.getMessageInfo().getMessageId()));
            // generate receipt
            Receipt rcp = new Receipt();
            // generate as4 receipt from xslt
            Document doc = XMLUtils.deserializeToDom(fos, isXSLT);
            rcp.getAnies().add(doc.getDocumentElement());
            sigMsg.setReceipt(rcp);

        } catch (JAXBException | TransformerException | ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(EBMSUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sigMsg;
    }

    public SignalMessage generateAS4ReceiptSignal(String refMessageId, String senderDomain, Element inboundMail) {
        SignalMessage sigMsg = new SignalMessage();
        try (InputStream isXSLT = getClass().getResourceAsStream("/xslt/soap2AS4Receipt.xsl")) {

            // add message infof
            sigMsg.setMessageInfo(createMessageInfo(senderDomain, refMessageId));
            // generate receipt
            Receipt rcp = new Receipt();
            // generate as4 receipt from xslt
            Document doc = XMLUtils.transform(inboundMail, isXSLT);
            rcp.getAnies().add(doc.getDocumentElement());
            sigMsg.setReceipt(rcp);

        } catch (JAXBException | TransformerException | ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(EBMSUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sigMsg;
    }

    public Messaging createMessaging(SoapVersion version) {
        Messaging msg = new Messaging();
        //ID must be an NCName. This means that it must start with a letter or underscore, 
        //and can only contain letters, digits, underscores, hyphens, and periods.
        msg.setId(ID_PREFIX_ + UUID.randomUUID().toString()); // generate unique id
        if (version.getVersion() != 1.1) {
            msg.setMustUnderstand(Boolean.TRUE);
        } else {
            msg.setS11MustUnderstand(Boolean.TRUE);;
        }
        return msg;

    }

    private MessageInfo createMessageInfo(String senderDomain, String refToMessage) {
        return createMessageInfo(UUID.randomUUID().toString(), senderDomain, refToMessage);
    }

    private MessageInfo createMessageInfo(String msgId, String senderDomain, String refToMessage) {
        if (msgId == null) {
            msgId = UUID.randomUUID().toString();
        }
        MessageInfo mi = new MessageInfo();
        mi.setMessageId(msgId + "@" + senderDomain);
        mi.setTimestamp(new Date());
        mi.setRefToMessageId(refToMessage);
        return mi;
    }

    public UserMessage createUserMessage(PMode pm, MSHOutMail mo, String senderDomain) {
        UserMessage usgMsg = new UserMessage();

        // UserMessage usgMsg = new UserMessage();
        // --------------------------------------
        // generate  MessageInfo 
        MessageInfo mi = createMessageInfo(mo.getMessageId(), senderDomain, mo.getRefToMessageId());
        usgMsg.setMessageInfo(mi);

        // generate from 
        usgMsg.setPartyInfo(new PartyInfo());
        usgMsg.getPartyInfo().setFrom(new From());
        // PUSH - MEP
        usgMsg.getPartyInfo().getFrom().setRole(pm.getInitiator().getRole()); // get from p-mode

        // add sender id
        PartyId piFrom = new PartyId();
        piFrom.setType(EbMSConstants.EBMS_PARTY_TYPE_NAME);
        piFrom.setValue(mo.getSenderName());
        usgMsg.getPartyInfo().getFrom().getPartyIds().add(piFrom);
        piFrom = new PartyId();
        piFrom.setType(EbMSConstants.EBMS_PARTY_TYPE_EBOX);
        piFrom.setValue(mo.getSenderEBox());
        usgMsg.getPartyInfo().getFrom().getPartyIds().add(piFrom);
        // generate to
        usgMsg.getPartyInfo().setTo(new To());
        usgMsg.getPartyInfo().getTo().setRole(pm.getResponder().getRole()); // get from p-mode
        PartyId piTo = new PartyId();
        piTo.setType(EbMSConstants.EBMS_PARTY_TYPE_NAME);
        piTo.setValue(mo.getReceiverName());
        usgMsg.getPartyInfo().getTo().getPartyIds().add(piTo);
        piTo = new PartyId();
        piTo.setType(EbMSConstants.EBMS_PARTY_TYPE_EBOX);
        piTo.setValue(mo.getReceiverEBox());
        usgMsg.getPartyInfo().getTo().getPartyIds().add(piTo);

        // set colloboration info
        //BusinessInfo bi = pm.getLegs().get(0).getBusinessInfo();
        usgMsg.setCollaborationInfo(new CollaborationInfo());
        usgMsg.getCollaborationInfo().setService(new org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service());
        usgMsg.getCollaborationInfo().getService().setValue(mo.getService());
        usgMsg.getCollaborationInfo().setAction(mo.getAction());
        usgMsg.getCollaborationInfo().setConversationId(mo.getConversationId());
        usgMsg.getCollaborationInfo().setAgreementRef(new AgreementRef());
        usgMsg.getCollaborationInfo().getAgreementRef().setPmode(pm.getId());
        usgMsg.getCollaborationInfo().getAgreementRef().setValue(pm.getAgreement());

        // add mail description
        List<Property> lstProperties = new ArrayList<>();
        if (mo.getSubject() != null) {
            Property p = new Property();
            p.setName(EbMSConstants.EBMS_PROPERTY_DESC);
            p.setValue(mo.getSubject());
            lstProperties.add(p);

        }
        // add submit date
        if (mo.getSubmitedDate() != null) {
            Calendar cal = new GregorianCalendar();
            cal.setTime(mo.getSubmitedDate());
            Property p = new Property();
            p.setName(EbMSConstants.EBMS_PROPERTY_SUBMIT_DATE);
            p.setValue(DatatypeConverter.printDateTime(cal));
            lstProperties.add(p);
        }
        // add aditional properties
        if (mo.getMSHOutProperties() != null && !mo.getMSHOutProperties().getMSHOutProperties().isEmpty()) {
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
                PartInfo pl = new PartInfo();
                pl.setHref("cid:" + mp.getEbmsId()); // all parts are attachments!
                if (mp.getDescription() != null && !mp.getDescription().isEmpty()) {
                    pl.setDescription(new Description());
                    pl.getDescription().setLang("EN");
                    pl.getDescription().setValue(mp.getDescription());
                }
                List<Property> fileProp = new ArrayList<>();
                if (!Utils.isEmptyString(mp.getName())) {
                    Property fp = new Property();
                    fp.setName(EbMSConstants.EBMS_FILE_PROPERTY_NAME);
                    fp.setValue(mp.getName());
                    fileProp.add(fp);
                }

                if (!Utils.isEmptyString(mp.getFilename())) {
                    Property fp = new Property();
                    fp.setName(EbMSConstants.EBMS_FILE_PROPERTY_FILENAME);
                    fp.setValue(mp.getFilename());
                    fileProp.add(fp);
                }
                if (!Utils.isEmptyString(mp.getEncoding())) {
                    Property fp = new Property();
                    fp.setName(EbMSConstants.EBMS_FILE_PROPERTY_ENCODING);
                    fp.setValue(mp.getEncoding());
                    fileProp.add(fp);
                }

                if (!Utils.isEmptyString(mp.getMimeType())) {
                    Property fp = new Property();
                    fp.setName(EbMSConstants.EBMS_FILE_PROPERTY_MIME);
                    fp.setValue(mp.getMimeType());
                    fileProp.add(fp);
                }

                if (!Utils.isEmptyString(mp.getType())) {
                    Property fp = new Property();
                    fp.setName(EbMSConstants.EBMS_FILE_PROPERTY_TYPE);
                    fp.setValue(mp.getType());
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

    public MSHInMail userMessage2MSHMail(UserMessage um) throws EBMSError {
        long l = mlog.logStart();

        MessageInfo mi = um.getMessageInfo();
        if (mi == null) {
            String errmsg = "Missing MessageInfo in UserMessage";
            throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
        }

        CollaborationInfo ca = um.getCollaborationInfo();
        if (ca == null) {
            String errmsg = "Missing CollaborationInfo in UserMessage";
            throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
        }

        MSHInMail mshmail = new MSHInMail();
        mshmail.setService(ca.getService().getValue());
        mshmail.setAction(ca.getAction());
        mshmail.setConversationId(ca.getConversationId());
        mshmail.setRefToMessageId(mi.getRefToMessageId());
        mshmail.setSentDate(mi.getTimestamp());
        mshmail.setMessageId(mi.getMessageId());

        if (um.getMessageProperties() != null && !um.getMessageProperties().getProperties().isEmpty()) {
            List<MSHInProperty> lstProp = new ArrayList<>();
            for (Property p : um.getMessageProperties().getProperties()) {
                if (p.getName() != null) {
                    switch (p.getName()) {
                        case EbMSConstants.EBMS_PROPERTY_DESC:
                            mshmail.setSubject(p.getValue());
                            break;
                        case EbMSConstants.EBMS_PROPERTY_SUBMIT_DATE:
                            Date dt = DatatypeConverter.parseDateTime(p.getValue()).getTime();
                            mshmail.setSubmitedDate(dt);
                            break;
                        default:
                            MSHInProperty mp = new MSHInProperty();
                            mp.setName(p.getName());
                            mp.setValue(p.getValue());
                            lstProp.add(mp);
                    }
                }else {
                    MSHInProperty mp = new MSHInProperty();
                    mp.setName(p.getName());
                    mp.setValue(p.getValue());
                    lstProp.add(mp);
                }
            }
            if (!lstProp.isEmpty()) {
                MSHInProperties mop = new MSHInProperties();
                mop.getMSHInProperties().addAll(lstProp);
                mshmail.setMSHInProperties(mop);
            }
        }
        if (um.getPartyInfo() == null) {
            String errmsg = "Missing PartyInfo in UserMessage";
            mlog.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
        }

        if (um.getPartyInfo().getFrom() == null) {
            String errmsg = "Missing PartyInfo/From in UserMessage";
            mlog.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
        }

        for (PartyId pi : um.getPartyInfo().getFrom().getPartyIds()) {
            if (pi.getType() != null) {
                switch (pi.getType()) {
                    case EbMSConstants.EBMS_PARTY_TYPE_EBOX:
                        mshmail.setSenderEBox(pi.getValue());
                        break;
                    case EbMSConstants.EBMS_PARTY_TYPE_NAME:
                        mshmail.setSenderName(pi.getValue());
                        break;
                    default:
                        String msgwrn = "Unknown type '" + pi.getType() + "' for From/PartyId: with value:'" + pi.getValue() + "'. Value is ignored!";
                        mlog.logWarn(l, msgwrn, null);
                        break;
                }
            } else {
                String msgwrn = "Missing type for From/PartyId: with value:'" + pi.getValue() + "'. Value is ignored!";
                mlog.logWarn(l, msgwrn, null);
            }
        }

        if (um.getPartyInfo().getTo() == null) {
            String errmsg = "Missing PartyInfo/To in UserMessage";
            throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
        }

        for (PartyId pi : um.getPartyInfo().getTo().getPartyIds()) {
            if (pi.getType() != null) {
                switch (pi.getType()) {
                    case EbMSConstants.EBMS_PARTY_TYPE_EBOX:
                        mshmail.setReceiverEBox(pi.getValue());
                        break;
                    case EbMSConstants.EBMS_PARTY_TYPE_NAME:
                        mshmail.setReceiverName(pi.getValue());
                        break;
                    default:
                        String msgwrn = "Unknown type '" + pi.getType() + "' for To/PartyId: with value:'" + pi.getValue() + "'. Value is ignored!";
                        mlog.logWarn(l, msgwrn, null);
                        break;
                }
            } else {
                String msgwrn = "Missing type for To/PartyId: with value:'" + pi.getValue() + "'. Value is ignored!";
                mlog.logWarn(l, msgwrn, null);

            }
        }

        if (mshmail.getReceiverEBox() == null) {
            String errmsg = "Missing receipt type: '" + EbMSConstants.EBMS_PARTY_TYPE_EBOX + "' ";
            throw new EBMSError(EBMSErrorCode.MissingReceipt, null, errmsg);
        }

        if (um.getPayloadInfo() != null && !um.getPayloadInfo().getPartInfos().isEmpty()) {
            List<MSHInPart> lstParts = new ArrayList<>();

            for (PartInfo pi : um.getPayloadInfo().getPartInfos()) {
                MSHInPart part = new MSHInPart();
                String href = pi.getHref();
                if (href!= null){
                    if (href.startsWith("cid:")){
                        part.setEbmsId(pi.getHref().substring(4)); // remove cid
                    }else if (href.startsWith("#")){
                        part.setEbmsId(pi.getHref().substring(1)); // remove hash
                    }else {
                        part.setEbmsId(pi.getHref());
                    }
                }
                part.setDescription(pi.getDescription() != null ? pi.getDescription().getValue() : null);
                if (pi.getPartProperties() != null && !pi.getPartProperties().getProperties().isEmpty()) {
                    for (Property p : pi.getPartProperties().getProperties()) {
                        if (p.getName() != null) {
                            switch (p.getName()) {
                                case EbMSConstants.EBMS_FILE_PROPERTY_NAME:
                                    part.setName(p.getValue());
                                    break;
                                case EbMSConstants.EBMS_FILE_PROPERTY_FILENAME:
                                    part.setFilename(p.getValue());
                                    break;
                                case EbMSConstants.EBMS_FILE_PROPERTY_MIME:
                                    part.setMimeType(p.getValue());
                                    break;
                                case EbMSConstants.EBMS_FILE_PROPERTY_ENCODING:
                                    part.setEncoding(p.getValue());
                                    break;
                                case EbMSConstants.EBMS_FILE_PROPERTY_TYPE:
                                    part.setType(p.getValue());
                                    break;
                                default:
                                    mlog.logWarn(l, "Unknown part property: '" + p.getName() + "' for message: '" + mshmail.getMessageId() + "' ", null);
                            }
                        }
                    }
                }
                
                lstParts.add(part);
            }
            if (!lstParts.isEmpty()) {
                mshmail.setMSHInPayload(new MSHInPayload());
                mshmail.getMSHInPayload().getMSHInParts().addAll(lstParts);
            }
        }

        return mshmail;
    }
    
    
    /*
    public static File createSoapLogFile(MSHMail mm, boolean isRequest, boolean isOutgoingMail) throws IOException {
        File f;
        String prefix = EbMSConstants.File_PREFIX_SOAP_Message + (mm != null ? mm.getConversationId() + "-" + mm.getAction() : "no_conv_id") + "-";
        f = File.createTempFile(prefix,
                isRequest ? EbMSConstants.File_Suffix_SOAP_Message_Request : EbMSConstants.File_Suffix_SOAP_Message_Response,
                isOutgoingMail ? Settings.getInstance().getOutboxFolder() : Settings.getInstance().getInboxFolder());

        return f;
    }*/

}
