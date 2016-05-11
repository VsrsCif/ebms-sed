/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.plugin.meps;

import java.util.Properties;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.JMSManagerInterface;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.interfaces.TaskExecutionInterface;
import si.sed.commons.interfaces.exception.TaskException;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.StringFormater;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(TaskExecutionInterface.class)
public class MEPSTask implements TaskExecutionInterface {

private static final SEDLogger LOG = new SEDLogger(MEPSTask.class);

    

    
    StringFormater msfFormat = new StringFormater();

    
    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mDB;

    @EJB(mappedName = SEDJNDI.JNDI_JMSMANAGER)
    JMSManagerInterface mJMS;
    
    @EJB(mappedName = SEDJNDI.JNDI_SEDLOOKUPS)
    SEDLookupsInterface msedLookup;
    


    @Override
    public String executeTask(Properties p) throws TaskException {

        return null;
    }

    @Override
    public String getType() {
        return "meps-plugin";
    }

    @Override
    public String getName() {
        return "MEPS plugin";
    }

    @Override
    public String getDesc() {
        return "Machine printing and enveloping task";
    }

    @Override
    public Properties getProperties() {
        Properties p = new Properties();
        

        return p;
    }
    
    
}
