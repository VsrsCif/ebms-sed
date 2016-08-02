/*
 * Copyright 2015, Supreme Court Republic of Slovenia
 * 
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package si.sed.commons.utils;

import java.io.StringWriter;
import static java.lang.Thread.currentThread;
import static java.util.Calendar.getInstance;
import org.apache.log4j.Logger;
import static org.apache.log4j.Logger.getLogger;

/**
 * Common method logger. 
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class SEDLogger {

  int miMethodStack = 3;
  private final Logger mlgLogger;

  /**
   *
   * @param clzz
   */
  public SEDLogger(Class clzz) {
    mlgLogger = getLogger(clzz != null ? clzz.getName() : this.getClass().getName());
  }

  /**
   *
   * @param clzz
   * @param iLogStackMethodLevel
   */
  public SEDLogger(Class clzz, int iLogStackMethodLevel) {
    mlgLogger = getLogger(clzz != null ? clzz.getName() : this.getClass().getName());
    miMethodStack = iLogStackMethodLevel;
  }

  /**
   *
   * @return
   */
  protected String getCurrentMethodName() {
    return currentThread().getStackTrace().length > miMethodStack ? currentThread().getStackTrace()[miMethodStack]
        .getMethodName() : "NULL METHOD";
  }

  /**
   *
   * @return
   */
  public long getTime() {
    return getInstance().getTimeInMillis();
  }

  /**
   *
   * @param param
   * @return
   */
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

 
  /**
   *
   * @param lTime
   * @param param
   */
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

    mlgLogger.info(getCurrentMethodName() + ": - END ( " + (getTime() - lTime) + " ms) "
        + strParams);
  }

  /**
   *
   * @param lTime
   * @param strMessage
   * @param ex
   */
  public void logError(long lTime, String strMessage, Exception ex) {
    mlgLogger.error(getCurrentMethodName() + ": - ERROR MSG: '" + strMessage + "' ( "
        + (getTime() - lTime) + " ms )", ex);
  }

  /**
   *
   * @param lTime
   * @param ex
   */
  public void logError(long lTime, Exception ex) {
    mlgLogger.error(getCurrentMethodName() + ": - ERROR MSG: '"
        + (ex != null ? ex.getMessage() : "") + "' ( " + (getTime() - lTime) + " ms )", ex);
  }

  /**
   *
   * @param param
   * @return
   */
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

    mlgLogger.debug(getCurrentMethodName() + ": - BEGIN' "
        + (strParams != null ? " params: " + strParams : ""));
    return mlTime;
  }

  /**
   *
   * @param lTime
   * @param strMessage
   * @param ex
   */
  public void logWarn(long lTime, String strMessage, Exception ex) {
    mlgLogger.warn(getCurrentMethodName() + ": - Warn MSG: '" + strMessage + "' ( "
        + (getTime() - lTime) + " ms )", ex);
  }

}
