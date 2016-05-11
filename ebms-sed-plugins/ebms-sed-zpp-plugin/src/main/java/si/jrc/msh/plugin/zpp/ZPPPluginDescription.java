/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.plugin.zpp;

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
public class ZPPPluginDescription implements PluginDescriptionInterface{

    @Override
    public String getSettingUrlContext() {
        return "/zpp-plugin";
    }

    @Override
    public List<String> getTaskJNDIs() {
        return Collections.singletonList("java:global/plugin-zpp/ZPPTask!si.sed.commons.interfaces.TaskExecutionInterface");
    }

    @Override
    public String getJNDIOutInterceptor() {
        return "java:global/plugin-zpp/ZPPOutInterceptor!si.sed.commons.interfaces.SoapInterceptorInterface";
    }

    @Override
    public String getJNDIInInterceptor() {
        return "java:global/plugin-zpp/ZPPOutInterceptor!si.sed.commons.interfaces.SoapInterceptorInterface";
    }

    @Override
    public String getType() {
        return "LegalZPP";
    }

    @Override
    public String getName() {
        return "ZPP plugin";
    }

    @Override
    public String getDesc() {
        return "ZPP - e-delivery: SVEV 2.0 service implementation";
    }

  
    
}
