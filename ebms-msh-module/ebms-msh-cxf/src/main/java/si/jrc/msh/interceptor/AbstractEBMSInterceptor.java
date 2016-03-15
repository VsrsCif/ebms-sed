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

import javax.annotation.Resource;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.msh.ebms.inbox.event.MSHInEvent;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.outbox.event.MSHOutEvent;
import org.msh.ebms.outbox.mail.MSHOutMail;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public abstract class AbstractEBMSInterceptor extends AbstractSoapInterceptor {

    String LOADED_CLASSES = "hibernate.ejb.loaded.classes";

    @Resource
    private UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_MSH_PU")
    private EntityManager memEManager;

    SEDLogger mlog = new SEDLogger(AbstractEBMSInterceptor.class);

    public AbstractEBMSInterceptor(String p) {
        super(p);
    }

    public AbstractEBMSInterceptor(String i, String p) {
        super(i, p);

    }

    public UserTransaction getUserTransaction() {
        long l = mlog.logStart();
        // for jetty 
        if (mutUTransaction == null) {

            try {

                InitialContext ic = new InitialContext();
                mutUTransaction = (UserTransaction) ic.lookup(getJNDIPrefix() + "jboss/UserTransaction");

            } catch (NamingException ex) {
                mlog.logWarn(l, "Error discovering 'jboss/UserTransaction'. Try again with 'UserTransaction'. ERROR:" + ex.getMessage(), null);
                try {
                    InitialContext ic = new InitialContext();
                    mutUTransaction = (UserTransaction) ic.lookup(getJNDIPrefix() + "UserTransaction");
                } catch (NamingException e1) {
                    mlog.logError(l, "Error discovering 'UserTransaction'." + ex.getExplanation(), e1);
                }
            }

        }
        return mutUTransaction;
    }

    private String getJNDIPrefix() {

        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/");
    }

    private String getJNDI_JMSPrefix() {
        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_JMS_PREFIX, "java:/jms/");
    }

    public EntityManager getEntityManager() {
        // for jetty 
        long l = mlog.logStart();
        
        if (memEManager == null) {
            String jndi = getJNDIPrefix() + "ebMS_MSH_PU";
            String jndi2 =  "__/ebMS_MSH_PU";
            try {
                InitialContext ic = new InitialContext();
                memEManager = (EntityManager) ic.lookup(jndi);

            } catch (NamingException ex) {
                mlog.logWarn(l, "Error discovering '"+jndi+"'. Try again with '"+jndi2+"'.", null);
                try {
                    InitialContext ic = new InitialContext();
                    memEManager = (EntityManager) ic.lookup(jndi2);

                } catch (NamingException ex1) {
                    mlog.logError(l, "Error discovering '"+jndi2+"'." + ex.getExplanation(), ex1);
                }
            }

        }
        return memEManager;
    }

    @Override
    public abstract void handleMessage(SoapMessage t) throws Fault;

    public void serializeMail(MSHOutMail mail, String userID, String applicationId, String pmodeId) {
        //  EntityManagerFactory emf = null;
        EntityManager em = null;

        // --------------------
        // serialize data to db
        try {
            //emf = getSEDEntityManagerFactory();
            //em = emf.createEntityManager();
            //em.getTransaction().begin();
            em = getEntityManager();
            getUserTransaction().begin();

            // persist mail    
            em.persist(mail);
            // persist mail event
            MSHOutEvent me = new MSHOutEvent();
            me.setMailId(mail.getId());
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());
            me.setSenderMessageId(mail.getSenderMessageId());
            me.setUserId(userID);
            me.setApplicationId(applicationId);
            em.persist(me);

            //em.getTransaction().commit();
            getUserTransaction().commit();

        } catch (Exception ex) {
            if (em != null) {
                em.getTransaction().rollback();
            }
            ex.printStackTrace();
        } finally {
            /* if (em != null) {
                em.close();
            }
            if (emf != null) {
                emf.close();
            }*/
        }

    }

    public void serializeInMail(MSHInMail mail) {

        // --------------------
        // serialize data to db
        try {

            getUserTransaction().begin();

            // persist mail    
            getEntityManager().persist(mail);

            // persist mail event
            MSHInEvent me = new MSHInEvent();
            me.setMailId(mail.getId());
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());
            getEntityManager().persist(me);
            getUserTransaction().commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    getUserTransaction().rollback();
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

}
