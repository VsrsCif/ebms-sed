/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
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

  /**
   *
   * @return
   */
  String getSettingUrlContext();

  /**
   *
   * @return
   */
  List<String> getTaskJNDIs();

  /**
   *
   * @return
   */
  String getJNDIOutInterceptor();

  /**
   *
   * @return
   */
  String getJNDIInInterceptor();

  /**
   *
   * @return
   */
  String getType();

  /**
   *
   * @return
   */
  String getName();

  /**
   *
   * @return
   */
  String getDesc();
}
