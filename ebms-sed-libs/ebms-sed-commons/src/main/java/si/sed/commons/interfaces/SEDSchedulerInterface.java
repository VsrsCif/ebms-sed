/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.interfaces;

import javax.ejb.Local;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;

/**
 *
 * @author sluzba
 */
@Local
public interface SEDSchedulerInterface {

    /**
     *
     * @return
     */
    int getChecks();

    /**
     *
     * @return
     */
    TimerService getServices();

    /**
     *
     * @param timer
     */
    @Timeout
    void timeout(Timer timer);

}
