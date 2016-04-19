/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import si.sed.commons.interfaces.SEDLookupsInterface;
import java.io.File;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.msh.ebms.cron.MSHCronJob;
import org.sed.ebms.ebox.Export;
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.user.SEDUser;

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

    @Resource
    public UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_SED_PU", name = "ebMS_SED_PU")
    public EntityManager memEManager;

    // min, sec, milis.  
    public static final long S_UPDATE_TIMEOUT = 10 * 60 * 1000; // 10 minutes

    private final HashMap<Class, Long> mlstTimeOut = new HashMap<>();
    private final HashMap<Class, List<?>> mlstCacheLookup = new HashMap<>();

    @Override
    public List<SEDBox> getSEDBoxes() {
        return getLookup(SEDBox.class);
    }

    @Override
    public boolean addSEDBox(SEDBox sb) {
        return add(sb);
    }

    @Override
    public boolean updateSEDBox(SEDBox sb) {
        return update(sb);
    }

    @Override
    public boolean removeSEDBox(SEDBox sb) {
        return remove(sb);
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
    public List<SEDUser> getSEDUsers() {
        return getLookup(SEDUser.class);
    }

    @Override
    public boolean addSEDUser(SEDUser sb) {
        return add(sb);
    }

    @Override
    public boolean updateSEDUser(SEDUser sb) {
        return update(sb);
    }

    @Override
    public boolean removeSEDUser(SEDUser sb) {
        return remove(sb);
    }

    @Override
    public List<MSHCronJob> getMSHCronJobs() {
        return getLookup(MSHCronJob.class);
    }

    @Override
    public boolean addMSHCronJob(MSHCronJob sb) {
        return add(sb);
    }

    @Override
    public boolean updateMSHCronJob(MSHCronJob sb) {
        return update(sb);
    }

    @Override
    public boolean removeMSHCronJob(MSHCronJob sb) {
        return remove(sb);
    }
    
      @Override
    public MSHCronJob getMSHCronJobById(BigInteger id) {
        if (id != null) {
            
            List<MSHCronJob> lst = getMSHCronJobs();
            for (MSHCronJob sb : lst) {
                if (id.equals(sb.getId())) {
                    return sb;
                }
            }
        }
        return null;
    }

    @PostConstruct
    void init() {
        long l = LOG.logStart();
        //----------------------------------
        // SEDBox
        List<SEDBox> lstBox = getSEDBoxes();
        if (lstBox.isEmpty()) {
            SEDBox b1 = new SEDBox();
            b1.setActiveFromDate(Calendar.getInstance().getTime());
            b1.setBoxName("izvrsba@sed-court.si");
            b1.setExport(new Export());
            b1.getExport().setActive(Boolean.TRUE);
            b1.getExport().setExportMetaData(Boolean.TRUE);
            b1.getExport().setFolder("${sed.home}" +File.separator +  "export-izvrsba");
            b1.getExport().setFileMask(("${Id}_${SenderEBox}_${Service}"));
            
            SEDBox b2 = new SEDBox();
            b2.setActiveFromDate(Calendar.getInstance().getTime());
            b2.setBoxName("k-vpisnik@sed-court.si");
            SEDBox b3 = new SEDBox();
            b3.setActiveFromDate(Calendar.getInstance().getTime());
            b3.setBoxName("eINS-vpisnik@sed-court.si");
            b3.setExport(new Export());
            b3.getExport().setActive(Boolean.TRUE);
            b3.getExport().setFolder("${sed.home}" +File.separator +  "export-eins");
            b3.getExport().setFileMask(("${Service}_${Id}"));
            
            lstBox.add(b1);
            lstBox.add(b2);
            lstBox.add(b3);

            try {
                mutUTransaction.begin();
                lstBox.stream().forEach((cs) -> {
                    memEManager.persist(cs);
                });
                mutUTransaction.commit();
                mlstCacheLookup.put(SEDBox.class, lstBox);
            } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
                String msg = "Error storing MSHCertStore!";
                LOG.logError(l, msg, ex);
                try {
                    if (mutUTransaction.getStatus() == Status.STATUS_ACTIVE) {
                        mutUTransaction.rollback();
                    }
                } catch (SystemException ex1) {
                    // ignore
                }
            }
        }

        //----------------------------------
        // SEDBox
        List<SEDUser> lstUser = getSEDUsers();
        if (lstUser.isEmpty()) {
            SEDUser u1 = new SEDUser();
            u1.setActiveFromDate(Calendar.getInstance().getTime());
            u1.setUserId("sed");
            u1.getSEDBoxes().addAll(lstBox);

            SEDUser u2 = new SEDUser();
            u2.setActiveFromDate(Calendar.getInstance().getTime());
            u2.setUserId("admin");
            u2.getSEDBoxes().addAll(lstBox);

            lstUser.add(u1);
            lstUser.add(u2);

            try {
                mutUTransaction.begin();
                lstUser.stream().forEach((cs) -> {
                    memEManager.persist(cs);
                });
                mlstCacheLookup.put(SEDUser.class, lstUser);
                mutUTransaction.commit();
            } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
                String msg = "Error storing MSHCertStore!";
                LOG.logError(l, msg, ex);
                try {
                    if (mutUTransaction.getStatus() == Status.STATUS_ACTIVE) {
                        mutUTransaction.rollback();
                    }
                } catch (SystemException ex1) {
                    // ignore
                }
            }

        }
        LOG.logEnd(l);
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

    private <T> boolean updateLookup(Class<T> c) {
        return !mlstTimeOut.containsKey(c)
                || (Calendar.getInstance().getTimeInMillis() - mlstTimeOut.get(c)) > S_UPDATE_TIMEOUT;
    }

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

    private <T> List<T> getFromCache(Class<T> c) {
        return mlstCacheLookup.containsKey(c) ? (List<T>) mlstCacheLookup.get(c) : null;
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

}
