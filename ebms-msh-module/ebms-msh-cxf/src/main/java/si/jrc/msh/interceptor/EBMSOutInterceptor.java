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
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.References;
import org.msh.svev.pmode.Security;
import org.msh.svev.pmode.X509;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cert.SEDCertificate;
import si.jrc.msh.client.sec.MSHKeyPasswordCallback;
import si.jrc.msh.exception.EBMSError;
import si.jrc.msh.exception.EBMSErrorCode;
import si.jrc.msh.utils.EBMSUtils;
import si.sed.commons.exception.ExceptionUtils;
import si.sed.commons.exception.SOAPExceptionCode;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.GZIPUtil;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.sec.KeystoreUtils;

/**
 * Sets up the outgoing chain to build a ebms 3.0 (AS4) form message. First it
 * will create Messaging object according pmode configuratin added as
 * "PMode.class" param in message context. For user message attachments are
 * added (and compressed according to pmode settings ) In the end encryption and
 * security interceptors are configured.
 *
 * @author Jože Rihtaršič
 */
public class EBMSOutInterceptor extends AbstractEBMSInterceptor {

    /**
     * Logger for EBMSOutInterceptor class
     */
    protected final static SEDLogger LOG = new SEDLogger(EBMSOutInterceptor.class);

    /**
     * ebms message tools for converting between ebms and ebms-sed message
     * entity
     */
    protected final EBMSUtils mEBMSUtil = new EBMSUtils();

    /**
     * GZIP utils
     */
    protected final GZIPUtil mGZIPUtils = new GZIPUtil();

    /**
     * Keystore tools
     */
    private final KeystoreUtils mKSUtis = new KeystoreUtils();

    /**
     * Contstructor EBMSOutInterceptor for setting instance in a phase
     * Phase.PRE_PROTOCOL
     */
    public EBMSOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }

    /**
     * Method transforms message to ebMS 3.0 (AS4) message form and sets
     * signature and encryption interceptors.
     *
     * @param msg: SoapMessage handled in CXF bus
     */
    @Override
    public void handleMessage(SoapMessage msg) {
        long l = LOG.logStart(msg);
        LOG.log("handleMessage");

        // --------------------------------------
        // validate outgoing message
        SoapVersion version = msg.getVersion();
        boolean isRequest = MessageUtils.isRequestor(msg);
        QName sv = (isRequest ? SoapFault.FAULT_CODE_CLIENT : SoapFault.FAULT_CODE_SERVER);

        if (version.getVersion() != 1.2) {
            String errmsg = "ebMS AS4 supports only soap 1.2 protocol!";
            LOG.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(SOAPExceptionCode.SoapVersionMismatch, sv, errmsg);
        }

        if (msg.getContent(SOAPMessage.class) == null) {
            String errmsg = "Internal error missing SOAPMessage!";
            LOG.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(SOAPExceptionCode.InternalFailure, sv, errmsg);
        }

        PMode pmd
                = msg.getExchange().get(PMode.class) == null ? (PMode) msg.getExchange().get(
                PMode.class.getName()) : msg.getExchange().get(PMode.class);

        if (pmd == null) {
            String errmsg = "Missing PMode configuration for: " + (isRequest ? "Request" : "Response");
            LOG.logError(l, errmsg, null);
            throw ExceptionUtils.createSoapFault(SOAPExceptionCode.ConfigurationFailure, sv, errmsg);

        }

        MSHOutMail outMail
                = msg.getExchange().get(MSHOutMail.class) == null ? (MSHOutMail) msg.getExchange().get(
                MSHOutMail.class.getName()) : msg.getExchange().get(MSHOutMail.class);

        SignalMessage signal = msg.getExchange().get(SignalMessage.class);
        try {
            // set attachment for wss signature!
            setAttachments(msg, outMail, true);
        } catch (StorageException ex) {
            LOG.logError(l, "Error adding attachments to soap", ex);
        }

        // MshIncomingMail inMail = msg.getExchange().get(MshIncomingMail.class);
        // EBMSError err = msg.getExchange().get(EBMSError.class);
        // Messaging mgsInboundMessage = msg.getExchange().get(Messaging.class);
        // SVEVEncryptionKey msgSvevKey = msg.getExchange().get(SVEVEncryptionKey.class);
        // File fInMessageRequest = (File)
        // msg.getExchange().get(EbMSConstants.ContextProperty_In_SOAP_Message_File);
        // boolean isRequestor = isRequestor(msg);
        // if sending usermessage, svevkey or as4 receipt -> pmode is mandatory
        // create MESSAGING
        Messaging msgHeader = mEBMSUtil.createMessaging(version);
        // add user message
        if (outMail != null) {
            outMail.setSentDate(Calendar.getInstance().getTime()); // reset sent date
            UserMessage um;
          try {
            um =
                mEBMSUtil.createUserMessage(pmd, outMail,
                    Utils.getDomainFromAddress(outMail.getSenderEBox()), outMail.getSentDate());
            msgHeader.getUserMessages().add(um);
          } catch (EBMSError ex) {
            
            LOG.logError(l, ex.getSubMessage(), ex);
            throw ExceptionUtils.createSoapFault(SOAPExceptionCode.ConfigurationFailure, sv, ex.getSubMessage());
          }
            
        }
        if (signal != null) {
            msgHeader.getSignalMessages().add(signal);
        }

        // add error signal
        EBMSError err = msg.getExchange().get(EBMSError.class);

        if (err != null) {
            SignalMessage sm
                    = mEBMSUtil.generateErrorSignal(err, getSettings().getDomain(), Calendar.getInstance()
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
            LOG.logError(l, "Error adding ebms header to soap", ex);
        }

        // if out mail add security / f
        if (pmd.getLegs().get(0).getSecurity() != null) {
            Security scPolicy = pmd.getLegs().get(0).getSecurity();
            WSS4JOutInterceptor sc;
            try {
                sc = configureSecurityInterceptors(scPolicy, outMail!= null?outMail.getMessageId():null);
                sc.handleMessage(msg);
            } catch (EBMSError ex) {
                LOG.logError(l, "Error security handling of ebms header to soap", ex);
            }
            
        } else {
            LOG.log("No Security policy for message: '" + (outMail != null ? outMail.getId() : "null")
                    + "' pmode: " + pmd.getId() + "!");
        }
        LOG.logEnd(l);
    }

    /**
     * Method creates signature property configuration for WSS4JOutInterceptor
     * inteceptor
     *
     * @param sc
     * @return
     */
    private Map<String, Object> createSignatureConfiguration(X509.Signature sig, String messageId) throws EBMSError {
        long l = LOG.logStart();
        Map<String, Object> prps = null;

        if (sig == null || sig.getSign() == null) {
            return prps;
        }
        References ref = sig.getSign();
        String alias = sig.getSign().getSignCertAlias();
        String strReference = getReferenceString(ref, messageId);

        // create signature priperties
        String cpropname = "SIG." + UUID.randomUUID().toString();
        SEDCertStore cs = getLookups().getSEDCertStoreByCertAlias(alias, true);
        SEDCertificate aliasCrt =getLookups().getSEDCertificatForAlias(alias, cs, true);
        if ( aliasCrt == null) {
            String msg = "Key for alias '" + alias + "' do not exists!";
            LOG.logError(l, msg, null);
            throw new EBMSError(EBMSErrorCode.BadPModeConfiguration, messageId, msg);
        }

        Properties cp = mKSUtis.getSignProperties(alias, cs);

        prps = new HashMap<>();
        prps.put(cpropname, cp);
        // set wss properties
        prps.put(WSHandlerConstants.ACTION, WSHandlerConstants.SIGNATURE);
        prps.put(WSHandlerConstants.SIGNATURE_PARTS, strReference);
        prps.put(WSHandlerConstants.SIGNATURE_USER, alias);
        prps.put(WSHandlerConstants.PW_CALLBACK_REF,
                new MSHKeyPasswordCallback(aliasCrt));
        prps.put(WSHandlerConstants.SIG_PROP_REF_ID, cpropname);

        if (sig.getAlgorithm() != null || !sig.getAlgorithm().isEmpty()) {
            prps.put(WSHandlerConstants.SIG_ALGO, sig.getAlgorithm());
        }
        if (sig.getHashFunction() != null || !sig.getHashFunction().isEmpty()) {
            prps.put(WSHandlerConstants.SIG_DIGEST_ALGO, sig.getHashFunction());
        }
        if (ref.getKeyIdentifierType() != null && !ref.getKeyIdentifierType().isEmpty()) {
            prps.put(WSHandlerConstants.SIG_KEY_ID, ref.getKeyIdentifierType());
        }

        return prps;
    }

    /**
     * Method creates encryption property configuration for WSS4JOutInterceptor
     * inteceptor
     *
     * @param sc
     * @return
     */
    private Map<String, Object> createEncryptionConfiguration(X509.Encryption enc, String messageId) throws EBMSError {
        long l = LOG.logStart();
        Map<String, Object> prps = null;

        if (enc == null || enc.getEncrypt() == null) {
            return prps;
        }
        References ref = enc.getEncrypt();
        String alias = enc.getEncrypt().getEncCertAlias();
        String strReference = getReferenceString(ref, messageId);

        // create signature priperties
        String cpropname = "ENC." + UUID.randomUUID().toString();
        // ecrypt with public key
        SEDCertStore cs = getLookups().getSEDCertStoreByCertAlias(alias, false);
        SEDCertificate aliasCrt = null;
        if (cs != null) {
          
            for (SEDCertificate crt : cs.getSEDCertificates()) {
                if (alias.equals(crt.getAlias())) {
                    aliasCrt = crt;
                    break;
                }
            }
        }

        if ( aliasCrt == null) {
            String msg = "Ecryptiong cert for alias '" + alias + "' do not exists!";
            LOG.logError(l, msg, null);
            throw new EBMSError(EBMSErrorCode.BadPModeConfiguration, messageId, msg);
        }

        Properties cp = mKSUtis.getSignProperties(alias, cs);

        prps = new HashMap<>();
        prps.put(cpropname, cp);
        // set wss properties
        prps.put(WSHandlerConstants.ACTION, WSHandlerConstants.ENCRYPT);
        prps.put(WSHandlerConstants.ENCRYPTION_PARTS, strReference);
        prps.put(WSHandlerConstants.ENCRYPTION_USER, alias);
        prps.put(WSHandlerConstants.ENC_PROP_REF_ID, cpropname);

        if (enc.getAlgorithm() != null || !enc.getAlgorithm().isEmpty()) {
            prps.put(WSHandlerConstants.ENC_SYM_ALGO, enc.getAlgorithm());
        }
        if (ref.getKeyIdentifierType() != null && !ref.getKeyIdentifierType().isEmpty()) {
            prps.put(WSHandlerConstants.ENC_KEY_ID, ref.getKeyIdentifierType());
        }

        return prps;
    }

    private String getReferenceString(References ref, String messageId) throws EBMSError {
        long l = LOG.logStart();
        StringWriter elmWr = new StringWriter();
        if (ref.getElements() != null
                && ref.getElements().getXPaths().size() > 0) {

            for (References.Elements.XPath el : ref.getElements().getXPaths()) {
                String[] lst = el.getXpath().split("/");
                if (lst.length > 0) {
                    String xpath = lst[lst.length - 1];
                    String[] nslst = xpath.split(":");
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
                        String msg = "Bad xpath definition: '" + xpath + "'. ";
                        LOG.logError(l, msg, null);
                        throw new EBMSError(EBMSErrorCode.BadPModeConfiguration, messageId, msg);
                    }
                }
            }
        }
        if (ref.getAllAttachments()) {
            elmWr.write("{}cid:Attachments;");
        }
        LOG.logEnd(l);
        return elmWr.toString();
    }

    public WSS4JOutInterceptor configureSecurityInterceptors(Security sc, String msgId) throws EBMSError {
        long l = LOG.logStart();
        WSS4JOutInterceptor sec = null;
        Map<String, Object> outProps = null;

        if (sc.getX509() == null) {
            LOG.logWarn(l, "Sending not message with not security policy. No security configuration (pmode) for message:" + msgId, null);
            return null;
        }

        if (sc.getX509().getSignature() != null && sc.getX509().getSignature().getSign() != null) {
            X509.Signature sig = sc.getX509().getSignature();
            outProps = createSignatureConfiguration(sig, msgId);
            if (outProps == null){
              LOG.logWarn(l, "Sending not signed message. Incomplete configuration: X509/Signature for message:  " + msgId, null);
            }
        } else {
          LOG.logWarn(l, "Sending not signed message. No configuration: X509/Signature/Sign for message:  " + msgId, null);
        }

        if (sc.getX509().getEncryption() != null && sc.getX509().getEncryption().getEncrypt() != null) {
            X509.Encryption enc = sc.getX509().getEncryption();
            Map<String, Object> penc = createEncryptionConfiguration(enc, msgId);
            if (enc == null){
              LOG.logWarn(l, "Sending not encrypted message. Incomplete configuration: X509/Encryption/Encryp for message:  " + msgId, null);
            }
            else if (outProps == null) {
                outProps = penc;
            } else {
                String action = (String) outProps.get(WSHandlerConstants.ACTION);
                action += " " + (String) penc.get(WSHandlerConstants.ACTION);
                outProps.putAll(penc);
                outProps.put(WSHandlerConstants.ACTION, action);
            }
        } else {
          LOG.logWarn(l, "Sending not encypted message. No configuration: X509/Encryption/Encrypt for message:  " + msgId, null);
        }

        if (outProps != null) {
          LOG.log("Set security parameters");
          for (String key : outProps.keySet()) {
            LOG.log(key + ": " + outProps.get(key));
}
            sec = new WSS4JOutInterceptor(outProps);
        } else {
           LOG.logWarn(l, "Sending not message with not security policy. Bad/incomplete security configuration (pmode) for message:" + msgId, null);
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
    private void setAttachments(SoapMessage msg, MSHOutMail mail, boolean compress)
            throws StorageException {
        long l = LOG.logStart();
        if (mail != null && mail.getMSHOutPayload() != null
                && !mail.getMSHOutPayload().getMSHOutParts().isEmpty()) {

            msg.setAttachments(new ArrayList<>(mail.getMSHOutPayload().getMSHOutParts().size()));
            for (MSHOutPart p : mail.getMSHOutPayload().getMSHOutParts()) {
                String id = UUID.randomUUID().toString();
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
                        String msgErr
                                = "Error compressing attachment: " + fatt.getAbsolutePath() + " for mail: "
                                + p.getId();
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
