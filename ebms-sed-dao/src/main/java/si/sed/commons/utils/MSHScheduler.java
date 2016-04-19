/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import java.math.BigInteger;
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
import org.msh.ebms.cron.MSHCronJob;
import si.sed.commons.SEDJNDI;
import si.sed.commons.email.EmailUtils;
import si.sed.commons.interfaces.SEDLookupsInterface;

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
        MSHCronJob mj = mdbLookups.getMSHCronJobById((BigInteger)(timer.getInfo()));
        
        LOG.log("Timeout for: " + mj);
        EmailUtils eu = new EmailUtils();
//        eu.sendMailMessage("joze.rihtar", subject, body);
        
        
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
