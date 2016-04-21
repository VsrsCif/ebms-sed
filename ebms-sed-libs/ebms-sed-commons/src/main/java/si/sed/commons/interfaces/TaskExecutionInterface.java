/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.interfaces;

import java.util.Properties;
import javax.ejb.Local;

/**
 *
 * @author sluzba
 */
@Local
public interface TaskExecutionInterface {
    
     public String executeTask(Properties p) throws Exception;

}
