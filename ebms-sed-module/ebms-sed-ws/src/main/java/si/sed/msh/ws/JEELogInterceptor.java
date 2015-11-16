/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.ws;

import java.util.Calendar;
import javax.annotation.Resource;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import org.apache.log4j.Logger;

/**
 *
 * @author sluzba
 */
public class JEELogInterceptor {

    @Resource
    WebServiceContext mwsCtxt;

    private final Logger mlgLogger = Logger.getLogger(JEELogInterceptor.class);

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {
        String ip = getCurrrentRemoteIP();

        long l = getTime();
        mlgLogger.debug(context.getMethod().getName() + " " + ip + " BEGIN");
        Object result = null;
        try {
            result = context.proceed();
        } catch (Exception e) {
            mlgLogger.error(context.getMethod().getName() + " " + ip + " ERROR" + " time: " + getDuration(l) + " ms.");
            throw e;
        }
        mlgLogger.info(context.getMethod().getName() + " " + ip + " END" + " time: " + getDuration(l) + " ms.");
        return result;
    }

    public long getTime() {
        return Calendar.getInstance().getTimeInMillis();
    }

    public long getDuration(long t) {
        return Calendar.getInstance().getTimeInMillis() - t;
    }

    protected String getCurrrentRemoteIP() {
        String clientIP = "";
        if (mwsCtxt != null) {
            try {

                MessageContext msgCtxt = mwsCtxt.getMessageContext();
                HttpServletRequest req = (HttpServletRequest) msgCtxt.get(MessageContext.SERVLET_REQUEST);
                clientIP = req.getRemoteAddr();
            } catch (Exception exc) {
                mlgLogger.error("JEELogInterceptor.getCurrrentRemoteIP  ERROR", exc);
            }
        }
        return clientIP;
    }
}
