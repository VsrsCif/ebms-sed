/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import si.sed.commons.interfaces.SEDDaoInterface;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Queue;
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
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.user.SEDUser;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.interfaces.JMSManagerInterface;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(SEDDaoInterface.class)
@TransactionManagement(TransactionManagementType.BEAN)
public class SEDDaoBean implements SEDDaoInterface {

    @Resource
    public UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_SED_PU", name = "ebMS_SED_PU")
    public EntityManager memEManager;

    @EJB(mappedName = SEDJNDI.JNDI_JMSMANAGER)
    JMSManagerInterface mJMS;

    protected Queue mqMSHQueue = null;

    protected static SEDLogger LOG = new SEDLogger(SEDDaoBean.class);

    @Override
    public SEDUser getSEDUser(String username) {
        TypedQuery<SEDUser> qUser = memEManager.createNamedQuery(SEDNamedQueries.SEDUSER_BY_ID, SEDUser.class);
        qUser.setParameter("id", username);
        try {
            return qUser.getSingleResult();
        } catch (NoResultException ignore) {
            return null;
        }
    }

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

    @Override
    public <T> List<T> getMailEventList(Class<T> type, BigInteger mailId) {
        long l = LOG.logStart(type, mailId);
        TypedQuery tq = memEManager.createNamedQuery(type.getName() + ".getMailEventList", type);
        tq.setParameter("mailId", mailId);
        List<T> mailEvents = tq.getResultList();
        LOG.logEnd(l, type);
        return mailEvents;
    }

    @Override
    public <T> T getMailById(Class<T> type, BigInteger mailId) {
        long l = LOG.logStart(type, mailId);
        TypedQuery<T> tq = memEManager.createNamedQuery(type.getName() + ".getById", type);
        tq.setParameter("id", mailId);
        T result = tq.getSingleResult();
        LOG.logEnd(l);
        return result;
    }

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

    @Override
    public void updateInMail(MSHInMail mail, String statusDesc) {

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
            me.setDate(mail.getStatusDate());
            memEManager.persist(me);
            mutUTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    // ignore 
                }
                /*  SEDException msherr = new SEDException();
                msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
                msherr.setMessage(ex.getMessage());
                throw new SEDException_Exception("Error occured while storing to DB", msherr, ex);*/
            }
        }

    }
    @Override
    public void serializeInMail(MSHInMail mail){
    // serialize data to db
        try {

            mutUTransaction.begin();

            // persist mail    
            memEManager.persist(mail);

            // persist mail event
            MSHInEvent me = new MSHInEvent();
            me.setMailId(mail.getId());
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());
            memEManager.persist(me);
            mutUTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    // ignore 
                }
                /*  SEDException msherr = new SEDException();
                msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
                msherr.setMessage(ex.getMessage());
                throw new SEDException_Exception("Error occured while storing to DB", msherr, ex);*/
            }
        }
    }

    @Override
    public void serializeOutMail(MSHOutMail mail, String userID, String applicationId, String pmodeId) {
        //  EntityManagerFactory emf = null;

        // --------------------
        // serialize data to db
        try {
            //emf = getSEDEntityManagerFactory();
            //em = emf.createEntityManager();
            //em.getTransaction().begin();

            mutUTransaction.begin();

            // persist mail    
            memEManager.persist(mail);
            // persist mail event
            MSHOutEvent me = new MSHOutEvent();
            me.setMailId(mail.getId());
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());
            me.setSenderMessageId(mail.getSenderMessageId());
            me.setUserId(userID);
            me.setApplicationId(applicationId);
            memEManager.persist(me);
            //em.getTransaction().commit();
            mutUTransaction.commit();
            mJMS.sendMessage(mail.getId().longValue(), pmodeId, 0, 0, false);

            
        } catch (Exception ex) {
            try {
                mutUTransaction.rollback();
            } catch (IllegalStateException | SecurityException | SystemException ex1) {
                Logger.getLogger(SEDDaoBean.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }

    }

    @Override
    public void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc) {
        long l = LOG.logStart();
        try {
            mutUTransaction.begin();
            mail.setStatusDate(Calendar.getInstance().getTime());
            mail.setStatus(status.getValue());
            // persist mail event
            MSHOutEvent me = new MSHOutEvent();
            me.setMailId(mail.getId());
            me.setDescription(desc == null ? status.getDesc() : desc);
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());
            me.setSenderMessageId(mail.getSenderMessageId());

            memEManager.merge(mail);
            memEManager.persist(me);
            mutUTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    // ignore 
                }
                LOG.logError(l, "Error commiting status to outboxmail: '" + mail.getId() + "'!", ex);
            }
        }
        LOG.logEnd(l);

    }

    @Override
    public void setStatusToInMail(MSHInMail mail, SEDInboxMailStatus status, String desc) {
        long l = LOG.logStart();
        try {
            mutUTransaction.begin();
            mail.setStatusDate(Calendar.getInstance().getTime());
            mail.setStatus(status.getValue());
            // persist mail event
            MSHInEvent me = new MSHInEvent();
            me.setMailId(mail.getId());
            me.setDescription(desc == null ? status.getDesc() : desc);
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());

            memEManager.merge(mail);
            memEManager.persist(me);
            mutUTransaction.commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    mutUTransaction.rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    // ignore 
                }
                LOG.logError(l, "Error commiting status to outboxmail: '" + mail.getId() + "'!", ex);
            }
        }
        LOG.logEnd(l);

    }

    @Override
    public <T> long getDataListCount(Class<T> type, Object filters) {
        long l = LOG.logStart(type, filters);
        CriteriaQuery<Long> cqCount = createSearchCriteria(type, filters, true, null, null);
        Long res = memEManager.createQuery(cqCount).getSingleResult();
        LOG.logEnd(l, type, filters);
        return res;
    }

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
                            } else if (searchValue instanceof String && !((String) searchValue).isEmpty()) {
                                System.out.println("Add param: '" + fieldName + "' value: '" + searchValue + "'");
                                lstPredicate.add(cb.equal(om.get(fieldName), searchValue));
                            }
                        }
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        LOG.logError(l, ex);
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

    @Override
    public SEDBox getSedBoxByName(String sbox) {

        TypedQuery<SEDBox> sq = memEManager.createNamedQuery("org.sed.ebms.ebox.SEDBox.getByName", SEDBox.class);
        sq.setParameter("BoxName", sbox);
        try {
            return sq.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }
}
