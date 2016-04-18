/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.interfaces;

import javax.ejb.Local;
import javax.jms.JMSException;
import javax.naming.NamingException;


@Local
public interface JMSManagerInterface {
    
    boolean sendMessage(long biPosiljkaId, String strPmodeId, int retry, long delay, boolean transacted) throws NamingException, JMSException;
    
    boolean executeProcessOnInMail(long biInMailId, String command, String parameters) throws NamingException, JMSException;
}
