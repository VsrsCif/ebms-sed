/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.interfaces;

import javax.ejb.Local;
import javax.jms.JMSException;
import javax.naming.NamingException;

/**
 *
 * @author sluzba
 */
@Local
public interface JMSManagerInterface {

    /**
     *
     * @param biPosiljkaId
     * @param strPmodeId
     * @param retry
     * @param delay
     * @param transacted
     * @return
     * @throws NamingException
     * @throws JMSException
     */
    boolean sendMessage(long biPosiljkaId, String strPmodeId, int retry, long delay, boolean transacted) throws NamingException, JMSException;

    /**
     *
     * @param biInMailId
     * @param command
     * @param parameters
     * @return
     * @throws NamingException
     * @throws JMSException
     */
    boolean executeProcessOnInMail(long biInMailId, String command, String parameters) throws NamingException, JMSException;
}
