/*
 * Copyright 2015, Supreme Court Republic of Slovenia
 * 
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package si.jrc.msh.interceptor;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.sed.pmode.PMode;
import si.sed.commons.interfaces.SoapInterceptorInterface;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author sluzba
 */
public class MSHPluginInInterceptor extends AbstractSoapInterceptor {

  /**
     *
     */
  protected final SEDLogger mlog = new SEDLogger(MSHPluginInInterceptor.class);

  /**
     *
     */
  public MSHPluginInInterceptor() {
    super(Phase.PRE_INVOKE);
  }

  /**
   *
   * @param msg
   */
  @Override
  public void handleMessage(SoapMessage msg) throws Fault {
    long l = mlog.logStart();
    /*PMode pmd = msg.getExchange().get(PMode.class);
    MSHInMail inMail = msg.getExchange().get(MSHInMail.class);
    MSHOutMail outMail = msg.getExchange().get(MSHOutMail.class);

    if (pmd != null && inMail != null) {
      // todo
      String str = pmd.getLegs().get(0).getBusinessInfo().getService().getInPlugin();
      if (str != null) {
        try {
          SoapInterceptorInterface example = InitialContext.doLookup(str);
          example.handleMessage(msg);
        } catch (NamingException ex) {
          mlog.logError(l, ex);
        }
        /*
         * String[] lst = str.split("!"); String filenamePlugin = lst[0]; String classNamePlugin =
         * lst[1];
         * 
         * AbstractPluginInterceptor ii =
         * PluginManager.getInterceptor(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) +
         * File.separator + SEDSystemProperties.SYS_PROP_FOLDER_PLUGINS_DEF + File.separator +
         * filenamePlugin, classNamePlugin); ii.handleMessage(msg);
         * /
      }
    } else if (pmd != null) {
      String str = pmd.getLegs().get(0).getBusinessInfo().getService().getInPlugin();

      if (str != null) {
        try {
          SoapInterceptorInterface example = InitialContext.doLookup(str);
          example.handleMessage(msg);
        } catch (NamingException ex) {
          mlog.logError(l, ex);
        }

        /*
         * String[] lst = str.split("!"); String filenamePlugin = lst[0]; String classNamePlugin =
         * lst[1]; mlog.log("Invoke: plugin :  " + str); AbstractPluginInterceptor ii =
         * PluginManager.getInterceptor(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) +
         * File.separator + SEDSystemProperties.SYS_PROP_FOLDER_PLUGINS_DEF + File.separator +
         * filenamePlugin, classNamePlugin); ii.handleMessage(msg);
         * /
      }
    }*/
    mlog.logEnd(l);
  }

}
