/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.ws;

import java.io.File;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.interceptor.Interceptors;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jws.WebService;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.msh.svev.pmode.PMode;
import org.sed.ebms.GetInMailRequest;
import org.sed.ebms.GetInMailResponse;
import org.sed.ebms.InMailEventListRequest;
import org.sed.ebms.InMailEventListResponse;
import org.sed.ebms.InMailListRequest;
import org.sed.ebms.InMailListResponse;
import org.sed.ebms.ModifyInMailRequest;
import org.sed.ebms.ModifyInMailResponse;
import org.sed.ebms.OutMailEventListRequest;
import org.sed.ebms.OutMailEventListResponse;
import org.sed.ebms.OutMailListRequest;
import org.sed.ebms.OutMailListResponse;
import org.sed.ebms.SEDException;
import org.sed.ebms.SEDExceptionCode;
import org.sed.ebms.SEDException_Exception;
import org.sed.ebms.SEDMailBoxWS;
import org.sed.ebms.SubmitMailRequest;
import org.sed.ebms.SubmitMailResponse;
import org.sed.ebms.inbox.event.InEvent;
import org.sed.ebms.inbox.mail.InMail;
import org.sed.ebms.inbox.payload.InPart;
import org.sed.ebms.outbox.mail.OutMail;
import org.sed.ebms.outbox.event.OutEvent;
import org.sed.ebms.outbox.payload.OutPart;
import org.sed.ebms.rcontrol.RControl;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.SEDValues;
import si.sed.commons.exception.HashException;
import si.sed.commons.exception.SVEVReturnValue;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.HashUtils;
import si.sed.commons.utils.PModeManager;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.Utils;

@Interceptors(JEELogInterceptor.class)
@WebService(serviceName = "SEDMailBoxWS", portName = "SEDMailBoxWSPort",
        endpointInterface = "org.sed.ebms.SEDMailBoxWS",
        targetNamespace = "http://ebms.sed.org/",
        wsdlLocation = "WEB-INF/wsdl/sed-mailbox.wsdl")
public class SEDMailBox implements SEDMailBoxWS {

    @Resource
    private UserTransaction userTransaction;

    @PersistenceContext
    private EntityManager entityManager;

    PModeManager mpModeManager = new PModeManager();
    HashUtils mpHU = new HashUtils();
    StorageUtils msuStorageUtils = new StorageUtils();
    SimpleDateFormat msdfDDMMYYYY_HHMMSS = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    /**
     * - validate message -> serialize message -> send message to ebms queue
     *
     * @param submitMailRequest
     * @return
     * @throws SEDException_Exception
     */
    @Override
    public SubmitMailResponse submitMail(SubmitMailRequest submitMailRequest) throws SEDException_Exception {

        OutMail mail = submitMailRequest.getData().getOutMail();

        // validate data
        SEDException se = vaildateMail(mail);
        if (se != null) {
            throw new SEDException_Exception("Invalid mail data", se);
        }
        // check if mail exists
        mailExists(mail);

        // get pmode
        String recDomain = mail.getReceiverEBox().substring(mail.getReceiverEBox().indexOf("@") + 1).trim();
        String sendDomain = mail.getSenderEBox().substring(mail.getSenderEBox().indexOf("@") + 1).trim();
        String pmodeId = mail.getService() + ":" + sendDomain + ":" + recDomain;

        PMode pmode = mpModeManager.getPModeById(pmodeId);
        if (pmode == null) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.P_MODE_NOT_EXISTS);
            msherr.setMessage(String.format("PMode '%s' not exist! Check PMode configuration. (pmodeId=[service:senderDomain:receiverDomain])", pmodeId));
            throw new SEDException_Exception("Missing pMode configuration or bad sender or receiver e-box, or bad service", msherr);
        }
        // serialize payload to cache FS and data to db
        serializeMail(mail, submitMailRequest.getControl().getUserId(), submitMailRequest.getControl().getApplicationId(), pmodeId);
        // submit to ebms que
        sendMessage(mail.getId(), pmodeId);
        //generate response 
        SubmitMailResponse rsp = new SubmitMailResponse();
        rsp.setRControl(new RControl());
        rsp.getRControl().setReturnValue(SVEVReturnValue.OK.getValue());

        rsp.setRData(new SubmitMailResponse.RData());
        rsp.getRData().setSubmitDate(mail.getSubmitedDate());
        rsp.getRData().setSenderMessageId(mail.getSenderMessageId());
        rsp.getRData().setMailId(mail.getId());

        return rsp;
    }

    @Override
    public OutMailListResponse getOutMailList(OutMailListRequest outMailListRequest) throws SEDException_Exception {
        OutMailListResponse resp = new OutMailListResponse();
        resp.setRData(new OutMailListResponse.RData());
        try {
            TypedQuery<OutMail> q = entityManager.createNamedQuery(NamedQueries.SED_NQ_OUTMAIL_GET_LIST, OutMail.class);
            List<OutMail> lst = q.getResultList();
            if (!lst.isEmpty()) {
                resp.getRData().getOutMails().addAll(lst);
            }

        } catch (NoResultException ex) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
            msherr.setMessage(ex.getMessage());
            throw new SEDException_Exception(ex.getMessage(), msherr, ex);
        }
        return resp;
    }

    @Override
    public InMailListResponse getInMailList(InMailListRequest intMailListRequest) throws SEDException_Exception {
        InMailListResponse resp = new InMailListResponse();
        resp.setRData(new InMailListResponse.RData());
        try {
            TypedQuery<InMail> q = entityManager.createNamedQuery(NamedQueries.SED_NQ_INMAIL_GET_LIST, InMail.class);
            List<InMail> lst = q.getResultList();
            if (!lst.isEmpty()) {
                resp.getRData().getInMails().addAll(lst);
            }

        } catch (NoResultException ex) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
            msherr.setMessage(ex.getMessage());
            throw new SEDException_Exception(ex.getMessage(), msherr, ex);
        }
        return resp;
    }

    @Override
    public InMailEventListResponse getInMailEventList(InMailEventListRequest p) throws SEDException_Exception {
        InMailEventListResponse resp = new InMailEventListResponse();
        resp.setRData(new InMailEventListResponse.RData());

        InMailEventListRequest.Data dt = p.getData();
        if (dt == null) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.MISSING_DATA);
            String msg = "Data in required element!";
            msherr.setMessage(msg);
            throw new SEDException_Exception(msg, msherr, null);
        }

        if (dt.getReceiverEBox() == null) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.MISSING_DATA);
            String msg = "receiverEBox in required attribute!";
            msherr.setMessage(msg);
            throw new SEDException_Exception(msg, msherr, null);
        }

        if (dt.getMailId() == null) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.MISSING_DATA);
            String msg = "mail id in required!";
            msherr.setMessage(msg);
            throw new SEDException_Exception(msg, msherr, null);
        }

        try {
            TypedQuery<InEvent> q = entityManager.createNamedQuery(NamedQueries.SED_NQ_INMAIL_GET_EVENTS, InEvent.class);
            q.setParameter(NamedQueries.NQ_PARAM_RECEIVER_EBOX, dt.getReceiverEBox());
            q.setParameter(NamedQueries.NQ_PARAM_MAIL_ID, dt.getMailId());

            List<InEvent> lst = q.getResultList();
            if (!lst.isEmpty()) {
                resp.getRData().getInEvents().addAll(lst);
            }

        } catch (NoResultException ex) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
            msherr.setMessage(ex.getMessage());
            throw new SEDException_Exception(ex.getMessage(), msherr, ex);
        }
        return resp;
    }

    @Override
    public OutMailEventListResponse getOutMailEventList(OutMailEventListRequest p) throws SEDException_Exception {
        OutMailEventListResponse resp = new OutMailEventListResponse();
        resp.setRData(new OutMailEventListResponse.RData());

        OutMailEventListRequest.Data dt = p.getData();
        if (dt == null) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.MISSING_DATA);
            String msg = "Data in required element!";
            msherr.setMessage(msg);
            throw new SEDException_Exception(msg, msherr, null);
        }

        if (dt.getSenderEBox() == null) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.MISSING_DATA);
            String msg = "SenderEBox in required attribute!";
            msherr.setMessage(msg);
            throw new SEDException_Exception(msg, msherr, null);
        }

        if (dt.getMailId() == null && dt.getSenderMessageId() == null) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.MISSING_DATA);
            String msg = "mail id or senderMessageId in required!";
            msherr.setMessage(msg);
            throw new SEDException_Exception(msg, msherr, null);
        }

        try {
            TypedQuery<OutEvent> q = entityManager.createNamedQuery(NamedQueries.SED_NQ_OUTMAIL_GET_EVENTS, OutEvent.class);
            q.setParameter(NamedQueries.NQ_PARAM_SENDER_EBOX, dt.getSenderEBox());
            q.setParameter(NamedQueries.NQ_PARAM_MAIL_ID, dt.getMailId());
            q.setParameter(NamedQueries.NQ_PARAM_SENDER_MAIL_ID, dt.getSenderMessageId() == null ? "" : dt.getSenderMessageId());
            List<OutEvent> lst = q.getResultList();
            if (!lst.isEmpty()) {
                resp.getRData().getOutEvents().addAll(lst);
            }

        } catch (NoResultException ex) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
            msherr.setMessage(ex.getMessage());
            throw new SEDException_Exception(ex.getMessage(), msherr, ex);
        }
        return resp;
    }

    @Override
    public ModifyInMailResponse modifyInMail(ModifyInMailRequest p) throws SEDException_Exception {
        ModifyInMailRequest.Data d = p.getData();

        ModifyInMailResponse rsp = new ModifyInMailResponse();
        rsp.setRData(new ModifyInMailResponse.RData());

        InMail im;
        try {
            TypedQuery<InMail> q = entityManager.createNamedQuery(NamedQueries.SED_NQ_INMAIL_GET_BY_ID_AND_RECBOX, InMail.class);
            q.setParameter(NamedQueries.NQ_PARAM_MAIL_ID, d.getMailId());
            q.setParameter(NamedQueries.NQ_PARAM_RECEIVER_EBOX, d.getReceiverEBox());
            im = q.getSingleResult();

            Date dt = Calendar.getInstance().getTime();

            im.setDeliveredDate(dt);
            im.setStatusDate(dt);
            im.setStatus(SEDInboxMailStatus.DELIVERED.getValue());
            InEvent ie =  setInMailStatus(im, SEDInboxMailStatus.DELIVERED.getDesc(), p.getControl().getUserId(), p.getControl().getApplicationId());
            rsp.getRData().setInEvent(ie);

        } catch (NoResultException ignore) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.MAIL_WITH_ID_ALREADY_SENT);
            String message = String.format("Mail with id '%s' and  ebox: %s not exists!", d.getMailId().toString(), d.getReceiverEBox());
            msherr.setMessage(message);
            throw new SEDException_Exception(message, msherr);
        }
        return rsp;
    }

    @Override
    public GetInMailResponse getInMail(GetInMailRequest p) throws SEDException_Exception {

        GetInMailRequest.Data d = p.getData();

        GetInMailResponse rsp = new GetInMailResponse();
        rsp.setRData(new GetInMailResponse.RData());

        InMail im;
        try {
            TypedQuery<InMail> q = entityManager.createNamedQuery(NamedQueries.SED_NQ_INMAIL_GET_BY_ID_AND_RECBOX, InMail.class);
            q.setParameter(NamedQueries.NQ_PARAM_MAIL_ID, d.getMailId());
            q.setParameter(NamedQueries.NQ_PARAM_RECEIVER_EBOX, d.getReceiverEBox());
            im = q.getSingleResult();

            if (im.getInPayload() != null && !im.getInPayload().getInParts().isEmpty()) {
                for (InPart ip : im.getInPayload().getInParts()) {
                    try {
                        ip.setValue(msuStorageUtils.getByteArray(ip.getFilepath()));
                    } catch (StorageException ingore) {

                    }
                }
            }

            rsp.getRData().setInMail(im);
        } catch (NoResultException ignore) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.MAIL_WITH_ID_ALREADY_SENT);
            String message = String.format("Mail with id '%s' and  ebox: %s not exists!", d.getMailId().toString(), d.getReceiverEBox());
            msherr.setMessage(message);
            throw new SEDException_Exception(message, msherr);
        }
        return rsp;

    }

    private void mailExists(OutMail mail) throws SEDException_Exception {

        OutMail omSent;
        try {
            TypedQuery<OutMail> q = entityManager.createNamedQuery(NamedQueries.SED_NQ_OUTMAIL_getByMessageIdAndSenderBox, OutMail.class);
            q.setParameter("sndMsgId", mail.getSenderMessageId());
            q.setParameter("senderBox", mail.getSenderEBox());
            omSent = q.getSingleResult();

        } catch (NoResultException ignore) {
            omSent = null;
        }

        if (omSent != null) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.MAIL_WITH_ID_ALREADY_SENT);
            String message = String.format("Mail with id '%s' already sent: %s with sed id: %d", omSent.getSenderMessageId(), msdfDDMMYYYY_HHMMSS.format(omSent.getSubmitedDate()), omSent.getId());
            msherr.setMessage(message);
            throw new SEDException_Exception(message, msherr);
        }
    }

    private void serializeMail(OutMail mail, String userID, String applicationId, String pmodeId) throws SEDException_Exception {

        // prepare mail to persist 
        Date dt = new Date();
        // set current status
        mail.setStatus(SEDOutboxMailStatus.SUBMITED.getValue());
        mail.setSubmitedDate(dt);
        mail.setStatusDate(dt);
        // --------------------
        // serialize payload
        try {

            if (mail.getOutPayload() != null && !mail.getOutPayload().getOutParts().isEmpty()) {
                for (OutPart p : mail.getOutPayload().getOutParts()) {
                    File fout = null;

                    if (p.getValue() != null) {
                        fout = msuStorageUtils.storeOutFile(p.getMimeType(), p.getValue());
                        // purge binary data
                        p.setValue(null);
                    } else if (!Utils.isEmptyString(p.getFilepath())) {
                        File fIn = new File(p.getFilepath());
                        if (fIn.exists()) {
                            fout = msuStorageUtils.storeOutFile(p.getMimeType(), fIn);
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
        } catch (StorageException ex) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
            msherr.setMessage(ex.getMessage());
            throw new SEDException_Exception("Error occured while storing payload", msherr, ex);
        } catch (HashException ex) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
            msherr.setMessage(ex.getMessage());
            throw new SEDException_Exception("Error occured while calculating payload hash (MD5)", msherr, ex);
        }
        // --------------------
        // serialize data to db
        try {

            userTransaction.begin();

            // persist mail    
            entityManager.persist(mail);

            // persist mail event
            OutEvent me = new OutEvent();
            me.setMailId(mail.getId());
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());
            me.setSenderMessageId(mail.getSenderMessageId());
            me.setUserId(userID);
            me.setApplicationId(applicationId);

            entityManager.persist(me);
            userTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    userTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    // ignore 
                }
                SEDException msherr = new SEDException();
                msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
                msherr.setMessage(ex.getMessage());
                throw new SEDException_Exception("Error occured while storing to DB", msherr, ex);
            }
        }

    }

    private InEvent setInMailStatus(InMail im, String desc, String userID, String applicationId) throws SEDException_Exception {
        InEvent me = new InEvent();
        me.setMailId(im.getId());
        me.setStatus(im.getStatus());
        me.setDate(im.getStatusDate());

        me.setUserId(userID);
        me.setApplicationId(applicationId);
        try {
            userTransaction.begin();
            entityManager.merge(im);
            entityManager.persist(me);
            userTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    userTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    // ignore 
                }
                SEDException msherr = new SEDException();
                msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
                msherr.setMessage(ex.getMessage());
                throw new SEDException_Exception("Error occured while storing to DB", msherr, ex);
            }
        }
        return me;

    }

    private SEDException vaildateMail(OutMail mail) {

        List<String> merrLst = new ArrayList<>();
        
        if (Utils.isEmptyString(mail.getSenderMessageId())) {
            merrLst.add("Missing mail id!");
        }

        if (mail.getOutPayload() == null || mail.getOutPayload().getOutParts().isEmpty()) {
            merrLst.add("No content in mail (Attachment is empty)!");
        }

        int iMP = 0;
        for (OutPart mp : mail.getOutPayload().getOutParts()) {
            iMP++;
            if (Utils.isEmptyString(mp.getMimeType())) {
                merrLst.add("Missing payload mimetype (index:'" + iMP + "')!");
            }
            if (mp.getValue() == null || Utils.isEmptyString(mp.getFilepath())) {
                merrLst.add("Missing payload data (index:'" + iMP + "')!");
            }
        }
        if (Utils.isEmptyString(mail.getReceiverName())) {
            merrLst.add("Missing Receiver name!");
        }

        if (Utils.isEmptyString(mail.getReceiverEBox())) {
            merrLst.add("Missing ReceiverEBox!");
        }
        
        if (Utils.isEmptyString(mail.getSenderName())) {
            merrLst.add("Missing sender name!");
        }
        
        if (Utils.isEmptyString(mail.getSenderEBox())) {
            merrLst.add("Missing sender EBox!");
        }

        
        
        
        if (Utils.isEmptyString(mail.getService())) {
            merrLst.add("Missing service (DeliveryType)!");
        }
        if (Utils.isEmptyString(mail.getAction())) {
            merrLst.add("Missing action!");
        }
        
        if (Utils.isEmptyString(mail.getConversationId())) {
            merrLst.add("Conversation id!");
        }

        
        

        if (!merrLst.isEmpty()) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.MISSING_DATA);
            msherr.setMessage(String.join(", ", merrLst));
            return msherr;
        }

        String mAdr = mail.getReceiverEBox().trim();
        if (!mAdr.contains("@")) {
            merrLst.add("Receiver address: '" + mAdr + "' is invalid!");
        }

        mAdr = mail.getSenderEBox().trim();
        if (!mAdr.contains("@")) {
            merrLst.add("Sender address: '" + mAdr + "' is invalid!");
        }

        if (!merrLst.isEmpty()) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.INVALID_DATA);
            msherr.setMessage(String.join(", ", merrLst));
            return msherr;
        }

        return null;
    }

    public boolean sendMessage(BigInteger biPosiljkaId, String strpModeId) throws SEDException_Exception {

        boolean suc = false;
        InitialContext ic = null;
        Connection connection = null;
        try {
            ic = new InitialContext();
            ConnectionFactory cf = (ConnectionFactory) ic.lookup("/ConnectionFactory");
            Queue queue = (Queue) ic.lookup("java:/jms/" + SEDValues.EBMS_QUEUE_JNDI);
            connection = cf.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer sender = session.createProducer(queue);
            Message message = session.createMessage();
            message.setLongProperty(SEDValues.EBMS_QUEUE_PARAM_MAIL_ID, biPosiljkaId.longValue());
            message.setStringProperty(SEDValues.EBMS_QUEUE_PARAM_PMODE_ID, strpModeId);
            message.setIntProperty(SEDValues.EBMS_QUEUE_PARAM_RETRY, 0);
            message.setLongProperty(SEDValues.EBMS_QUEUE_PARAM_DELAY, 0);
            sender.send(message);
            suc = true;
        } catch (NamingException | JMSException ex) {
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
            msherr.setMessage(ex.getMessage());
            throw new SEDException_Exception("Error occured while submiting mail to ebms queue", msherr, ex);
        } finally {
            if (ic != null) {
                try {
                    ic.close();
                } catch (Exception ignore) {
                }
            }
            closeConnection(connection);
        }

        return suc;
    }

    protected void closeConnection(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (JMSException jmse) {
            // ignore
        }
    }

}
