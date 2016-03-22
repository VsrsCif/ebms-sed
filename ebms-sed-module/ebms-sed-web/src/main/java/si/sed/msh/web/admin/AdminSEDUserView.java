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

import si.sed.msh.web.gui.*;
import java.util.Calendar;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.DualListModel;
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.user.SEDUser;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.SEDLookups;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "adminSEDUserView")

public class AdminSEDUserView extends AbstractJSFView {

    private static final SEDLogger LOG = new SEDLogger(AdminSEDUserView.class);

    @EJB
    private SEDLookups mdbLookups;

    private SEDUser msbCurrentSedUser;
    private SEDUser msbNewSedUser;
    private SEDUser msbEditableSedUser;

    private DualListModel<SEDBox> msbCBDualList = new DualListModel<>();

    public List<SEDUser> getSEDUsers() {
        return  mdbLookups.getSEDUsers();
    }

   

    public void onRowSelect(SelectEvent event) {
        msbEditableSedUser  = (SEDUser) event.getObject();
    }

    public void onRowUnselect(UnselectEvent event) {
        long l = LOG.logStart();
        msbEditableSedUser  = null;
        LOG.logEnd(l);
    }

    public SEDUser getCurrentSedUser() {
        return msbCurrentSedUser;
        
    }
  
    public void setCurrentSedUser(SEDUser currentSEDUser) {        
        this.msbCurrentSedUser = currentSEDUser;
        
    }

    public SEDUser getEditableSedUser() {
        return msbCurrentSedUser;
    }

    public void setEditableSedUser(SEDUser eEditableSedUser) {
        if (msbCurrentSedUser != eEditableSedUser){
            this.msbCurrentSedUser = eEditableSedUser;
            if (this.msbCurrentSedUser!= null) {
                this.msbCBDualList.setSource(mdbLookups.getSEDBoxes());
                this.msbCBDualList.setTarget(msbCurrentSedUser.getSEDBoxes());

            } else {
                this.msbCBDualList.setTarget(null);
            }
        }
        
        
        
    }
    
    

    public SEDUser getSEDBoxById(String userId) {
        List<SEDUser> lst = mdbLookups.getSEDUsers();
        for (SEDUser sb : lst) {
            if (sb.getUserId().equalsIgnoreCase(userId)) {
                return sb;
            }
        }
        return null;

    }
    public void createSEDUser() {
        long l = LOG.logStart();
       
        String sbname = "user_%03d";
        int i = 1;
        while (getSEDUserByUsername(String.format(sbname, i)) != null) {
            i++;
        }

        msbNewSedUser = new SEDUser();
        msbNewSedUser.setUserId(String.format(sbname, i));
        msbNewSedUser.setActiveFromDate(Calendar.getInstance().getTime());
        
        setEditableSedUser(msbNewSedUser);
        LOG.logEnd(l);
    }
   

    public void updateOrAddSEDUser() {
        long l = LOG.logStart();
        if (msbNewSedUser!= null){
            msbNewSedUser.getSEDBoxes().clear();
            msbNewSedUser.getSEDBoxes().addAll(msbCBDualList.getTarget());
            mdbLookups.addSEDUser(msbNewSedUser);
            msbNewSedUser = null;
        } else if (msbEditableSedUser != null) {
            msbEditableSedUser.getSEDBoxes().clear();
            msbEditableSedUser.getSEDBoxes().addAll(msbCBDualList.getTarget());
            mdbLookups.updateSEDUser(msbEditableSedUser);
        }
        LOG.logEnd(l);
    }
    
     public void removeCurrentSEDUser(){
        if (msbCurrentSedUser != null) {
            mdbLookups.removeSEDUser(msbCurrentSedUser);
            msbCurrentSedUser = null;
        }
    }

    
    public boolean isCurrentIsNew() {
        return msbNewSedUser != null;
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
 
 public DualListModel<SEDBox> getCurrentPickupDualSEDBoxList() {
        return msbCBDualList = new DualListModel<>(mdbLookups.getSEDBoxes(), getEditableSedUser()!=null?getEditableSedUser().getSEDBoxes():null);
    }

 public void setCurrentPickupDualSEDBoxList(DualListModel<SEDBox> dl) {
         msbCBDualList = dl;
    }
    

}
