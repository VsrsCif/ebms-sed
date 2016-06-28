/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package si.sed.commons.utils;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.sed.ebms.cron.SEDCronJob;
import org.sed.ebms.cron.SEDTaskExecution;
import org.sed.ebms.cron.SEDTaskProperty;
import org.sed.ebms.cron.SEDTaskType;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDTaskStatus;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.interfaces.SEDSchedulerInterface;
import si.sed.commons.interfaces.TaskExecutionInterface;
import si.sed.commons.interfaces.exception.TaskException;

/**
 *
 * @author sluzba
 */
@Singleton
@Local(SEDSchedulerInterface.class)
@Lock(LockType.READ)
// allows timers to execute in parallel
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
    List<SEDCronJob> lst = mdbLookups.getSEDCronJobs();
    for (SEDCronJob ecj : lst) {
      if (ecj.getActive() != null && ecj.getActive()) {
        ScheduleExpression se =
            new ScheduleExpression().second(ecj.getSecond()).minute(ecj.getMinute())
                .hour(ecj.getHour()).dayOfMonth(ecj.getDayOfMonth()).month(ecj.getMonth())
                .dayOfWeek(ecj.getDayOfWeek());
        TimerConfig checkTest = new TimerConfig(ecj.getId(), false);
        getServices().createCalendarTimer(se, checkTest);
      }
    }
  }

  /**
   *
   * @param timer
   */
  @Timeout
  @Override
  public void timeout(Timer timer) {
    long l = LOG.logStart();

    BigInteger bi = (BigInteger) (timer.getInfo());
    SEDTaskExecution te = new SEDTaskExecution();
    te.setCronId(bi);
    te.setStatus(SEDTaskStatus.INIT.getValue());
    te.setStartTimestamp(Calendar.getInstance().getTime());

    try {
      mdbDao.addExecutionTask(te);
    } catch (StorageException ex) {
      LOG.logEnd(l, "Error storing task: '" + te.getType() + "' ", ex);
      return;
    }

    // get cron job
    SEDCronJob mj = mdbLookups.getSEDCronJobById(bi);

    te.setName(mj.getSEDTask().getTaskType());
    te.setType(mj.getSEDTask().getTaskType());
    if (!mj.getActive()) {
      te.setStatus(SEDTaskStatus.ERROR.getValue());
      te.setResult(String.format("Task cron id:  %d  not active!", bi));
      te.setEndTimestamp(Calendar.getInstance().getTime());
      try {
        mdbDao.updateExecutionTask(te);
      } catch (StorageException ex) {
        LOG.logEnd(l, "Error updating task: '" + te.getType() + "' ", ex);
      }
      return;
    }
    SEDTaskType tt = mdbLookups.getSEDTaskTypeByType(mj.getSEDTask().getTaskType());

    TaskExecutionInterface tproc = null;
    try {
      tproc = InitialContext.doLookup(tt.getJndi());
    } catch (NamingException ex) {
      te.setStatus(SEDTaskStatus.ERROR.getValue());
      te.setResult(String.format("Error getting taskexecutor: %s. ERROR: %s", tt.getJndi(),
          ex.getMessage()));
      te.setEndTimestamp(Calendar.getInstance().getTime());
      try {
        mdbDao.updateExecutionTask(te);
      } catch (StorageException ex2) {
        LOG.logEnd(l, "Error updating task: '" + te.getType() + "' ", ex2);
      }
      return;
    }
    Properties p = new Properties();
    for (SEDTaskProperty tp : mj.getSEDTask().getSEDTaskProperties()) {
      if (tp.getValue() != null) {
        p.setProperty(tp.getKey(), tp.getValue());
      }
    }

    te.setStatus(SEDTaskStatus.PROGRESS.getValue());
    try {
      mdbDao.updateExecutionTask(te);
    } catch (StorageException ex2) {
      LOG.logEnd(l, "Error updating task: '" + te.getType() + "' ", ex2);
      return;
    }

    try {
      String result = tproc.executeTask(p);
      te.setStatus(SEDTaskStatus.SUCCESS.getValue());
      te.setResult(result);
      te.setEndTimestamp(Calendar.getInstance().getTime());

      try {
        mdbDao.updateExecutionTask(te);
      } catch (StorageException ex2) {
        LOG.logEnd(l, "Error updating task: '" + te.getType() + "' ", ex2);
        return;
      }
    } catch (TaskException ex) {

      te.setStatus(SEDTaskStatus.ERROR.getValue());
      te.setResult(String.format("TASK ERROR: %s. Err. desc: %s", tt.getJndi(), ex.getMessage()));
      te.setEndTimestamp(Calendar.getInstance().getTime());
      LOG.logEnd(l, "Error updating task: '" + te.getType() + "' ", ex);
      try {
        mdbDao.updateExecutionTask(te);
      } catch (StorageException ex2) {
        LOG.logEnd(l, "Error updating task: '" + te.getType() + "' ", ex2);
        return;
      }
    }
    LOG.logEnd(l);
  }

  private void checkTest() {
    int i = checks.incrementAndGet();
    LOG.log("checkTest: " + i);
  }

  /**
   *
   * @return
   */
  @Override
  public int getChecks() {
    return checks.get();
  }

  /**
   *
   * @return
   */
  @Override
  public TimerService getServices() {
    return timerService;
  }

}
