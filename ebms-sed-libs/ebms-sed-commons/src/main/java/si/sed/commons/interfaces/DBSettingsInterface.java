/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.interfaces;

import java.util.Properties;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;

/**
 *
 * @author sluzba
 */
@Local
public interface DBSettingsInterface {

    @Lock(value = LockType.READ)
    String getDomain();

    @Lock(value = LockType.READ)
    String getHomeFolderPath();

    @Lock(value = LockType.READ)
    String getPModeFileName();

    Properties getProperties();

    @Lock(value = LockType.READ)
    String getSecurityFolderPath();

    void initialize();

}
