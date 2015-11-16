/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package si.sed.ebms.ws;

import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
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
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceProvider;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.msh.ebms.inbox.event.MSHInEvent;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.svev.pmode.PMode;
import si.jrc.msh.utils.EBMSUtils;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDSystemProperties;

import si.sed.commons.utils.HashUtils;
import si.sed.commons.utils.StorageUtils;

@WebServiceProvider(serviceName = "ebms-msh")
@ServiceMode(value = Service.Mode.MESSAGE)
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
@org.apache.cxf.interceptor.InInterceptors(interceptors
        = {
            "org.apache.cxf.interceptor.LoggingInInterceptor",
            "si.jrc.msh.interceptor.EBMSLogInInterceptor",
            "si.jrc.msh.interceptor.EBMSInInterceptor",
            "si.sed.msh.plugin.MSHPluginInInterceptor"
        })
@org.apache.cxf.interceptor.OutInterceptors(interceptors
        = {
            "org.apache.cxf.interceptor.LoggingOutInterceptor",
            "si.jrc.msh.interceptor.EBMSLogOutInterceptor",
            "si.jrc.msh.interceptor.EBMSOutInterceptor",
            "si.sed.msh.plugin.MSHPluginOutInterceptor"
        })
public class EBMSEndpoint implements Provider<SOAPMessage> {

    @Resource
    WebServiceContext wsContext;

    @Resource
    private UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_MSH_PU")
    private EntityManager memEManager;

    StorageUtils msuStorageUtils = new StorageUtils();
    HashUtils mpHU = new HashUtils();
    EBMSUtils mebmsUtils = new EBMSUtils();

    public EBMSEndpoint() {

    }

    @Override
    public SOAPMessage invoke(SOAPMessage request) {
        SOAPMessage response = null;
        try {
            // create empty response
            MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            response = mf.createMessage();

            // Using this cxf specific code you can access the CXF Message and Exchange objects
            WrappedMessageContext wmc = (WrappedMessageContext) wsContext.getMessageContext();
            Message msg = wmc.getWrappedMessage();
            Exchange ex = msg.getExchange();

            PMode pmd = (PMode) ex.get(PMode.class);
            MSHInMail inmail = (MSHInMail) ex.get(MSHInMail.class);

            serializeMail(inmail, msg.getAttachments());

           // SignalMessage as4Receipt = mebmsUtils.generateAS4ReceiptSignal(inmail.getMessageId(), Utils.getDomainFromAddress(inmail.getReceiverEBox()), request.getSOAPPart().getDocumentElement());
           // msg.getExchange().put(SignalMessage.class, as4Receipt);

            /*   EBMSError err = ex.get(EBMSError.class);
                Messaging mgsInboundMessage = ex.get(Messaging.class);
                PMode pmd = ex.get(PMode.class);
                
                if (inMail != null) {
                MSHMail mm = inMail.getMshMail();
                //if (mm.getService())                
                mDB.persist(inMail);

                if (SVEVConstants.SVEV_ACTION_DeliveryOfAdvice.equals(mm.getAction())) {
                MSHMail outMail = mDB.getMSHIngoingMailByMessageId(mm.getConversationId());
                // ex.put(SVEVEncryptionKey.class, outMail.getSVEVEncryptionKey());
                }
                }
                
                File fInMessageRequest = (File) ex.get(EbMSConstants.ContextProperty_In_SOAP_Message_File);
                System.out.println("Got File: " + fInMessageRequest);
                // check if receiver exists
                // check payloads
                     } catch (SEDException_Exception ex1) {
                Logger.getLogger(EBMSEndpoint.class.getName()).log(Level.SEVERE, null, ex1);
            }*/
        } catch (SOAPException ex) {
            ex.printStackTrace();
        }
        return response;
    }

    private void serializeMail(MSHInMail mail, Collection<Attachment> lstAttch) {

        // prepare mail to persist 
        Date dt = new Date();
        // set current status
        mail.setStatus(SEDInboxMailStatus.RECEIVED.getValue());
        mail.setStatusDate(dt);
        mail.setReceivedDate(dt);

        /*
        // --------------------
        // serialize payload
        try {

            if (mail.getMSHInPayload() != null && !mail.getMSHInPayload().getMSHInParts().isEmpty()) {
                for (MSHInPart p : mail.getMSHInPayload().getMSHInParts()) {
                    DataHandler dh = null;
                    for (Attachment a : lstAttch) {
                        if (a.getId().equals(p.getEbmsId())) {
                            dh = a.getDataHandler();
                            break;
                        }
                    }

                    File fout = null;
                    if (dh != null) {
                        fout = msuStorageUtils.storeInFile(p.getMimeType(), dh.getInputStream());
                    }
                    // set MD5 and relative path;
                    if (fout != null) {
                        String strMD5 = mpHU.getMD5Hash(fout);
                        String relPath = StorageUtils.getRelativePath(fout);
                        p.setFilepath(relPath);
                        p.setMd5(strMD5);

                        if (Utils.isEmptyString(p.getFilename())) {
                            p.setFilename(fout.getName());
                        }
                        if (Utils.isEmptyString(p.getName())) {
                            p.setName(p.getFilename().substring(p.getFilename().lastIndexOf(".")));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            /*
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
            msherr.setMessage(ex.getMessage());
            throw new SEDException_Exception("Error occured while storing payload", msherr, ex);* /
        } catch (StorageException ex) {
            /*
            SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
            msherr.setMessage(ex.getMessage());
            throw new SEDException_Exception("Error occured while storing payload", msherr, ex);* /
        } catch (HashException ex) {
            /*SEDException msherr = new SEDException();
            msherr.setErrorCode(SEDExceptionCode.SERVER_ERROR);
            msherr.setMessage(ex.getMessage());
            throw new SEDException_Exception("Error occured while calculating payload hash (MD5)", msherr, ex);* /
        }
    
         */
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
    private UserTransaction getUserTransaction() {
        // for jetty 
        if (mutUTransaction == null) {
            try {
                InitialContext ic = new InitialContext();
                
                mutUTransaction = (UserTransaction) ic.lookup(getJNDIPrefix() +"UserTransaction");

            } catch (NamingException ex) {
                Logger.getLogger(EBMSEndpoint.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return mutUTransaction;
    }
     private String getJNDIPrefix(){
         
        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/");
    }
       private EntityManager getEntityManager() {
        // for jetty 
        if (memEManager == null) {
            try {
                InitialContext ic = new InitialContext();
                Context t = (Context) ic.lookup("java:comp");               
                memEManager = (EntityManager) ic.lookup(getJNDIPrefix() +"ebMS_PU");

            } catch (NamingException ex) {
                Logger.getLogger(EBMSEndpoint.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return memEManager;
    }
}
