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
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.NamingException;
import javax.persistence.NoResultException;
import org.msh.ebms.outbox.mail.MSHOutMail;

import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.ReceptionAwareness;
import si.jrc.msh.client.MshClient;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDOutboxMailStatus;

import si.sed.commons.SEDValues;
import si.sed.commons.interfaces.JMSManagerInterface;
import si.sed.commons.interfaces.SEDDaoInterface;
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
    @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "${org.sed.msh.sender.workers.count}")
})
@TransactionManagement(TransactionManagementType.BEAN)
public class MSHQueueBean implements MessageListener {

    SEDLogger mlog = new SEDLogger(MSHQueueBean.class);
    PModeManager mpModeManager = new PModeManager();
    MshClient mmshClient = new MshClient();

    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mDB;

    @EJB(mappedName = SEDJNDI.JNDI_JMSMANAGER)
    JMSManagerInterface mJMS;

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

        MSHOutMail mail;
        try {
            mail = mDB.getMailById(MSHOutMail.class, BigInteger.valueOf(idMsg));
        } catch (NoResultException ex) {
            mlog.logError(t, "Message with id: '" + idMsg + "' not exists!", ex);
            return;
        }

        if (pModeID == null || pModeID.isEmpty()) {

            String recDomain = mail.getReceiverEBox().substring(mail.getReceiverEBox().indexOf("@") + 1).trim();
            String sendDomain = mail.getSenderEBox().substring(mail.getSenderEBox().indexOf("@") + 1).trim();
            pModeID = mail.getService() + ":" + sendDomain + ":" + recDomain;

        }

        // get pmode 
        PMode pMode = mpModeManager.getPModeById(pModeID);
        if (pMode == null) {
            String errDesc = "PMode with id: '" + pModeID + "' not exists! Message with id '" + idMsg + "' is not procesed!";
            mlog.logError(t, errDesc, null);
            mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, errDesc);
            return;
        }
        // start sending        

        mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.SENDING, null);
        try {
            mmshClient.sendMessage(mail, pMode);
            mail.setSentDate(Calendar.getInstance().getTime());
            mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.SENT, null);

        } catch (Exception ex) {
            ex.printStackTrace();
            mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, ex.getMessage());
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
                        mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.SCHEDULE, "Resend message in '" + lDelay + "'ms");
                        mJMS.sendMessage(idMsg, pModeID, iRet, lDelay, false);
                    } catch (NamingException | JMSException ex1) {
                        String errDesc = "Error resending message with id: '" + pModeID + "'!";
                        mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, errDesc + " " + ex.getMessage());
                        mlog.logError(t, errDesc, ex1);
                    }
                } else {
                    mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, ex.getMessage());
                }
            } else {
                mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, ex.getMessage());
            }

        }
        mlog.logEnd(t, idMsg);
    }

}
