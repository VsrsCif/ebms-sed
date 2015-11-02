/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.interceptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;

import si.jrc.msh.exception.ExceptionUtils;
import si.jrc.msh.exception.SOAPExceptionCode;
import si.jrc.msh.utils.EBMSLogUtils;
import si.sed.commons.utils.SEDLogger;
import si.jrc.msh.utils.EbMSConstants;

/**
 *
 * @author sluzba
 */
public class EBMSLogInInterceptor extends AbstractSoapInterceptor {

    protected final SEDLogger mlog = new SEDLogger(EBMSLogInInterceptor.class);

    public EBMSLogInInterceptor() {
        super(Phase.RECEIVE);
    }

    @Override
    public void handleMessage(SoapMessage msg) throws Fault {
        long l = mlog.logStart();
        SoapVersion version = msg.getVersion();
        
        boolean isRequestor = MessageUtils.isRequestor(msg);
        String base = (String)msg.getExchange().get(EbMSConstants.EBMS_CP_BASE_LOG_SOAP_MESSAGE_FILE);
        File f = EBMSLogUtils.getInboundFileName(isRequestor, base);
        base = EBMSLogUtils.getBaseFileName(f);
        msg.getExchange().put(EbMSConstants.EBMS_CP_BASE_LOG_SOAP_MESSAGE_FILE, base);
        msg.getExchange().put(EbMSConstants.EBMS_CP_IN_LOG_SOAP_MESSAGE_FILE, f);
        
        try {
         
            InputStream is = msg.getContent(InputStream.class);
            if (is != null) {
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    InputStream bis = is instanceof DelegatingInputStream ? ((DelegatingInputStream) is).getInputStream() : is;
                    IOUtils.copy(bis, fos);
                    fos.flush();
                    msg.getExchange().put(EbMSConstants.EBMS_CP_IN_LOG_SOAP_MESSAGE_FILE, f);
                }
                // restore soap message input stream
                FileInputStream fis = new FileInputStream(f);
                if (is instanceof DelegatingInputStream) {
                    ((DelegatingInputStream) is).setInputStream(fis);
                } else {
                    msg.setContent(InputStream.class, fis);
                }
            } else {
                String errmsg = "Application error store inbound message to file! No input stream!";
                mlog.logError(l, errmsg, null);
                throw ExceptionUtils.createSoapFault(SOAPExceptionCode.StoreInboundMailFailure, version.getReceiver());
            }
        } catch (IOException ex) {
            String errmsg = "Application error store inbound message to file: '" + (f != null ? f.getAbsolutePath() : "null") + "'! Error: " + ex.getMessage();
            mlog.logError(l, errmsg, ex);
            throw ExceptionUtils.createSoapFault(SOAPExceptionCode.StoreInboundMailFailure, version.getReceiver());
        }
        mlog.logEnd(l);
    }

   

}
