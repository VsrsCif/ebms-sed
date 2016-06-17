/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.interfaces;

import java.util.List;
import javax.ejb.Local;

/**
 *
 * @author sluzba
 */
@Local
public interface PluginDescriptionInterface {

    String getSettingUrlContext();

    List<String> getTaskJNDIs();

    String getJNDIOutInterceptor();

    String getJNDIInInterceptor();

    String getType();

    String getName();

    String getDesc();
}
