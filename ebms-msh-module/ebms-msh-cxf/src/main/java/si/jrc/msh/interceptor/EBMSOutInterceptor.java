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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.msh.svev.pmode.Certificate;
import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.References;
import org.msh.svev.pmode.Security;
import org.msh.svev.pmode.X509;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cert.SEDCertificate;
import si.jrc.msh.client.sec.SimplePasswordCallback;
import si.jrc.msh.exception.EBMSError;
import si.jrc.msh.utils.EBMSUtils;
import si.sed.commons.exception.ExceptionUtils;
import si.sed.commons.exception.SOAPExceptionCode;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.sec.KeystoreUtils;

/**
 *
 * @author Jože Rihtaršič
 */
public class EBMSOutInterceptor extends AbstractEBMSInterceptor {

    protected final SEDLogger mlog = new SEDLogger(EBMSOutInterceptor.class);

    EBMSUtils mEBMSUtil = new EBMSUtils();

    public EBMSOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addAfter(SAAJOutInterceptor.class.getName());

    }

    @Override
    public void handleMessage(SoapMessage msg) {
        long l = mlog.logStart(msg);

        SoapVersion version = msg.getVersion();
        boolean isRequest = MessageUtils.isRequestor(msg);
        QName sv = (isRequest ? SoapFault.FAULT_CODE_CLIENT : SoapFault.FAULT_CODE_SERVER);

        if (version.getVersion() != 1.2) {
            String errmsg = "ebMS AS4 supports only soap 1.2 protocol!";
            mlog.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(SOAPExceptionCode.SoapVersionMismatch, sv, errmsg);
        }

        if (msg.getContent(SOAPMessage.class) == null) {
            String errmsg = "Internal error missing SOAPMessage!";
            mlog.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(SOAPExceptionCode.InternalFailure, sv, errmsg);
        }

        PMode pmd = msg.getExchange().get(PMode.class) == null ? (PMode) msg.getExchange().get(PMode.class.getName()) : msg.getExchange().get(PMode.class);

        if (pmd == null) {
            String errmsg = "Missing PMode configuration for: " + (isRequest ? "Request" : "Response");
            mlog.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(SOAPExceptionCode.ConfigurationFailure, sv, errmsg);

        }

        MSHOutMail outMail = msg.getExchange().get(MSHOutMail.class) == null ? (MSHOutMail) msg.getExchange().get(MSHOutMail.class.getName()) : msg.getExchange().get(MSHOutMail.class);

        SignalMessage signal = msg.getExchange().get(SignalMessage.class);
        try {
            // set attachment fpr wss signature!
            setAttachments(msg, outMail);
        } catch (StorageException ex) {
            mlog.logError(l, "Error adding attachments to soap", ex);
        }

        // MshIncomingMail inMail = msg.getExchange().get(MshIncomingMail.class);
        //EBMSError err = msg.getExchange().get(EBMSError.class);
        //Messaging mgsInboundMessage = msg.getExchange().get(Messaging.class);
        //SVEVEncryptionKey msgSvevKey = msg.getExchange().get(SVEVEncryptionKey.class);
        //File fInMessageRequest = (File) msg.getExchange().get(EbMSConstants.ContextProperty_In_SOAP_Message_File);
        //boolean isRequestor = isRequestor(msg);
        // if sending usermessage, svevkey or as4 receipt -> pmode is mandatory
        // create  MESSAGING
        Messaging msgHeader = mEBMSUtil.createMessaging(version);
        // add user message
        if (outMail != null) {
            outMail.setSentDate(Calendar.getInstance().getTime()); // reset sent date
            UserMessage um = mEBMSUtil.createUserMessage(pmd, outMail, Utils.getDomainFromAddress(outMail.getSenderEBox()), outMail.getSentDate());
            msgHeader.getUserMessages().add(um);
        }
        if (signal != null) {
            msgHeader.getSignalMessages().add(signal);
        }

        // add error signal
        EBMSError err = msg.getExchange().get(EBMSError.class);

        if (err != null) {
            SignalMessage sm = mEBMSUtil.generateErrorSignal(err, getSettings().getDomain(), Calendar.getInstance().getTime());

            msgHeader.getSignalMessages().add(sm);
        }
        // add svev signal
        // add error
        /*  if (msgSvevKey != null) {
         SignalMessage sm = mEBMSUtil.generateSVEVKeySignal(msgSvevKey, mSettings.getDomain());
         sm.getMessageInfo().setRefToMessageId(mgsInboundMessage.getUserMessages().get(0).getMessageInfo().getMessageId());
         msgHeader.getSignalMessages().add(sm);
         }*/
        try {
            SOAPMessage request = msg.getContent(SOAPMessage.class);
            SOAPHeader sh = request.getSOAPHeader();
            Marshaller marshaller = JAXBContext.newInstance(Messaging.class).createMarshaller();
            marshaller.marshal(msgHeader, sh);
            request.saveChanges();
        } catch (JAXBException | SOAPException ex) {
            mlog.logError(l, "Error adding ebms header to soap", ex);
        }

        // if out mail add security / f
        if (pmd.getLegs().get(0).getSecurity() != null) {
            Security scPolicy = pmd.getLegs().get(0).getSecurity();
            WSS4JOutInterceptor sc = configureSecurityInterceptors(scPolicy);
            sc.handleMessage(msg);
        } else {
            mlog.log("No Security policy for message: '" + outMail.getId().toString() + "' pmode: " + pmd.getId() + "!");
        }
        mlog.logEnd(l);
    }

    /**
     * Method sets security configuration to WSS4JOutInterceptor inteceptor
     *
     * @param sc
     * @return
     */
    public WSS4JOutInterceptor configureSecurityInterceptors(Security sc) {
        long l = mlog.logStart();
        WSS4JOutInterceptor sec = null;
        Map<String, Object> outProps = new HashMap<>();

        if (sc.getX509() != null && sc.getX509().getSignature() != null
                && sc.getX509().getSignature().getSign() != null) {
            X509.Signature sig = sc.getX509().getSignature();
            String alias = sig.getSign().getSignCertAlias();
            References sign = sig.getSign();
            Certificate c = sig.getCertificate();
            StringWriter elmWr = new StringWriter();
            if (sign.getSignElements()
                    && sign.getElements() != null
                    && sign.getElements().getXPaths().size() > 0) {
                for (References.Elements.XPath el : sign.getElements().getXPaths()) {
                    String[] lst = el.getXpath().split("/");
                    if (lst.length > 0) {
                        String[] nslst = lst[lst.length - 1].split(":");
                        if (nslst.length == 1) {
                            elmWr.write(";");
                            elmWr.write("{Element}");
                            elmWr.write(nslst[0]);
                            elmWr.write(";");
                        }
                        if (nslst.length == 2) {
                            elmWr.write("{Element}");
                            elmWr.write("{");

                            for (References.Elements.XPath.Namespace n : el.getNamespaces()) {
                                if (n.getPrefix().equals(nslst[0])) {
                                    elmWr.write(n.getNamespace());
                                    elmWr.write("}");
                                }
                            }
                            elmWr.write(nslst[1]);
                            elmWr.write(";");
                        } else {
                            mlog.logError(l, "bad xpath definition!", null);
                        }
                    }
                }
            }
            if (sig.getSign().getSignAttachments()) {
                elmWr.write("{}cid:Attachments;");
            }

            // create signature priperties
            String cpropname = "CP." + UUID.randomUUID().toString();
            SEDCertStore cs = getLookups().getSEDCertStoreByCertAlias(alias, true);
            SEDCertificate aliasCrt = null;
            if (cs != null) {
                for (SEDCertificate crt : cs.getSEDCertificates()) {
                    if (crt.isKeyEntry() && alias.equals(crt.getAlias())) {
                        aliasCrt = crt;
                        break;
                    }
                }
            }
            if (cs == null || aliasCrt == null) {
                mlog.logError(l, "Key for alias '" + alias + "' do not exists!", null);
                // TODO throw error
                return null;
            }
            //Properties cp = CertificateUtils.getInstance().getVerifySignProperties();
            Properties cp = KeystoreUtils.getSignProperties(alias, cs);
            //Properties cp = CertificateUtils.getInstance().getSignProperties(alias);
            outProps.put(cpropname, cp);
            // set wss properties
            outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
            outProps.put(WSHandlerConstants.SIGNATURE_PARTS, elmWr.toString());
            outProps.put(WSHandlerConstants.SIGNATURE_USER, alias);
            outProps.put(WSHandlerConstants.USER, alias);

            outProps.put(WSHandlerConstants.PW_CALLBACK_REF, new SimplePasswordCallback(aliasCrt.getKeyPassword()));

            outProps.put(WSHandlerConstants.SIG_PROP_REF_ID, cpropname);

            if (sig.getAlgorithm() != null || !sig.getAlgorithm().isEmpty()) {
                outProps.put(WSHandlerConstants.SIG_ALGO, sig.getAlgorithm());
            }
            if (sig.getHashFunction() != null || !sig.getHashFunction().isEmpty()) {
                outProps.put(WSHandlerConstants.SIG_DIGEST_ALGO, sig.getHashFunction());
            }
            sec = new WSS4JOutInterceptor(outProps);
        }
        mlog.logEnd(l);
        return sec;
    }

    /**
     * Method sets attachments to outgoing ebmsUserMessage.
     *
     * @param msg - SAOP message
     * @param mail - MSH out mail
     * @throws StorageException
     */
    private void setAttachments(SoapMessage msg, MSHOutMail mail) throws StorageException {
        if (mail != null && mail.getMSHOutPayload() != null && !mail.getMSHOutPayload().getMSHOutParts().isEmpty()) {

            msg.setAttachments(new ArrayList<>(mail.getMSHOutPayload().getMSHOutParts().size()));
            for (MSHOutPart p : mail.getMSHOutPayload().getMSHOutParts()) {
                String id = UUID.randomUUID().toString();
                p.setEbmsId(id);
                AttachmentImpl att = new AttachmentImpl(p.getEbmsId());
                att.setHeader("id", id);
                DataHandler dh = new DataHandler(new FileDataSource(StorageUtils.getFile(p.getFilepath())));
                att.setDataHandler(dh);
                msg.getAttachments().add(att);
            }
        }

    }

    @Override
    public void handleFault(SoapMessage message) {
        super.handleFault(message); //To change body of generated methods, choose Tools | Templates.
        mlog.log("handleFault 1");
    }

}
