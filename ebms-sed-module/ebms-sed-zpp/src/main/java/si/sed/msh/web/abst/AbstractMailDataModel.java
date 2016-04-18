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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedProperty;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.UserTransaction;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import si.sed.msh.web.gui.InMailDataModel;
import si.sed.msh.web.gui.UserSessionData;

/**
 *
 * @author sluzba
 */
public abstract class AbstractMailDataModel<T> extends LazyDataModel<T> {

    protected static final long serialVersionUID = 1L;
    protected final Class<T> type;
    protected UserTransaction mutUTransaction;
    protected EntityManager memEManager;
    protected List<T> mDataList;

    
    protected UserSessionData messageBean;

    //must povide the setter method
    public void setUserSessionData(UserSessionData messageBean) {
        this.messageBean = messageBean;
    }
    public UserSessionData getUserSessionData() {
        return this.messageBean;
    }

    public AbstractMailDataModel(Class<T> type, UserTransaction utUTransaction, EntityManager emEManager) {
        this.type = type;
        this.memEManager = emEManager;
        this.mutUTransaction = utUTransaction;

    }

    @Override
    public List<T> load(int startingAt, int maxPerPage, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        System.out.println("start index: " + startingAt + " max per page: " + maxPerPage + " filtri " + filters);
        System.out.println("user : " + messageBean);
        int iStarIndex;
        int iResCountIndex;
        String strOrderParam = "Id";
        String strSortOrder = "DESC";
        // validate data
        iStarIndex = startingAt;
        iResCountIndex = maxPerPage;
        try {
            CriteriaQuery<Long> cqCount = createSearchCriteria( externalFilters(), true);
            CriteriaQuery<T> cq = createSearchCriteria( externalFilters(), false);
            Long l = getEntityManager().createQuery(cqCount).getSingleResult();
            setRowCount(l.intValue());
            TypedQuery<T> q = getEntityManager().createQuery(cq);
            if (iResCountIndex > 0) {
                q.setMaxResults(iResCountIndex);
            }
            if (iStarIndex > 0) {
                q.setFirstResult(iStarIndex);
            }
            mDataList = q.getResultList();
        } catch (NoResultException ex) {
            mDataList = new ArrayList<>();
        }
        // set the page dize
        setPageSize(maxPerPage);
        return mDataList;
    }

    protected <T> CriteriaQuery createSearchCriteria(Object searchParams, boolean forCount) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery cq = forCount ? cb.createQuery(Long.class) : cb.createQuery(type);
        Root<T> om = cq.from(type);
        if (forCount) {
            cq.select(cb.count(om));
        } else {
            cq.orderBy(cb.desc(om.get("Id")));
        }
        List<Predicate> lstPredicate = new ArrayList<>();
        // set order by
        if (searchParams != null) {
            Class cls = searchParams.getClass();
            Method[] methodList = cls.getDeclaredMethods();
            for (Method m : methodList) {
                // only getters  (public, starts with get, no arguments)
                String mName = m.getName();
                if (Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 0 && !m.getReturnType().equals(Void.TYPE) && (mName.startsWith("get") || mName.startsWith("is"))) {
                    String fieldName = mName.substring(mName.startsWith("get") ? 3 : 2);
                    try {
                        cls.getMethod("set" + fieldName, new Class[]{m.getReturnType()});
                    } catch (NoSuchMethodException | SecurityException ex) {
                        // method does not have setter
                        continue;
                    }
                    try {
                        // get returm parameter
                        Object searchValue = m.invoke(searchParams, new Object[]{});
                        if (searchValue != null) {
                            if (fieldName.endsWith("From") && searchValue instanceof Comparable) {
                                lstPredicate.add(cb.greaterThanOrEqualTo(om.get(fieldName.substring(0, fieldName.lastIndexOf("From"))), (Comparable) searchValue));
                            } else if (fieldName.endsWith("To") && searchValue instanceof Comparable) {
                                lstPredicate.add(cb.lessThan(om.get(fieldName.substring(0, fieldName.lastIndexOf("To"))), (Comparable) searchValue));
                            } else if (searchValue instanceof String && !((String)searchValue).isEmpty()) {
                                System.out.println("Add param: '"+fieldName+"' value: '"+searchValue+"'");
                                lstPredicate.add(cb.equal(om.get(fieldName), searchValue));
                            }
                        }
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        Logger.getLogger(InMailDataModel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            if (!lstPredicate.isEmpty()) {
                Predicate[] tblPredicate = lstPredicate.stream().toArray(Predicate[]::new);
                cq.where(cb.and(tblPredicate));
            }
        }
        return cq;
    }

    protected EntityManager getEntityManager() {
        return memEManager;
    }

    public List<T> getCurrentData() {
        return mDataList;
    }
    
    abstract public Object externalFilters();

}
