/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import java.util.Calendar;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.UserTransaction;
import org.sed.ebms.report.SEDReportBoxStatus;
import org.sed.ebms.report.Status;
import si.sed.commons.interfaces.SEDReportInterface;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(SEDReportInterface.class)
@TransactionManagement(TransactionManagementType.BEAN)
public class SEDReportBean implements SEDReportInterface {

    /**
     *
     */
    @Resource
    public UserTransaction mutUTransaction;

    /**
     *
     */
    @PersistenceContext(unitName = "ebMS_SED_PU", name = "ebMS_SED_PU")
    public EntityManager memEManager;

    /**
     *
     * @param strSedBox
     * @return
     */
    @Override
    public SEDReportBoxStatus getStatusReport(String strSedBox) {
        SEDReportBoxStatus rbs = new SEDReportBoxStatus();
        rbs.setSedbox(strSedBox);
        rbs.setReportDate(Calendar.getInstance().getTime());
        rbs.setOutMail(new SEDReportBoxStatus.OutMail());
        rbs.setInMail(new SEDReportBoxStatus.InMail());

        TypedQuery<Status> tqIn = memEManager.createNamedQuery(
                "org.sed.ebms.report.getInMailStatusesByBox",
                Status.class);
        TypedQuery<Status> tqOut = memEManager.createNamedQuery(
                "org.sed.ebms.report.getOutMailStatusesByBox",
                Status.class);
        tqIn.setParameter("sedBox", strSedBox);
        tqOut.setParameter("sedBox", strSedBox);

        rbs.getInMail().getStatuses().addAll(tqIn.getResultList());
        rbs.getOutMail().getStatuses().addAll(tqOut.getResultList());

        return rbs;
    }

}
