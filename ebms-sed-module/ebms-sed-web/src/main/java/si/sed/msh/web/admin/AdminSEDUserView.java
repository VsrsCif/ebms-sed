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
package si.sed.msh.web.admin;

import si.sed.msh.web.abst.AbstractAdminJSFView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.primefaces.model.DualListModel;
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.user.SEDUser;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "adminSEDUserView")

public class AdminSEDUserView extends AbstractAdminJSFView<SEDUser> {

    private static final SEDLogger LOG = new SEDLogger(AdminSEDUserView.class);

    @EJB (mappedName=SEDJNDI.JNDI_SEDLOOKUPS)
    private SEDLookupsInterface mdbLookups;

    private DualListModel<SEDBox> msbCBDualList = new DualListModel<>();

    public DualListModel<SEDBox> getCurrentPickupDualSEDBoxList() {

                
        List<String> sbIDs = new ArrayList<>();
        if (getEditable() != null) {
            getEditable().getSEDBoxes().stream().forEach((sb) -> {
                sbIDs.add(sb.getBoxName());
            });
        }
        List<SEDBox> src = new ArrayList<>();
        List<SEDBox> trg = new ArrayList<>();
        mdbLookups.getSEDBoxes().stream().forEach((se) -> {
            if (sbIDs.contains(se.getBoxName())) {
                trg.add(se);
            } else {
                src.add(se);
            }
        });

        return msbCBDualList = new DualListModel<>(src, trg);
    }

    public void setCurrentPickupDualSEDBoxList(DualListModel<SEDBox> dl) {
        msbCBDualList = dl;
    }

    public SEDUser getSEDUserByUsername(String username) {
        List<SEDUser> lst = mdbLookups.getSEDUsers();
        for (SEDUser sb : lst) {
            if (sb.getUserId().equalsIgnoreCase(username)) {
                return sb;
            }
        }
        return null;

    }

    @Override
    public void createEditable() {
        long l = LOG.logStart();

        String sbname = "user_%03d";
        int i = 1;
        while (getSEDUserByUsername(String.format(sbname, i)) != null) {
            i++;
        }

        SEDUser su = new SEDUser();
        su.setUserId(String.format(sbname, i));
        su.setActiveFromDate(Calendar.getInstance().getTime());

        setNew(su);
        LOG.logEnd(l);
    }

    @Override
    public void removeSelected() {
        SEDUser sb = getSelected();
        if (sb != null) {
            mdbLookups.removeSEDUser(sb);
            setSelected(null);
        }
    }

    @Override
    public void persistEditable() {
        SEDUser sb = getEditable();
        if (sb != null) {
            sb.getSEDBoxes().clear();
            if (msbCBDualList.getTarget() != null && !msbCBDualList.getTarget().isEmpty()) {
                sb.getSEDBoxes().addAll(msbCBDualList.getTarget());
            }
            mdbLookups.addSEDUser(sb);
            setEditable(null);
        }
    }

    @Override
    public void updateEditable() {
        SEDUser sb = getEditable();
        if (sb != null) {
            System.out.println("UPATE:_ " + msbCBDualList.getTarget().size());
            sb.getSEDBoxes().clear();
            if (msbCBDualList.getTarget() != null && !msbCBDualList.getTarget().isEmpty()) {
                sb.getSEDBoxes().addAll(msbCBDualList.getTarget());
            }
            mdbLookups.updateSEDUser(sb);
            setEditable(null);
        }
    }

    @Override
    public List<SEDUser> getList() {
        return mdbLookups.getSEDUsers();
    }

}
