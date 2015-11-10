/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.ws;

import java.util.Calendar;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import org.apache.log4j.Logger;

/**
 *
 * @author sluzba
 */
public class JEELogInterceptor {

    private final Logger mlgLogger = Logger.getLogger(JEELogInterceptor.class);
            

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {

        long l = getTime();
        System.out.println(context.getMethod().getName() +  " BEGIN" );
        Object result = null;
        try {
            result = context.proceed();
        } catch (Exception e) {
            System.out.println(context.getMethod().getName() + " ERROR" + " time: "+getDuration(l)+" ms.");
            throw e;
        }
        System.out.println(context.getMethod().getName() + " END" + " time: "+getDuration(l)+" ms.");
        return result;
    }

    public long getTime() {
        return Calendar.getInstance().getTimeInMillis();
    }
    
     public long getDuration(long t) {
        return Calendar.getInstance().getTimeInMillis() -t;
    }
}
