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

import java.math.BigInteger;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;
import org.sed.ebms.inbox.mail.InMail;

/**
 *
 * @author Jože Rihtaršič
 */
public class InMailDataModel extends AbstractMailDataModel<InMail> {

    public InMailDataModel(Class<InMail> type, UserTransaction mutUTransaction, EntityManager memEManager) {
        super(type, mutUTransaction, memEManager);
    }

    @Override
    public Object getRowKey(InMail inMail) {
        return inMail.getId();
    }

    @Override
    public InMail getRowData(String inMailId) {
        BigInteger id = new BigInteger(inMailId);

        for (InMail player : getCurrentData()) {
            if (id.equals(player.getId())) {
                return player;
            }
        }

        return null;
    }


}
