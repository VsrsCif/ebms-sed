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

import java.util.UUID;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.outbox.mail.MSHOutMail;

/**
 * MISC Utils
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class Utils {

  /**
   *
   */
  private static Utils mInstance = null;

  /**
   *
   * @param strVal
   * @return
   */
  public static String getDomainFromAddress(String strVal) {
    if (isEmptyString(strVal)) {
      return "NO_DOMAIN";
    }
    if (strVal.contains("@")) {
      return strVal.substring(strVal.indexOf('@') + 1);
    }
    return strVal;

  }

  /**
   *
   * @return
   */
  public static synchronized Utils getInstance() {
    return mInstance = mInstance == null ? new Utils() : mInstance;
  }

  /**
   *
   * @param mim
   * @return
   */
  public static String getPModeIdFromInMail(MSHInMail mim) {
    if (mim == null) {
      return null;
    }
    return mim.getService() + ":" + getDomainFromAddress(mim.getSenderEBox());
  }

  /**
   *
   * @param mom
   * @return
   */
  public static String getPModeIdFromOutMail(MSHOutMail mom) {
    if (mom == null) {
      return null;
    }
    return mom.getService() + ":" + getDomainFromAddress(mom.getReceiverEBox());
  }

  /**
   *
   * @param strVal
   * @return
   */
  public static boolean isEmptyString(String strVal) {
    return strVal == null || strVal.trim().isEmpty();
  }

 
  private Utils() {

  }

  /**
   * Returns java.util.UUID.randomUUID() as string.
   *
   * @return uuid string representation.
   */
  public String getGuidString() {
    return UUID.randomUUID().toString();
  }
  
  public static String getUUID(String prefix) {
    return prefix + "-"+ UUID.randomUUID().toString();
  }
  public static String getUUIDWithDomain(String domain) {
    return UUID.randomUUID().toString() + "@" + domain ;
  }

}
