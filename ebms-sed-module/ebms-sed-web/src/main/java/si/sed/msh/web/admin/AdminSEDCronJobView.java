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
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.sed.ebms.cron.SEDCronJob;
import org.sed.ebms.cron.SEDTask;
import org.sed.ebms.cron.SEDTaskProperty;
import org.sed.ebms.cron.SEDTaskType;
import org.sed.ebms.cron.SEDTaskTypeProperty;

import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.interfaces.SEDSchedulerInterface;

import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "adminSEDCronJobView")
public class AdminSEDCronJobView extends AbstractAdminJSFView<SEDCronJob> {

    private static final SEDLogger LOG = new SEDLogger(AdminSEDCronJobView.class);

    @EJB(mappedName = SEDJNDI.JNDI_SEDLOOKUPS)
    private SEDLookupsInterface mdbLookups;

    @EJB(mappedName = SEDJNDI.JNDI_SEDSCHEDLER)
    private SEDSchedulerInterface mshScheduler;



    public SEDTaskProperty getEditableProperty(String name) {
        if (getEditable() != null && getEditable().getSEDTask() != null) {
            for (SEDTaskProperty mp : getEditable().getSEDTask().getSEDTaskProperties()) {
                if (mp.getKey().equalsIgnoreCase(name)) {
                    return mp;
                }
            }
        }
        return null;
    }

    public void setEditableProperty(String name, String val) {
        SEDTaskProperty pp = null;
        if (getEditable() != null) {
            return;
        }
        if (getEditable().getSEDTask() != null) {
            for (SEDTaskProperty mp : getEditable().getSEDTask().getSEDTaskProperties()) {
                if (mp.getKey().equalsIgnoreCase(name)) {
                    pp = mp;
                    break;
                }
            }
        } else {
            getEditable().setSEDTask(new SEDTask());
        }
        if (pp == null) {
            pp = new SEDTaskProperty();
            pp.setKey(val);
            getEditable().getSEDTask().getSEDTaskProperties().add(pp);
        }
        pp.setValue(val);

    }

    public SEDCronJob getMSHCronJobByName(BigInteger id) {
        List<SEDCronJob> lst = mdbLookups.getSEDCronJobs();
        for (SEDCronJob sb : lst) {
            if (sb.getId().equals(id)) {
                return sb;
            }
        }
        return null;
    }

    @Override
    public void createEditable() {
        SEDCronJob ecj = new SEDCronJob();
        ecj.setActive(true);
        ecj.setSecond("*/20");
        ecj.setMinute("*");
        ecj.setHour("*");
        ecj.setDayOfMonth("*");
        ecj.setMonth("*");
        ecj.setDayOfWeek("*");

        ecj.setSEDTask(new SEDTask());
        ecj.getSEDTask().setTaskType("DeliveredMail");
        SEDTaskProperty mtp = new SEDTaskProperty();
        mtp.setKey("email");
        mtp.setValue("test@mail.com");
        ecj.getSEDTask().getSEDTaskProperties().add(mtp);
        mtp = new SEDTaskProperty();
        mtp.setKey("subject");
        mtp.setValue("[SED] Delivered mail");
        ecj.getSEDTask().getSEDTaskProperties().add(mtp);
        mtp = new SEDTaskProperty();
        mtp.setKey("NotifyOnEmptyBox");
        mtp.setValue("true");
        ecj.getSEDTask().getSEDTaskProperties().add(mtp);
        setNew(ecj);

    }

    @Override
    public void removeSelected() {
        if (getSelected() != null) {
            mdbLookups.removeSEDCronJob(getSelected());
            setSelected(null);

        }
    }

    @Override
    public void persistEditable() {
        SEDCronJob ecj = getEditable();
        if (ecj != null) {
            mdbLookups.addSEDCronJob(ecj);
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
        SEDCronJob ecj = getEditable();
        if (ecj != null) {
            mdbLookups.updateSEDCronJob(ecj);
            for (Timer t : mshScheduler.getServices().getAllTimers()) {
                if (t.getInfo().equals(ecj.getId())) {
                    t.cancel();
                    break;
                }
            }
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
    public List<SEDCronJob> getList() {
        long l = LOG.logStart();
        List<SEDCronJob> lst = mdbLookups.getSEDCronJobs();
        LOG.logEnd(l, lst != null ? lst.size() : "null");
        return lst;
    }

    public List<String> getTaskTypeList() {
        long l = LOG.logStart();
        List<String> rstLst = new ArrayList<>();
        List<SEDTaskType> lst = mdbLookups.getSEDTaskTypes();
        lst.stream().forEach((tsk) -> {
            rstLst.add(tsk.getType());
        });
        LOG.logEnd(l, lst != null ? lst.size() : "null");
        return rstLst;
    }
    
    
    public void setEditableTask(String task){
        System.out.println("Set task:"  + task);
        SEDCronJob scj = getEditable();
        if (scj !=null){
            System.out.println("Set task: 1");
            if (scj.getSEDTask()==null 
                    || scj.getSEDTask().getTaskType()==null
                    || !scj.getSEDTask().getTaskType().equals(task)) {
                System.out.println("Set task: 2");
                SEDTaskType sdt =  mdbLookups.getSEDTaskTypeByType(task);
                if (sdt!= null){
                    System.out.println("Set task: 3");
                    SEDTask  tsk = new SEDTask();
                    tsk.setTaskType(sdt.getType());
                    for (SEDTaskTypeProperty  p: sdt.getSEDTaskTypeProperties()){
                        SEDTaskProperty tp = new SEDTaskProperty();
                        tp.setKey(p.getKey());
                        tsk.getSEDTaskProperties().add(tp);
                    }
                    System.out.println("Set task: 4");
                    scj.setSEDTask(tsk);
                }
                
                
            }
            
        }
        
    }
    
    public String getEditableTask(){
        if (getEditable()!=null && getEditable().getSEDTask()!=null){
            return getEditable().getSEDTask().getTaskType();
        }
        return null;
    }
    
}
