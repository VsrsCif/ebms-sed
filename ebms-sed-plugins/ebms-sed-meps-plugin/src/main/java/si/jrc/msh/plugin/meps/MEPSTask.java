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
import org.sed.ebms.cron.SEDTaskType;
import org.sed.ebms.cron.SEDTaskTypeProperty;
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

    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mDB;

    @EJB(mappedName = SEDJNDI.JNDI_JMSMANAGER)
    JMSManagerInterface mJMS;

    @EJB(mappedName = SEDJNDI.JNDI_SEDLOOKUPS)
    SEDLookupsInterface msedLookup;

    StringFormater msfFormat = new StringFormater();

    private SEDTaskTypeProperty createTTProperty(String key, String desc, boolean mandatory, String type, String valFormat, String valList) {
        SEDTaskTypeProperty ttp = new SEDTaskTypeProperty();
        ttp.setKey(key);
        ttp.setDescription(desc);
        ttp.setMandatory(mandatory);
        ttp.setType(type);
        ttp.setValueFormat(valFormat);
        ttp.setValueList(valList);
        return ttp;
    }

    private SEDTaskTypeProperty createTTProperty(String key, String desc) {
        return createTTProperty(key, desc, true, "string", null, null);
    }

    @Override
    public String executeTask(Properties p) throws TaskException {

        return null;
    }

    @Override
    public SEDTaskType getTaskDefinition() {
        SEDTaskType tt = new SEDTaskType();
        tt.setType("meps-plugin");
        tt.setName("MEPS plugin");
        tt.setDescription("Machine printing and enveloping task");
        return tt;
    }

}
