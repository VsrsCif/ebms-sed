/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import si.sed.commons.interfaces.DBCertStoresInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.msh.ebms.cert.MSHCertStore;
import org.msh.ebms.cert.MSHCertificate;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.interfaces.DBSettingsInterface;

/**
 *
 * @author sluzba
 */
@Startup
@Singleton
@AccessTimeout(value = 60000)
@Local(DBCertStoresInterface.class)
@TransactionManagement(TransactionManagementType.BEAN)
public class DBCertStores implements DBCertStoresInterface {

    @EJB (mappedName = "java:global/ebms-sed-dao/DBSettings!si.sed.commons.interfaces.DBSettingsInterface")
    DBSettingsInterface mdsSettings;

    @Resource
    public UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_SED_PU", name = "ebMS_SED_PU")
    public EntityManager memEManager;

    protected SEDLogger mlog = new SEDLogger(DBCertStores.class);
    final protected List<MSHCertStore> mprpCertStores = new ArrayList<>();
    protected TimerTask mRefreshTask;
    protected Timer mtTimer = new Timer(true);

    protected long m_iLastRefreshTime = 0;
    protected long m_iRefreshInterval = 1800 * 1000; // 30 min    
    protected static final String SYSTEM_SETTINGS = "SYSTEM";

    public DBCertStores() {
        this.mRefreshTask = new TimerTask() {
            @Override
            public void run() {
                refreshData();
            }
        };
    }

    @PostConstruct
    private void startup() {
       // refreshData();
       // mtTimer.scheduleAtFixedRate(mRefreshTask, m_iRefreshInterval / 2, m_iRefreshInterval);

    }

    final protected void refreshData() {
        long l = mlog.logStart();
        TypedQuery<MSHCertStore> q = getEntityManager().createNamedQuery("MSHCertStore.getAll", MSHCertStore.class);
        List<MSHCertStore> lst = q.getResultList();

        if (lst.isEmpty()) {
            
            String path = mdsSettings.getSecurityFolderPath();
            List<MSHCertStore> lsttst = createTestData();
            try {
                getUserTransaction().begin();
                for (MSHCertStore cs: lsttst) {
                    getEntityManager().persist(cs);
                }
                getUserTransaction().commit();
            } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
                String msg = "Error storing MSHCertStore!";
                mlog.logError(l, msg, ex);
                try {
                    if (getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                        getUserTransaction().rollback();
                    }
                } catch (SystemException ex1) {
                    // ignore
                }
            }
        }
        q = getEntityManager().createNamedQuery("MSHCertStore.getAll", MSHCertStore.class);
        lst = q.getResultList();

        synchronized (mprpCertStores) {
            mprpCertStores.clear();
            mprpCertStores.addAll(lst);
        }
        mlog.logEnd(l);
    }

    private List<MSHCertStore> createTestData() {

        List<MSHCertStore> lstTS = new ArrayList<>();
        // Create keystore
        MSHCertStore ks = new MSHCertStore();
        ks.setFilePath("${sed.home}/security/security/msh.e-box-a-keystore.jks");
        ks.setPassword("test1234");
        ks.setType("JKS");
        MSHCertificate mhsCert = new MSHCertificate();
        mhsCert.setAlias("msh.e-box-a.si");
        mhsCert.setKeyPassword("key1234");
        ks.getMSHCertificates().add(mhsCert);

        // Create truststore
        MSHCertStore ts = new MSHCertStore();
        ts.setFilePath("${sed.home}/security/security/msh.e-box-a-truststore.jks");
        ts.setPassword("test1234");
        ts.setType("JKS");
        MSHCertificate mhtCert1 = new MSHCertificate();
        mhtCert1.setAlias("msh.e-box-a.si");
        MSHCertificate mhtCert2 = new MSHCertificate();
        mhtCert2.setAlias("msh.e-box-b.si");
        ts.getMSHCertificates().add(mhtCert1);
        ts.getMSHCertificates().add(mhtCert2);

        lstTS.add(ks);
        lstTS.add(ts);
        return lstTS;
    }

    @Lock(LockType.READ)
    @Override
    public List<MSHCertStore> getCertStores() {
        return mprpCertStores;
    }

  

    private EntityManager getEntityManager() {
        // for jetty 
        if (memEManager == null) {
            try {
                InitialContext ic = new InitialContext();
                memEManager = (EntityManager) ic.lookup(getJNDIPrefix() + "ebMS_PU");

            } catch (NamingException ex) {
                mlog.logError(mlog.getTime(), "Error retrieving EntityManager", ex);
            }

        }
        return memEManager;
    }

    private UserTransaction getUserTransaction() {
        // for jetty 
        if (mutUTransaction == null) {
            try {
                InitialContext ic = new InitialContext();

                mutUTransaction = (UserTransaction) ic.lookup("UserTransaction");

            } catch (NamingException ex) {
                mlog.logError(mlog.getTime(), "Error retrieving EntityManager", ex);
            }

        }
        return mutUTransaction;
    }

    private String getJNDIPrefix() {
        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/jboss/");
    }

}
