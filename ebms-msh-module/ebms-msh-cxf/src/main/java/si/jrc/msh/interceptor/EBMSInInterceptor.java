/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.interceptor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
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

import org.msh.svev.pmode.Certificate;
import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.References;
import org.msh.svev.pmode.Security;

import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import si.jrc.msh.client.sec.SecurityProperties;

import si.jrc.msh.exception.EBMSError;
import si.jrc.msh.exception.EBMSErrorCode;
import si.jrc.msh.exception.ExceptionUtils;
import si.jrc.msh.exception.SOAPExceptionCode;
import si.jrc.msh.utils.EBMSUtils;
import si.sed.commons.utils.PModeManager;
import si.jrc.msh.utils.EbMSConstants;

import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author sluzba
 */
public class EBMSInInterceptor extends AbstractSoapInterceptor {

    private static final Set<QName> HEADERS = new HashSet<>();

    static {
        HEADERS.add(new QName(EbMSConstants.EBMS_NS, EbMSConstants.EBMS_ROOT_ELEMENT_NAME));
        WSS4JInInterceptor me = new WSS4JInInterceptor();
        HEADERS.addAll(me.getUnderstoodHeaders());
    }

    protected final SEDLogger mlog = new SEDLogger(EBMSOutInterceptor.class);

    PModeManager mPModeManage = new PModeManager();
    EBMSUtils mebmsUtils = new EBMSUtils();
    CryptoCoverageChecker checker = new CryptoCoverageChecker();
    WSS4JInInterceptor wssInterceptor = new WSS4JInInterceptor();

    public EBMSInInterceptor() {
        super(Phase.PRE_PROTOCOL);
        getAfter().add(WSS4JInInterceptor.class.getName());
    }

    public EBMSInInterceptor(String phase) {
        super(phase);
    }

    @Override
    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }

    @Override
    public void handleMessage(SoapMessage msg) {
        long l = mlog.logStart();
        SoapVersion version = msg.getVersion();
        boolean isRequestor = MessageUtils.isRequestor(msg);
        // check for Messaging header
        QName sv = (isRequestor ? SoapFault.FAULT_CODE_CLIENT : SoapFault.FAULT_CODE_SERVER);

        try {
            //validate in message
            Messaging msgHeader = vaildateMessagingData(msg);
            msg.getExchange().put(Messaging.class, msgHeader);
            // get processing mode for message!
            PMode pm = getProcessingMode(msg, msgHeader);
            if (pm == null) {
                String wrnmsg = "PMode for header" + msgHeader.getId();
                mlog.logWarn(l, wrnmsg, null);
            } else if (pm.getLegs().size() > 0 && pm.getLegs().get(0).getSecurity() != null) {
                // check signed elements
                checkSecurity(pm.getLegs().get(0).getSecurity(), msg);
            } else {
                String wrnmsg = "No security is defined for pmode: '" + pm.getId() + "'";
                mlog.logWarn(l, wrnmsg, null);
            }
            msg.getExchange().put(PMode.class.getName(), pm);

            // create receive message entitity
            // process signals 
            // process userMessage
            if (isRequestor) {
                if (!msgHeader.getUserMessages().isEmpty()) {
                    String errmsg = "For response only signal response is expected! UserMessage is ignored";
                    mlog.logError(l, errmsg, null);;
                }
                if (msgHeader.getSignalMessages().size() > 0) {
                    // receive as4receipt
                    // receive errors
                    processResponseSignals(msgHeader.getSignalMessages(), pm, msg);

                } else {
                    String errmsg = "For SOAP response error signal or receipt is expected!";
                    mlog.logError(l, errmsg, null);;
                    throw new SoapFault(errmsg, version.getReceiver());
                }
            } else if (!msgHeader.getUserMessages().isEmpty()) {
                // 
                MSHInMail mMail = mebmsUtils.userMessage2MSHMail(msgHeader.getUserMessages().get(0));
                msg.getExchange().put(MSHInMail.class.getName(), mMail);
                
                
                System.out.println("GOT ATTACHMENTS: " + msg.getAttachments().size());
                
                List<String> lstSoapAtt = new  ArrayList<>();
                List<String> lstEBMSAtt = new  ArrayList<>();
                for (Attachment  a: msg.getAttachments()){
                    lstSoapAtt.add(a.getId());
                }
                if (mMail.getMSHInPayload()!= null && !mMail.getMSHInPayload().getMSHInParts().isEmpty()) {
                    for (MSHInPart ip: mMail.getMSHInPayload().getMSHInParts()){
                        if (lstSoapAtt.contains(ip.getEbmsId())){
                            lstSoapAtt.remove(ip.getEbmsId());
                        } else {
                            lstEBMSAtt.add(ip.getEbmsId());
                        }
                    }
                }
                
                if (!lstSoapAtt.isEmpty() || !lstEBMSAtt.isEmpty() ){
                    String errmsg = "Ebms Payloads does note match soap attachments." ;
                    mlog.logError(l, errmsg, null);;
                    throw new EBMSError(EBMSErrorCode.ValueInconsistent, null, errmsg);
                }
                // generate receipt 
                
                
            
               
                // check 

                // rename filename
                //File f = (File) msg.getExchange().get(EbMSConstants.ContextProperty_In_SOAP_Message_File);
                /* if (f != null) {
                        File nFile;
                        try {
                            nFile = EBMSUtils.createSoapLogFile(mMail, !isRequestor, isRequestor);
                            f.renameTo(nFile);
                            f = nFile;
                            msg.getExchange().put(EbMSConstants.ContextProperty_In_SOAP_Message_File, nFile);

                        } catch (IOException ex) {
                            // ignore use old file
                        }

                        mim.setSoapRequestFileName(f.getName());
                    }*/

            } else {
                String errmsg = "Missing userMessage! In a SVEV-MSH  pull-MEP is not exepected!";
                mlog.logError(l, errmsg, null);;
                throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg);

            }
        } catch (EBMSError ex) {
            if (!isRequestor) {
                try {

                    Endpoint e = msg.getExchange().get(Endpoint.class);
                    if (!msg.getExchange().isOneWay()) {
                        Message responseMsg = new MessageImpl();
                        responseMsg.setExchange(msg.getExchange());
                        responseMsg = e.getBinding().createMessage(responseMsg);
                        msg.getExchange().setOutMessage(responseMsg);

                        MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
                        SOAPMessage soapMessage = mf.createMessage();
                        soapMessage.saveChanges();

                        responseMsg.setContent(SOAPMessage.class, soapMessage);
                        responseMsg.getExchange().put(EBMSError.class, ex);

                        InterceptorChain chain
                                = OutgoingChainInterceptor.getOutInterceptorChain(msg
                                        .getExchange());
                        responseMsg.setInterceptorChain(chain);
                        chain.doInterceptStartingAfter(responseMsg,
                                SoapPreProtocolOutInterceptor.class.getName());
                    }

                    // abport message
                    InterceptorChain chain = msg.getInterceptorChain();
                    chain.abort();
                } catch (SOAPException ex1) {
                    java.util.logging.Logger.getLogger(EBMSInInterceptor.class.getName()).log(Level.SEVERE, null, ex1);
                }

            }

        }
    }

    // receive 
    private void processResponseSignals(List<SignalMessage> lstSignals, PMode pm, SoapMessage msg) throws EBMSError {
        SoapVersion version = msg.getVersion();
        /*
         for (SignalMessage sm : lstSignals) {
         MessageInfo mi = sm.getMessageInfo();
         if (mi == null) {
         String errmsg = "Missing MessageInfo in SignalMessage";
         mlog.error(errmsg, null);
         throw new SoapFault(errmsg, version.getReceiver());
         }

         if (sm.getPullRequest() != null) {
         String errmsg = "Pull MEP is not supported! Pull signal is ignored";
         mlog.error(errmsg, null);

         }

         if (mi.getRefToMessageId() == null || mi.getRefToMessageId().trim().isEmpty()) {
         String errmsg = "Missing missing RefToMessageId";
         mlog.error(errmsg, null);
         throw new SoapFault(errmsg, version.getReceiver());
         }
         MshOutgoingMail outmsg = msg.getExchange().get(MshOutgoingMail.class);
         String strOutMsg = outmsg.getId() + "@" + mSettings.getDomain();
         if (!strOutMsg.equals(mi.getRefToMessageId())) {
         String errmsg = "Outgoing msg ID '" + strOutMsg + "' not equals to received response signal RefToMessageId: '" + mi.getRefToMessageId() + "' ";
         mlog.error(errmsg, null);
         throw new SoapFault(errmsg, version.getReceiver());
         }

         if (sm.getReceipt() != null) {
         outmsg.getMmshMail().setSentDate(mi.getTimestamp());
         outmsg.getMmshMail().setStatus(MSHStatusType.Sent.name());
         outmsg.setStatusChangeDate(Calendar.getInstance().getTime());
         mSHDB.update(outmsg);
         }

         for (Element e : sm.getAnies()) {

         if (e.getLocalName().equals("SVEVEncryptionKey")) {
         System.out.println("********************** got encryptionKey");
         try {
         SVEVEncryptionKey se = (SVEVEncryptionKey) XMLUtils.deserialize(e, SVEVEncryptionKey.class);
         MshIncomingMail mm = mSHDB.getIncomingMailByActionAndByConversationId(SVEVConstants.SVEV_ACTION_DeliveryNotification, se.getId());
         if (mm == null) {
         String errmsg = "Incoming mail with message ID: " + se.getId() + " not exists!";
         mlog.error(errmsg);
         throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg);
         }
         System.out.println("SET enc key");
         mm.getMmshMail().setSVEVEncryptionKey(se);
         System.out.println("update incoming mail!!!!");
         mSHDB.update(mm);

         } catch (JAXBException ex) {
         String errmsg = "Error parsing  '" + e.getNamespaceURI() + "', tagname: '" + e.getLocalName() + "'! Error: " + ex.getMessage();
         mlog.error(errmsg, ex);
         throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg, ex);
         }
         } else {
         String errmsg = "Error parsing  '" + e.getNamespaceURI() + "', tagname: '" + e.getLocalName() + "'!";
         mlog.error(errmsg);
         throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg);
         }
         }

         }*/
    }

    private Messaging vaildateMessagingData(SoapMessage msg) throws SoapFault, EBMSError {
        long l = mlog.logStart();
        SoapVersion version = msg.getVersion();
        boolean isRequestor = MessageUtils.isRequestor(msg);
        QName sv = (isRequestor ? SoapFault.FAULT_CODE_CLIENT : SoapFault.FAULT_CODE_SERVER);

        if (version.getVersion() != 1.2) {
            String errmsg = "ebMS AS4 supports only soap 1.2 protocol!";
            mlog.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(SOAPExceptionCode.SoapVersionMismatch, sv, errmsg);
        }

        SOAPMessage request = msg.getContent(SOAPMessage.class);

        NodeList lstND = null;
        try {
            lstND = request.getSOAPHeader().getElementsByTagNameNS(EbMSConstants.EBMS_NS, EbMSConstants.EBMS_ROOT_ELEMENT_NAME);
        } catch (SOAPException ex) {
            String errmsg = "Error parsing EMBS header! Error: " + ex.getMessage();
            mlog.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(SOAPExceptionCode.SoapParseFailure, sv);

        }
        if (lstND == null || lstND.getLength() == 0) {
            String errmsg = "Missing EBMS header: " + EbMSConstants.EBMS_NS + ":" + EbMSConstants.EBMS_ROOT_ELEMENT_NAME + "!";
            mlog.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
        }

        if (lstND.getLength() != 1) {
            String errmsg = "Onyl one EBMS header (" + EbMSConstants.EBMS_NS + ":" + EbMSConstants.EBMS_ROOT_ELEMENT_NAME + ") found: " + lstND.getLength() + "!";
            mlog.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
        }

        Element elmn = (Element) lstND.item(0); // expected only one
        Messaging msgHeader = null;
        try {
            msgHeader = (Messaging) XMLUtils.deserialize(elmn, Messaging.class);
        } catch (JAXBException ex) {
            String errmsg = "Error reading EMBS header! Error: " + ex.getMessage();
            mlog.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
        }

        String lstErrors = XMLUtils.validateBySchema(msgHeader, Messaging.class.getResourceAsStream("/schemas/ebms-header-3_0-200704.xsd"), "/schemas/");
        if (!lstErrors.isEmpty()) {
            throw new EBMSError(EBMSErrorCode.InvalidHeader, getMessageId(msgHeader), lstErrors);
        }

        // zero signal or usermessage is expected
        if (msgHeader.getUserMessages().isEmpty() && msgHeader.getSignalMessages().isEmpty()) {
            String errmsg = "UserMessage or SignalMessage is exptected!";
            mlog.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, getMessageId(msgHeader), errmsg);
        }
        // only one ser message is expected
        if (msgHeader.getUserMessages().size() > 1) {
            String errmsg = "Zero or  one UserMessage is exptected!";
            mlog.logError(l, errmsg, null);
            throw new EBMSError(EBMSErrorCode.InvalidHeader, getMessageId(msgHeader), errmsg);
        }

        return msgHeader;

    }

    private PMode getProcessingMode(SoapMessage msg, Messaging msgHeader) throws EBMSError {
        long l = mlog.logStart();
        boolean requestor = MessageUtils.isRequestor(msg);
        QName sv = (requestor ? SoapFault.FAULT_CODE_SERVER : SoapFault.FAULT_CODE_CLIENT);

        PMode pmd = msg.getExchange().get(PMode.class); // 
        // if pmd not in exchange - then this should be user message            
        if (pmd == null && requestor) {
            String errmsg = "Invalid ebms configuration! Set PMode to exchange: as ' client.getRequestContext().put(PMode.class.getName(), pmod)'";
            mlog.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(SOAPExceptionCode.StoreInboundMailFailure, sv, errmsg);
        }
        if (pmd == null) {

            // if user message 
            if (!msgHeader.getUserMessages().isEmpty()) {
                CollaborationInfo ca = msgHeader.getUserMessages().get(0).getCollaborationInfo();
                if (ca != null) {
                    if (ca == null) {
                        String errmsg = "Missing CollaborationInfo!";
                        mlog.logError(l, errmsg, null);
                        throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);

                    }
                    if (ca.getService() == null || ca.getService().getValue() == null || ca.getService().getValue().isEmpty()) {
                        String errmsg = "Missing 'service' value!";
                        mlog.logError(l, errmsg, null);
                        throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg);
                    }

                    pmd = mPModeManage.getPModeById(ca.getAgreementRef().getPmode());
                    if (pmd == null) {
                        String errmsg = "PMode with id: '" + ca.getAgreementRef().getPmode() + "' not exist!";
                        mlog.logError(l, errmsg, null);
                        throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg);
                    }

                } else {
                    String errmsg = "Missing userMessage! In a SVEV-MSH  pull-MEP is not exepected!";
                    mlog.logError(l, errmsg, null);
                    throw new EBMSError(EBMSErrorCode.ProcessingModeMismatch, null, errmsg);
                }
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
        Properties cp = SecurityProperties.getInstance().getVerifySignProperties(sc.getX509().getSignature().getCertificate().getAlias());
        inProps.put(cpropname, cp);

        inProps.put(WSHandlerConstants.SIG_VER_PROP_REF_ID, cpropname);
        //inProps.put(WSHandlerConstants.SIG_PROP_REF_ID, cpropname);

        wssInterceptor.setProperties(inProps);
        wssInterceptor.handleMessage(msg);

        // check signed elements // todo for attachments
        if (sc.getX509() != null && sc.getX509().getSignature() != null
                && sc.getX509().getSignature().getSign() != null
                && sc.getX509().getSignature().getSign().getElements() != null
                && sc.getX509().getSignature().getSign().getElements().getXPaths().size() > 0) {
            Map<String, String> prefixes = new HashMap<>();
            List<CryptoCoverageChecker.XPathExpression> xpaths = new ArrayList<>();
            int i = 0;
            for (References.Elements.XPath el : sc.getX509().getSignature().getSign().getElements().getXPaths()) {
                for (References.Elements.XPath.Namespace ns : el.getNamespaces()) {
                    prefixes.put(ns.getPrefix(), ns.getNamespace());
                }
                xpaths.add(new CryptoCoverageChecker.XPathExpression(el.getXpath(), CryptoCoverageUtil.CoverageType.SIGNED,
                        CryptoCoverageUtil.CoverageScope.ELEMENT));

                i++;
            }
            checker = new CryptoCoverageChecker(prefixes, xpaths);

            checker.handleMessage(msg);
        }
    }

    @Override
    public void handleFault(SoapMessage message) {
        super.handleFault(message);
        System.out.println("todo: Handle Fault message!");
    }

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

}
