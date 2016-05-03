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
import org.sed.ebms.cron.SEDTaskExecution;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.msh.web.gui.entities.CronExecutionFilter;

/**
 *
 * @author Jože Rihtaršič
 */
public class CronExecutionModel extends AbstractMailDataModel<SEDTaskExecution> {

    CronExecutionFilter imtFilter = new CronExecutionFilter();

    public CronExecutionModel(Class<SEDTaskExecution> type, UserSessionData userSessionData, SEDDaoInterface db) {
        super(type);
        setUserSessionData(userSessionData, db);
    }

    @Override
    public Object getRowKey(SEDTaskExecution inMail) {
        return inMail.getId();
    }

    @Override
    public SEDTaskExecution getRowData(String inMailId) {
        BigInteger id = new BigInteger(inMailId);

        for (SEDTaskExecution player : getCurrentData()) {
            if (id.equals(player.getId())) {
                return player;
            }
        }

        return null;
    }

    @Override
    public Object externalFilters() {
        if (imtFilter == null) {
            imtFilter = new CronExecutionFilter();
        }
       

        return imtFilter;
    }

    public CronExecutionFilter getFilter() {
        return imtFilter;
    }

    public void setFilter(CronExecutionFilter imtFilter) {
        this.imtFilter = imtFilter;
    }

}
