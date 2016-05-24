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
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.rmi.CORBA.Util;
import org.msh.ebms.outbox.mail.MSHOutMail;

import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.ReceptionAwareness;
import si.jrc.msh.client.MshClient;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDOutboxMailStatus;

import si.sed.commons.SEDValues;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.JMSManagerInterface;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.utils.PModeManager;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.Utils;

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

    public static final SEDLogger LOG = new SEDLogger(MSHQueueBean.class);
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
        long t = LOG.logStart();
        // get outbox mail
        long idMsg;
        String pModeID;
        // Read property get id
        try {
            idMsg = msg.getLongProperty(SEDValues.EBMS_QUEUE_PARAM_MAIL_ID);
        } catch (JMSException ex) {
            LOG.logError(t, "Bad message with no property:'" + SEDValues.EBMS_QUEUE_PARAM_MAIL_ID + "'!", ex);
            return;
        }

        // get pmode
        try {
            pModeID = msg.getStringProperty(SEDValues.EBMS_QUEUE_PARAM_PMODE_ID);

        } catch (JMSException ex) {
            LOG.logError(t, "Bad message with no property:'" + SEDValues.EBMS_QUEUE_PARAM_PMODE_ID + "'!", ex);
            return;
        }

        MSHOutMail mail;
        try {
            mail = mDB.getMailById(MSHOutMail.class, BigInteger.valueOf(idMsg));
        } catch (NoResultException ex) {
            LOG.logError(t, "Message with id: '" + idMsg + "' not exists!", ex);
            return;
        }

        if (pModeID == null || pModeID.isEmpty()) {
            pModeID = Utils.getPModeIdFromOutMail(mail);
        }

        // get pmode 
        PMode pMode = mpModeManager.getPModeById(pModeID);
        if (pMode == null) {
            String errDesc = "PMode with id: '" + pModeID + "' not exists! Message with id '" + idMsg + "' is not procesed!";
            LOG.logError(t, errDesc, null);
            try {
                mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, errDesc);
            } catch (StorageException ex) {
                LOG.logError(t, "Error setting status ERROR to MSHOutMail :'" + mail.getId() + "'!", ex);
            }
            return;
        }
        // start sending        

        try {
            mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.SENDING, "Start sending to receiver MSH");
        } catch (StorageException ex) {
            LOG.logError(t, "Error setting status SENDING to MSHOutMail :'" + mail.getId() + "'!", ex);
            return;
        }

        try {
            mmshClient.sendMessage(mail, pMode);
        } catch (Exception ex) {
            LOG.logError(t, "Error occurred while submitting mail  to receiver MSH:'" + mail.getId() + "'!", ex);

            try {
                mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, ex.getMessage());
            } catch (StorageException ex1) {
                LOG.logError(t, "Error setting status ERROR to MSHOutMail :'" + mail.getId() + "'!", ex1);
                return;
            }

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
                        try {
                            mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.SCHEDULE, "Resend message in '" + lDelay + "'ms");
                        } catch (StorageException ex1) {
                            LOG.logError(t, "Error occurred while setting status SCHEDULE to MSHOutMail :'" + mail.getId() + "'!", ex1);
                            return;
                        }
                        mJMS.sendMessage(idMsg, pModeID, iRet, lDelay, false);
                    } catch (NamingException | JMSException ex1) {
                        String errDesc = "Error occured while resending message with id: '" + pModeID + "'!";
                        LOG.logError(t, errDesc, ex1);
                        
                        try {
                            mDB.setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, errDesc + " " + ex.getMessage());
                        } catch (StorageException ex2) {
                            LOG.logError(t, "Error occurred while setting status ERROR to MSHOutMail :'" + mail.getId() + "'!", ex2);
                            return;
                        }
                        
                    }
                }
            }

        }
        LOG.logEnd(t, idMsg);
    }

}
