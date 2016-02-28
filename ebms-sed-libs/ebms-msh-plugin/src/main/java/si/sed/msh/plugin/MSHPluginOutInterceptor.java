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
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.svev.pmode.PMode;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author sluzba
 */
public class MSHPluginOutInterceptor extends AbstractSoapInterceptor {

    private static final String PLUGIN_FOLDER = "plugins";

    protected final SEDLogger mlog = new SEDLogger(MSHPluginOutInterceptor.class);

    public MSHPluginOutInterceptor() {
        super(Phase.USER_LOGICAL);
    }

    @Override
    public void handleMessage(SoapMessage msg) throws Fault {
        long l = mlog.logStart();
        PMode pmd = msg.getExchange().get(PMode.class);
        MSHOutMail outMail = msg.getExchange().get(MSHOutMail.class);
        MSHInMail inMail = msg.getExchange().get(MSHInMail.class);
        if (pmd != null && outMail != null) {
            // todo
            String str = pmd.getLegs().get(0).getBusinessInfo().getService().getOutPlugin();
            if (str != null) {
                String[] lst = str.split("!");
                String filenamePlugin = lst[0];
                String classNamePlugin = lst[1];
                mlog.log("Invoke: plugin :  " + str);
                AbstractPluginInterceptor ii = PluginManager.getInterceptor(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator + PLUGIN_FOLDER + File.separator + filenamePlugin, classNamePlugin);
                ii.handleMessage(msg);
            }
        }

        mlog.logEnd(l);
    }

}
