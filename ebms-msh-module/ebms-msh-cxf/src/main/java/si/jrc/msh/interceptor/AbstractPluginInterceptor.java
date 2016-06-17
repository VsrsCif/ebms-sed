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

import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.Context;
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
import si.sed.commons.SEDValues;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public abstract class AbstractPluginInterceptor extends AbstractSoapInterceptor {

    String LOADED_CLASSES = "hibernate.ejb.loaded.classes";
    @PersistenceContext(unitName = "ebMS_MSH_PU")
    private EntityManager memEManager;
    protected Queue mqMSHQueue = null;

    @Resource
    private UserTransaction mutUTransaction;

    public AbstractPluginInterceptor(String p) {
        super(p);
    }

    public AbstractPluginInterceptor(String i, String p) {
        super(i, p);

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

    public EntityManager getEntityManager() {
        // for jetty
        if (memEManager == null) {
            try {
                InitialContext ic = new InitialContext();
                Context t = (Context) ic.lookup("java:comp");
                memEManager = (EntityManager) ic.lookup(getJNDIPrefix() + "EBMS_MSH_PLUGIN_PU");

            } catch (NamingException ex) {
                Logger.getLogger(AbstractPluginInterceptor.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return memEManager;
    }

    private String getJNDIPrefix() {

        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/jboss/");
    }

    private String getJNDI_JMSPrefix() {
        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_JMS_PREFIX, "java:/jms/");
    }

    private Queue getMSHQueue() throws NamingException {
        if (mqMSHQueue == null) {
            String jndiName = getJNDI_JMSPrefix() + SEDValues.JNDI_QUEUE_EBMS;
            InitialContext ic = new InitialContext();
            mqMSHQueue = (Queue) ic.lookup(jndiName);

        }
        return mqMSHQueue;
    }

    public UserTransaction getUserTransaction() {
        // for jetty
        if (mutUTransaction == null) {
            try {
                InitialContext ic = new InitialContext();

                mutUTransaction = (UserTransaction) ic.lookup(getJNDIPrefix() + "UserTransaction");

            } catch (NamingException ex) {
                Logger.getLogger(AbstractPluginInterceptor.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return mutUTransaction;
    }

    @Override
    public abstract void handleMessage(SoapMessage t) throws Fault;

    public boolean sendMessage(BigInteger biPosiljkaId, String strpModeId) {

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

            sendMessage(mail.getId(), pmodeId);
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

    public void updateInMail(MSHInMail mail, String statusDesc) {

        // --------------------
        // serialize data to db
        try {

            getUserTransaction().begin();

            // persist mail    
            getEntityManager().merge(mail);

            // persist mail event
            MSHInEvent me = new MSHInEvent();
            me.setDescription(LOADED_CLASSES);
            me.setMailId(mail.getId());
            me.setStatus(mail.getStatus());
            me.setDescription(statusDesc);
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
