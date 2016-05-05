/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Properties;
import si.sed.commons.interfaces.SEDSchedulerInterface;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.sed.ebms.cron.SEDCronJob;
import org.sed.ebms.cron.SEDTaskExecution;
import org.sed.ebms.cron.SEDTaskProperty;
import org.sed.ebms.cron.SEDTaskType;

import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDTaskStatus;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.interfaces.TaskExecutionInterface;

/**
 *
 * @author sluzba
 */
@Singleton
@Local(SEDSchedulerInterface.class)
@Lock(LockType.READ) // allows timers to execute in parallel
@Startup
public class MSHScheduler implements SEDSchedulerInterface {

    private static final SEDLogger LOG = new SEDLogger(MSHScheduler.class);
    private final AtomicInteger checks = new AtomicInteger();

    @EJB(mappedName = SEDJNDI.JNDI_SEDLOOKUPS)
    private SEDLookupsInterface mdbLookups;

    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    private SEDDaoInterface mdbDao;

    @Resource
    private TimerService timerService;

    @PostConstruct
    private void construct() {
        // read from 

        //final TimerConfig checkTest = new TimerConfig("checkTest", false);
        //timerService.createCalendarTimer(new ScheduleExpression().second("*/5").minute("*").hour("*"), checkTest);
    }

    @Timeout
    @Override
    public void timeout(Timer timer) {
        BigInteger bi = (BigInteger) (timer.getInfo());
        SEDTaskExecution te = new SEDTaskExecution();
        te.setCronId(bi);
        te.setStatus(SEDTaskStatus.INIT.getValue());
        te.setStartTimestamp(Calendar.getInstance().getTime());
        mdbDao.addExecutionTask(te);

        // get cron job
        SEDCronJob mj = mdbLookups.getSEDCronJobById(bi);

        te.setName(mj.getSEDTask().getTaskType());
        te.setType(mj.getSEDTask().getTaskType());
        if (!mj.getActive()) {
            te.setStatus(SEDTaskStatus.ERROR.getValue());
            te.setResult(String.format("Task cron id:  %d  not active!", bi));
            te.setEndTimestamp(Calendar.getInstance().getTime());
            mdbDao.updateExecutionTask(te);
            return;
        }
        SEDTaskType tt = mdbLookups.getSEDTaskTypeByType(mj.getSEDTask().getTaskType());

        TaskExecutionInterface tproc = null;
        try {
            tproc = InitialContext.doLookup(tt.getJndi());
        } catch (NamingException ex) {
            te.setStatus(SEDTaskStatus.ERROR.getValue());
            te.setResult(String.format("Error getting taskexecutor: %s. ERROR: %s", tt.getJndi(), ex.getMessage()));
            te.setEndTimestamp(Calendar.getInstance().getTime());
            mdbDao.updateExecutionTask(te);
            return;
        }
        Properties p = new Properties();
        for (SEDTaskProperty tp : mj.getSEDTask().getSEDTaskProperties()) {
            if (tp.getValue()!=null) {
                p.setProperty(tp.getKey(), tp.getValue());
            }
        }

        te.setStatus(SEDTaskStatus.PROGRESS.getValue());
        mdbDao.updateExecutionTask(te);

        try {
            String result = tproc.executeTask(p);
            te.setStatus(SEDTaskStatus.SUCCESS.getValue());
            te.setResult(result);
            te.setEndTimestamp(Calendar.getInstance().getTime());

            mdbDao.updateExecutionTask(te);
        } catch (Exception ex) {

            te.setStatus(SEDTaskStatus.ERROR.getValue());
            te.setResult(String.format("TASK ERROR: %s. Err. desc: %s", tt.getJndi(), ex.getMessage()));
            te.setEndTimestamp(Calendar.getInstance().getTime());
            mdbDao.updateExecutionTask(te);
        }

    }

    private void checkTest() {
        int i = checks.incrementAndGet();
        System.out.println("checkTest: " + i);
    }

    @Override
    public int getChecks() {
        return checks.get();
    }

    @Override
    public TimerService getServices() {
        return timerService;
    }

}
