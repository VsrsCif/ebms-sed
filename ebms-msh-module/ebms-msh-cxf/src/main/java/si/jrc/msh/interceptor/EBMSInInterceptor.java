
/*
* Copyright 2015, Supreme Court Republic of Slovenia 
*
* Licensed under the EUPL, Version 1.1 or – as soon they will be approved by 
* the European Commission - subsequent versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the Licence.
* You may obtain a copy of the Licence at:
*
* https://joinup.ec.europa.eu/software/page/eupl
*
* Unless required by applicable law or agreed to in writing, software 
* distributed under the Licence is distributed on an "AS IS" basis, WITHOUT 
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the Licence for the specific language governing permissions and  
* limitations under the Licence.
 */
package si.jrc.msh.interceptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import javax.activation.DataHandler;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageChecker;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.inbox.payload.MSHInPart;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.svev.pmode.Certificate;
import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.References;
import org.msh.svev.pmode.Security;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyId;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.ebox.SEDBox;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import si.jrc.msh.exception.EBMSError;
import si.jrc.msh.exception.EBMSErrorCode;
import si.jrc.msh.utils.EBMSUtils;
import si.jrc.msh.utils.EbMSConstants;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.exception.ExceptionUtils;
import si.sed.commons.exception.HashException;
import si.sed.commons.exception.PModeException;
import si.sed.commons.exception.SOAPExceptionCode;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.GZIPUtil;
import si.sed.commons.utils.HashUtils;
import si.sed.commons.utils.PModeManager;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.sec.KeystoreUtils;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author sluzba
 */
public class EBMSInInterceptor extends AbstractEBMSInterceptor {

    private static final Set<QName> HEADERS = new HashSet<>();

    static {
        HEADERS.add(new QName(EbMSConstants.EBMS_NS,
                EbMSConstants.EBMS_ROOT_ELEMENT_NAME));
        WSS4JInInterceptor me = new WSS4JInInterceptor();
        HEADERS.addAll(me.getUnderstoodHeaders());
    }

    /**
     *
     */
    protected final static SEDLogger LOG =
            new SEDLogger(EBMSInInterceptor.class);

    StorageUtils msuStorageUtils = new StorageUtils();
    protected final HashUtils mpHU = new HashUtils();
    protected final GZIPUtil mGZIPUtils = new GZIPUtil();

    PModeManager mPModeManage = new PModeManager();
    EBMSUtils mebmsUtils = new EBMSUtils();
    CryptoCoverageChecker checker = new CryptoCoverageChecker();
    WSS4JInInterceptor wssInterceptor = new WSS4JInInterceptor();

    /**
     *
     */
    public EBMSInInterceptor() {
        //super(Phase.USER_PROTOCOL);
        super(Phase.PRE_PROTOCOL); // user preprotocol for generating receipt
        // in user_protocol wss in removed!
        getAfter().add(WSS4JInInterceptor.class.getName());
    }

    /**
     *
     * @param phase
     */
    public EBMSInInterceptor(String phase) {
        super(phase);
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
        boolean isRequestor = MessageUtils.isRequestor(msg);
        // check for Messaging header
        QName sv = (isRequestor ? SoapFault.FAULT_CODE_CLIENT :
                SoapFault.FAULT_CODE_SERVER);

        try {
            //validate in message
            Messaging msgHeader = vaildateMessagingData(msg);
            msg.getExchange().put(Messaging.class, msgHeader);
            // get processing mode for message!
            PMode pm = getProcessingMode(msg, msgHeader);

            msg.getExchange().put(PMode.class, pm);
            if (pm == null) {
                String wrnmsg = "PMode for header" + msgHeader.getId();
                LOG.logWarn(l, wrnmsg, null);
            } else if (pm.getLegs().size() > 0 &&
                    pm.getLegs().get(0).getSecurity() != null) {
                // check signed elements
                checkSecurity(pm.getLegs().get(0).getSecurity(), msg);
            } else {
                String wrnmsg = "No security is defined for pmode: '" +
                        pm.getId() + "'";
                LOG.logWarn(l, wrnmsg, null);
            }

            // create receive message entitity
            // process signals 
            // process userMessage
            if (isRequestor) {
                if (!msgHeader.getUserMessages().isEmpty()) {
                    String errmsg =
                            "For response only signal response is expected! UserMessage is ignored";
                    LOG.logError(l, errmsg, null);
                }

                if (msgHeader.getSignalMessages().size() > 0) {
                    // receive as4receipt
                    // receive errors
                    processResponseSignals(msgHeader.getSignalMessages(), pm,
                            msg);

                } else {
                    String errmsg =
                            "For SOAP response error signal or receipt is expected!";
                    LOG.logError(l, errmsg, null);
                    throw new SoapFault(errmsg, version.getReceiver());
                }
            } else if (!msgHeader.getUserMessages().isEmpty()) {
                // 
                MSHInMail mMail = mebmsUtils.userMessage2MSHMail(
                        msgHeader.getUserMessages().get(0));
                String receiverBox = mMail.getReceiverEBox();
                if (receiverBox == null || receiverBox.trim().isEmpty()) {
                    String errmsg = "Missing receiver box!";
                    LOG.logError(l, errmsg, null);
                    throw new EBMSError(EBMSErrorCode.Other,
                            mMail.getMessageId(), errmsg);
                }

                SEDBox inSb = getSedBoxByName(mMail.getReceiverEBox());
                if (inSb == null || (inSb.getActiveToDate() != null &&
                        inSb.getActiveToDate().before(Calendar.
                                getInstance().getTime()))) {
                    String errmsg =
                            "Receiver box: '" + mMail.getReceiverEBox() +
                            "' not exists or is not active.";
                    LOG.logError(l, errmsg, null);
                    throw new EBMSError(EBMSErrorCode.Other,
                            mMail.getMessageId(), errmsg);
                }

                msg.getExchange().put(SEDBox.class, inSb);

                // validate attachments
                List<String> lstSoapAtt = new ArrayList<>();
                List<String> lstEBMSAtt = new ArrayList<>();
                for (Attachment a : msg.getAttachments()) {
                    lstSoapAtt.add(a.getId());
                }
                if (mMail.getMSHInPayload() != null &&
                        !mMail.getMSHInPayload().getMSHInParts().isEmpty()) {
                    for (MSHInPart ip : mMail.getMSHInPayload().getMSHInParts()) {
                        if (lstSoapAtt.contains(ip.getEbmsId())) {
                            lstSoapAtt.remove(ip.getEbmsId());
                        } else {
                            lstEBMSAtt.add(ip.getEbmsId());
                        }
                    }
                }

                if (!lstSoapAtt.isEmpty() || !lstEBMSAtt.isEmpty()) {
                    String errmsg =
                            "Ebms Payloads does note match soap attachments.";
                    LOG.logError(l, errmsg, null);
                    throw new EBMSError(EBMSErrorCode.ValueInconsistent, null,
                            errmsg);
                }
                // serialize attachments
                if (mMail.getMSHInPayload() != null &&
                        !mMail.getMSHInPayload().getMSHInParts().isEmpty()) {
                    for (MSHInPart p : mMail.getMSHInPayload().getMSHInParts()) {
                        try {
                            serializeAttachments(p, msg.getAttachments(), true);
                        } catch (StorageException | IOException | HashException ex) {
                            String errmsg = "Error reading attachments .";
                            LOG.logError(l, errmsg, null);
                            throw new EBMSError(
                                    EBMSErrorCode.ExternalPayloadError, null,
                                    errmsg);
                        }
                    }

                }

                // serializa data  DB
                // prepare mail to persist 
                Date dt = Calendar.getInstance().getTime();
                // set current status
                mMail.setStatus(SEDInboxMailStatus.RECEIVE.getValue());
                mMail.setStatusDate(dt);
                mMail.setReceivedDate(dt);
                try {
                    getDAO().serializeInMail(mMail, "ebms-msh-ws");
                } catch (StorageException ex) {
                    String errmsg =
                            "Internal error occured while serializing incomming mail.";
                    LOG.logError(l, errmsg, ex);
                    throw ExceptionUtils.createSoapFault(
                            SOAPExceptionCode.StoreInboundMailFailure,
                            SoapFault.FAULT_CODE_SERVER, errmsg);
                }

                msg.getExchange().put(MSHInMail.class, mMail);

                SOAPMessage request = msg.getContent(SOAPMessage.class);

                SignalMessage as4Receipt = mebmsUtils.generateAS4ReceiptSignal(
                        mMail.getMessageId(), Utils.
                        getDomainFromAddress(mMail.getReceiverEBox()),
                        request.getSOAPPart().getDocumentElement(), dt);
                msg.getExchange().put(SignalMessage.class, as4Receipt);
            } else {
                String errmsg =
                        "Missing userMessage! In a SVEV-MSH  pull-MEP is not exepected!";
                LOG.logError(l, errmsg, null);
                throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null,
                        errmsg);

            }
        } catch (EBMSError ex) {
            LOG.logError(l, ex);
            if (!isRequestor) {
                try {

                    Endpoint e = msg.getExchange().get(Endpoint.class);
                    if (!msg.getExchange().isOneWay()) {
                        Message responseMsg = new MessageImpl();
                        responseMsg.setExchange(msg.getExchange());
                        responseMsg = e.getBinding().createMessage(responseMsg);
                        msg.getExchange().setOutMessage(responseMsg);

                        MessageFactory mf = MessageFactory.newInstance(
                                SOAPConstants.SOAP_1_2_PROTOCOL);
                        SOAPMessage soapMessage = mf.createMessage();
                        soapMessage.saveChanges();

                        responseMsg.setContent(SOAPMessage.class, soapMessage);
                        responseMsg.getExchange().put(EBMSError.class, ex);

                        InterceptorChain chain =
                                OutgoingChainInterceptor.getOutInterceptorChain(
                                        msg
                                        .getExchange());
                        responseMsg.setInterceptorChain(chain);
                        chain.doInterceptStartingAfter(responseMsg,
                                SoapPreProtocolOutInterceptor.class.getName());
                    }

                    // abort message
                    InterceptorChain chain = msg.getInterceptorChain();
                    chain.abort();
                } catch (SOAPException ex1) {
                    LOG.logError(l, ex1);
                }

            }

        }
        LOG.logEnd(l);
    }

    private SEDBox getSedBoxByName(String sbox) {
        return getLookups().getSEDBoxByName(sbox);

    }

    // receive 
    private void processResponseSignals(List<SignalMessage> lstSignals, PMode pm,
            SoapMessage msg)
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
                String errmsg =
                        "Pull MEP is not supported! Pull signal is ignored";
                LOG.logError(l, errmsg, null);

            }

            if (mi.getRefToMessageId() == null ||
                    mi.getRefToMessageId().trim().isEmpty()) {
                String errmsg = "Missing missing RefToMessageId";
                LOG.logError(l, errmsg, null);
                throw new SoapFault(errmsg, version.getReceiver());
            }

            MSHOutMail outmsg = msg.getExchange().get(MSHOutMail.class);
            String strOutMsg = outmsg.getMessageId() + "@" +
                    getSettings().getDomain();
            if (strOutMsg != null && !strOutMsg.equals(mi.getRefToMessageId())) {
                String errmsg = "Outgoing msg ID '" + strOutMsg +
                        "' not equals to received response signal RefToMessageId: '" +
                        mi.getRefToMessageId() + "' ";
                LOG.logError(l, errmsg, null);
                //throw new SoapFault(errmsg, version.getReceiver());
            }

            if (sm.getErrors() != null && !sm.getErrors().isEmpty()) {
                String desc = "";
                for (org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Error er
                        : sm.getErrors()) {
                    desc = er.getOrigin() + "" + er.getSeverity() + " " +
                            er.getErrorCode() + " " + er.getErrorDetail();
                    break;
                }

                try {
                    getDAO().setStatusToOutMail(outmsg,
                            SEDOutboxMailStatus.EBMSERROR, desc);
                } catch (StorageException ex) {
                    String msgErr =
                            "Error occured when setting MSHOutMail (id" +
                            outmsg.getId() + ") status to EBMSERROR";
                    LOG.logError(l, msgErr, ex);
                }

            } else if (sm.getReceipt() != null) {
                outmsg.setReceivedDate(mi.getTimestamp());
                try {
                    getDAO().setStatusToOutMail(outmsg, SEDOutboxMailStatus.SENT,
                            "Mail received to receiver MSH");
                } catch (StorageException ex) {
                    String msgErr =
                            "Error occured when setting MSHOutMail (id" +
                            outmsg.getId() + ") status to SENT";
                    LOG.logError(l, msgErr, ex);
                }

            }

            msg.getExchange().put("SIGNAL_ELEMENTS", sm.getAnies());

            for (Element e : sm.getAnies()) {
                mlog.log("Got elements in signal: " + e.getLocalName());
                /* if (e.getLocalName().equals("SVEVEncryptionKey")) {
                    System.out.println("********************** got encryptionKey");
                    try {
                        SVEVEncryptionKey se = (SVEVEncryptionKey) XMLUtils.deserialize(e, SVEVEncryptionKey.class);
                        MshIncomingMail mm = mSHDB.getIncomingMailByActionAndByConversationId(SVEVConstants.SVEV_ACTION_DeliveryNotification, se.getId());
                        if (mm == null) {
                            String errmsg = "Incoming mail with message ID: " + se.getId() + " not exists!";
                            LOG.error(errmsg);
                            throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg);
                        }
                        System.out.println("SET enc key");
                        mm.getMmshMail().setSVEVEncryptionKey(se);
                        System.out.println("update incoming mail!!!!");
                        mSHDB.update(mm);

                    } catch (JAXBException ex) {
                        String errmsg = "Error parsing  '" + e.getNamespaceURI() + "', tagname: '" + e.getLocalName() + "'! Error: " + ex.getMessage();
                        LOG.error(errmsg, ex);
                        throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg, ex);
                    }
                } else {
                    String errmsg = "Error parsing  '" + e.getNamespaceURI() + "', tagname: '" + e.getLocalName() + "'!";
                    LOG.logError(l, errmsg);
                    throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg);
                }
                 */
            }

        }
    }

    private Messaging vaildateMessagingData(SoapMessage msg)
            throws SoapFault, EBMSError {
        long l = LOG.logStart();
        SoapVersion version = msg.getVersion();
        boolean isRequestor = MessageUtils.isRequestor(msg);
        QName sv = (isRequestor ? SoapFault.FAULT_CODE_CLIENT :
                SoapFault.FAULT_CODE_SERVER);

        if (version.getVersion() != 1.2) {
            String errmsg = "ebMS AS4 supports only soap 1.2 protocol!";
            LOG.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(
                    SOAPExceptionCode.SoapVersionMismatch, sv, errmsg);
        }

        SOAPMessage request = msg.getContent(SOAPMessage.class);

        NodeList lstND = null;
        try {
            lstND = request.getSOAPHeader().getElementsByTagNameNS(
                    EbMSConstants.EBMS_NS,
                    EbMSConstants.EBMS_ROOT_ELEMENT_NAME);
        } catch (SOAPException ex) {
            String errmsg = "Error parsing EMBS header! Error: " +
                    ex.getMessage();
            LOG.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(
                    SOAPExceptionCode.SoapParseFailure, sv);

        }
        if (lstND == null || lstND.getLength() == 0) {
            String errmsg = "Missing EBMS header: " + EbMSConstants.EBMS_NS +
                    ":" +
                    EbMSConstants.EBMS_ROOT_ELEMENT_NAME + "!";
            LOG.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
        }

        if (lstND.getLength() != 1) {
            String errmsg = "Onyl one EBMS header (" + EbMSConstants.EBMS_NS +
                    ":" +
                    EbMSConstants.EBMS_ROOT_ELEMENT_NAME + ") found: " + lstND.
                    getLength() + "!";
            LOG.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
        }

        Element elmn = (Element) lstND.item(0); // expected only one
        Messaging msgHeader = null;
        try {
            msgHeader = (Messaging) XMLUtils.deserialize(elmn, Messaging.class);
        } catch (JAXBException ex) {
            String errmsg = "Error reading EMBS header! Error: " +
                    ex.getMessage();
            LOG.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
        }

        if (msgHeader == null) {
            String errmsg = "Missing header";
            LOG.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, getMessageId(
                    msgHeader), errmsg);
        }

        String lstErrors = XMLUtils.validateBySchema(msgHeader,
                Messaging.class.getResourceAsStream(
                        "/schemas/ebms-header-3_0-200704.xsd"), "/schemas/");
        if (!lstErrors.isEmpty()) {
            String errmsg = "Error validating by schema: " + lstErrors;
            LOG.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, getMessageId(
                    msgHeader), lstErrors);
        }

        // zero signal or usermessage is expected
        if (msgHeader.getUserMessages().isEmpty() &&
                msgHeader.getSignalMessages().isEmpty()) {
            String errmsg = "UserMessage or SignalMessage is exptected!";
            LOG.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, getMessageId(
                    msgHeader), errmsg);
        }
        // only one ser message is expected
        if (msgHeader.getUserMessages().size() > 1) {
            String errmsg = "Zero or  one UserMessage is exptected!";
            LOG.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, getMessageId(
                    msgHeader), errmsg);
        }

        return msgHeader;

    }

    private PMode getProcessingMode(SoapMessage msg, Messaging msgHeader)
            throws EBMSError {
        long l = LOG.logStart();
        boolean requestor = MessageUtils.isRequestor(msg);
        QName sv = (requestor ? SoapFault.FAULT_CODE_SERVER :
                SoapFault.FAULT_CODE_CLIENT);

        PMode pmd = msg.getExchange().get(PMode.class); // 
        // if pmd not in exchange - then this should be user message            
        if (pmd == null && requestor) {
            String errmsg =
                    "Invalid ebms configuration! Set PMode to exchange: as " +
                    "'client.getRequestContext().put(PMode.class.getName(), pmod)'";
            LOG.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(
                    SOAPExceptionCode.StoreInboundMailFailure, sv, errmsg);
        }
        if (pmd == null) {

            // simple validating user message
            if (msgHeader.getUserMessages().isEmpty()) {
                String errmsg =
                        "Missing userMessage! In a SVEV-MSH  pull-MEP is not exepected!";
                LOG.logError(l, errmsg, null);
                throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null,
                        errmsg);
            }

            if (msgHeader.getUserMessages().get(0).getCollaborationInfo() ==
                    null) {
                String errmsg = "Missing CollaborationInfo in UserMessage!";
                LOG.logError(l, errmsg, null);
                throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null,
                        errmsg);
            }

            if (msgHeader.getUserMessages().get(0).getCollaborationInfo() ==
                    null) {
                String errmsg = "Missing CollaborationInfo!";
                LOG.logError(l, errmsg, null);
                throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
            }
            CollaborationInfo ca =
                    msgHeader.getUserMessages().get(0).getCollaborationInfo();

            if (ca.getService() == null || ca.getService().getValue() == null ||
                    ca.getService().getValue().isEmpty()) {
                String errmsg = "Missing 'service' value!";
                LOG.logError(l, errmsg, null);
                throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
            }

            if (msgHeader.getUserMessages().get(0).getPartyInfo() == null) {
                String errmsg = "Missing 'PartyInfo' value!";
                LOG.logError(l, errmsg, null);
                throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
            }

            if (msgHeader.getUserMessages().get(0).getPartyInfo().getFrom() ==
                    null ||
                    msgHeader.getUserMessages().get(0).getPartyInfo().getFrom().getPartyIds().isEmpty()) {

                String errmsg = "Missing 'PartyInfo/From/PartyId' value!";
                LOG.logError(l, errmsg, null);
                throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
            }
            List<PartyId> plst =
                    msgHeader.getUserMessages().get(0).getPartyInfo().getFrom().getPartyIds();
            String senderBox = null;
            for (PartyId p : plst) {
                if (p.getType() != null &&
                        EbMSConstants.EBMS_PARTY_TYPE_EBOX.equals(p.getType())) {
                    senderBox = p.getValue();
                    break;
                }
            }
            if (senderBox == null) {
                String errmsg =
                        "Missing senderEBox: 'PartyInfo/From/PartyId' for type: '" +
                        EbMSConstants.EBMS_PARTY_TYPE_EBOX + "' value!";
                LOG.logError(l, errmsg, null);
                throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
            }

            String srv = ca.getService().getValue();
            String pmodeId = srv + ":" + Utils.getDomainFromAddress(senderBox);
            try {
                // if user message
                pmd = mPModeManage.getPModeById(pmodeId);
            } catch (PModeException ex) {
                String errmsg = "Error reading PModes for id: '" +
                        ca.getAgreementRef().getPmode() + "'! Err:" + ex.
                        getMessage();
                LOG.logError(l, errmsg, ex);
                throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null,
                        errmsg, ex);
            }
            if (pmd == null) {
                String errmsg = "PMode with id: '" +
                        ca.getAgreementRef().getPmode() + "' not exist!";
                LOG.logError(l, errmsg, null);
                throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null,
                        errmsg);
            }
            msg.getExchange().put(PMode.class, pmd);
        }
        return pmd;

    }

    private void checkSecurity(Security sc, SoapMessage msg) {

        //  check signatures 
        Certificate c = sc.getX509().getSignature().getCertificate();
        Map<String, Object> inProps = new HashMap<>();
        inProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);

        String cpropname = "CP." + UUID.randomUUID().toString();
        String alias = sc.getX509().getSignature().getCertificate().getAlias();
        SEDCertStore cs = getLookups().getSEDCertStoreByCertAlias(alias, false);
        //Properties cp = CertificateUtils.getInstance().getVerifySignProperties();
        Properties cp = KeystoreUtils.getVerifySignProperties(alias, cs);
        inProps.put(cpropname, cp);

        inProps.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, cpropname);
        //inProps.put(WSHandlerConstants.SIG_PROP_REF_ID, cpropname);

        wssInterceptor.setProperties(inProps);
        wssInterceptor.handleMessage(msg);

        // check signed elements // todo for attachments
        if (sc.getX509() != null && sc.getX509().getSignature() != null &&
                sc.getX509().getSignature().getSign() != null &&
                sc.getX509().getSignature().getSign().getElements() != null &&
                sc.getX509().getSignature().getSign().getElements().getXPaths().size() >
                0) {
            Map<String, String> prefixes = new HashMap<>();
            List<CryptoCoverageChecker.XPathExpression> xpaths =
                    new ArrayList<>();
            int i = 0;
            for (References.Elements.XPath el
                    : sc.getX509().getSignature().getSign().getElements().getXPaths()) {
                for (References.Elements.XPath.Namespace ns : el.getNamespaces()) {
                    prefixes.put(ns.getPrefix(), ns.getNamespace());
                }
                xpaths.add(new CryptoCoverageChecker.XPathExpression(
                        el.getXpath(),
                        CryptoCoverageUtil.CoverageType.SIGNED,
                        CryptoCoverageUtil.CoverageScope.ELEMENT));

                i++;
            }
            checker = new CryptoCoverageChecker(prefixes, xpaths);

            checker.handleMessage(msg);
        }
    }

    /**
     *
     * @param message
     */
    @Override
    public void handleFault(SoapMessage message) {
        super.handleFault(message);
    }

    /**
     *
     * @param msgHeader
     * @return
     */
    public String getMessageId(Messaging msgHeader) {
        String msgId = null;
        if (msgHeader != null) {
            MessageInfo mi = null;
            if (!msgHeader.getUserMessages().isEmpty()) {
                mi = msgHeader.getUserMessages().get(0).getMessageInfo();
            } else if (!msgHeader.getSignalMessages().isEmpty()) {
                mi = msgHeader.getSignalMessages().get(0).getMessageInfo();
            }
            if (mi != null) {
                msgId = mi.getMessageId();
            }
        }
        return msgId;

    }

    private void serializeAttachments(MSHInPart p,
            Collection<Attachment> lstAttch, boolean compressed)
            throws
            StorageException, IOException, HashException {
        DataHandler dh = null;
        for (Attachment a : lstAttch) {
            if (a.getId().equals(p.getEbmsId())) {
                dh = a.getDataHandler();
                break;
            }
        }

        File fout = null;
        if (dh != null) {
            fout = msuStorageUtils.storeInFile(p.getMimeType(),
                    dh.getInputStream());
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
                p.setName(p.getFilename().substring(p.getFilename().lastIndexOf(
                        ".")));
            }
        }

    }

}
