/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.user.SEDUser;

/**
 *
 * @author sluzba
 */
@Startup
@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class SEDLookups {

    protected static SEDLogger LOG = new SEDLogger(SEDLookups.class);

    @Resource
    public UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_PU", name = "ebMS_PU")
    public EntityManager memEManager;

    // min, sec, milis.  
    public static final long S_UPDATE_TIMEOUT = 10 * 60 * 1000; // 10 minutes

    private final HashMap<Class, Long> mlstTimeOut = new HashMap<>();
    private final HashMap<Class, List<?>> mlstCacheLookup = new HashMap<>();

    public List<SEDBox> getSEDBoxes() {        
        return getLookup(SEDBox.class);
    }
    
    public void addSEDBox(SEDBox sb) {   
        add(sb);        
    }
    public void updateSEDBox(SEDBox sb) {        
        update(sb);
    }
    public void removeSEDBox(SEDBox sb) {        
        remove(sb);
    }
    public List<SEDUser> getSEDUsers() {        
        return getLookup(SEDUser.class);
    }

    public void addSEDUser(SEDUser sb) {   
        add(sb);        
    }
    public void updateSEDUser(SEDUser sb) {        
        update(sb);
    }
    public void removeSEDUser(SEDUser sb) {        
        remove(sb);
    }
    
    
    public List<MSHCronJob> getMSHCronJobs() {        
        return getLookup(MSHCronJob.class);
    }

    public void addMSHCronJob(MSHCronJob sb) {   
        add(sb);        
    }
    public void updateMSHCronJob(MSHCronJob sb) {        
        update(sb);
    }
    public void removeMSHCronJob(MSHCronJob sb) {        
        remove(sb);
    }
   
    @PostConstruct
    void init(){
        long l = LOG.logStart();
         //----------------------------------
        // SEDBox
         List<SEDBox> lstBox =  getSEDBoxes();
        if (lstBox.isEmpty()) {
            SEDBox b1 = new SEDBox();
            b1.setActiveFromDate(Calendar.getInstance().getTime());
            b1.setBoxName("izvrsba@sed-court.si");
            SEDBox b2 = new SEDBox();
            b2.setActiveFromDate(Calendar.getInstance().getTime());
            b2.setBoxName("k-vpisnik@sed-court.si");
            SEDBox b3 = new SEDBox();
            b3.setActiveFromDate(Calendar.getInstance().getTime());
            b3.setBoxName("eINS-vpisnik@sed-court.si");
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
    
    public <T> void add(T o) {        
        try {
            mutUTransaction.begin();
            memEManager.persist(o);
            mutUTransaction.commit();
            mlstTimeOut.remove(o.getClass()); // remove timeout to refresh lookup at next call            
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            LOG.logError(0, ex);
        }
    }
    
    public <T> void update(T o) {        
        try {
            mutUTransaction.begin();
            memEManager.merge(o);
            mutUTransaction.commit();
            mlstTimeOut.remove(o.getClass()); // remove timeout to refresh lookup at next call            
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            LOG.logError(0, ex);
        }
    }
     public <T> void remove(T o) {        
        try {
            mutUTransaction.begin();
            memEManager.remove(memEManager.contains(o) ? o : memEManager.merge(o));
            mutUTransaction.commit();
            mlstTimeOut.remove(o.getClass()); // remove timeout to refresh lookup at next call            
        } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException | IllegalStateException ex) {
            LOG.logError(0, ex);
        }
    }

    private <T> List<T> getFromCache(Class<T> c) {
        return mlstCacheLookup.containsKey(c) ? (List<T>) mlstCacheLookup.get(c) : null;
    }

    private  <T> void cacheLookup(List<T> lst, Class<T> c) {
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
