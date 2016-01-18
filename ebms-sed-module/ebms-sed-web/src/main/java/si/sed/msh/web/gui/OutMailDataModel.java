/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.sed.ebms.outbox.mail.OutMail;

/**
 *
 * @author sluzba
 */
public class OutMailDataModel extends LazyDataModel<OutMail> {

    private static final long serialVersionUID = 1L;

    protected UserTransaction mutUTransaction;

    protected EntityManager memEManager;

    public OutMailDataModel(UserTransaction mutUTransaction, EntityManager memEManager) {
        this.mutUTransaction = mutUTransaction;
        this.memEManager = memEManager;
    }

    private List<OutMail> mOutMail;

    @Override
    public List<OutMail> load(int startingAt, int maxPerPage, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        System.out.println("start index: " + startingAt + " max per page: " + maxPerPage);
 
        int iStarIndex;
        int iResCountIndex;
        String strOrderParam = "Id";
        String strSortOrder = "DESC";
        // validate data

        iStarIndex = startingAt;
        iResCountIndex = maxPerPage;

        try {

            CriteriaQuery<Long> cqCount = createSearchCriteria(null, OutMail.class, true);
            CriteriaQuery<OutMail> cq = createSearchCriteria(null, OutMail.class, false);

            Long l = getEntityManager().createQuery(cqCount).getSingleResult();

            setRowCount(l.intValue());

            TypedQuery<OutMail> q = getEntityManager().createQuery(cq);
            if (iResCountIndex > 0) {
                q.setMaxResults(iResCountIndex);
            }
            if (iStarIndex > 0) {
                q.setFirstResult(iStarIndex);
            }

            mOutMail = q.getResultList();

        } catch (NoResultException ex) {
               mOutMail = new ArrayList<>();
        }

        // set the page dize
        setPageSize(maxPerPage);

        return mOutMail;
    }

    @Override
    public Object getRowKey(OutMail inMail) {
        return inMail.getId();
    }

    @Override
    public OutMail getRowData(String inMailId) {
        BigInteger id = new BigInteger(inMailId);

        for (OutMail player : mOutMail) {
            if (id.equals(player.getId())) {
                return player;
            }
        }

        return null;
    }

    private CriteriaQuery createSearchCriteria(Object searchParams, Class resultClass, boolean forCount) {

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery cq = forCount ? cb.createQuery(Long.class) : cb.createQuery(resultClass);
        Root<OutMail> om = cq.from(resultClass);
        if (forCount) {
            cq.select(cb.count(om));
        }

        List<Predicate> lstPredicate = new ArrayList<>();

        if (searchParams != null) {
            Class cls = searchParams.getClass();
            Method[] methodList = cls.getDeclaredMethods();
            for (Method m : methodList) {

                // only getters  (public, starts with get, no arguments)
                String mName = m.getName();
                if (Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 0 && !m.getReturnType().equals(Void.TYPE)
                        && (mName.startsWith("get") || mName.startsWith("is"))) {
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
                            } else {
                                lstPredicate.add(cb.equal(om.get(fieldName), searchValue));
                            }
                        }
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                        Logger.getLogger(OutMailDataModel.class.getName()).log(Level.SEVERE, null, ex);
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

    private EntityManager getEntityManager() {
        return memEManager;
    }
    
    public int rowIndex(OutMail om){
        return  mOutMail.contains(om)? mOutMail.indexOf(om):-1;
    }

}
