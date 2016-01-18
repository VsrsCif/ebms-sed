/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.UserTransaction;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.LazyDataModel;
import org.sed.ebms.inbox.event.InEvent;
import org.sed.ebms.inbox.mail.InMail;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDSystemProperties;

@ViewScoped
@ManagedBean(name = "InMailDataView")
public class InMailDataView implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final InMail S_EMPTY_MAIL =  new InMail();
    
    private int mTabActiveIndex = 0;

     @Resource
    protected UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_PU", name = "ebMS_PU")
    protected EntityManager memEManager;

    private LazyDataModel<InMail> mInMailModel = null;
    private InMail mInMail;
    private List<InEvent> mCurretntInMailEvents;

    public LazyDataModel<InMail> getMailList() {
        System.out.println("InMailDataView: getMailList");
        if (mInMailModel == null) {
            mInMailModel = new InMailDataModel(getUserTransaction(), getEntityManager());
        }

        return mInMailModel;
    }

    public InMail getInMail() {
        System.out.println("Get inmail");
        return mInMail == null?S_EMPTY_MAIL:mInMail;
    }

    public void setInMail(InMail inMail) {
        System.out.println("Set inmail");
        this.mInMail = inMail;
        if (this.mInMail!=null){
            TypedQuery tq = memEManager.createNamedQuery("org.sed.ebms.inbox.event.InEvent.getMailEventList", InEvent.class);
            tq.setParameter("mailId", mInMail.getId());
            mCurretntInMailEvents = tq.getResultList();
        } else {
            mCurretntInMailEvents = null;
        }
        
        
    }

    public List<InEvent> getCurretntInMailEvents() {
        return mCurretntInMailEvents;
    }

    public void setCurretntInMailEvents(List<InEvent> mCurretntInMailEvents) {
        this.mCurretntInMailEvents = mCurretntInMailEvents;
    }
    
    

    public void onRowSelect(SelectEvent event) {
        setInMail((InMail) event.getObject());      
    }

    public void onRowUnselect(UnselectEvent event) {
        setInMail(null);
        FacesMessage msg = new FacesMessage("InMail Unselected", ((InMail) event.getObject()).getId().toString());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }
    
    public String getColor(String name){
        return SEDInboxMailStatus.getColor(name);
        
    }
    
    public int rowIndex(InMail om){
        //return  moutMailModel.rowIndex(om);
        return  mInMailModel.getRowIndex();
        
    }
    
    public List<InEvent> getCurrentInMailEvents() {
        return mCurretntInMailEvents;
    }

    public void setCurrentInMailEvents(List<InEvent> ce) {
        this.mCurretntInMailEvents = ce;
    }
    
    
     public void search(ActionEvent event) {
         String res = (String) event.getComponent().getAttributes().get("status");
         System.out.println("Res:" + res);
    }
     
     public void setTabActiveIndex(int itindex) {
        mTabActiveIndex = itindex;        
    }

    public int getTabActiveIndex() {
        return mTabActiveIndex;
    }
    
    
     private EntityManager getEntityManager() {
        // for jetty 
        if (memEManager == null) {
            try {
                InitialContext ic = new InitialContext();
                Context t = (Context) ic.lookup("java:comp");
         
                memEManager = (EntityManager) ic.lookup(getJNDIPrefix() + "ebMS_PU");

            } catch (NamingException ex) {
                Logger.getLogger(InMailDataView.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return memEManager;
    }

    private UserTransaction getUserTransaction() {
        // for jetty 
        if (mutUTransaction == null) {
            try {
                InitialContext ic = new InitialContext();

                mutUTransaction = (UserTransaction) ic.lookup(getJNDIPrefix() + "UserTransaction");

            } catch (NamingException ex) {
                Logger.getLogger(InMailDataView.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return mutUTransaction;
    }
    
     private String getJNDIPrefix() {

        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/jboss/");
    }

}
