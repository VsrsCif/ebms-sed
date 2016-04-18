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

import si.sed.msh.web.abst.AbstractMailDataModel;
import java.math.BigInteger;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;
import org.msh.ebms.outbox.mail.MSHOutMail;
import si.sed.msh.web.gui.entities.OutMailTableFilter;


/**
 *
 * @author Jože Rihtaršič
 */
public class OutMailDataModel extends AbstractMailDataModel<MSHOutMail> {
    
    OutMailTableFilter outFilter = new OutMailTableFilter();

    public OutMailDataModel(Class<MSHOutMail> type, UserTransaction mutUTransaction, EntityManager memEManager, UserSessionData messageBean) {
        super(type, mutUTransaction, memEManager);
        setUserSessionData(messageBean);
    }

    @Override
    public Object getRowKey(MSHOutMail inMail) {
        return inMail.getId();
    }

    @Override
    public MSHOutMail getRowData(String inMailId) {
        BigInteger id = new BigInteger(inMailId);

        for (MSHOutMail player : getCurrentData()) {
            if (id.equals(player.getId())) {
                return player;
            }
        }
        return null;
    }

     @Override
    public Object externalFilters() {
        outFilter.setSenderEBox(getUserSessionData().getCurrentSEDBox());
        return outFilter;
    }
    
    public OutMailTableFilter getFilter() {
        return outFilter;
    }

    public void setFilter(OutMailTableFilter imtFilter) {
        this.outFilter = imtFilter;
    }
    
}
