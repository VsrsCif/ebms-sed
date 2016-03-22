/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.property.SEDProperty;
import org.sed.ebms.user.SEDUser;
import si.sed.commons.SEDSystemProperties;
import static si.sed.commons.utils.abst.ASettings.newProperties;

/**
 *
 * @author sluzba
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class SEDPersistenceBean {

    @Resource
    public UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_PU", name = "ebMS_PU")
    public EntityManager memEManager;

    protected static SEDLogger LOG = new SEDLogger(SEDPersistenceBean.class);
   
    
    public SEDUser getSEDUser(String username){
        TypedQuery<SEDUser> qUser = memEManager.createNamedQuery(SEDNamedQueries.SEDUSER_BY_ID, SEDUser.class);
        qUser.setParameter("id", username);
        try {
            return qUser.getSingleResult();
        } catch(NoResultException ignore){
            return null;
        }
    }

    
    
}
