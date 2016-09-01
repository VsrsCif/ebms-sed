/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package si.jrc.msh.interceptor;

import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.sed.pmode.PluginType;
import si.sed.commons.cxf.SoapUtils;

import si.sed.commons.interfaces.SoapInterceptorInterface;
import si.sed.commons.pmode.EBMSMessageContext;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.Utils;

/**
 *
 * @author sluzba
 */
public class MSHPluginOutInterceptor extends AbstractSoapInterceptor {

  /**
     *
     */
  protected final static SEDLogger LOG = new SEDLogger(MSHPluginOutInterceptor.class);

  /**
     *
     */
  public MSHPluginOutInterceptor() {
    super(Phase.USER_LOGICAL);
  }

  /**
   *
   * @param msg
   */
  @Override
  public void handleMessage(SoapMessage msg) throws Fault {
    long l = LOG.logStart();
     EBMSMessageContext ectx = SoapUtils.getEBMSMessageOutContext(msg);
    MSHOutMail outMail = SoapUtils.getMSHOutMail(msg);
    if (outMail == null){
      LOG.logWarn("No MSHOutMail object  found to process!", null);
    } else if (ectx == null){
      LOG.formatedlog("No EBMSMessageContext context for out mail: '%d'." ,outMail.getId() );
    } else if (ectx.getPMode().getOutPlugins() != null) {
      List<PluginType> lst = ectx.getPMode().getOutPlugins().getPlugins();
      for (PluginType pt : lst) {
        // todo
        String str = pt.getValue();
        if (!Utils.isEmptyString(str)) {
          try {
            SoapInterceptorInterface example = InitialContext.doLookup(str);
            example.handleMessage(msg);
          } catch (NamingException ex) {
            LOG.logError(l, ex);
          }
        }
      }
    } else {
      LOG.formatedlog("No plugin interceptor found for mail: '%d' pmode '%s'." ,outMail.getId(), ectx.getPMode().getId() );
    }
    LOG.logEnd(l);
  }

}
