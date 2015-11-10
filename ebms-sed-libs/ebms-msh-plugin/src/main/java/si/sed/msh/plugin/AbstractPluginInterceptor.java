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
package si.sed.msh.plugin;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Properties;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.msh.ebms.outbox.event.MSHOutEvent;
import org.msh.ebms.outbox.mail.MSHOutMail;
import si.sed.commons.SEDValues;
/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public abstract class AbstractPluginInterceptor extends AbstractSoapInterceptor {
    private static String EBMS_MSH_PLUGIN_PU = "ebMS_MSH_PLUGIN_PU";
     String LOADED_CLASSES = "hibernate.ejb.loaded.classes";

    public AbstractPluginInterceptor(String p) {
        super(p);
    }

    public AbstractPluginInterceptor(String i, String p) {
        super(i, p);
 
    }
 
    public EntityManagerFactory getSEDEntityManagerFactory(Class ... entCls){ 
        if (entCls!= null){
            Properties mp = new Properties();
            mp.put(LOADED_CLASSES,  Arrays.asList(entCls));
            return Persistence.createEntityManagerFactory(EBMS_MSH_PLUGIN_PU, mp);
        }
        return Persistence.createEntityManagerFactory(EBMS_MSH_PLUGIN_PU);
    }
    
    

    @Override
    public abstract void handleMessage(SoapMessage t) throws Fault;
    
    public void serializeMail(MSHOutMail mail, String userID, String applicationId, String pmodeId) {
        EntityManagerFactory emf = null;
        EntityManager em = null;

        // --------------------
        // serialize data to db
        try {
            emf = getSEDEntityManagerFactory();
            em = emf.createEntityManager();
         

            em.getTransaction().begin();

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

            em.getTransaction().commit();

            sendMessage(mail.getId(), pmodeId);
        } catch (Exception  ex) {
            if (em!= null){
                em.getTransaction().rollback();
            }
            ex.printStackTrace();
        }finally {
            if (em!= null){
                em.close();
            }
            if (emf!= null){
                emf.close();
            }
        }
        

    }

    public boolean sendMessage(BigInteger biPosiljkaId, String strpModeId) {

        boolean suc = false;
        InitialContext ic = null;
        Connection connection = null;
        try {
            ic = new InitialContext();
            ConnectionFactory cf = (ConnectionFactory) ic.lookup("/ConnectionFactory");
            Queue queue = (Queue) ic.lookup("java:/jms/" + SEDValues.EBMS_QUEUE_JNDI);
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

    protected void closeConnection(Connection con) {
        try {
            if (con != null) {
                con.close();
            }
        } catch (JMSException jmse) {
            // ignore
        }
    }
    
}
