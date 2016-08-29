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

package si.sed.commons;

/**
 * Class contains EJB JNDI addresses
 * 
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */

public class SEDJNDI {

  /**
     *
     */
  public static final String JNDI_DBCERTSTORE =
      "java:global/ebms-sed-dao/DBCertStores!si.sed.commons.interfaces.DBCertStoresInterface";

  /**
     *
     */
  public static final String JNDI_DBSETTINGS =
      "java:global/ebms-sed-dao/DBSettings!si.sed.commons.interfaces.DBSettingsInterface";

  /**
     *
     */
  public static final String JNDI_JMSMANAGER =
      "java:global/ebms-sed-dao/JMSManager!si.sed.commons.interfaces.JMSManagerInterface";

  /**
     *
     */
  public static final String JNDI_SEDDAO =
      "java:global/ebms-sed-dao/SEDDaoBean!si.sed.commons.interfaces.SEDDaoInterface";

  /**
     *
     */
  public static final String JNDI_SEDLOOKUPS =
      "java:global/ebms-sed-dao/SEDLookups!si.sed.commons.interfaces.SEDLookupsInterface";

  /**
     *
     */
  public static final String JNDI_SEDREPORTS =
      "java:global/ebms-sed-dao/SEDReportBean!si.sed.commons.interfaces.SEDReportInterface";

  /**
     *
     */
  public static final String JNDI_SEDSCHEDLER =
      "java:global/ebms-sed-dao/MSHScheduler!si.sed.commons.interfaces.SEDSchedulerInterface";
  
  /**
     *
     */
  public static final String JNDI_PMODE =
      "java:global/ebms-sed-dao/PModeManagerBean!si.sed.commons.interfaces.PModeInterface";

}
