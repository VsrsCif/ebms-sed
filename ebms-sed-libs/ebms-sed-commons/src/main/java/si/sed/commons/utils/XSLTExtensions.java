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

import static com.jrc.xml.DateAdapter.parseDateTime;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.getInstance;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class XSLTExtensions {

  private static final SimpleDateFormat S_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");
  private static final SimpleDateFormat S_DATE_TIME_FORMAT = new SimpleDateFormat(
      "dd.MM.yyyy HH:mm:ss");

  /**
   *
   * @return
   */
  public static Object currentDate() {
    return S_DATE_FORMAT.format(getInstance().getTime());
  }

  /**
   *
   * @return
   */
  public static Object currentDateTime() {
    return S_DATE_TIME_FORMAT.format(getInstance().getTime());
  }

  /**
   *
   * @param str
   * @return
   */
  public static Object formatDate(String str) {
    if (str == null || str.trim().isEmpty()) {
      return null;
    }

    Date dt = parseDateTime(str);
    return S_DATE_FORMAT.format(dt);
  }

  /**
   *
   * @param str
   * @return
   */
  public static Object getZPPFictionDate(String str) {
    if (str == null || str.trim().isEmpty()) {
      return null;
    }
    Date dt = parseDateTime(str);
    Calendar c = new GregorianCalendar();
    c.setTime(dt);
    c.add(DAY_OF_MONTH, 15);
    return S_DATE_FORMAT.format(c.getTime());

  }

}
