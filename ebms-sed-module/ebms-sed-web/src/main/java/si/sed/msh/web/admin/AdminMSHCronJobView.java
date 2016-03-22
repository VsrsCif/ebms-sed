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

import java.math.BigInteger;
import si.sed.msh.web.gui.*;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.msh.ebms.cron.MSHCronJob;
import org.msh.ebms.cron.MSHTask;
import org.msh.ebms.cron.MSHTaskProperty;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;

import si.sed.commons.utils.DBSettings;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.SEDLookups;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "adminMSHCronJobView")
public class AdminMSHCronJobView extends AbstractJSFView {

    private static final SEDLogger LOG = new SEDLogger(AdminMSHCronJobView.class);

    @EJB
    private DBSettings mdbSettings;

    @EJB
    private SEDLookups mdbLookups;

    private MSHCronJob msbCurrentMSHCronJob;
    private MSHCronJob msbNewMSHCronJob;
    private MSHCronJob msbEditableMSHCronJob;

    public List<MSHCronJob> getMSHCronJobs() {
        List<MSHCronJob> lst = mdbLookups.getMSHCronJobs();
        return lst;
    }

    public String boxPrefix(String strBoxName) {
        return strBoxName != null ? strBoxName.substring(0, strBoxName.indexOf("@")) : "";
    }

    public void onRowSelect(SelectEvent event) {
        msbEditableMSHCronJob = (MSHCronJob) event.getObject();
    }

    public void onRowUnselect(UnselectEvent event) {
        long l = LOG.logStart();
        msbEditableMSHCronJob = null;
        LOG.logEnd(l);
    }

    public MSHCronJob getCurrentMSHCronJob() {
        return msbCurrentMSHCronJob;

    }

    public void setCurrentMSHCronJob(MSHCronJob currentMSHCronJob) {
        this.msbCurrentMSHCronJob = currentMSHCronJob;
    }

    public MSHCronJob getEditableMSHCronJob() {
        return msbEditableMSHCronJob;
    }

    public void setEditableMSHCronJob(MSHCronJob sbEditableMSHCronJob) {
        this.msbEditableMSHCronJob = sbEditableMSHCronJob;
    }
    
    public MSHTaskProperty getEditablePropertyByMail(String name){
        if (msbEditableMSHCronJob!=null && msbEditableMSHCronJob.getMSHTask()!=null 
                && msbEditableMSHCronJob.getMSHTask().getMSHTaskProperties()!= null) {
            for (MSHTaskProperty mp: msbEditableMSHCronJob.getMSHTask().getMSHTaskProperties()){
                if (mp.getValue().equalsIgnoreCase(name)){
                    return mp;
                }
            }
            
            
        }
        return null;
    }
    
    public String getEditableMail() {
        MSHTaskProperty mtp = getEditablePropertyByMail("email");
        return mtp!=null?mtp.getValue():null;
    }

    public void setEditableMail(String str) {
        MSHTaskProperty mtp = getEditablePropertyByMail("email");
        if (mtp!= null){
            mtp.setValue(str);
        }
    }
     public String getEditableMailSubject() {
        MSHTaskProperty mtp = getEditablePropertyByMail("subject");
        return mtp!=null?mtp.getValue():null;
    }

    public void setEditableMailSubject(String str) {
        MSHTaskProperty mtp = getEditablePropertyByMail("subject");
        if (mtp!= null){
            mtp.setValue(str);
        }
    }
    
    

    public void removeCurrentMSHCronJob() {
        if (msbCurrentMSHCronJob != null) {

            mdbLookups.removeMSHCronJob(msbCurrentMSHCronJob);
            msbCurrentMSHCronJob = null;

        }
    }

    public MSHCronJob getMSHCronJobByName(BigInteger id) {
        List<MSHCronJob> lst = mdbLookups.getMSHCronJobs();
        for (MSHCronJob sb : lst) {
            if (sb.getId().equals(id)) {
                return sb;
            }
        }
        return null;

    }

    public void createMSHCronJob() {
        long l = LOG.logStart();

        msbNewMSHCronJob = new MSHCronJob();
        msbNewMSHCronJob.setDayOfMonth("*");
        msbNewMSHCronJob.setDayOfWeek("*");
        msbNewMSHCronJob.setHour("*");
        msbNewMSHCronJob.setMinute("*");
        msbNewMSHCronJob.setMonth("*");
        msbNewMSHCronJob.setMSHTask(new MSHTask());
        msbNewMSHCronJob.getMSHTask().setTaskType("DeliveredMail");
        MSHTaskProperty mtp = new MSHTaskProperty();
        mtp.setName("email");
        mtp.setValue("joze.rihtarsic@sodisce.si");        
        msbNewMSHCronJob.getMSHTask().getMSHTaskProperties().add(mtp);
        mtp = new MSHTaskProperty();
        mtp.setName("Subject");
        mtp.setValue("[SED] Delivered mail");        
        msbNewMSHCronJob.getMSHTask().getMSHTaskProperties().add(mtp);
        mtp = new MSHTaskProperty();
        mtp.setName("NotifyOnEmptyBox");
        mtp.setValue("true");        
        msbNewMSHCronJob.getMSHTask().getMSHTaskProperties().add(mtp);

        setEditableMSHCronJob(msbNewMSHCronJob);
        LOG.logEnd(l);
    }

    public void updateOrAddMSHCronJob() {
        long l = LOG.logStart();
        if (msbNewMSHCronJob != null) {
            mdbLookups.addMSHCronJob(msbNewMSHCronJob);
            msbNewMSHCronJob = null;
        } else if (msbEditableMSHCronJob != null) {
            mdbLookups.updateMSHCronJob(msbEditableMSHCronJob);
        }
        LOG.logEnd(l);
    }

    public boolean isCurrentIsNew() {
        return msbNewMSHCronJob == msbEditableMSHCronJob;
    }

}
