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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import org.msh.ebms.inbox.event.MSHInEvent;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.inbox.payload.MSHInPart;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDJNDI;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.utils.StorageUtils;
import si.sed.msh.web.abst.AbstractMailView;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "InMailDataView")
public class InMailDataView extends AbstractMailView<MSHInMail, MSHInEvent>
        implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mDB;

    @ManagedProperty(value = "#{userSessionData}")
    private UserSessionData userSessionData;

    @PostConstruct
    private void init() {
        mMailModel = new InMailDataModel(MSHInMail.class, userSessionData, mDB);
    }

    /**
     *
     * @param messageBean
     */
    public void setUserSessionData(UserSessionData messageBean) {
        this.userSessionData = messageBean;

    }

    /**
     *
     * @return
     */
    public UserSessionData getUserSessionData() {
        return this.userSessionData;
    }

    /**
     *
     * @return
     */
    public InMailDataModel getInMailModel() {
        return (InMailDataModel) mMailModel;
    }

    /**
     *
     * @param status
     * @return
     */
    @Override
    public String getStatusColor(String status) {
        return SEDInboxMailStatus.getColor(status);
    }

    /**
     *
     */
    @Override
    public void updateEventList() {
        if (this.mMail != null) {
            mlstMailEvents = mDB.getMailEventList(MSHInEvent.class,
                    mMail.getId());
        } else {
            mlstMailEvents = null;
        }
    }

    /**
     *
     * @param bi
     * @return
     */
    @Override
    public StreamedContent getFile(BigInteger bi) {
        MSHInPart inpart = null;

        if (mMail == null || mMail.getMSHInPayload() == null ||
                mMail.getMSHInPayload().getMSHInParts().isEmpty()) {
            return null;
        }

        for (MSHInPart ip : mMail.getMSHInPayload().getMSHInParts()) {
            if (ip.getId().equals(bi)) {
                inpart = ip;
                break;
            }
        }
        if (inpart != null) {
            try {
                File f = StorageUtils.getFile(inpart.getFilepath());
                return new DefaultStreamedContent(new FileInputStream(f),
                        inpart.getMimeType(), inpart.getFilename());
            } catch (StorageException | FileNotFoundException ex) {
                Logger.getLogger(InMailDataView.class.getName()).log(
                        Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    /**
     *
     * @return
     */
    public List<SEDInboxMailStatus> getInStatuses() {
        return Arrays.asList(SEDInboxMailStatus.values());
    }

}
