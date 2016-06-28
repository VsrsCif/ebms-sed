/*
 * Copyright 2016, Supreme Court Republic of Slovenia
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
package si.sed.msh.web.admin;

import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.ActionEvent;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.sed.ebms.cron.SEDTaskType;
import org.sed.ebms.cron.SEDTaskTypeProperty;
import org.sed.ebms.plugin.SEDPlugin;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.PluginDescriptionInterface;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.interfaces.TaskExecutionInterface;
import si.sed.commons.utils.SEDLogger;
import si.sed.msh.web.abst.AbstractAdminJSFView;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "adminSEDPluginView")
public class AdminSEDPluginView extends AbstractAdminJSFView<SEDPlugin> {

  private static final SEDLogger LOG = new SEDLogger(AdminSEDPluginView.class);

  @EJB(mappedName = SEDJNDI.JNDI_SEDLOOKUPS)
  private SEDLookupsInterface mdbLookups;

  SEDPlugin selectedViewPlugin;

  /**
     *
     */
  @Override
  public void createEditable() {
    SEDPlugin ecj = new SEDPlugin();

    ecj.setJndi("java:global[/application name]/module name/enterprise bean name[/interface name]");
    ecj.setType("unique-type");
    ecj.setName("name");
    ecj.setDescription("Enter JNDI and refresh data from EJB task!");

    setNew(ecj);
  }

  /**
     *
     */
  public void refreshDataFromEJB() {
    if (getEditable() == null || getEditable().getJndi() == null
        || getEditable().getJndi().isEmpty()) {
      return;
    }
    try {
      PluginDescriptionInterface pdi = InitialContext.doLookup(getEditable().getJndi());
      getEditable().setDescription(pdi.getDesc());
      getEditable().setName(pdi.getName());
      getEditable().setType(pdi.getType());
      getEditable().setJndiInInterceptor(pdi.getJNDIInInterceptor());
      getEditable().setJndiOutInterceptor(pdi.getJNDIOutInterceptor());
      getEditable().setWebContext(pdi.getSettingUrlContext());
      getEditable().setTasksJNDIs(String.join(",", pdi.getTaskJNDIs()));

    } catch (NamingException ex) {

    }

  }

  /**
     *
     */
  @Override
  public void removeSelected() {
    if (getSelected() != null) {
      mdbLookups.removeSEDPlugin(getSelected());
      setSelected(null);
    }

  }

  /**
     *
     */
  @Override
  public void persistEditable() {
    SEDPlugin ecj = getEditable();
    if (ecj != null) {
      String tjndis = ecj.getTasksJNDIs();
      if (tjndis != null && !tjndis.trim().isEmpty()) {
        String[] lst = tjndis.split(",");
        for (String jndi : lst) {
          SEDTaskType td = getSEDTaskType(jndi);
          if (td != null) {
            mdbLookups.addSEDTaskType(td);
          }

        }
      }
      mdbLookups.addSEDPlugin(ecj);

    }
  }

  private SEDTaskType getSEDTaskType(String jndi) {
    try {
      SEDTaskType td = new SEDTaskType();
      TaskExecutionInterface tproc = InitialContext.doLookup(jndi);
      td.setJndi(jndi);

      td.setDescription(tproc.getTaskDefinition().getDescription());
      td.setName(tproc.getTaskDefinition().getName());
      td.setType(tproc.getTaskDefinition().getType());
      td.getSEDTaskTypeProperties().clear();
      if (!tproc.getTaskDefinition().getSEDTaskTypeProperties().isEmpty()) {
        td.getSEDTaskTypeProperties().addAll(tproc.getTaskDefinition().getSEDTaskTypeProperties());
      }
      return td;
    } catch (NamingException ex) {

      return null;
    }

  }

  /**
   *
   * @param key
   * @param name
   * @param mandatory
   * @return
   */
  public SEDTaskTypeProperty createTypeProperty(String key, String name, boolean mandatory) {
    SEDTaskTypeProperty sp = new SEDTaskTypeProperty();
    sp.setKey(key);
    sp.setDescription(name);
    sp.setMandatory(mandatory);
    return sp;
  }

  /**
     *
     */
  @Override
  public void updateEditable() {
    SEDPlugin ecj = getEditable();
    if (ecj != null) {
      mdbLookups.updateSEDPlugin(ecj);
    }
  }

  /**
   *
   * @return
   */
  @Override
  public List<SEDPlugin> getList() {
    long l = LOG.logStart();
    List<SEDPlugin> lst = mdbLookups.getSEDPlugin();
    LOG.logEnd(l, lst != null ? lst.size() : "null");
    return lst;
  }

  /**
   *
   * @return
   */
  public String getSelectedWebContext() {
    return selectedViewPlugin != null ? selectedViewPlugin.getWebContext() : "";
  }

  /**
   *
   * @param event
   */
  public void onSelectedViewPluginAction(ActionEvent event) {
    long l = LOG.logStart();
    if (event != null) {

      LOG.log("set selected plugin");
      SEDPlugin res = (SEDPlugin) event.getComponent().getAttributes().get("pluginItem");

      selectedViewPlugin = res;
      LOG.log("set selected plugin setted: " + selectedViewPlugin);
    } else {
      LOG.log("set selected plugin setted to null");
      selectedViewPlugin = null;
    }
    LOG.logEnd(l);

  }

}
