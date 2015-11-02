/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.client;

import java.util.UUID;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.crypto.SecretKey;
import javax.xml.namespace.QName;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.DispatchImpl;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.msh.svev.pmode.PMode;
import si.jrc.msh.exception.MSHException;
import si.jrc.msh.exception.MSHExceptionCode;

import si.jrc.msh.interceptor.EBMSInInterceptor;
import si.jrc.msh.interceptor.EBMSOutInterceptor;
import si.sed.commons.utils.SEDLogger;


import si.jrc.msh.utils.SvevUtils;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.StorageUtils;

/**
 *
 * @author sluzba
 */
public class MshClient {

    protected final SEDLogger mlog = new SEDLogger(MshClient.class);
    SvevUtils msvevUtils = new SvevUtils();
    public void sendMessage(MSHOutMail mail, PMode pmode) throws MSHException {

        long l = mlog.logStart(mail);

        if (mail == null) {
            MSHException me = new MSHException(MSHExceptionCode.EmptyMail);
            mlog.logError(l, me.getMessage(), null);
            throw me;
        }

        // create client (define bus)
        Dispatch<SOAPMessage> client = getClient(pmode);

        DispatchImpl dimpl = (org.apache.cxf.jaxws.DispatchImpl) client;

        // configure svev-msh transport
        client.getRequestContext().put(PMode.class.getName(), pmode);
        client.getRequestContext().put(MSHOutMail.class.getName(), mail);
               
    
        // create empty soap mesage
        MessageFactory mf;
        SOAPMessage soapReq;
        try {
            mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            soapReq = mf.createMessage();
        } catch (SOAPException ex) {
            throw new MSHException(MSHExceptionCode.EmptyMail, ex);
        }

        try {
            for (MSHOutPart p : mail.getMSHOutPayload().getMSHOutParts()) {
                String id = UUID.randomUUID().toString();
                p.setEbmsId(id);
                AttachmentPart apr = soapReq.createAttachmentPart();
                apr.setContentId(id);
                apr.setMimeHeader("id", id);
                DataHandler dh = new DataHandler(new FileDataSource(StorageUtils.getFile(p.getFilepath())));
                apr.setDataHandler(dh);
                soapReq.addAttachmentPart(apr);
            }

        } catch (StorageException ex) {
            throw new MSHException(MSHExceptionCode.InvalidMail, ex);
        }
        dimpl.invoke(soapReq);

        mlog.logEnd(l);
    }
    

    public Dispatch<SOAPMessage> getClient(PMode pmode) throws MSHException {

         if (pmode.getLegs().isEmpty() || pmode.getLegs().get(0).getProtocol() == null
                || pmode.getLegs().get(0).getProtocol().getAddress() == null
                || pmode.getLegs().get(0).getProtocol().getAddress().trim().isEmpty()) {
            throw new MSHException(MSHExceptionCode.InvalidPMode, pmode.getId(), "Missing Protocol/Address value");
        }
         
        // get sending leg!
        String url = pmode.getLegs().get(0).getProtocol().getAddress();

        QName serviceName1 = new QName("", "");
        QName portName1 = new QName("", "");
        Service s = Service.create(serviceName1);

        s.addPort(portName1, SOAPBinding.SOAP12HTTP_MTOM_BINDING, url);
        // AddressingFeature addrFt = new AddressingFeature(true, true);
        MTOMFeature mtomFt = new MTOMFeature(true);

        Dispatch<SOAPMessage> dispSOAPMsg = s.createDispatch(portName1, SOAPMessage.class, Service.Mode.MESSAGE, mtomFt);
        DispatchImpl dimpl = (org.apache.cxf.jaxws.DispatchImpl) dispSOAPMsg;
        SOAPBinding sb = (SOAPBinding) dispSOAPMsg.getBinding();
        sb.setMTOMEnabled(true);
        // configure interceptors
        Client cxfClient = dimpl.getClient();
        cxfClient.getInInterceptors().add(new LoggingInInterceptor());
        //cxfClient.getInInterceptors().add(new EBMSLogInInterceptor());
        cxfClient.getInInterceptors().add(new EBMSInInterceptor());

        cxfClient.getOutInterceptors().add(new EBMSOutInterceptor());
        cxfClient.getOutInterceptors().add(new LoggingOutInterceptor());
        //cxfClient.getOutInterceptors().add(new EBMSLogOutInterceptor());

        return dispSOAPMsg;
    }

  

    
}
