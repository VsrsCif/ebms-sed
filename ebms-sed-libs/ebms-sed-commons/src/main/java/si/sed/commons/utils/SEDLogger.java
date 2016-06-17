/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import java.io.StringWriter;
import java.util.Calendar;
import org.apache.log4j.Logger;

/**
 *
 * @author logos
 */
public class SEDLogger {

    int miMethodStack = 3;
    private final Logger mlgLogger;

    public SEDLogger(Class clzz) {
        mlgLogger = Logger.getLogger(clzz != null ? clzz.getName() : this.getClass().getName());
    }

    public SEDLogger(Class clzz, int iLogStackMethodLevel) {
        mlgLogger = Logger.getLogger(clzz != null ? clzz.getName() : this.getClass().getName());
        miMethodStack = iLogStackMethodLevel;
    }

    protected String getCurrentMethodName() {
        return Thread.currentThread().getStackTrace().length > miMethodStack
                ? Thread.currentThread().getStackTrace()[miMethodStack].getMethodName() : "NULL METHOD";
    }

    public long getTime() {
        return Calendar.getInstance().getTimeInMillis();
    }

    public long log(final Object... param) {
        long mlTime = getTime();
        String strParams = null;
        if (param != null && param.length != 0) {
            StringWriter sw = new StringWriter();
            int i = 0;
            for (Object o : param) {
                if (i != 0) {
                    sw.append(",");
                }
                sw.append("'" + o + "' ");
            }
            strParams = sw.toString();
        }

        mlgLogger.info(getCurrentMethodName() + ":" + (strParams != null ? strParams : ""));
        return mlTime;
    }

    /*
    public void logEnd(long lTime ) {
        mlgLogger.info(getCurrentMethodName() + ": - END ( " + (getTime() - lTime) + " ms)");
    }*/
    public void logEnd(long lTime, final Object... param) {
        String strParams = "";
        if (param != null && param.length != 0) {
            StringWriter sw = new StringWriter();
            int i = 0;
            for (Object o : param) {
                sw.append((++i) + ".-> '" + o + "' ");
            }
            strParams = sw.toString();
        }

        mlgLogger.info(getCurrentMethodName() + ": - END ( " + (getTime() - lTime) + " ms) " + strParams);
    }

    public void logError(long lTime, String strMessage, Exception ex) {
        mlgLogger.error(getCurrentMethodName() + ": - ERROR MSG: '" + strMessage + "' ( " + (getTime() - lTime) + " ms )", ex);
    }

    public void logError(long lTime, Exception ex) {
        mlgLogger.error(getCurrentMethodName() + ": - ERROR MSG: '" + (ex != null ? ex.getMessage() : "") + "' ( " + (getTime() - lTime) + " ms )", ex);
    }

    public long logStart(final Object... param) {
        long mlTime = getTime();
        String strParams = null;
        if (param != null && param.length != 0) {
            StringWriter sw = new StringWriter();
            int i = 0;
            for (Object o : param) {
                if (i != 0) {
                    sw.append(",");
                }
                sw.append((++i) + ":'" + o + "'");
            }
            strParams = sw.toString();
        }

        mlgLogger.debug(getCurrentMethodName() + ": - BEGIN' " + (strParams != null ? " params: " + strParams : ""));
        return mlTime;
    }

    public void logWarn(long lTime, String strMessage, Exception ex) {
        mlgLogger.warn(getCurrentMethodName() + ": - Warn MSG: '" + strMessage + "' ( " + (getTime() - lTime) + " ms )", ex);
    }

}
