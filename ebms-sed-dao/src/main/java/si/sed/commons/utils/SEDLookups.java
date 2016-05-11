/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import generated.SedLookups;
import si.sed.commons.interfaces.SEDLookupsInterface;
import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.xml.bind.JAXBException;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cert.SEDCertificate;
import org.sed.ebms.cron.SEDCronJob;
import org.sed.ebms.cron.SEDTaskType;
import org.sed.ebms.cron.SEDTaskTypeProperty;

import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.plugin.SEDPlugin;
import org.sed.ebms.user.SEDUser;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.interfaces.DBSettingsInterface;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author sluzba
 */
@Startup
@Singleton
@Local(SEDLookupsInterface.class)
@TransactionManagement(TransactionManagementType.BEAN)
public class SEDLookups implements SEDLookupsInterface {


    protected static SEDLogger LOG = new SEDLogger(SEDLookups.class);
    // min, sec, milis.
    public static final long S_UPDATE_TIMEOUT = 10 * 60 * 1000; // 10 minutes
    @EJB(mappedName = SEDJNDI.JNDI_DBSETTINGS)
    private DBSettingsInterface mdbSettings;

    @PersistenceContext(unitName = "ebMS_SED_PU", name = "ebMS_SED_PU")
    public EntityManager memEManager;
    private final HashMap<Class, List<?>> mlstCacheLookup = new HashMap<>();

    private final HashMap<Class, Long> mlstTimeOut = new HashMap<>();
    @Resource
    public UserTransaction mutUTransaction;
    
    public <T> boolean add(T o) {
        long l = LOG.logStart();
        boolean suc = false;
        try {
            mutUTransaction.begin();
            memEManager.persist(o);
            mutUTransaction.commit();
            mlstTimeOut.remove(o.getClass()); // remove timeout to refresh lookup at next call
            suc = true;
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            try {
                LOG.logError(l, ex.getMessage(), ex);
                mutUTransaction.rollback();
            } catch (IllegalStateException | SecurityException | SystemException ex1) {
                LOG.logWarn(l, "Rollback failed", ex1);
            }
        }
        return suc;
    }
    

    @Override
    public boolean addSEDBox(SEDBox sb) {
        return add(sb);
    }
    
    @Override
    public boolean addSEDCertStore(SEDCertStore sb) {
        return add(sb);
    }

    @Override
    public boolean addSEDCronJob(SEDCronJob sb) {
        return add(sb);
    }
    @Override
    public boolean addSEDPlugin(SEDPlugin sb) {
        return add(sb);
    }
    
    @Override
    public boolean addSEDTaskType(SEDTaskType sb) {
        return add(sb);
    }

    @Override
    public boolean addSEDUser(SEDUser sb) {
        return add(sb);
    }
    
    private <T> void cacheLookup(List<T> lst, Class<T> c) {
        if (mlstCacheLookup.containsKey(c)) {
            mlstCacheLookup.get(c).clear();
            mlstCacheLookup.replace(c, lst);
        } else {
            mlstCacheLookup.put(c, lst);
        }
        
        if (mlstTimeOut.containsKey(c)) {
            mlstTimeOut.replace(c, Calendar.getInstance().getTimeInMillis());
        } else {
            mlstTimeOut.put(c, Calendar.getInstance().getTimeInMillis());
        }
    }
    
    @Override
    public void exportLookups(File f) {
        long l = LOG.logStart();
        SedLookups slps = new SedLookups();
        slps.setExportDate(Calendar.getInstance().getTime());
        
        slps.setSEDBoxes(new SedLookups.SEDBoxes());
        slps.setSEDCronJobs(new SedLookups.SEDCronJobs());
        slps.setSEDProperties(new SedLookups.SEDProperties());
        slps.setSEDTaskTypes(new SedLookups.SEDTaskTypes());
        slps.setSEDUsers(new SedLookups.SEDUsers());
        slps.setSEDCertStores(new SedLookups.SEDCertStores());
        slps.setSEDPlugins(new SedLookups.SEDPlugins());
        
        slps.getSEDBoxes().getSEDBoxes().addAll(getSEDBoxes());
        slps.getSEDCronJobs().getSEDCronJobs().addAll(getSEDCronJobs());
        slps.getSEDProperties().getSEDProperties().addAll(mdbSettings.getSEDProperties());
        slps.getSEDTaskTypes().getSEDTaskTypes().addAll(getSEDTaskTypes());
        
        slps.getSEDUsers().getSEDUsers().addAll(getSEDUsers());
        slps.getSEDCertStores().getSEDCertStores().addAll(getSEDCertStore());
        slps.getSEDPlugins().getSEDPlugins().addAll(getSEDPlugin());
        try {
            XMLUtils.serialize(slps, new File(f, "sed-settings.xml"));
        } catch (JAXBException | FileNotFoundException ex) {
            LOG.logError(l, ex.getMessage(), ex);
        }
        LOG.logEnd(l);
    }
    private <T> List<T> getFromCache(Class<T> c) {
        return mlstCacheLookup.containsKey(c) ? (List<T>) mlstCacheLookup.get(c) : null;
    }
    
    private <T> List<T> getLookup(Class<T> c) {
        List<T> t;
        if (updateLookup(c)) {
            TypedQuery<T> query = memEManager.createNamedQuery(c.getName() + ".getAll", c);
            t = query.getResultList();
            cacheLookup(t, c);
        } else {
            t = getFromCache(c);
        }
        return t;
    }
    
    @Override
    public SEDBox getSEDBoxByName(String strname) {
        if (strname != null && !strname.trim().isEmpty()) {
            String sedBox = strname.trim();
            List<SEDBox> lst = getSEDBoxes();
            for (SEDBox sb : lst) {
                if (sb.getBoxName().equalsIgnoreCase(sedBox)) {
                    return sb;
                }
            }
        }
        return null;
    }
    
    @Override
    public List<SEDBox> getSEDBoxes() {
        return getLookup(SEDBox.class);
    }
    
    @Override
    public List<SEDCertStore> getSEDCertStore() {
        return getLookup(SEDCertStore.class);
    }

    @Override
    public SEDCertStore getSEDCertStoreByCertAlias(String alias, boolean isKey) {
        SEDCertStore rsCS =null;
        List<SEDCertStore> lst = getSEDCertStore();
        for (SEDCertStore cs: lst){
            for (SEDCertificate c: cs.getSEDCertificates()){
                if (c.getAlias().equalsIgnoreCase(alias)){
                    if (c.isKeyEntry() == isKey){
                        return cs;
                    } else if (!isKey) { // if searching for cert not key but key is fund search on 
                        rsCS = cs; // alias is not repeated in keystore
                        break;
                    }
                }
            }
            
        }
        return rsCS;
        
        
    }

    @Override
    public SEDCronJob getSEDCronJobById(BigInteger id) {
        if (id != null) {

            List<SEDCronJob> lst = getSEDCronJobs();
            for (SEDCronJob sb : lst) {
                if (id.equals(sb.getId())) {
                    return sb;
                }
            }
        }
        return null;
    }
    
    @Override
    public List<SEDCronJob> getSEDCronJobs() {
        return getLookup(SEDCronJob.class);
    }
    
    @Override
    public List<SEDPlugin> getSEDPlugin() {
        return getLookup(SEDPlugin.class);
    }

    @Override
    public SEDTaskType getSEDTaskTypeByType(String type) {
        if (type != null) {

            List<SEDTaskType> lst = getSEDTaskTypes();
            for (SEDTaskType sb : lst) {
                if (type.equals(sb.getType())) {
                    return sb;
                }
            }
        }
        return null;
    }
    
     @Override
    public SEDPlugin getSEDPluginByType(String type) {
        if (type != null) {

            List<SEDPlugin> lst = getSEDPlugin();
            for (SEDPlugin sb : lst) {
                if (type.equals(sb.getType())) {
                    return sb;
                }
            }
        }
        return null;
    }
    
    @Override
    public List<SEDTaskType> getSEDTaskTypes() {
        return getLookup(SEDTaskType.class);
    }
    @Override
    public SEDUser getSEDUserByUserId(String userId) {
        if (userId != null && !userId.trim().isEmpty()) {
            String ui = userId.trim();
            List<SEDUser> lst = getSEDUsers();
            for (SEDUser sb : lst) {
                if (sb.getUserId().equalsIgnoreCase(ui)) {
                    return sb;
                }
            }
        }
        return null;
    }
    
    @Override
    public List<SEDUser> getSEDUsers() {
        return getLookup(SEDUser.class);
    }

    @PostConstruct
    void init() {
        long l = LOG.logStart();
        LOG.log("System property: " +SEDSystemProperties.SYS_PROP_INIT_LOOKUPS + " exists: " +System.getProperties().containsKey(SEDSystemProperties.SYS_PROP_INIT_LOOKUPS) );
        if (System.getProperties().containsKey(SEDSystemProperties.SYS_PROP_INIT_LOOKUPS)) {
            
            File f = new File(System.getProperty(SEDSystemProperties.SYS_PROP_INIT_LOOKUPS));
            LOG.log("Update data from database: " +f.getAbsolutePath());
            try {
                SedLookups cls = (SedLookups) XMLUtils.deserialize(f, SedLookups.class);
                if (cls.getSEDBoxes() != null && !cls.getSEDBoxes().getSEDBoxes().isEmpty()) {
                    cls.getSEDBoxes().getSEDBoxes().stream().forEach((cb) -> {
                        if (getSEDBoxByName(cb.getBoxName()) == null) {
                            addSEDBox(cb);
                        }
                    });
                }

                if (cls.getSEDCertStores() != null && !cls.getSEDCertStores().getSEDCertStores().isEmpty()) {
                    cls.getSEDCertStores().getSEDCertStores().stream().forEach((cb) -> {
                        cb.setId(null);
                        cb.getSEDCertificates().stream().forEach((c) -> {
                            c.setId(null);
                        });
                        add(cb);
                    });
                }

                if (cls.getSEDCronJobs() != null && !cls.getSEDCronJobs().getSEDCronJobs().isEmpty()) {
                    cls.getSEDCronJobs().getSEDCronJobs().stream().forEach((cb) -> {
                        cb.setId(null);
                        if (cb.getSEDTask() != null) {
                            cb.getSEDTask().getSEDTaskProperties().stream().forEach((c) -> {
                                c.setId(null);
                            });
                        }
                        add(cb);
                    });
                }

                if (cls.getSEDPlugins() != null && !cls.getSEDPlugins().getSEDPlugins().isEmpty()) {
                    cls.getSEDPlugins().getSEDPlugins().stream().forEach((cb) -> {
                      if (getSEDUserByUserId(cb.getType()) == null) {
                            add(cb);
                        }
                        
                    });
                }

                if (cls.getSEDTaskTypes() != null && !cls.getSEDTaskTypes().getSEDTaskTypes().isEmpty()) {
                    cls.getSEDTaskTypes().getSEDTaskTypes().stream().forEach((cb) -> {
                        if (getSEDTaskTypeByType(cb.getType()) == null) {
                            for (SEDTaskTypeProperty c: cb.getSEDTaskTypeProperties()){
                                c.setId(null);
                             }
                            add(cb);
                        }
                    });
                }

                if (cls.getSEDUsers() != null && !cls.getSEDUsers().getSEDUsers().isEmpty()) {
                    cls.getSEDUsers().getSEDUsers().stream().forEach((cb) -> {
                        if (getSEDUserByUserId(cb.getUserId()) == null) {
                            add(cb);
                        }
                    });
                }

                if (cls.getSEDProperties() != null && !cls.getSEDProperties().getSEDProperties().isEmpty()) {
                    mdbSettings.setSEDProperties(cls.getSEDProperties().getSEDProperties());
                }

            } catch (JAXBException ex) {
                LOG.logError(l, ex);
            }

        }

        LOG.logEnd(l);
    }
    
    public <T> boolean remove(T o) {
        long l = LOG.logStart();
        boolean suc = false;
        try {
            mutUTransaction.begin();
            memEManager.remove(memEManager.contains(o) ? o : memEManager.merge(o));
            mutUTransaction.commit();
            mlstTimeOut.remove(o.getClass()); // remove timeout to refresh lookup at next call            
            suc = true;
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            try {
                LOG.logError(l, ex.getMessage(), ex);
                mutUTransaction.rollback();
            } catch (IllegalStateException | SecurityException | SystemException ex1) {
                LOG.logWarn(l, "Rollback failed", ex1);
            }
        }
        return suc;
    }
    
    @Override
    public boolean removeEDCertStore(SEDCertStore sb) {
        return remove(sb);
    }
    
    @Override
    public boolean removeSEDBox(SEDBox sb) {
        return remove(sb);
    }
    
    @Override
    public boolean removeSEDCronJob(SEDCronJob sb) {
        return remove(sb);
    }
    
    @Override
    public boolean removeSEDPlugin(SEDPlugin sb) {
        return remove(sb);
    }
    
    @Override
    public boolean removeSEDTaskType(SEDTaskType sb) {
        return remove(sb);
    }
    
    @Override
    public boolean removeSEDUser(SEDUser sb) {
        return remove(sb);
    }
    
    public <T> boolean update(T o) {
        long l = LOG.logStart();
        boolean suc = false;
        try {
            mutUTransaction.begin();
            memEManager.merge(o);
            mutUTransaction.commit();
            mlstTimeOut.remove(o.getClass()); // remove timeout to refresh lookup at next call            
            suc = true;
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            try {
                LOG.logError(l, ex.getMessage(), ex);
                mutUTransaction.rollback();
            } catch (IllegalStateException | SecurityException | SystemException ex1) {
                LOG.logWarn(l, "Rollback failed", ex1);
            }
        }
        return suc;
    }
    
    private <T> boolean updateLookup(Class<T> c) {
        return !mlstTimeOut.containsKey(c)
                || (Calendar.getInstance().getTimeInMillis() - mlstTimeOut.get(c)) > S_UPDATE_TIMEOUT;
    }
    
    @Override
    public boolean updateSEDBox(SEDBox sb) {
        return update(sb);
    }
    
    @Override
    public boolean updateSEDCertStore(SEDCertStore sb) {
       return update(sb);
    }
    
    @Override
    public boolean updateSEDCronJob(SEDCronJob sb) {
        return update(sb);
    }

    @Override
    public boolean updateSEDPlugin(SEDPlugin sb) {
        return update(sb);
    }
    
    @Override
    public boolean updateSEDTaskType(SEDTaskType sb) {
        SEDTaskType st = getSEDTaskTypeByType(sb.getType());
        for (SEDTaskTypeProperty tp: st.getSEDTaskTypeProperties() ) {
            System.out.println("Remove task prop" + tp.getId());
            remove(tp);   
        }
        System.out.println("Type task prop" + sb.getSEDTaskTypeProperties().size());
        return update(sb);
    }
    
    @Override
    public boolean updateSEDUser(SEDUser sb) {
        return update(sb);
    }

}
