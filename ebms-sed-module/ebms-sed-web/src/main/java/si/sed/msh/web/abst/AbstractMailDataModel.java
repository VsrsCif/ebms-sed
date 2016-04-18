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
package si.sed.msh.web.abst;

import java.util.List;
import java.util.Map;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.msh.web.gui.UserSessionData;

/**
 *
 * @author sluzba
 */
public abstract class AbstractMailDataModel<T> extends LazyDataModel<T> {

    protected static final long serialVersionUID = 1L;
    protected final Class<T> type;
    protected List<T> mDataList;

    
    SEDDaoInterface mDB;
    
    protected UserSessionData messageBean;

    //must povide the setter method
    public void setUserSessionData(UserSessionData messageBean, SEDDaoInterface db) {
        mDB = db;
        this.messageBean = messageBean;
    }
    public UserSessionData getUserSessionData() {
        return this.messageBean;
    }

    public AbstractMailDataModel(Class<T> type) {
        this.type = type;
      }

    @Override
    public List<T> load(int startingAt, int maxPerPage, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        String strSortOrder = "DESC";
        // validate data        
        mDataList = mDB.getDataList(type, startingAt, maxPerPage, sortField, strSortOrder, externalFilters());
        long l = mDB.getDataListCount(type, filters);
        setRowCount((int)l);
     
        setPageSize(maxPerPage);
        return mDataList;
    }


    public List<T> getCurrentData() {
        return mDataList;
    }
    
    abstract public Object externalFilters();

}
