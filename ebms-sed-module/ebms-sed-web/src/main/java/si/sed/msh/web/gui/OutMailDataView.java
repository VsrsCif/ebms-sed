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
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.LazyDataModel;
import org.sed.ebms.outbox.event.OutEvent;
import org.sed.ebms.outbox.mail.OutMail;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.SEDSystemProperties;

@ViewScoped
@ManagedBean(name = "OutMailDataView")
public class OutMailDataView implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final OutMail S_EMPTY_MAIL = new OutMail();

    private int mTabActiveIndex = 0;

    @Resource
    protected UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_PU", name = "ebMS_PU")
    protected EntityManager memEManager;

    private OutMailDataModel moutMailModel = null;
    private OutMail moutMail;
    private List<OutEvent> mCurretntoutMailEvents;

    public LazyDataModel<OutMail> getMailList() {
        System.out.println("OutMailDataView: getMailList");
        if (moutMailModel == null) {
            moutMailModel = new OutMailDataModel(getUserTransaction(), getEntityManager());
        }

        return moutMailModel;
    }

    public OutMail getOutMail() {
        System.out.println("Get OutMail");
        return moutMail == null ? S_EMPTY_MAIL : moutMail;
    }

    public void setOutMail(OutMail inMail) {
        System.out.println("Set OutMail");
        this.moutMail = inMail;
        if (this.moutMail != null) {
            TypedQuery tq = memEManager.createNamedQuery("org.sed.ebms.outbox.event.OutEvent.getMailEventList", OutEvent.class);
            tq.setParameter("mailId", moutMail.getId());
            mCurretntoutMailEvents = tq.getResultList();
        } else {
            mCurretntoutMailEvents = null;
        }

    }

    public List<OutEvent> getCurretntOutMailEvents() {
        return mCurretntoutMailEvents;
    }

    public void setCurretntoutMailEvents(List<OutEvent> mCurretntoutMailEvents) {
        this.mCurretntoutMailEvents = mCurretntoutMailEvents;
    }

    public void onRowSelect(SelectEvent event) {
        System.out.println("On row selected");
        setOutMail((OutMail) event.getObject());
        FacesMessage msg = new FacesMessage("outMail Selected", ((OutMail) event.getObject()).getId().toString());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void onRowUnselect(UnselectEvent event) {
        setOutMail(null);
        FacesMessage msg = new FacesMessage("outMail Unselected", ((OutMail) event.getObject()).getId().toString());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public String getColor(String name) {
        return SEDOutboxMailStatus.getColor(name);

    }

    public int rowIndex(OutMail om) {
        //return  moutMailModel.rowIndex(om);
        return moutMailModel.getRowIndex();

    }

    public void setTabActiveIndex(int itindex) {
        mTabActiveIndex = itindex;        
    }

    public int getTabActiveIndex() {
        return mTabActiveIndex;
    }

    public void onTabChange(TabChangeEvent event) {
        TabView tv = (TabView) event.getComponent();
        mTabActiveIndex = tv.getActiveIndex();
    }
    
     public void search(ActionEvent event) {
         String res = (String) event.getComponent().getAttributes().get("status");
         System.out.println("Res:" + res);
    }
     
     private EntityManager getEntityManager() {
        // for jetty 
        if (memEManager == null) {
            try {
                InitialContext ic = new InitialContext();
                Context t = (Context) ic.lookup("java:comp");
         
                memEManager = (EntityManager) ic.lookup(getJNDIPrefix() + "ebMS_PU");

            } catch (NamingException ex) {
                Logger.getLogger(OutMailDataView.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(OutMailDataView.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return mutUTransaction;
    }
    
     private String getJNDIPrefix() {

        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/jboss/");
    }
}
