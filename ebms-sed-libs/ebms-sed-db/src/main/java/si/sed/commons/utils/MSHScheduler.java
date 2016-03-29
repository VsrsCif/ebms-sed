/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

/**
 *
 * @author sluzba
 */
@Singleton
@Lock(LockType.READ) // allows timers to execute in parallel
@Startup
public class MSHScheduler {

    private static final SEDLogger LOG = new SEDLogger(MSHScheduler.class);
    private final AtomicInteger checks = new AtomicInteger();

    @Resource
    private TimerService timerService;

    @PostConstruct
    private void construct() {
        // read from 

        //final TimerConfig checkTest = new TimerConfig("checkTest", false);
        //timerService.createCalendarTimer(new ScheduleExpression().second("*/5").minute("*").hour("*"), checkTest);
     
    }

    @Timeout
    public void timeout(Timer timer) {
        LOG.log("Timeout for: " + timer.getInfo());
        if ("checkTest".equals(timer.getInfo())) {

            checkTest();
        }
    }

    private void checkTest() {
        int i = checks.incrementAndGet();
        System.out.println("checkTest: " + i);
    }

    public int getChecks() {
        return checks.get();
    }

    public TimerService getServices() {
        return timerService;
    }

}
