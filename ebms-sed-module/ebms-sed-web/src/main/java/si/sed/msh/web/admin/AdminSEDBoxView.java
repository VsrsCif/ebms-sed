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

import java.io.StringWriter;
import si.sed.msh.web.gui.*;
import java.util.Calendar;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.user.SEDUser;
import si.sed.commons.utils.DBSettings;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.SEDLookups;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "adminSEDBoxView")
public class AdminSEDBoxView extends AbstractJSFView {

    private static final SEDLogger LOG = new SEDLogger(AdminSEDBoxView.class);

    @EJB
    private DBSettings mdbSettings;

    @EJB
    private SEDLookups mdbLookups;

    private SEDBox msbCurrentSedBox;
    private SEDBox msbNewSedBox;
    private SEDBox msbEditableSedBox;


    public List<SEDBox> getSEDBoxes() {
        List<SEDBox> lst = mdbLookups.getSEDBoxes();
        return lst;
    }

    public String boxPrefix(String strBoxName) {
        return strBoxName != null ? strBoxName.substring(0, strBoxName.indexOf("@")) : "";
    }

    public void onRowSelect(SelectEvent event) {
        msbEditableSedBox  = (SEDBox) event.getObject();
    }

    public void onRowUnselect(UnselectEvent event) {
        long l = LOG.logStart();
        msbEditableSedBox  = null;
        LOG.logEnd(l);
    }

    public SEDBox getCurrentSedBox() {
        return msbCurrentSedBox;
        
    }
  
    public void setCurrentSedBox(SEDBox currentSedBox) {        
        this.msbCurrentSedBox = currentSedBox;
    }

    public SEDBox getEditableSedBox() {
        return msbEditableSedBox;
    }

    public void setEditableSedBox(SEDBox msbEditableSedBox) {
        this.msbEditableSedBox = msbEditableSedBox;
    }
    
    public void removeCurrentSEDBox(){
        if (msbCurrentSedBox != null) {
            LOG.log("Delete:" + msbCurrentSedBox.getBoxName() + " sed users:" +msbCurrentSedBox.getSEDUsers().size());
            if (!msbCurrentSedBox.getSEDUsers().isEmpty()){
                StringWriter sw = new StringWriter();
                sw.append("Users: ");
                boolean isFirst = true;
                for (SEDUser su: msbCurrentSedBox.getSEDUsers()){
                    if (!isFirst){
                        sw.append(",");
                    }
                    sw.append(su.getUserId());
                    isFirst = false;                    
                }
                
                 FacesMessage msg = new FacesMessage("Box has users! Delete box from users first!", sw.toString() );
                 facesContext().addMessage("messages", msg);
                
            } else {        
                mdbLookups.removeSEDBox(msbCurrentSedBox);
                msbCurrentSedBox = null;
            }
        }
    }
    
    

    public SEDBox getSEDBoxByName(String sedBox) {
        List<SEDBox> lst = mdbLookups.getSEDBoxes();
        for (SEDBox sb : lst) {
            if (sb.getBoxName().equalsIgnoreCase(sedBox)) {
                return sb;
            }
        }
        return null;

    }
  
    public void createSEDBox() {
        long l = LOG.logStart();

        String domain = mdbSettings.getDomain();
        String sbname = "name.%03d@%s";
        int i = 1;
        while (getSEDBoxByName(String.format(sbname, i, domain)) != null) {
            i++;
        }

        msbNewSedBox = new SEDBox();
        msbNewSedBox.setBoxName(String.format(sbname, i, domain));
        msbNewSedBox.setActiveFromDate(Calendar.getInstance().getTime());
        
        setEditableSedBox(msbNewSedBox);
        LOG.logEnd(l);
    }

    public void updateOrAddSEDBox() {
        long l = LOG.logStart();
        if (msbNewSedBox!= null){
            mdbLookups.addSEDBox(msbNewSedBox);
            msbNewSedBox = null;
        } else if (msbEditableSedBox != null) {
            mdbLookups.updateSEDBox(msbEditableSedBox);
        }
        LOG.logEnd(l);
    }

    
    public boolean isCurrentIsNew() {
        return msbNewSedBox == msbEditableSedBox;
    }


    

}
