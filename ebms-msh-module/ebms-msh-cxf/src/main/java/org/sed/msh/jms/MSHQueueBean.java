/*
 * Copyright 2016, Supreme Court Republic of Slovenia
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
package org.sed.msh.jms;

import java.math.BigInteger;
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
import org.msh.sed.pmode.ReceptionAwareness;
import si.jrc.msh.client.MshClient;
import si.jrc.msh.client.Result;
import si.jrc.msh.exception.EBMSErrorCode;
import si.sed.commons.MimeValues;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.SEDValues;
import si.sed.commons.exception.PModeException;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.JMSManagerInterface;
import si.sed.commons.interfaces.PModeInterface;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.pmode.EBMSMessageContext;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.Utils;

/**
 *
 * @author Jože Rihtaršič
 */
@MessageDriven(
    activationConfig = {
      @ActivationConfigProperty(propertyName = "acknowledgeMode",
          propertyValue = "Auto-acknowledge"),
      @ActivationConfigProperty(propertyName = "destinationType",
          propertyValue = "javax.jms.Queue"),
      @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/MSHQueue"),
      @ActivationConfigProperty(propertyName = "maxSession",
          propertyValue = "${org.sed.msh.sender.workers.count}")})
@TransactionManagement(TransactionManagementType.BEAN)
public class MSHQueueBean implements MessageListener {

  /**
   *
   */
  public static final SEDLogger LOG = new SEDLogger(MSHQueueBean.class);

  @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
  SEDDaoInterface mDB;

  @EJB(mappedName = SEDJNDI.JNDI_JMSMANAGER)
  JMSManagerInterface mJMS;

  @EJB
  MshClient mmshClient;

  @EJB(mappedName = SEDJNDI.JNDI_PMODE)
  PModeInterface mpModeManager;
  StorageUtils msStorageUtils = new StorageUtils();
  /**
   * implementation of onMessage methods for submiting MSH out user message.
   *
   * @param msg jms wiht parameters Mail id, jmsRetryCount and jmsRetryDelay
   */
  @Override
  public void onMessage(Message msg) {
    long t = LOG.logStart();
    // parse JMS Message data 
    long jmsMessageId; // 
    int jmsRetryCount = 0;
    long jmsRetryDelay = -1;
    // Read property Mail ID
    try {
      jmsMessageId = msg.getLongProperty(SEDValues.EBMS_QUEUE_PARAM_MAIL_ID);
    } catch (JMSException ex) {
      LOG.logError(t, String.format("Bad JMS message for queue: 'MSHQueue' with no property: '%s'",
          SEDValues.EBMS_QUEUE_PARAM_MAIL_ID), ex);
      return;
    }

    try {
      jmsRetryCount = msg.getIntProperty(SEDValues.EBMS_QUEUE_PARAM_RETRY);
    } catch (JMSException ex) {
      LOG.logError(t, String.format("JMS message for queue: 'MSHQueue' with no property: '%s'",
          SEDValues.EBMS_QUEUE_PARAM_RETRY), ex);
    }
    try {
      jmsRetryDelay = msg.getLongProperty(SEDValues.EBMS_QUEUE_PARAM_DELAY);
    } catch (JMSException ex) {
      LOG.logError(t, String.format("JMS message for queue: 'MSHQueue' with no property: '%s'",
          SEDValues.EBMS_QUEUE_PARAM_DELAY), ex);
    }

    MSHOutMail mail;
    try {
      mail = mDB.getMailById(MSHOutMail.class, BigInteger.valueOf(jmsMessageId));
    } catch (NoResultException ex) {
      LOG.logError(t, "Message with id: '" + jmsMessageId + "' not exists in DB!", ex);
      return;
    }
    LOG.formatedlog("Get EBMSMessageContext for message: %s", jmsMessageId);
    // get pmode EBMSMessageContext
    EBMSMessageContext sd;
    try {
      sd = mpModeManager.createMessageContextForOutMail(mail);
    } catch (PModeException ex) {
      String errDesc = String.format(
          "Error retrieving EBMSMessageContext for message id: '%d'. Error: %s",
          jmsMessageId, ex.getMessage());
      LOG.logError(t, errDesc, ex);
      setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, errDesc, ex);
      return;
    }

    // create ebms-message id
    if (Utils.isEmptyString(mail.getMessageId())) {
      mail.setMessageId(Utils.getUUIDWithDomain(sd.getSenderPartyIdentitySet().getDomain()));
    }

    if (!sd.isPushTransfrer()) {
      setStatusToOutMail(mail, SEDOutboxMailStatus.PULLREADY, "Message ready for pull signal!");
      LOG.formatedlog("Start pushing  message: %s", jmsMessageId);
    } else {
      LOG.formatedlog("Start pushing  message: %s", jmsMessageId);
      setStatusToOutMail(mail, SEDOutboxMailStatus.PUSHING, "Start pushing to receiver MSH");
      // transport protocol

      Result sm = mmshClient.pushMessage(mail, sd);
      if (sm.getError() != null) {
        setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, sm.getError().getSubMessage(),
            sm.getResultFile(), sm.getMimeType());
        
        if (sm.getError().getEbmsErrorCode().equals(EBMSErrorCode.ConnectionFailure) ||
            sm.getError().getEbmsErrorCode().equals(EBMSErrorCode.DeliveryFailure) ||
            sm.getError().getEbmsErrorCode().equals(EBMSErrorCode.Other)) {
          resendMail(mail, sd, jmsRetryCount, jmsRetryDelay);
        }
      } else {

        setStatusToOutMail(mail, SEDOutboxMailStatus.SENT, "Message sent to receiver MSH",
            sm.getResultFile(), sm.getMimeType());
      }

    }
    LOG.logEnd(t, jmsMessageId);
  }

  /**
   * Method sets message back in queue for sending
   *
   * @param mail - mail to be resend
   * @param sd
   * @param jmsRetryCount
   * @param jmsRetryDelay
   */
  public void resendMail(MSHOutMail mail, EBMSMessageContext sd, int jmsRetryCount,
      long jmsRetryDelay) {
    long t = LOG.logStart();
    LOG.formatedlog("Resend mail: %d retry %d delay %d", mail.getId(), jmsRetryCount, jmsRetryDelay);
    ReceptionAwareness.Retry rt = null;
    if (sd.getReceptionAwareness() != null && sd.getReceptionAwareness().getRetry() != null) {
      ReceptionAwareness.Retry rty = sd.getReceptionAwareness().getRetry();

      if (jmsRetryCount < rty.getMaxRetries()) {
        jmsRetryCount++;
        if (jmsRetryDelay <= 0) {
          jmsRetryDelay = rty.getPeriod();
        } else {
          jmsRetryDelay *= rty.getMultiplyPeriod();
        }
        try {
          setStatusToOutMail(mail, SEDOutboxMailStatus.SCHEDULE, "Resend message in '" +
              jmsRetryDelay + "'ms");

          mJMS.sendMessage(mail.getId().longValue(), jmsRetryCount, jmsRetryDelay, false);
        } catch (NamingException | JMSException ex1) {
          String errDesc = "Error occured while resending message with id: '" + mail.getId() +
              "'!";
          LOG.logError(t, errDesc, ex1);
          setStatusToOutMail(mail, SEDOutboxMailStatus.ERROR, errDesc + " " + ex1.getMessage());
        }
      }

    }

  }

  /**
   * Set status to out mail - method chages mail status log event to "event table"
   *
   * @param mail -
   * @param status - status
   * @param desc - description of event
   * @param ex - if throwable not nul "getMessage()" is appended to desc parameter
   */
  public void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc,
      Throwable ex) {
    String strpath = null;
    if (ex != null) {
      String msg = String.format("%s, Error: %s", desc, ex.getMessage());
      try {
        strpath = msStorageUtils.storeThrowableAndGetRelativePath(ex);
      } catch (StorageException ex1) {
        LOG.logError(LOG.getTime(), "Error storing evidence error", ex1);
      }
    }
    if (Utils.isEmptyString(strpath)) {
      setStatusToOutMail(mail, status, desc);
    } else {
      setStatusToOutMail(mail, status, desc, strpath, MimeValues.MIME_TXT.getMimeType());
    }

  }

  /**
   * Set status to out mail - method chages mail status log event to "event table"
   *
   * @param mail -
   * @param status - status
   * @param desc - description of event
   */
  public void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc) {
    long l = LOG.logStart();
    try {
      mDB.setStatusToOutMail(mail, status, desc);
    } catch (StorageException ex2) {
      LOG.logError(l,
          "Error occurred while setting status " + status.getValue() + " to MSHOutMail :'" +
          mail.getId() +
          "'!", ex2);
    }
  }

  public void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc,
      String fileName, String mime) {
    long l = LOG.logStart();
    try {
      mDB.setStatusToOutMail(mail, status, desc, null, null, fileName, mime);
    } catch (StorageException ex2) {
      LOG.logError(l,
          "Error occurred while setting status " + status.getValue() + " to MSHOutMail :'" +
          mail.getId() +
          "'!", ex2);
    }
  }

}
