/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.task;

import java.util.Properties;
import javax.ejb.Local;
import javax.ejb.Stateless;
import si.sed.commons.interfaces.TaskExecutionInterface;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(TaskExecutionInterface.class)
public class TaskEmailReport implements TaskExecutionInterface {

    private static final SEDLogger LOG = new SEDLogger(TaskEmailReport.class);

    @Override
    public String executeTask(Properties p) throws Exception {
        return "suc";
    }
}
