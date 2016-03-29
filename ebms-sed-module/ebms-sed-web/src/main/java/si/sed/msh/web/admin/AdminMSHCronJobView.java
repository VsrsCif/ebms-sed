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
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.TimerConfig;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.msh.ebms.cron.MSHCronJob;
import org.msh.ebms.cron.MSHTask;
import org.msh.ebms.cron.MSHTaskProperty;
import si.sed.commons.utils.MSHScheduler;

import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.SEDLookups;

/**
 *
 * @author Jože Rihtaršič
 */


@SessionScoped
@ManagedBean(name = "adminMSHCronJobView")
public class AdminMSHCronJobView extends AbstractAdminJSFView<MSHCronJob> {

    private static final SEDLogger LOG = new SEDLogger(AdminMSHCronJobView.class);

    @EJB
    private SEDLookups mdbLookups;

    @EJB
    private MSHScheduler mshScheduler;


    public List<MSHCronJob> getMSHCronJobs() {
        long l = LOG.logStart();
        List<MSHCronJob> lst = mdbLookups.getMSHCronJobs();
        LOG.logEnd(l, lst != null ? lst.size() : "null");
        return lst;
    }

    public String boxPrefix(String strBoxName) {
        return strBoxName != null ? strBoxName.substring(0, strBoxName.indexOf("@")) : "";
    }

   
    public MSHTaskProperty getEditableProperty(String name) {
        if (getEditable() != null && getEditable().getMSHTask() != null) {
            for (MSHTaskProperty mp : getEditable().getMSHTask().getMSHTaskProperties()) {
                if (mp.getName().equalsIgnoreCase(name)) {
                    return mp;
                }
            }
        }
        return null;
    }

    public void setEditableProperty(String name, String val) {
        MSHTaskProperty pp = null;
        if (getEditable() != null) {
            return;
        }
        if (getEditable().getMSHTask() != null) {
            for (MSHTaskProperty mp : getEditable().getMSHTask().getMSHTaskProperties()) {
                if (mp.getName().equalsIgnoreCase(name)) {
                    pp = mp;
                    break;
                }
            }
        } else {
            getEditable().setMSHTask(new MSHTask());
        }
        if (pp == null) {
            pp = new MSHTaskProperty();
            pp.setName(val);
            getEditable().getMSHTask().getMSHTaskProperties().add(pp);
        }
        pp.setValue(val);

    }

    public String getEditableMail() {
        MSHTaskProperty mtp = getEditableProperty("email");
        return mtp != null ? mtp.getValue() : null;
    }

    public void setEditableMail(String str) {
        setEditableProperty("email", str);
    }

    public String getEditableMailSubject() {
        MSHTaskProperty mtp = getEditableProperty("subject");
        return mtp != null ? mtp.getValue() : null;
    }

    public void setEditableMailSubject(String str) {
        setEditableProperty("subject", str);
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

  
    @Override
    public void createEditable() {
        MSHCronJob ecj = new MSHCronJob();
        ecj.setActive(true);
        ecj.setSecond("*/20");
        ecj.setMinute("*");
        ecj.setHour("*");
        ecj.setDayOfMonth("*");
        ecj.setMonth("*");
        ecj.setDayOfWeek("*");

        ecj.setMSHTask(new MSHTask());
        ecj.getMSHTask().setTaskType("DeliveredMail");
        MSHTaskProperty mtp = new MSHTaskProperty();
        mtp.setName("email");
        mtp.setValue("test@mail.com");
        ecj.getMSHTask().getMSHTaskProperties().add(mtp);
        mtp = new MSHTaskProperty();
        mtp.setName("subject");
        mtp.setValue("[SED] Delivered mail");
        ecj.getMSHTask().getMSHTaskProperties().add(mtp);
        mtp = new MSHTaskProperty();
        mtp.setName("NotifyOnEmptyBox");
        mtp.setValue("true");
        ecj.getMSHTask().getMSHTaskProperties().add(mtp);
        setNew(ecj);
        
    }

    @Override
    public void removeSelected() {
        if (getSelected() != null) {
            mdbLookups.removeMSHCronJob(getSelected());
            setSelected(null);

        }
    }

    @Override
    public void persistEditable() {
        MSHCronJob ecj = getEditable();
        if (ecj != null) {
            mdbLookups.addMSHCronJob(ecj);
            if (ecj.getActive() != null && ecj.getActive()) {
                LOG.log("Register timer to TimerService");
                ScheduleExpression se = new ScheduleExpression()
                        .second(ecj.getSecond())
                        .minute(ecj.getMinute())
                        .hour(ecj.getHour())
                        .dayOfMonth(ecj.getDayOfMonth())
                        .month(ecj.getMonth())
                        .dayOfWeek(ecj.getDayOfWeek());
                TimerConfig checkTest = new TimerConfig(ecj.getId(), false);
                mshScheduler.getServices().createCalendarTimer(se, checkTest);

            }
        }
        
    }

    @Override
    public void updateEditable() {
        MSHCronJob ecj = getEditable();
        if (ecj != null) {
            mdbLookups.updateMSHCronJob(ecj);            
        }
    }

    @Override
    public List<MSHCronJob> getList() {
        long l = LOG.logStart();
        List<MSHCronJob> lst = mdbLookups.getMSHCronJobs();
        LOG.logEnd(l, lst != null ? lst.size() : "null");
        return lst;
    }

   
}
