/*
* Copyright 2015, Supreme Court Republic of Slovenia 
*
* Licensed under the EUPL, Version 1.1 or – as soon they will be approved by 
* the European Commission - subsequent versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the Licence.
* You may obtain a copy of the Licence at:
*
* https://joinup.ec.europa.eu/software/page/eupl
*
* Unless required by applicable law or agreed to in writing, software 
* distributed under the Licence is distributed on an "AS IS" basis, WITHOUT 
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the Licence for the specific language governing permissions and  
* limitations under the Licence.
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
public class MSHPluginInInterceptor extends AbstractSoapInterceptor {
    
    private static final String PLUGIN_FOLDER="plugins" ;

    protected final SEDLogger mlog = new SEDLogger(MSHPluginInInterceptor.class);

    public MSHPluginInInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    @Override
    public void handleMessage(SoapMessage msg) throws Fault {
        long l = mlog.logStart();
        // todo read plugins form service
        String filenamePlugin = "ebms-sed-zpp-plugin-1.0.jar";
        String classNamePlugin = "si.jrc.msh.plugin.zpp.ZPPInInterceptor";
        AbstractPluginInterceptor ii = PluginManager.getInterceptor(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator + PLUGIN_FOLDER + File.separator + filenamePlugin, classNamePlugin);
        ii.handleMessage(msg);
        
        mlog.logEnd(l);
    }

   

}
