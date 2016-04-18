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

import si.sed.msh.web.abst.AbstractMailView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.jms.JMSException;
import javax.naming.NamingException;
import org.msh.ebms.outbox.event.MSHOutEvent;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import si.sed.commons.SEDJNDI;

import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.JMSManagerInterface;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.utils.StorageUtils;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "OutMailDataView")
public class OutMailDataView extends AbstractMailView<MSHOutMail, MSHOutEvent> implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mDB;
    
    @EJB (mappedName=SEDJNDI.JNDI_JMSMANAGER)
   JMSManagerInterface mJMS;

    @ManagedProperty(value = "#{userSessionData}")
    private UserSessionData userSessionData;

    @PostConstruct
    private void init() {
        mMailModel = new OutMailDataModel(MSHOutMail.class, getUserSessionData(), mDB);
    }

    public void setUserSessionData(UserSessionData messageBean) {
        this.userSessionData = messageBean;
    }

    public UserSessionData getUserSessionData() {
        return this.userSessionData;
    }

    public OutMailDataModel getOutMailModel() {
        if (mMailModel == null) {
            mMailModel = new OutMailDataModel(MSHOutMail.class, getUserSessionData(), mDB);
        }
        return (OutMailDataModel) mMailModel;
    }

    @Override
    public String getStatusColor(String status) {
        return SEDOutboxMailStatus.getColor(status);
    }

    public List<SEDOutboxMailStatus> getOutStatuses() {
        return Arrays.asList(SEDOutboxMailStatus.values());
    }

    @Override
    public void updateEventList() {
        if (this.mMail != null) {
            mlstMailEvents = mDB.getMailEventList(MSHOutEvent.class, mMail.getId());
        } else {
            this.mlstMailEvents = null;
        }
    }

    @Override
    public StreamedContent getFile(BigInteger bi) {
        MSHOutPart part = null;

        if (mMail == null || mMail.getMSHOutPayload() == null || mMail.getMSHOutPayload().getMSHOutParts().isEmpty()) {
            return null;
        }

        for (MSHOutPart ip : mMail.getMSHOutPayload().getMSHOutParts()) {
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
    
     public void resendSelectedMail() throws IOException {
       if (this.mMail != null) {
           try {
                // get pmode
       
               mJMS.sendMessage(this.mMail.getId().longValue(),null, 0, 0, false);
           } catch (NamingException | JMSException ex) {
               Logger.getLogger(OutMailDataView.class.getName()).log(Level.SEVERE, null, ex);
           }
        } 
    }
      public void deleteSelectedMail() throws IOException {
       if (this.mMail != null) {
           
               mDB.setStatusToOutMail(this.mMail, SEDOutboxMailStatus.DELETED, "Manual deleted by " + getUserSessionData().getUser().getUserId());               
           
        } 
    }

}
