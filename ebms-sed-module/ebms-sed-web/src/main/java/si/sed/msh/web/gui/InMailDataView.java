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
import org.msh.ebms.inbox.event.MSHInEvent;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.inbox.payload.MSHInPart;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.StreamedContent;

import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.StorageUtils;

/**
 *
 * @author Jože Rihtaršič
 */

@ViewScoped
@ManagedBean(name = "InMailDataView")
public class InMailDataView extends AbstractMailView<MSHInMail, MSHInEvent> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public LazyDataModel<MSHInMail> getMailList() {
        if (mInMailModel == null) {
            mInMailModel = new InMailDataModel(MSHInMail.class, getUserTransaction(), getEntityManager());
        }
        return mInMailModel;
    }

    @Override
    public String getStatusColor(String status) {
        return SEDInboxMailStatus.getColor(status);
    }

    @Override
    public void updateEventList() {
        if (this.mMail != null) {
            TypedQuery tq = memEManager.createNamedQuery("org.msh.ebms.inbox.event.MSHInEvent.getMailEventList", MSHInEvent.class);
            tq.setParameter("mailId", mMail.getId());
            mlstMailEvents = tq.getResultList();
        } else {
            mlstMailEvents = null;
        }
    }

    @Override
    public StreamedContent getFile(BigInteger bi) {
        MSHInPart inpart = null;

        if (mMail == null || mMail.getMSHInPayload() == null || mMail.getMSHInPayload().getMSHInParts().isEmpty()) {
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
                return new DefaultStreamedContent(new FileInputStream(f), inpart.getMimeType(), inpart.getFilename());
            } catch (StorageException | FileNotFoundException ex) {
                Logger.getLogger(InMailDataView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

}
