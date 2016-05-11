/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.plugin.meps;

import java.util.Collections;
import java.util.List;
import javax.ejb.Local;
import javax.ejb.Stateless;
import si.sed.commons.interfaces.PluginDescriptionInterface;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(PluginDescriptionInterface.class)
public class MEPSPluginDescription implements PluginDescriptionInterface{

    @Override
    public String getSettingUrlContext() {
        return "/meps";
    }

    @Override
    public List<String> getTaskJNDIs() {
        return Collections.singletonList("java:global/plugin-meps/MEPSTask!si.sed.commons.interfaces.TaskExecutionInterface");
    }

    @Override
    public String getJNDIOutInterceptor() {
        return "java:global/plugin-meps/MEPSOutInterceptor!si.sed.commons.interfaces.SoapInterceptorInterface";
    }

    @Override
    public String getJNDIInInterceptor() {
        return "java:global/plugin-meps/MEPSOutInterceptor!si.sed.commons.interfaces.SoapInterceptorInterface";
    }

    @Override
    public String getType() {
        return "MEPSPlugin";
    }

    @Override
    public String getName() {
        return "MEPS-plugin";
    }

    @Override
    public String getDesc() {
        return "";
    }

  
    
}
