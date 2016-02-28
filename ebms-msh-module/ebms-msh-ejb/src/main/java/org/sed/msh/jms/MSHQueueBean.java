/*
* Copyright 2016, Supreme Court Republic of Slovenia 
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
package org.sed.msh.jms;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
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
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.event.MSHOutEvent;

import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.ReceptionAwareness;
import si.jrc.msh.client.MshClient;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.SEDValues;
import si.sed.commons.utils.PModeManager;
import si.sed.commons.utils.SEDLogger;


/**
 *
 * @author Jože Rihtaršič
 */

@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/MSHQueue"),
    @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "${org.sed.msh.maxWorkers:5}")
})
@TransactionManagement(TransactionManagementType.BEAN)
public class MSHQueueBean implements MessageListener {

    SEDLogger mlog = new SEDLogger(MSHQueueBean.class);
    PModeManager mpModeManager = new PModeManager();
    MshClient mmshClient = new MshClient();

    @Resource
    public UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_MSH_PU")
    public EntityManager memEManager;

    public MSHQueueBean() {
    }

    @Override
    public void onMessage(Message msg) {
        long t = mlog.logStart();
        // get outbox mail
        long idMsg;
        String pModeID;
        // get id
        try {
            idMsg = msg.getLongProperty(SEDValues.EBMS_QUEUE_PARAM_MAIL_ID);
        } catch (JMSException ex) {
            mlog.logError(t, "Bad message with no property:'" + SEDValues.EBMS_QUEUE_PARAM_MAIL_ID + "'!", ex);
            return;
        }

        // get pmode
        try {
            pModeID = msg.getStringProperty(SEDValues.EBMS_QUEUE_PARAM_PMODE_ID);
        } catch (JMSException ex) {
            mlog.logError(t, "Bad message with no property:'" + SEDValues.EBMS_QUEUE_PARAM_PMODE_ID + "'!", ex);
            return;
        }

        MSHOutMail mail = null;
        try {
            // get outbox mail
            TypedQuery<MSHOutMail> q = getEntityManager().createNamedQuery("MSHOutMail.getById", MSHOutMail.class);
            q.setParameter("id", BigInteger.valueOf(idMsg));
            mail = q.getSingleResult();
        } catch (NoResultException ex) {
            mlog.logError(t, "Message with id: '" + idMsg + "' not exists!", ex);
            return;
        }

        // get pmode 
        PMode pMode = mpModeManager.getPModeById(pModeID);
        if (pMode == null) {
            String errDesc = "PMode with id: '" + pModeID + "' not exists! Message with id '" + idMsg + "' is not procesed!";
            mlog.logError(t, errDesc, null);
            setStatusToMail(mail, SEDOutboxMailStatus.ERROR, errDesc);
            return;
        }
        // start sending        

        setStatusToMail(mail, SEDOutboxMailStatus.SENDING, null);
        try {
            mmshClient.sendMessage(mail, pMode);
            mail.setSentDate(Calendar.getInstance().getTime());
            setStatusToMail(mail, SEDOutboxMailStatus.SENT, null);

        } catch (Exception ex) {
            ex.printStackTrace();
            setStatusToMail(mail, SEDOutboxMailStatus.ERROR, ex.getMessage());
            if (pMode.getReceptionAwareness() != null && pMode.getReceptionAwareness().getRetry() != null) {
                ReceptionAwareness.Retry rty = pMode.getReceptionAwareness().getRetry();
                int iRet = 0;
                long lDelay = -1;
                try {
                    iRet = msg.getIntProperty(SEDValues.EBMS_QUEUE_PARAM_RETRY);
                } catch (JMSException ignore) {
                }
                try {
                    lDelay = msg.getLongProperty(SEDValues.EBMS_QUEUE_PARAM_DELAY);
                } catch (JMSException ignore) {
                }
                if (iRet < rty.getMaxRetries()) {
                    iRet++;
                    if (lDelay <= 0) {
                        lDelay = rty.getPeriod();
                    } else {
                        lDelay *= rty.getMultiplyPeriod();
                    }
                    try {
                        setStatusToMail(mail, SEDOutboxMailStatus.SCHEDULE, "Resend message in '" + lDelay + "'ms");
                        sendMessage(idMsg, pModeID, iRet, lDelay);
                    } catch (NamingException | JMSException ex1) {
                        String errDesc = "Error resending message with id: '" + pModeID + "'!";
                        setStatusToMail(mail, SEDOutboxMailStatus.ERROR, errDesc + " " + ex.getMessage());
                        mlog.logError(t, errDesc, ex1);
                    }
                } else {
                    setStatusToMail(mail, SEDOutboxMailStatus.ERROR, ex.getMessage());
                }
            } else {
                setStatusToMail(mail, SEDOutboxMailStatus.ERROR, ex.getMessage());
            }

        }
        mlog.logEnd(t, idMsg);
    }

    public void setStatusToMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc) {
        long t = mlog.logStart();
        try {
            getUserTransaction().begin();
            mail.setStatusDate(Calendar.getInstance().getTime());
            mail.setStatus(status.getValue());
            // persist mail event
            MSHOutEvent me = new MSHOutEvent();
            me.setMailId(mail.getId());
            me.setDescription(desc == null ? status.getDesc() : desc);
            me.setStatus(mail.getStatus());
            me.setDate(mail.getStatusDate());
            me.setSenderMessageId(mail.getSenderMessageId());

            getEntityManager().merge(mail);
            getEntityManager().persist(me);
            getUserTransaction().commit();
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            {
                try {
                    getUserTransaction().rollback();
                } catch (IllegalStateException | SecurityException | SystemException ex1) {
                    // ignore 
                }
                mlog.logError(t, "Error commiting status to outboxmail: '" + mail.getId() + "'!", ex);
            }
        }

    }

    public boolean sendMessage(long biPosiljkaId, String strPmodeId, int retry, long delay) throws NamingException, JMSException {
        System.out.println("Resent " + retry + " delay : " + delay);
        boolean suc = false;
        InitialContext ic = null;
        Connection connection = null;
        try {
            ic = new InitialContext();
            ConnectionFactory cf = (ConnectionFactory) ic.lookup("/ConnectionFactory");
            Queue queue = (Queue) ic.lookup("java:/jms/queue/MSHQueue");
            connection = cf.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer sender = session.createProducer(queue);
            Message message = session.createMessage();

            message.setLongProperty(SEDValues.EBMS_QUEUE_PARAM_MAIL_ID, biPosiljkaId);
            message.setStringProperty(SEDValues.EBMS_QUEUE_PARAM_PMODE_ID, strPmodeId);

            message.setIntProperty(SEDValues.EBMS_QUEUE_PARAM_RETRY, retry);
            message.setLongProperty(SEDValues.EBMS_QUEUE_PARAM_DELAY, delay);
            message.setLongProperty(SEDValues.EBMS_QUEUE_DELAY_HQ, System.currentTimeMillis() + delay);

            sender.send(message);
            suc = true;
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

        }
    }

    private EntityManager getEntityManager() {
        // for jetty 
        if (memEManager == null) {
            try {
                InitialContext ic = new InitialContext();
                Context t = (Context) ic.lookup("__");
                listContext(t, "");
                System.out.println(" get em: " + getJNDIPrefix() + "ebMS_PU");
                memEManager = (EntityManager) ic.lookup(getJNDIPrefix() + "ebMS_PU");

            } catch (NamingException ex) {
                Logger.getLogger(MSHQueueBean.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(MSHQueueBean.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return mutUTransaction;
    }

    private String getJNDIPrefix() {
        return "__/";
        // return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/");
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
}
