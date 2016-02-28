/*
* Copyright 2016, Supreme Court Republic of Slovenia 
*
* Licensed under the EUPL, Version 1.1 or – as soon they will be approved by 
* the European Commission - subsequent versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the Licence.
* You may obtain a copy of the Licence at:
*
* https://joinup.ec.europa.eu/software/page/eupl
*
* Unless required by applicable law or agreed to in writing, software 
* distributed under the Licence is distributed on an "AS IS" basis, WITHOUT 
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the Licence for the specific language governing permissions and  
* limitations under the Licence.
 */
package si.sed.msh.web.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.persistence.TypedQuery;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.StreamedContent;
import org.sed.ebms.outbox.event.OutEvent;
import org.sed.ebms.outbox.mail.OutMail;
import org.sed.ebms.outbox.payload.OutPart;

import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.StorageUtils;


/**
 *
 * @author Jože Rihtaršič
 */
@ViewScoped
@ManagedBean(name = "OutMailDataView")
public class OutMailDataView extends AbstractMailView<OutMail, OutEvent> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public LazyDataModel<OutMail> getMailList() {
        System.out.println("InMailDataView: getMailList");
        if (mInMailModel == null) {
            mInMailModel = new OutMailDataModel(OutMail.class, getUserTransaction(), getEntityManager());
        }
        return mInMailModel;
    }

    @Override
    public String getStatusColor(String status) {
        return SEDOutboxMailStatus.getColor(status);
    }

    @Override
    public void updateEventList() {
        if (this.mMail != null) {
            TypedQuery tq = memEManager.createNamedQuery("org.sed.ebms.outbox.event.OutEvent.getMailEventList", OutEvent.class);
            tq.setParameter("mailId", this.mMail.getId());
            this.mlstMailEvents = tq.getResultList();
        } else {
            this.mlstMailEvents = null;
        }
    }

    @Override
    public StreamedContent getFile(BigInteger bi) {
        OutPart part = null;

        if (mMail == null || mMail.getOutPayload() == null || mMail.getOutPayload().getOutParts().isEmpty()) {
            return null;
        }

        for (OutPart ip : mMail.getOutPayload().getOutParts()) {
            if (ip.getId().equals(bi)) {
                part = ip;
                break;
            }
        }
        if (part != null) {
            try {
                File f = StorageUtils.getFile(part.getFilepath());
                return new DefaultStreamedContent(new FileInputStream(f), part.getMimeType(), part.getFilename());
            } catch (StorageException | FileNotFoundException ex) {
                Logger.getLogger(InMailDataView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

}


/*
public class OutMailDataView implements Serializable {

    
    private static final long serialVersionUID = 1L;    
    private static final OutMail S_EMPTY_MAIL = new OutMail();
    private static final SimpleDateFormat SDF_DDMMYYY_HH_MM_SS = new SimpleDateFormat("dd.MM.YYYY HH:mm:ss");
    SEDLogger mlog = new SEDLogger(OutMailDataView.class);

    private int mTabActiveIndex = 0;

    @Resource
    protected UserTransaction mutUTransaction;

    @PersistenceContext(unitName = "ebMS_PU", name = "ebMS_PU")
    protected EntityManager memEManager;

    private OutMailDataModel moutMailModel = null;
    private OutMail moutMail;
    private List<OutEvent> mCurretntoutMailEvents;

    public LazyDataModel<OutMail> getMailList() {
        long l = mlog.getTime();
        if (moutMailModel == null) {
            moutMailModel = new OutMailDataModel(OutMail.class,  getUserTransaction(), getEntityManager());
        }
        mlog.logEnd(l);
        return moutMailModel;
    }

    public OutMail getOutMail() {
        return moutMail == null ? S_EMPTY_MAIL : moutMail;
    }

    public void setOutMail(OutMail outMail) {
        long l = mlog.getTime();
        this.moutMail = outMail;
        if (this.moutMail != null) {
            TypedQuery tq = memEManager.createNamedQuery("org.sed.ebms.outbox.event.OutEvent.getMailEventList", OutEvent.class);
            tq.setParameter("mailId", outMail.getId());
            mCurretntoutMailEvents = tq.getResultList();
        } else {
            mCurretntoutMailEvents = null;
        }
        mlog.logEnd(l);

    }

    public List<OutEvent> getCurretntOutMailEvents() {
        return mCurretntoutMailEvents;
    }

    public void setCurretntoutMailEvents(List<OutEvent> mCurretntoutMailEvents) {
        this.mCurretntoutMailEvents = mCurretntoutMailEvents;
    }

    public void onRowSelect(SelectEvent event) {
        setOutMail((OutMail) event.getObject());
        //FacesMessage msg = new FacesMessage("outMail Selected", ((OutMail) event.getObject()).getId().toString());
        //FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void onRowUnselect(UnselectEvent event) {
        setOutMail(null);
        //FacesMessage msg = new FacesMessage("outMail Unselected", ((OutMail) event.getObject()).getId().toString());
        //FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public String getStatusColor(String status) {
        return SEDOutboxMailStatus.getColor(status);

    }
    public String formatDate(Date date) {
        return SDF_DDMMYYY_HH_MM_SS.format(date);

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
    }
     
     private EntityManager getEntityManager() {
         long l = mlog.getTime();
        // for jetty 
        if (memEManager == null) {
            try {
                InitialContext ic = new InitialContext();
                Context t = (Context) ic.lookup("java:comp");
                memEManager = (EntityManager) ic.lookup(getJNDIPrefix() + "ebMS_PU");
            } catch (NamingException ex) {
                mlog.logError(l, ex);
            }

        }
        mlog.logEnd(l);
        return memEManager;
    }

    private UserTransaction getUserTransaction() {
        long l = mlog.getTime();
        // for jetty 
        if (mutUTransaction == null) {
            try {
                InitialContext ic = new InitialContext();
                mutUTransaction = (UserTransaction) ic.lookup(getJNDIPrefix() + "UserTransaction");
            } catch (NamingException ex) {
                mlog.logError(l, ex);
            }

        }
        mlog.logEnd(l);
        return mutUTransaction;
    }
    
     private String getJNDIPrefix() {
        return System.getProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:/jboss/");
    }
}
*/