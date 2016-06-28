/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package si.sed.commons.interfaces;

import javax.ejb.Local;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;

/**
 *
 * @author sluzba
 */
@Local
public interface SoapInterceptorInterface {

  /**
   *
   * @param t
   */
  public void handleMessage(SoapMessage t) throws Fault;

  /**
   *
   * @param t
   */
  public void handleFault(SoapMessage t);
}
