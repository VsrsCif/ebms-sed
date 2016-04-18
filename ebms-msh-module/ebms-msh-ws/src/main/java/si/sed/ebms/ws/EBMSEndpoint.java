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
package si.sed.ebms.ws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;
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
import javax.xml.bind.JAXBException;
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
import org.msh.ebms.inbox.payload.MSHInPart;
import org.msh.svev.pmode.PMode;
import org.sed.ebms.ebox.Export;
import org.sed.ebms.ebox.SEDBox;
import si.jrc.msh.utils.EBMSUtils;
import si.sed.commons.MimeValues;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.SEDDaoInterface;

import si.sed.commons.utils.HashUtils;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.StringFormater;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.xml.XMLUtils;

@WebServiceProvider(serviceName = "ebms")
@ServiceMode(value = Service.Mode.MESSAGE)
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
@org.apache.cxf.interceptor.InInterceptors(interceptors
        = {
            "si.jrc.msh.interceptor.EBMSLogInInterceptor",
            "si.jrc.msh.interceptor.EBMSInInterceptor",
            "si.jrc.msh.interceptor.MSHPluginInInterceptor"
        })
@org.apache.cxf.interceptor.OutInterceptors(interceptors
        = {
            "si.jrc.msh.interceptor.EBMSLogOutInterceptor",
            "si.jrc.msh.interceptor.EBMSOutInterceptor",
            "si.jrc.msh.interceptor.MSHPluginOutInterceptor"
        })
public class EBMSEndpoint implements Provider<SOAPMessage> {

    private static final SEDLogger LOG = new SEDLogger(EBMSEndpoint.class);

    @Resource
    WebServiceContext wsContext;

    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mDB;

    StorageUtils msuStorageUtils = new StorageUtils();
    HashUtils mpHU = new HashUtils();
    EBMSUtils mebmsUtils = new EBMSUtils();
    StringFormater msfFormat = new StringFormater();

    public EBMSEndpoint() {

    }

    @Override
    public SOAPMessage invoke(SOAPMessage request) {
        long l = LOG.logStart();
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

            String rName = inmail.getReceiverEBox();

            SEDBox sb = (SEDBox) ex.get(SEDBox.class);

            if (sb == null) {
                // return error
            } else if (inmail.getStatus().equals(SEDInboxMailStatus.RECEIVE.getValue())) {
                // othervise in ERROR or plugin_locked
                serializeMail(inmail, msg.getAttachments(), sb);
            }

        } catch (SOAPException ex) {
            LOG.logError(l, ex);
        }
        LOG.logEnd(l);
        return response;
    }

    private void serializeMail(MSHInMail mail, Collection<Attachment> lstAttch, SEDBox sb) {
        long l = LOG.logStart();
        // prepare mail to persist 
        Date dt = new Date();
        // set current status
        mail.setStatus(SEDInboxMailStatus.RECEIVED.getValue());
        mail.setStatusDate(dt);
        mail.setReceivedDate(dt);

        // --------------------
        // serialize data to db
        
            mDB.setStatusToInMail(mail, SEDInboxMailStatus.RECEIVED, null);
         
            // serialize to file
            Export e = sb.getExport();
            if (e != null && e.getActive()) {

                String val = msfFormat.format(e.getFileMask(), mail);
                int i = 1;
                try {
                    String folder = Utils.replaceProperties(e.getFolder());
                    File fld = new File(folder);
                    if (!fld.exists()) {
                        fld.mkdirs();
                    }
                    String filPrefix = fld.getAbsolutePath() + File.separator + val;
                    if (e.getExportMetaData()) {
                        String fileMetaData = filPrefix + "." + MimeValues.MIME_XML.getSuffix();
                        try {

                            XMLUtils.serialize(mail, fld.getAbsolutePath() + File.separator + val + "." + MimeValues.MIME_XML.getSuffix());
                        } catch (JAXBException | FileNotFoundException ex) {
                            LOG.logError(l, "Export metadata ERROR. Export file:" + fileMetaData + ".", ex);
                        }
                    }
                    for (MSHInPart mp : mail.getMSHInPayload().getMSHInParts()) {
                        msuStorageUtils.copyInFile(mp.getFilepath(), new File(filPrefix + "_" + i + "." + MimeValues.getSuffixBYMimeType(mp.getMimeType())));
                    }
                } catch (IOException | StorageException ex) {
                    LOG.logError(l, "Export ERROR", ex);
                }
            }

      
        LOG.logEnd(l);
    }

   

    private String getJNDIPrefix() {

        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/jboss/");
    }

    
}
