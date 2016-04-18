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
import org.msh.ebms.inbox.mail.MSHInMail;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.msh.web.gui.entities.InMailTableFilter;


/**
 *
 * @author Jože Rihtaršič
 */
public class InMailDataModel extends AbstractMailDataModel<MSHInMail> {
    InMailTableFilter imtFilter = new InMailTableFilter();

    public InMailDataModel(Class<MSHInMail> type, UserSessionData userSessionData,  SEDDaoInterface db) {
        super(type);
        setUserSessionData(userSessionData, db);
    }

    @Override
    public Object getRowKey(MSHInMail inMail) {
        return inMail.getId();
    }

    @Override
    public MSHInMail getRowData(String inMailId) {
        BigInteger id = new BigInteger(inMailId);

        for (MSHInMail player : getCurrentData()) {
            if (id.equals(player.getId())) {
                return player;
            }
        }

        return null;
    }

    @Override
    public Object externalFilters() {
        imtFilter.setReceiverEBox(getUserSessionData().getCurrentSEDBox());
        return imtFilter;
    }

    public InMailTableFilter getFilter() {
        return imtFilter;
    }

    public void setFilter(InMailTableFilter imtFilter) {
        this.imtFilter = imtFilter;
    }

}
