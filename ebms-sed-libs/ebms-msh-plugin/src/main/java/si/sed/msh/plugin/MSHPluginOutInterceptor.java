/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.plugin;

import java.io.File;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author sluzba
 */
public class MSHPluginOutInterceptor extends AbstractSoapInterceptor {
    private static final String PLUGIN_FOLDER="plugins" ;

    protected final SEDLogger mlog = new SEDLogger(MSHPluginOutInterceptor.class);

    public MSHPluginOutInterceptor() {
        super(Phase.USER_LOGICAL);
    }

    @Override
    public void handleMessage(SoapMessage msg) throws Fault {
        long l = mlog.logStart();
       
        
        // todo read plugins form service
        String filenamePlugin = "ebms-sed-zpp-plugin-1.0.jar";
        String classNamePlugin = "si.jrc.msh.plugin.zpp.ZPPOutInterceptor";
        AbstractPluginInterceptor ii = PluginManager.getInterceptor(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator + PLUGIN_FOLDER + File.separator + filenamePlugin, classNamePlugin);
        ii.handleMessage(msg);
        
        
        
        mlog.logEnd(l);
    }

}
