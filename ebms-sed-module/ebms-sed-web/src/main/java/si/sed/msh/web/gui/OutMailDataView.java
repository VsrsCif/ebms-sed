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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.jms.JMSException;
import javax.naming.NamingException;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.outbox.event.MSHOutEvent;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import org.msh.ebms.outbox.payload.MSHOutPayload;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.DualListModel;
import org.primefaces.model.StreamedContent;
import si.sed.commons.SEDJNDI;

import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.exception.HashException;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.JMSManagerInterface;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.utils.HashUtils;
import si.sed.commons.utils.ReflectUtils;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.StringFormater;
import si.sed.commons.utils.Utils;
import si.sed.msh.web.admin.AdminSEDUserView;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "OutMailDataView")
public class OutMailDataView extends AbstractMailView<MSHOutMail, MSHOutEvent> implements Serializable {

    private static final SEDLogger LOG = new SEDLogger(AdminSEDUserView.class);
     HashUtils mpHU = new HashUtils();
    StorageUtils msuStorageUtils = new StorageUtils();

    MSHOutMail newOutMail;
    String newMailBody;

    private static final long serialVersionUID = 1L;

    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mDB;

    @EJB(mappedName = SEDJNDI.JNDI_JMSMANAGER)
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

                mJMS.sendMessage(this.mMail.getId().longValue(), null, 0, 0, false);
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

    public void composeNewMail() {
        long l = LOG.logStart();
        MSHOutMail m = new MSHOutMail();
        m.setSenderEBox("izvrsba@sed-court.si");
        m.setReceiverEBox("k-vpisnik@sed-court.si");
        m.setService("LegalDelivery_ZPP");
        m.setAction("DeliveryNotification");
        m.setSenderMessageId(Utils.getInstance().getGuidString());
        m.setConversationId(Utils.getInstance().getGuidString());
        m.setSubject("VL 1/2016 Predložitveno poročilo, spis I 291/2014");

        newMailBody = "Pozdravljeni!<br />to je testno besedilo<br /> Lep pozdrav";
        setNewOutMail(m);
        LOG.logEnd(l);
    }

    public MSHOutMail getNewOutMail() {
        return newOutMail;
    }

    public void setNewOutMail(MSHOutMail newOutMail) {
        this.newOutMail = newOutMail;
    }

    public void sendComposedMail() {
        if (newOutMail != null) {
            try {
                String recDomain = newOutMail.getReceiverEBox().substring(newOutMail.getReceiverEBox().indexOf("@") + 1).trim();
                String sendDomain = newOutMail.getSenderEBox().substring(newOutMail.getSenderEBox().indexOf("@") + 1).trim();
                String pmodeId = newOutMail.getService() + ":" + sendDomain + ":" + recDomain;
                newOutMail.setReceiverName(newOutMail.getReceiverEBox());
                newOutMail.setSenderName(newOutMail.getSenderEBox());

                MSHOutPart p = new MSHOutPart();
                p.setEncoding("UTF-8");
                p.setDescription("Mail body");
                p.setMimeType("plain/text");
               
                // mp.setValue();
                
                

                StorageUtils su = new StorageUtils();
                File fout = su.storeFile("out", ".txt", getComposedMailBody().getBytes("UTF-8"));
                String strMD5 = mpHU.getMD5Hash(fout);
                String relPath = StorageUtils.getRelativePath(fout);
                p.setFilepath(relPath);
                p.setMd5(strMD5);

                if (Utils.isEmptyString(p.getFilename())) {
                    p.setFilename(fout.getName());
                }
                if (Utils.isEmptyString(p.getName())) {
                    p.setName(p.getFilename().substring(p.getFilename().lastIndexOf(".")));
                }

                newOutMail.setMSHOutPayload(new MSHOutPayload());
                newOutMail.getMSHOutPayload().getMSHOutParts().add(p);
                
                newOutMail.setSubmittedDate(Calendar.getInstance().getTime());
                mDB.serializeOutMail(newOutMail, userSessionData.getUser().getUserId(), "ebms-sed-web", pmodeId);
            } catch (UnsupportedEncodingException | StorageException | HashException ex) {
                LOG.logError(0, ex);
            }
        }
    }

    public String getComposedMailBody() {
        return newMailBody;
    }

    public void setComposedMailBody(String body) {
        newMailBody = body;
    }
    
   
    
    
   

}
