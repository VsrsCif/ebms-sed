/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.interceptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import si.jrc.msh.utils.EBMSLogUtils;
import si.sed.commons.utils.SEDLogger;
import si.jrc.msh.utils.EbMSConstants;

/**
 *
 * @author sluzba
 */
public class EBMSLogOutInterceptor extends AbstractSoapInterceptor {

    protected final SEDLogger mlog = new SEDLogger(EBMSLogOutInterceptor.class);

    public EBMSLogOutInterceptor() {
        super(Phase.POST_STREAM);
    }

    @Override
    public void handleMessage(SoapMessage msg) throws Fault {
        long l = mlog.logStart();
       
        boolean isRequestor = MessageUtils.isRequestor(msg);
        
        String base = (String) msg.getExchange().get(EbMSConstants.EBMS_CP_BASE_LOG_SOAP_MESSAGE_FILE);
        File f = EBMSLogUtils.getOutboundFileName(isRequestor, base);
        base = EBMSLogUtils.getBaseFileName(f);
        msg.getExchange().put(EbMSConstants.EBMS_CP_BASE_LOG_SOAP_MESSAGE_FILE, base);
        msg.getExchange().put(EbMSConstants.EBMS_CP_OUT_LOG_SOAP_MESSAGE_FILE, f);
        if (f != null) {
            SOAPMessage rq = msg.getContent(SOAPMessage.class);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                rq.writeTo(fos);
            } catch (IOException | SOAPException ex) {
                String errmsg = "Error storing outgoing mail to file: '" + f.getAbsolutePath() + "'. Error: " + ex.getMessage();
                mlog.logError(l, errmsg, null);
            }
        }
        mlog.logEnd(l);
    }

}
