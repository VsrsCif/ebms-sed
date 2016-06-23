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
package si.sed.msh.jms;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.SEDValues;
import si.sed.commons.utils.PModeManager;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.Utils;

/**
 *
 * @author Jože Rihtaršič
 */
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue =
            "Auto-acknowledge"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue =
            "javax.jms.Queue"),
    @ActivationConfigProperty(propertyName = "destination", propertyValue =
            "queue/SEDExecutionQueue"),
    @ActivationConfigProperty(propertyName = "maxSession",
            propertyValue = "${" +
            SEDSystemProperties.SYS_PROP_EXECUTION_WORKERS + ":5}")
})
@TransactionManagement(TransactionManagementType.BEAN)
public class MSHExecutionBean implements MessageListener {

    private static final SEDLogger mlog = new SEDLogger(MSHExecutionBean.class);

    PModeManager mpModeManager = new PModeManager();

    /**
     *
     * @param con
     */
    protected void closeConnection(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (JMSException jmse) {

        }
    }

    private String getJNDIPrefix() {

        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX,
                "java:/jboss/");
    }

    /*private String getJNDIPrefix() {
        return "__/";
        // return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/");
    }*/
    private String getJNDI_JMSPrefix() {
        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_JMS_PREFIX,
                "java:/jms/");
    }

    /**
     *
     * @param msg
     */
    @Override
    public void onMessage(Message msg) {
        try {
            long t = mlog.logStart();
            String command = msg.getStringProperty(SEDValues.EXEC_COMMAND);
            command = Utils.replaceProperties(command);
            String param = msg.getStringProperty(SEDValues.EXEC_PARAMS);

            ProcessBuilder builder = new ProcessBuilder(command, param);
            Process process = builder.start();
            long l = process.waitFor();

            mlog.log("execution of command " + command + " return value: " + l);
            mlog.logEnd(t);
        } catch (JMSException | IOException ex) {
            Logger.getLogger(MSHExecutionBean.class.getName()).log(Level.SEVERE,
                    null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(MSHExecutionBean.class.getName()).log(Level.SEVERE,
                    null, ex);
        }
    }

    /**
     *
     * @param biPosiljkaId
     * @param strPmodeId
     * @param retry
     * @param delay
     * @return
     * @throws NamingException
     * @throws JMSException
     */
    public boolean sendMessage(long biPosiljkaId, String strPmodeId, int retry,
            long delay)
            throws NamingException,
            JMSException {
        boolean suc = false;
        InitialContext ic = null;
        Connection connection = null;
        String msgFactoryJndiName = getJNDIPrefix() +
                SEDValues.EBMS_JMS_CONNECTION_FACTORY_JNDI;
        String msgQueueJndiName = getJNDI_JMSPrefix() +
                SEDValues.JNDI_QUEUE_EBMS;
        try {
            ic = new InitialContext();
            ConnectionFactory cf = (ConnectionFactory) ic.lookup(
                    msgFactoryJndiName);
            Queue queue = (Queue) ic.lookup(msgQueueJndiName);
            connection = cf.createConnection();
            Session session = connection.createSession(false,
                    Session.AUTO_ACKNOWLEDGE);
            MessageProducer sender = session.createProducer(queue);
            Message message = session.createMessage();

            message.setLongProperty(SEDValues.EBMS_QUEUE_PARAM_MAIL_ID,
                    biPosiljkaId);
            message.setStringProperty(SEDValues.EBMS_QUEUE_PARAM_PMODE_ID,
                    strPmodeId);

            message.setIntProperty(SEDValues.EBMS_QUEUE_PARAM_RETRY, retry);
            message.setLongProperty(SEDValues.EBMS_QUEUE_PARAM_DELAY, delay);
            message.setLongProperty(SEDValues.EBMS_QUEUE_DELAY_AMQ, delay);
            message.setLongProperty(SEDValues.EBMS_QUEUE_DELAY_Artemis,
                    System.currentTimeMillis() + delay);

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
}
