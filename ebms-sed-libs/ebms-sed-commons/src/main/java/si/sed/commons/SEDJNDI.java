/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons;

/**
 *
 * @author sluzba
 */
public class SEDJNDI {
    public static final String JNDI_DBCERTSTORE ="java:global/ebms-sed-dao/DBCertStores!si.sed.commons.interfaces.DBCertStoresInterface";
    public static final String JNDI_DBSETTINGS="java:global/ebms-sed-dao/DBSettings!si.sed.commons.interfaces.DBSettingsInterface";
    public static final String JNDI_JMSMANAGER="java:global/ebms-sed-dao/JMSManager!si.sed.commons.interfaces.JMSManagerInterface";
    public static final String JNDI_SEDDAO="java:global/ebms-sed-dao/SEDDaoBean!si.sed.commons.interfaces.SEDDaoInterface";
    public static final String JNDI_SEDLOOKUPS="java:global/ebms-sed-dao/SEDLookups!si.sed.commons.interfaces.SEDLookupsInterface";
    public static final String JNDI_SEDSCHEDLER="java:global/ebms-sed-dao/MSHScheduler!si.sed.commons.interfaces.SEDSchedulerInterface";
    
    
}
