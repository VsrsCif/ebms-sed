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
package si.sed.msh.ws;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import javax.naming.Binding;
import javax.naming.Context;

import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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
import org.sed.ebms.ModifyActionCode;
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
import org.sed.ebms.control.Control;
import org.sed.ebms.inbox.event.InEvent;
import org.sed.ebms.inbox.mail.InMail;
import org.sed.ebms.inbox.payload.InPart;
import org.sed.ebms.outbox.mail.OutMail;
import org.sed.ebms.outbox.event.OutEvent;
import org.sed.ebms.outbox.payload.OutPart;
import org.sed.ebms.rcontrol.RControl;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.SEDValues;
import si.sed.commons.exception.HashException;
import si.sed.commons.exception.SVEVReturnValue;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.HashUtils;
import si.sed.commons.utils.PModeManager;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.Utils;
import si.sed.msh.ws.utils.SEDRequestUtils;

/**
 *
 * @author Jože Rihtaršič
 */
@Interceptors(JEELogInterceptor.class)
@WebService(serviceName = "SEDMailBoxWS", portName = "SEDMailBoxWSPort",
        endpointInterface = "org.sed.ebms.SEDMailBoxWS",
        targetNamespace = "http://ebms.sed.org/",
        wsdlLocation = "WEB-INF/wsdl/sed-mailbox.wsdl")
public class SEDMailBox implements SEDMailBoxWS {

    @Resource
    protected UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_PU", name = "ebMS_PU")
    protected EntityManager memEManager;

    protected Queue mqMSHQueue = null;

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

        // validate data
        if (submitMailRequest == null) {
            throw SEDRequestUtils.createSEDException("Empty request: SubmitMailRequest", SEDExceptionCode.MISSING_DATA);
        }
        if (submitMailRequest.getData() == null) {
            throw SEDRequestUtils.createSEDException("Empty data in request: SubmitMailRequest/Data", SEDExceptionCode.MISSING_DATA);
        }
        if (submitMailRequest.getData().getOutMail() == null) {
            throw SEDRequestUtils.createSEDException("Empty OutMail in request: SubmitMailRequest/Data/OutMail", SEDExceptionCode.MISSING_DATA);
        }
        // validate control data
        SEDRequestUtils.validateControl(submitMailRequest.getControl());
        // get out mail 
        OutMail mail = submitMailRequest.getData().getOutMail();
        // check for missing data
        SEDRequestUtils.checkOutMailForMissingData(mail);
        // check if mail already exists
        OutMail om = mailExists(mail);
        if (om == null) {
            // validate mail data
            PMode pmd = validateOutMailData(mail);

            // serialize payload to cache FS and data to db
            serializeMail(mail, submitMailRequest.getControl().getUserId(), submitMailRequest.getControl().getApplicationId(), pmd.getId());
            // submit to ebms que
            sendMessage(mail.getId(), pmd.getId());
        }
        //generate response 
        SubmitMailResponse rsp = new SubmitMailResponse();
        rsp.setRControl(new RControl());
        rsp.getRControl().setReturnValue(om != null ? SVEVReturnValue.WARNING.getValue() : SVEVReturnValue.OK.getValue());
        rsp.getRControl().setReturnText(om != null ? String.format("Mail with sender message id '%s' already sent before: %s", om.getSenderMessageId(), msdfDDMMYYYY_HHMMSS.format(om.getSubmittedDate())) : "");
        // set data
        rsp.setRData(new SubmitMailResponse.RData());
        rsp.getRData().setSubmittedDate(om != null ? om.getSubmittedDate() : mail.getSubmittedDate());
        rsp.getRData().setSenderMessageId(om != null ? om.getSenderMessageId() : mail.getSenderMessageId());
        rsp.getRData().setMailId(om != null ? om.getId() : mail.getId());

        return rsp;
    }

    @Override
    public OutMailListResponse getOutMailList(OutMailListRequest outMailListRequest) throws SEDException_Exception {

        int iStarIndex = -1;
        int iResCountIndex = -1;
        String strOrderParam = "Id";
        String strSortOrder = "DESC";
        // validate data
        if (outMailListRequest == null) {
            throw SEDRequestUtils.createSEDException("Empty request: OutMailListRequest", SEDExceptionCode.MISSING_DATA);
        }
        // validate control
        Control c = outMailListRequest.getControl();
        SEDRequestUtils.validateControl(c);
        iStarIndex = c.getStartIndex() == null ? -1 : c.getStartIndex().intValue();
        iResCountIndex = c.getResponseSize() == null ? -1 : c.getResponseSize().intValue();
        strOrderParam = c.getSortBy() == null ? "Id" : c.getSortBy();
        strSortOrder = c.getSortOrder() == null ? "DESC" : c.getSortOrder();
        // validate data        
        OutMailListRequest.Data data = outMailListRequest.getData();
        if (data == null) {
            throw SEDRequestUtils.createSEDException("Empty data in request: OutMailList/Data", SEDExceptionCode.MISSING_DATA);
        }

        OutMailListResponse rsp = new OutMailListResponse();
        RControl rc = new RControl();
        rc.setReturnValue(SVEVReturnValue.OK.getValue());
        rc.setStartIndex(BigInteger.valueOf(iStarIndex));
        rsp.setRControl(rc);
        rsp.setRData(new OutMailListResponse.RData());

        try {

            CriteriaQuery<Long> cqCount = createSearchCriteria(data, OutMail.class, true);
            CriteriaQuery<OutMail> cq = createSearchCriteria(data, OutMail.class, false);

            Long l = getEntityManager().createQuery(cqCount).getSingleResult();
            rc.setResultSize(BigInteger.valueOf(l));

            TypedQuery<OutMail> q = getEntityManager().createQuery(cq);
            if (iResCountIndex > 0) {
                q.setMaxResults(iResCountIndex);
            }
            if (iStarIndex > 0) {
                q.setFirstResult(iStarIndex);
            }

            List<OutMail> lst = q.getResultList();
            if (!lst.isEmpty()) {
                rsp.getRData().getOutMails().addAll(lst);
            }
            rc.setResponseSize(BigInteger.valueOf(lst.size()));
        } catch (NoResultException ex) {
            rsp.getRControl().setReturnValue(SVEVReturnValue.WARNING.getValue());
        }
        return rsp;
    }

    @Override
    public OutMailEventListResponse getOutMailEventList(OutMailEventListRequest param) throws SEDException_Exception {

        int iStarIndex = -1;
        int iResCountIndex = -1;
        String strOrderParam = "Id";
        String strSortOrder = "DESC";
        // validate data
        if (param == null) {
            throw SEDRequestUtils.createSEDException("Empty request: OutMailEventListRequest", SEDExceptionCode.MISSING_DATA);
        }
        // validate control
        Control c = param.getControl();
        SEDRequestUtils.validateControl(c);
        iStarIndex = c.getStartIndex() == null ? -1 : c.getStartIndex().intValue();
        iResCountIndex = c.getResponseSize() == null ? -1 : c.getResponseSize().intValue();
        strOrderParam = c.getSortBy() == null ? "Id" : c.getSortBy();
        strSortOrder = c.getSortOrder() == null ? "DESC" : c.getSortOrder();
        // validate data        
        OutMailEventListRequest.Data dt = param.getData();
        if (dt == null) {
            throw SEDRequestUtils.createSEDException("Empty data in request: OutMailEventListRequest/Data", SEDExceptionCode.MISSING_DATA);
        }

        if (dt.getSenderEBox() == null) {
            throw SEDRequestUtils.createSEDException("SenderEBox is required attribute", SEDExceptionCode.MISSING_DATA);
        }

        if (dt.getMailId() == null && dt.getSenderMessageId() == null) {
            throw SEDRequestUtils.createSEDException("Mail id or senderMessageId is required!", SEDExceptionCode.MISSING_DATA);
        }
        // init response
        OutMailEventListResponse rsp = new OutMailEventListResponse();
        RControl rc = new RControl();
        rc.setReturnValue(SVEVReturnValue.OK.getValue());
        rc.setStartIndex(BigInteger.valueOf(iStarIndex));
        rsp.setRControl(rc);
        rsp.setRData(new OutMailEventListResponse.RData());

        CriteriaQuery<Long> cqCount = createSearchCriteria(dt, OutEvent.class, true);
        CriteriaQuery<OutEvent> cq = createSearchCriteria(dt, OutEvent.class, false);

        Long l = getEntityManager().createQuery(cqCount).getSingleResult();
        rc.setResultSize(BigInteger.valueOf(l));

        TypedQuery<OutEvent> q = getEntityManager().createQuery(cq);
        if (iResCountIndex > 0) {
            q.setMaxResults(iResCountIndex);
        }
        if (iStarIndex > 0) {
            q.setFirstResult(iStarIndex);
        }

        List<OutEvent> lst = q.getResultList();
        if (!lst.isEmpty()) {
            rsp.getRData().getOutEvents().addAll(lst);
        }
        rc.setResponseSize(BigInteger.valueOf(lst.size()));

        return rsp;
    }

    @Override
    public InMailListResponse getInMailList(InMailListRequest intMailListRequest) throws SEDException_Exception {
        int iStarIndex;
        int iResCountIndex;
        String strOrderParam = "Id";
        String strSortOrder = "DESC";
        // validate data
        if (intMailListRequest == null) {
            throw SEDRequestUtils.createSEDException("Empty request: InMailListRequest", SEDExceptionCode.MISSING_DATA);
        }
        // validate control
        Control c = intMailListRequest.getControl();
        SEDRequestUtils.validateControl(c);
        iStarIndex = c.getStartIndex() == null ? -1 : c.getStartIndex().intValue();
        iResCountIndex = c.getResponseSize() == null ? -1 : c.getResponseSize().intValue();
        strOrderParam = c.getSortBy() == null ? "Id" : c.getSortBy();
        strSortOrder = c.getSortOrder() == null ? "DESC" : c.getSortOrder();
        // validate data        
        InMailListRequest.Data data = intMailListRequest.getData();
        if (data == null) {
            throw SEDRequestUtils.createSEDException("Empty data in request: OutMailList/Data", SEDExceptionCode.MISSING_DATA);
        }

        InMailListResponse rsp = new InMailListResponse();
        RControl rc = new RControl();
        rc.setReturnValue(SVEVReturnValue.OK.getValue());
        rc.setStartIndex(BigInteger.valueOf(iStarIndex));
        rsp.setRControl(rc);
        rsp.setRData(new InMailListResponse.RData());

        try {

            CriteriaQuery<Long> cqCount = createSearchCriteria(data, InMail.class, true);
            CriteriaQuery<InMail> cq = createSearchCriteria(data, InMail.class, false);

            Long l = getEntityManager().createQuery(cqCount).getSingleResult();
            rc.setResultSize(BigInteger.valueOf(l));

            TypedQuery<InMail> q = getEntityManager().createQuery(cq);
            if (iResCountIndex > 0) {
                q.setMaxResults(iResCountIndex);
            }
            if (iStarIndex > 0) {
                q.setFirstResult(iStarIndex);
            }

            List<InMail> lst = q.getResultList();
            if (!lst.isEmpty()) {
                rsp.getRData().getInMails().addAll(lst);
            }
            rc.setResponseSize(BigInteger.valueOf(lst.size()));
        } catch (NoResultException ex) {
            rsp.getRControl().setReturnValue(SVEVReturnValue.WARNING.getValue());
        }
        return rsp;
    }

    @Override
    public InMailEventListResponse getInMailEventList(InMailEventListRequest param) throws SEDException_Exception {
        int iStarIndex = -1;
        int iResCountIndex = -1;
        String strOrderParam = "Id";
        String strSortOrder = "DESC";
        // validate data
        if (param == null) {
            throw SEDRequestUtils.createSEDException("Empty request: OutMailEventListRequest", SEDExceptionCode.MISSING_DATA);
        }
        // validate control
        Control c = param.getControl();
        SEDRequestUtils.validateControl(c);
        iStarIndex = c.getStartIndex() == null ? -1 : c.getStartIndex().intValue();
        iResCountIndex = c.getResponseSize() == null ? -1 : c.getResponseSize().intValue();
        strOrderParam = c.getSortBy() == null ? "Id" : c.getSortBy();
        strSortOrder = c.getSortOrder() == null ? "DESC" : c.getSortOrder();
        // validate data        
        InMailEventListRequest.Data dt = param.getData();
        if (dt == null) {
            throw SEDRequestUtils.createSEDException("Empty data in request: OutMailEventListRequest/Data", SEDExceptionCode.MISSING_DATA);
        }

        if (Utils.isEmptyString(dt.getReceiverEBox())) {
            throw SEDRequestUtils.createSEDException("ReceiverEBox is required attribute", SEDExceptionCode.MISSING_DATA);
        }

        if (dt.getMailId() == null) {
            throw SEDRequestUtils.createSEDException("Mail id  is required!", SEDExceptionCode.MISSING_DATA);
        }
        // init response
        InMailEventListResponse rsp = new InMailEventListResponse();
        RControl rc = new RControl();
        rc.setReturnValue(SVEVReturnValue.OK.getValue());
        rc.setStartIndex(BigInteger.valueOf(iStarIndex));
        rsp.setRControl(rc);
        rsp.setRData(new InMailEventListResponse.RData());

        CriteriaQuery<Long> cqCount = createSearchCriteria(dt, InEvent.class, true);
        CriteriaQuery<InEvent> cq = createSearchCriteria(dt, InEvent.class, false);

        Long l = getEntityManager().createQuery(cqCount).getSingleResult();
        rc.setResultSize(BigInteger.valueOf(l));

        TypedQuery<InEvent> q = getEntityManager().createQuery(cq);
        if (iResCountIndex > 0) {
            q.setMaxResults(iResCountIndex);
        }
        if (iStarIndex > 0) {
            q.setFirstResult(iStarIndex);
        }

        List<InEvent> lst = q.getResultList();
        if (!lst.isEmpty()) {
            rsp.getRData().getInEvents().addAll(lst);
        }
        rc.setResponseSize(BigInteger.valueOf(lst.size()));

        return rsp;
    }

    @Override
    public ModifyInMailResponse modifyInMail(ModifyInMailRequest param) throws SEDException_Exception {

        if (param == null) {
            throw SEDRequestUtils.createSEDException("Empty request: ModifyInMailRequest", SEDExceptionCode.MISSING_DATA);
        }
        // validate control
        Control c = param.getControl();
        SEDRequestUtils.validateControl(c);

        // validate data        
        ModifyInMailRequest.Data dt = param.getData();
        if (dt == null) {
            throw SEDRequestUtils.createSEDException("Empty data in request: ModifyInMailRequest/Data", SEDExceptionCode.MISSING_DATA);
        }

        if (Utils.isEmptyString(dt.getReceiverEBox())) {
            throw SEDRequestUtils.createSEDException("ReceiverEBox is required attribute", SEDExceptionCode.MISSING_DATA);
        }

        if (dt.getMailId() == null) {
            throw SEDRequestUtils.createSEDException("Mail id is required!", SEDExceptionCode.MISSING_DATA);
        }

        if (dt.getAction() == null) {
            throw SEDRequestUtils.createSEDException("Action is required!", SEDExceptionCode.MISSING_DATA);
        }

        // init response
        ModifyInMailResponse rsp = new ModifyInMailResponse();
        RControl rc = new RControl();
        rc.setReturnValue(SVEVReturnValue.OK.getValue());

        rsp.setRControl(rc);
        rsp.setRData(new ModifyInMailResponse.RData());

        InMail im;
        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<InMail> cq = cb.createQuery(InMail.class);
            Root<InMail> om = cq.from(InMail.class);
            cq.where(cb.and(cb.equal(om.get("ReceiverEBox"), dt.getReceiverEBox()),
                    cb.equal(om.get("Id"), dt.getMailId())));

            TypedQuery<InMail> q = getEntityManager().createQuery(cq);
            im = q.getSingleResult();

            if (SEDInboxMailStatus.RECEIVED.getValue().equals(im.getStatus())
                    || SEDInboxMailStatus.LOCKED.getValue().equals(im.getStatus())) {

                SEDInboxMailStatus st = dt.getAction().equals(ModifyActionCode.ACCEPT) ? SEDInboxMailStatus.DELIVERED : SEDInboxMailStatus.LOCKED;
                Date date = Calendar.getInstance().getTime();
                im.setDeliveredDate(date);
                im.setStatusDate(date);
                im.setStatus(st.getValue());
                InEvent ie = setInMailStatus(im, st.getDesc(), c.getUserId(), c.getApplicationId());
                rsp.getRData().setInEvent(ie);

            }

        } catch (NoResultException ignore) {
            String message = String.format("Mail with id '%s' and  ebox: %s not exists!", param.getData().getMailId().toString(), param.getData().getReceiverEBox());
            throw SEDRequestUtils.createSEDException(message, SEDExceptionCode.REQUIRED_DATA_NOT_EXISTS);
        }
        return rsp;
    }

    @Override
    public GetInMailResponse getInMail(GetInMailRequest param) throws SEDException_Exception {

        if (param == null) {
            throw SEDRequestUtils.createSEDException("Empty request: GetInMailRequest", SEDExceptionCode.MISSING_DATA);
        }
        // validate control
        Control c = param.getControl();
        SEDRequestUtils.validateControl(c);

        // validate data        
        GetInMailRequest.Data dt = param.getData();
        if (dt == null) {
            throw SEDRequestUtils.createSEDException("Empty data in request: GetInMailRequest/Data", SEDExceptionCode.MISSING_DATA);
        }

        if (dt.getReceiverEBox() == null) {
            throw SEDRequestUtils.createSEDException("ReceiverEBox is required attribute", SEDExceptionCode.MISSING_DATA);
        }

        if (dt.getMailId() == null) {
            throw SEDRequestUtils.createSEDException("Mail id  is required!", SEDExceptionCode.MISSING_DATA);
        }
        // init response
        GetInMailResponse rsp = new GetInMailResponse();
        RControl rc = new RControl();
        rc.setReturnValue(SVEVReturnValue.OK.getValue());

        rsp.setRControl(rc);
        rsp.setRData(new GetInMailResponse.RData());

        InMail im;
        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<InMail> cq = cb.createQuery(InMail.class);
            Root<InMail> om = cq.from(InMail.class);
            cq.where(cb.and(cb.equal(om.get("ReceiverEBox"), dt.getReceiverEBox()),
                    cb.equal(om.get("Id"), dt.getMailId())));

            TypedQuery<InMail> q = getEntityManager().createQuery(cq);
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
            String message = String.format("Mail with id '%s' and  ebox: %s not exists!", param.getData().getMailId().toString(), param.getData().getReceiverEBox());
            throw SEDRequestUtils.createSEDException(message, SEDExceptionCode.REQUIRED_DATA_NOT_EXISTS);
        }
        return rsp;

    }

    private OutMail mailExists(OutMail mail) {

        TypedQuery<OutMail> q = getEntityManager().createNamedQuery(NamedQueries.SED_NQ_OUTMAIL_getByMessageIdAndSenderBox, OutMail.class);
        q.setParameter("sndMsgId", mail.getSenderMessageId());
        q.setParameter("senderBox", mail.getSenderEBox());
        List<OutMail> lst = q.getResultList();
        return lst.size() > 0 ? lst.get(0) : null;
    }

    private void serializeMail(OutMail mail, String userID, String applicationId, String pmodeId) throws SEDException_Exception {

        // prepare mail to persist 
        Date dt = new Date();
        // set current status
        mail.setStatus(SEDOutboxMailStatus.SUBMITED.getValue());
        mail.setSubmittedDate(dt);
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
                        //p.setValue(null);
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

            getUserTransaction().begin();

            // persist mail    
            getEntityManager().persist(mail);

            // persist mail event
            OutEvent me = new OutEvent();

            me.setMailId(mail.getId());
            me.setSenderEBox(mail.getSenderEBox());
            me.setSenderMessageId(mail.getSenderMessageId());
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());
            me.setUserId(userID);
            me.setApplicationId(applicationId);

            getEntityManager().persist(me);
            getUserTransaction().commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    getUserTransaction().rollback();
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
        me.setReceiverEBox(im.getReceiverEBox());
        me.setStatus(im.getStatus());
        me.setDate(im.getStatusDate());
        me.setDescription(desc);
        me.setUserId(userID);
        me.setApplicationId(applicationId);
        try {
            getUserTransaction().begin();
            getEntityManager().merge(im);
            getEntityManager().persist(me);
            getUserTransaction().commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    getUserTransaction().rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    // ignore 
                }
                throw SEDRequestUtils.createSEDException(ex.getMessage(), SEDExceptionCode.SERVER_ERROR);
            }
        }
        return me;

    }

    public boolean sendMessage(BigInteger biPosiljkaId, String strpModeId) throws SEDException_Exception {

        boolean suc = false;
        InitialContext ic = null;
        Connection connection = null;
        try {
            Queue queue = getMSHQueue();

            ic = new InitialContext();
            String jndiName = getJNDIPrefix() + SEDValues.EBMS_JMS_CONNECTION_FACTORY_JNDI;
            ConnectionFactory cf = (ConnectionFactory) ic.lookup(jndiName);

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
            ex.printStackTrace();
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

    private Queue getMSHQueue() throws NamingException {
        if (mqMSHQueue == null) {
            String jndiName = getJNDI_JMSPrefix() + SEDValues.EBMS_QUEUE_JNDI;
            InitialContext ic = new InitialContext();
            mqMSHQueue = (Queue) ic.lookup(jndiName);

        }
        return mqMSHQueue;
    }

    private PMode validateOutMailData(OutMail mail) throws SEDException_Exception {
        PMode pm = null;

        if (SEDRequestUtils.isValidMailAddress(mail.getReceiverEBox())) {
            throw SEDRequestUtils.createSEDException("Invalid format: ReceiverEBox", SEDExceptionCode.INVALID_DATA);
        }

        if (SEDRequestUtils.isValidMailAddress(mail.getSenderEBox())) {
            throw SEDRequestUtils.createSEDException("Invalid format: SenderEBox", SEDExceptionCode.INVALID_DATA);
        }

        // get pmode
        String recDomain = mail.getReceiverEBox().substring(mail.getReceiverEBox().indexOf("@") + 1).trim();
        String sendDomain = mail.getSenderEBox().substring(mail.getSenderEBox().indexOf("@") + 1).trim();
        String pmodeId = mail.getService() + ":" + sendDomain + ":" + recDomain;

        pm = mpModeManager.getPModeById(pmodeId);
        if (pm == null) {
            throw SEDRequestUtils.createSEDException(String.format("PMode '%s' not exist! Check PMode configuration. (pmodeId=[service:senderDomain:receiverDomain])", pmodeId), SEDExceptionCode.INVALID_DATA);
        }
        return pm;
    }

    /**
     * Generate search criteria from search parameter. Result class should have
     * same "method name" as is in search parameter. If we would like to search
     * by "getAction" parameter . result entity must have getAction and
     * setAction methods. if searhc method ends on To Or From result entity must
     * have method without to of From. Example: for search parameter getDateFrom
     * end entity must have getDate/setDate method and parameter must inherit
     * comparable!
     *
     * @param searchParams
     * @param resultClass
     * @param forCount
     * @return
     */
    private CriteriaQuery createSearchCriteria(Object searchParams, Class resultClass, boolean forCount) {
        Class cls = searchParams.getClass();
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery cq = forCount ? cb.createQuery(Long.class) : cb.createQuery(resultClass);
        Root<OutMail> om = cq.from(resultClass);
        if (forCount) {
            cq.select(cb.count(om));
        }

        List<Predicate> lstPredicate = new ArrayList<>();

        Method[] methodList = cls.getDeclaredMethods();
        for (Method m : methodList) {

            // only getters  (public, starts with get, no arguments)
            String mName = m.getName();
            if (Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 0 && !m.getReturnType().equals(Void.TYPE)
                    && (mName.startsWith("get") || mName.startsWith("is"))) {
                String fieldName = mName.substring(mName.startsWith("get") ? 3 : 2);
                try {
                    cls.getMethod("set" + fieldName, new Class[]{m.getReturnType()});
                } catch (NoSuchMethodException | SecurityException ex) {
                    // method does not have setter 
                    continue;
                }

                try {
                    // get returm parameter
                    Object searchValue = m.invoke(searchParams, new Object[]{});

                    if (searchValue != null) {
                        if (fieldName.endsWith("From") && searchValue instanceof Comparable) {
                            lstPredicate.add(cb.greaterThanOrEqualTo(om.get(fieldName.substring(0, fieldName.lastIndexOf("From"))), (Comparable) searchValue));
                        } else if (fieldName.endsWith("To") && searchValue instanceof Comparable) {
                            lstPredicate.add(cb.lessThan(om.get(fieldName.substring(0, fieldName.lastIndexOf("To"))), (Comparable) searchValue));
                        } else {
                            lstPredicate.add(cb.equal(om.get(fieldName), searchValue));
                        }
                    }
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    Logger.getLogger(SEDMailBox.class.getName()).log(Level.SEVERE, null, ex);
                }

            }

        }

        if (!lstPredicate.isEmpty()) {
            Predicate[] tblPredicate = lstPredicate.stream().toArray(Predicate[]::new);
            cq.where(cb.and(tblPredicate));
        }
        return cq;
    }

    private EntityManager getEntityManager() {
        // for jetty 
        if (memEManager == null) {
            try {
                InitialContext ic = new InitialContext();
                Context t = (Context) ic.lookup("java:comp");
                listContext(t, "");
                memEManager = (EntityManager) ic.lookup(getJNDIPrefix() + "ebMS_PU");

            } catch (NamingException ex) {
                Logger.getLogger(SEDMailBox.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return memEManager;
    }

    private UserTransaction getUserTransaction() {
        // for jetty 
        if (mutUTransaction == null) {
            try {
                InitialContext ic = new InitialContext();

                mutUTransaction = (UserTransaction) ic.lookup(getJNDIPrefix() + "UserTransaction");

            } catch (NamingException ex) {
                Logger.getLogger(SEDMailBox.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return mutUTransaction;
    }

    private static final void listContext(Context ctx, String indent) {
        try {
            NamingEnumeration list = ctx.listBindings("");
            while (list.hasMore()) {
                Binding item = (Binding) list.next();
                String className = item.getClassName();
                String name = item.getName();
                System.out.println(indent + className + " " + name);
                Object o = item.getObject();
                if (o instanceof javax.naming.Context) {
                    listContext((Context) o, indent + " ");
                }
            }
        } catch (NamingException ex) {
            System.out.println(ex);
        }
    }

    private String getJNDI_JMSPrefix() {
        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_JMS_PREFIX, "java:/jms/");
    }

    private String getJNDIPrefix() {

        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/");
    }

}
