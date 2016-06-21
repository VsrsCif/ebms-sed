package si.sed.commons.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
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
import org.msh.ebms.inbox.event.MSHInEvent;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.outbox.event.MSHOutEvent;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.sed.ebms.cron.SEDTaskExecution;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.SEDTaskStatus;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.JMSManagerInterface;
import si.sed.commons.interfaces.SEDDaoInterface;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(SEDDaoInterface.class)
@TransactionManagement(TransactionManagementType.BEAN)
public class SEDDaoBean implements SEDDaoInterface {

    /**
     *
     */
    protected static SEDLogger LOG = new SEDLogger(SEDDaoBean.class);

    @EJB(mappedName = SEDJNDI.JNDI_JMSMANAGER)
    JMSManagerInterface mJMS;

    /**
     *
     */
    @PersistenceContext(unitName = "ebMS_SED_PU", name = "ebMS_SED_PU")
    public EntityManager memEManager;

    /**
     *
     */
    protected Queue mqMSHQueue = null;

    /**
     *
     */
    @Resource
    public UserTransaction mutUTransaction;

    /**
     *
     * @param <T>
     * @param o
     * @return
     */
    public <T> boolean add(T o) {
        long l = LOG.logStart();
        boolean suc = false;
        try {
            mutUTransaction.begin();
            memEManager.persist(o);
            mutUTransaction.commit();
            suc = true;
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            try {
                LOG.logError(l, ex.getMessage(), ex);
                mutUTransaction.rollback();
            } catch (IllegalStateException | SecurityException | SystemException ex1) {
                LOG.logWarn(l, "Rollback failed", ex1);
            }
        }
        return suc;
    }

    /**
     *
     * @param ad
     * @return
     */
    @Override
    public boolean addExecutionTask(SEDTaskExecution ad) {
        return add(ad);
    }

    /**
     *
     * @param <T>
     * @param type
     * @param searchParams
     * @param forCount
     * @param sortField
     * @param sortOrder
     * @return
     */
    protected <T> CriteriaQuery createSearchCriteria(Class<T> type, Object searchParams, boolean forCount, String sortField, String sortOrder) {
        long l = LOG.logStart();
        CriteriaBuilder cb = memEManager.getCriteriaBuilder();
        CriteriaQuery cq = forCount ? cb.createQuery(Long.class) : cb.createQuery(type);
        Root<T> om = cq.from(type);
        if (forCount) {
            cq.select(cb.count(om));
        } else if (sortField != null) {
            if (sortOrder != null && sortOrder.equalsIgnoreCase("desc")) {
                cq.orderBy(cb.asc(om.get(sortField)));
            } else {
                cq.orderBy(cb.desc(om.get(sortField)));
            }
        } else {
            cq.orderBy(cb.desc(om.get("Id")));
        }
        List<Predicate> lstPredicate = new ArrayList<>();
        // set order by
        if (searchParams != null) {
            Class cls = searchParams.getClass();
            Method[] methodList = cls.getDeclaredMethods();
            for (Method m : methodList) {
                // only getters  (public, starts with get, no arguments)
                String mName = m.getName();
                if (Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 0 && !m.getReturnType().equals(Void.TYPE) && (mName.startsWith("get") || mName.startsWith("is"))) {
                    String fieldName = mName.substring(mName.startsWith("get") ? 3 : 2);
                    // get returm parameter
                    Object searchValue;
                    try {
                        searchValue = m.invoke(searchParams, new Object[]{});
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        LOG.logError(l, ex);
                        continue;
                    }

                    if (searchValue == null) {
                        continue;
                    }

                    if (fieldName.endsWith("List") && searchValue instanceof List && !((List) searchValue).isEmpty()) {
                        lstPredicate.add(om.get(fieldName.substring(0, fieldName.lastIndexOf("List"))).in(((List) searchValue).toArray()));                        
                    } else {
                        try {
                            cls.getMethod("set" + fieldName, new Class[]{m.getReturnType()});
                        } catch (NoSuchMethodException | SecurityException ex) {
                            // method does not have setter // ignore other methods
                            continue;
                        }

                        if (fieldName.endsWith("From") && searchValue instanceof Comparable) {
                            lstPredicate.add(cb.greaterThanOrEqualTo(om.get(fieldName.substring(0, fieldName.lastIndexOf("From"))), (Comparable) searchValue));
                        } else if (fieldName.endsWith("To") && searchValue instanceof Comparable) {
                            lstPredicate.add(cb.lessThan(om.get(fieldName.substring(0, fieldName.lastIndexOf("To"))), (Comparable) searchValue));
                        } else if (searchValue instanceof String && !((String) searchValue).isEmpty()) {
                            lstPredicate.add(cb.equal(om.get(fieldName), searchValue));
                        }

                    }

                }
            }
            if (!lstPredicate.isEmpty()) {
                Predicate[] tblPredicate = lstPredicate.stream().toArray(Predicate[]::new);
                cq.where(cb.and(tblPredicate));
            }
        }
        return cq;
    }

    /**
     *
     * @param <T>
     * @param type
     * @param startingAt
     * @param maxResultCnt
     * @param sortField
     * @param sortOrder
     * @param filters
     * @return
     */
    @Override
    public <T> List<T> getDataList(Class<T> type, int startingAt, int maxResultCnt, String sortField, String sortOrder, Object filters) {
        long l = LOG.logStart(type, startingAt, maxResultCnt, sortField, sortOrder, filters);
        List<T> lstResult;
        try {
            CriteriaQuery<T> cq = createSearchCriteria(type, filters, false, sortField, sortOrder);
            TypedQuery<T> q = memEManager.createQuery(cq);
            if (maxResultCnt > 0) {
                q.setMaxResults(maxResultCnt);
            }
            if (startingAt > 0) {
                q.setFirstResult(startingAt);
            }
            lstResult = q.getResultList();
        } catch (NoResultException ex) {
            lstResult = new ArrayList<>();
        }
        LOG.logEnd(l, type, startingAt, maxResultCnt, sortField, sortOrder, filters);
        return lstResult;
    }

    /**
     *
     * @param <T>
     * @param type
     * @param filters
     * @return
     */
    @Override
    public <T> long getDataListCount(Class<T> type, Object filters) {
        long l = LOG.logStart(type, filters);
        CriteriaQuery<Long> cqCount = createSearchCriteria(type, filters, true, null, null);
        Long res = memEManager.createQuery(cqCount).getSingleResult();
        LOG.logEnd(l, type, filters);
        return res;
    }

    /**
     *
     * @param action
     * @param convId
     * @return
     */
    @Override
    public List<MSHInMail> getInMailConvIdAndAction(String action, String convId) {
        long l = LOG.logStart(action, convId);
        Query q = memEManager.createNamedQuery("org.msh.ebms.inbox.mail.MSHInMail.getByConvIdAndAction", MSHInMail.class);
        q.setParameter("convId", convId);
        q.setParameter("action", action);
        List<MSHInMail> lst = q.getResultList();

        LOG.logEnd(l);
        return lst;
    }

    /**
     *
     * @param type
     * @return
     */
    @Override
    public SEDTaskExecution getLastSuccesfullTaskExecution(String type) {
        long l = LOG.logStart();
        SEDTaskExecution dt = null;

        TypedQuery<SEDTaskExecution> tq = memEManager.createNamedQuery("org.sed.ebms.cron.SEDTaskExecution.getByStatusAndType", SEDTaskExecution.class);

        tq.setParameter("status", SEDTaskStatus.SUCCESS.getValue());
        tq.setParameter("type", type);
        tq.setMaxResults(1);
        try {
            dt = tq.getSingleResult();
        } catch (NoResultException ign) {
            LOG.logWarn(l, "No succesfull task execution for type: " + type, null);
        }

        return dt;

    }

    /**
     *
     * @param <T>
     * @param type
     * @param mailId
     * @return
     */
    @Override
    public <T> T getMailById(Class<T> type, BigInteger mailId) {
        long l = LOG.logStart(type, mailId);
        TypedQuery<T> tq = memEManager.createNamedQuery(type.getName() + ".getById", type);
        tq.setParameter("id", mailId);
        T result = tq.getSingleResult();
        LOG.logEnd(l);
        return result;
    }

    /**
     *
     * @param <T>
     * @param type
     * @param mailId
     * @return
     */
    @Override
    public <T> List<T> getMailEventList(Class<T> type, BigInteger mailId) {
        long l = LOG.logStart(type, mailId);
        TypedQuery tq = memEManager.createNamedQuery(type.getName() + ".getMailEventList", type);
        tq.setParameter("mailId", mailId);
        List<T> mailEvents = tq.getResultList();
        LOG.logEnd(l, type);
        return mailEvents;
    }

    /**
     *
     * @param <T>
     * @param o
     * @return
     */
    public <T> boolean remove(T o) {
        long l = LOG.logStart();
        boolean suc = false;
        try {
            mutUTransaction.begin();
            memEManager.remove(memEManager.contains(o) ? o : memEManager.merge(o));
            mutUTransaction.commit();
            suc = true;
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            try {
                LOG.logError(l, ex.getMessage(), ex);
                mutUTransaction.rollback();
            } catch (IllegalStateException | SecurityException | SystemException ex1) {
                LOG.logWarn(l, "Rollback failed", ex1);
            }
        }
        return suc;
    }

    /**
     *
     * @param bi
     * @throws StorageException
     */
    @Override
    public void removeInMail(BigInteger bi) throws StorageException {
        removeMail(MSHInMail.class, MSHInEvent.class, bi);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param type
     * @param typeEvent
     * @param bi
     * @throws StorageException
     */
    public <T, E> void removeMail(Class<T> type, Class<E> typeEvent, BigInteger bi) throws StorageException {
        long l = LOG.logStart(type);
        T mail = getMailById(type, bi);
        try {
            mutUTransaction.begin();

            // remove events
            List<E> lst = getMailEventList(typeEvent, bi);
            for (E e : lst) {
                memEManager.remove(memEManager.contains(e) ? e : memEManager.merge(e));
                
            }
            //remove mail
            memEManager.remove(memEManager.contains(mail) ?mail : memEManager.merge(mail) );
            mutUTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    LOG.logWarn(l, "", ex);
                }
                String msg = "Error occurred where removing mail type: '" + type + "', id: '" + bi + "'! Err:" + ex.getMessage();
                LOG.logError(l, msg, ex);
                throw new StorageException(msg, ex);
            }
        }

        LOG.logEnd(l);
    }

    /**
     *
     * @param bi
     * @throws StorageException
     */
    @Override
    public void removeOutMail(BigInteger bi) throws StorageException {
        removeMail(MSHOutMail.class, MSHOutEvent.class, bi);
    }

    /**
     *
     * @param mail
     * @param applicationId
     * @throws StorageException
     */
    @Override
    public void serializeInMail(MSHInMail mail, String applicationId) throws StorageException {
        long l = LOG.logStart();
        try {

            mutUTransaction.begin();
            // persist mail    
            memEManager.persist(mail);
            // persist mail event
            MSHInEvent me = new MSHInEvent();
            me.setMailId(mail.getId());
            me.setStatus(mail.getStatus());
            me.setDescription("Mail stored to inbox");
            me.setApplicationId(applicationId);
            me.setDate(mail.getStatusDate());
            memEManager.persist(me);
            mutUTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    LOG.logWarn(l, "", ex);
                }
                String msg = "Error occurred on serializing mail! Err:" + ex.getMessage();
                LOG.logError(l, msg, ex);
                throw new StorageException(msg, ex);
            }
        }
        LOG.logEnd(l);
    }

    /**
     *
     * @param mail
     * @param userID
     * @param applicationId
     * @param pmodeId
     * @throws StorageException
     */
    @Override
    public void serializeOutMail(MSHOutMail mail, String userID, String applicationId, String pmodeId) throws StorageException {
        long l = LOG.logStart();
        try {
            mutUTransaction.begin();
            // persist mail    
            if (mail.getStatusDate() == null) {
                mail.setStatusDate(Calendar.getInstance().getTime());

            }
            if (mail.getStatus() == null) {
                mail.setStatus(SEDOutboxMailStatus.SUBMITTED.getValue());
            }
            memEManager.persist(mail);
            // persist mail event
            MSHOutEvent me = new MSHOutEvent();
            me.setDescription("Mail composed in ebms-sed-gui.");
            me.setMailId(mail.getId());
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());
            me.setSenderMessageId(mail.getSenderMessageId());
            me.setUserId(userID);
            me.setApplicationId(applicationId);
            memEManager.persist(me);

            mutUTransaction.commit();

            mJMS.sendMessage(mail.getId().longValue(), pmodeId, 0, 0, false);

        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    LOG.logWarn(l, "", ex);
                }
                String msg = "Error occurred on serializing mail! Err:" + ex.getMessage();
                LOG.logError(l, msg, ex);
                throw new StorageException(msg, ex);
            }
        } catch (NamingException | JMSException ex) {
            String msg = "Error submitting mail Out JMS:" + ex.getMessage();
            LOG.logError(l, msg, ex);
            setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, msg);
            throw new StorageException(msg, ex);
        }
        LOG.logEnd(l);

    }

    /**
     *
     * @param mail
     * @param status
     * @param desc
     * @throws StorageException
     */
    @Override
    public void setStatusToInMail(MSHInMail mail, SEDInboxMailStatus status, String desc) throws StorageException {
        setStatusToInMail(mail, status, desc, null, null);
    }

    /**
     *
     * @param mail
     * @param status
     * @param desc
     * @param userID
     * @param applicationId
     * @throws StorageException
     */
    @Override
    public void setStatusToInMail(MSHInMail mail, SEDInboxMailStatus status, String desc, String userID, String applicationId) throws StorageException {
        long l = LOG.logStart();
        try {
            mutUTransaction.begin();
            mail.setStatusDate(Calendar.getInstance().getTime());
            mail.setStatus(status.getValue());

            Query updq = memEManager.createNamedQuery(SEDNamedQueries.UPDATE_INMAIL);
            updq.setParameter("id", mail.getId());
            updq.setParameter("statusDate", mail.getStatusDate());
            updq.setParameter("status", mail.getStatus());

            // persist mail event
            MSHInEvent me = new MSHInEvent();
            me.setMailId(mail.getId());
            me.setDescription(desc == null ? status.getDesc() : desc);
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());
            me.setUserId(userID);
            me.setApplicationId(applicationId);

            int iVal = updq.executeUpdate();
            if (iVal != 1) {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    LOG.logWarn(l, ex1.getMessage(), ex1);
                }
                String msg = "Status not setted to MSHInMail:" + mail.getId() + " result: '" + iVal + "'. Mail not exists or id duplicates?";
                LOG.logError(l, msg, null);
                throw new StorageException(msg, null);
            }
            memEManager.persist(me);
            mutUTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    LOG.logWarn(l, ex1.getMessage(), ex1);
                }
                String msg = "Error commiting status to incommingxmail: '" + mail.getId() + "'! Err:" + ex.getMessage();
                LOG.logError(l, msg, ex);
                throw new StorageException(msg, ex);
            }
        }
        LOG.logEnd(l);

    }

    /**
     *
     * @param mail
     * @param status
     * @param desc
     * @throws StorageException
     */
    @Override
    public void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc) throws StorageException {
        setStatusToOutMail(mail, status, desc, null, null);
    }

    /**
     *
     * @param mail
     * @param status
     * @param desc
     * @param userID
     * @param applicationId
     * @throws StorageException
     */
    @Override
    public void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc, String userID, String applicationId) throws StorageException {
        long l = LOG.logStart();

        try {
            Date dt = Calendar.getInstance().getTime();

            mail.setStatusDate(Calendar.getInstance().getTime());
            mail.setStatus(status.getValue());

            Query updq = memEManager.createNamedQuery(SEDNamedQueries.UPDATE_OUTMAIL);
            updq.setParameter("id", mail.getId());
            updq.setParameter("statusDate", mail.getStatusDate());
            updq.setParameter("status", mail.getStatus());

            // persist mail event
            MSHOutEvent me = new MSHOutEvent();
            me.setMailId(mail.getId());
            me.setDescription(desc == null ? status.getDesc() : desc);
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());
            me.setSenderMessageId(mail.getSenderMessageId());
            me.setUserId(userID);
            me.setApplicationId(applicationId);

            mutUTransaction.begin();

            int iVal = updq.executeUpdate();
            if (iVal != 1) {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    LOG.logWarn(l, ex1.getMessage(), ex1);
                }
                String msg = "Status not setted to MSHOutMail:" + mail.getId() + " result: '" + iVal + "'. Mail not exists or id duplicates?";
                LOG.logError(l, msg, null);
                throw new StorageException(msg, null);
            }
            memEManager.persist(me);
            mutUTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    LOG.logWarn(l, ex1.getMessage(), ex1);
                }
                String msg = "Error commiting status to outboxmail: '" + mail.getId() + "'! Err:" + ex.getMessage();
                LOG.logError(l, msg, ex);
                throw new StorageException(msg, ex);
            }
        }
        LOG.logEnd(l);
    }

    /**
     *
     * @param <T>
     * @param o
     * @return
     */
    public <T> boolean update(T o) {
        long l = LOG.logStart();
        boolean suc = false;
        try {
            mutUTransaction.begin();
            memEManager.merge(o);
            mutUTransaction.commit();
            suc = true;
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            try {
                LOG.logError(l, ex.getMessage(), ex);
                mutUTransaction.rollback();
            } catch (IllegalStateException | SecurityException | SystemException ex1) {
                LOG.logWarn(l, "Rollback failed", ex1);
            }
        }
        return suc;
    }

    ;
    
    /**
     *
     * @param ad
     * @return
     */
    @Override
    public boolean updateExecutionTask(SEDTaskExecution ad) {
        return update(ad);
    }

    /**
     *
     * @param mail
     * @param statusDesc
     * @param user
     * @throws StorageException
     */
    @Override
    public void updateInMail(MSHInMail mail, String statusDesc, String user) throws StorageException {
        long l = LOG.logStart();
        // --------------------
        // serialize data to db
        try {

            mutUTransaction.begin();
            // persist mail
            memEManager.merge(mail);

            // persist mail event
            MSHInEvent me = new MSHInEvent();
            me.setMailId(mail.getId());
            me.setStatus(mail.getStatus());
            me.setDescription(statusDesc);
            me.setUserId(user);
            me.setDate(mail.getStatusDate());
            memEManager.persist(me);
            mutUTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    LOG.logWarn(l, "", ex);
                }
                String msg = "Error occurred on update in mail: '" + mail.getId() + "'! Err:" + ex.getMessage();
                LOG.logError(l, msg, ex);
                throw new StorageException(msg, ex);
            }
        }
        LOG.logEnd(l);
    }

}
