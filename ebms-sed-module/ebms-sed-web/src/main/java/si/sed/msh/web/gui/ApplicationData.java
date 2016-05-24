/*
* Copyright 2016, Supreme Court Republic of Slovenia 
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
package si.sed.msh.web.gui;

import si.sed.msh.web.abst.AbstractJSFView;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.application.ViewHandler;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.xml.ws.WebServiceContext;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.interfaces.DBSettingsInterface;
import si.sed.commons.interfaces.SEDLookupsInterface;


/**
 *
 * @author Jože Rihtaršič
 */
@ApplicationScoped
@ManagedBean(name = "ApplicationData")
public class ApplicationData extends AbstractJSFView {

    @Resource
    WebServiceContext context;

    @EJB (mappedName=SEDJNDI.JNDI_DBSETTINGS)
    private DBSettingsInterface mdbSettings;
    
    @EJB (mappedName=SEDJNDI.JNDI_SEDLOOKUPS)
    private SEDLookupsInterface msedLookups;
    


    public String getHomeFolder() {
        return System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR);
    }

    public String getPModeFileName() {
        return mdbSettings.getPModeFileName();
    }

    public String getSecurityFileName() {
        return SEDSystemProperties.SYS_PROP_CERT_DEF;
    }


    public String getPluginsFolder() {
        return SEDSystemProperties.SYS_PROP_FOLDER_PLUGINS_DEF;

    }

    public String getStorageFolder() {
        return SEDSystemProperties.SYS_PROP_FOLDER_STORAGE_DEF;

    }

    public List<String> getPlugins() {
        List<String> plLSt = new ArrayList<>();
        File fldPlugins = new File(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator + SEDSystemProperties.SYS_PROP_FOLDER_PLUGINS_DEF);
        if (fldPlugins.exists() && fldPlugins.isDirectory()) {
            for (File f : fldPlugins.listFiles((File dir, String name) -> name.toLowerCase().endsWith(".jar"))) {
                plLSt.add(f.getName());
            }
        }
        return plLSt;
    }

    public List<String> getSystemPropertyKeys() {
        Set<String> s = System.getProperties().stringPropertyNames();
        List<String> lst = new ArrayList<>(s);
        Collections.sort(lst);
        return lst;

    }

    public String getSystemPropertyValue(String strVal) {
        return System.getProperty(strVal);

    }

    public List<String> getSEDPropertyKeys() {

        Set<String> s = mdbSettings.getProperties().stringPropertyNames();
        List<String> lst = new ArrayList<>(s);
        Collections.sort(lst);
        return lst;

    }

    public String getSEDPropertyValue(String strVal) {
        return mdbSettings.getProperties().getProperty(strVal);

    }

    
      public String getDomain() {        
        return "@" + mdbSettings.getDomain();
    }
 
     
    public void onEdit(RowEditEvent event) {  
        //FacesMessage msg = new FacesMessage("Item Edited",((OrderBean) event.getObject()).getItem());  
        //FacesContext.getCurrentInstance().addMessage(null, msg);  
    }  
       
    public void onCancel(RowEditEvent event) {  
        //FacesMessage msg = new FacesMessage("Item Cancelled");   
        //FacesContext.getCurrentInstance().addMessage(null, msg); 
        //orderList.remove((OrderBean) event.getObject());
    }  
    
   
  

    public void refreshMainPanel() {
        FacesContext facesContext = facesContext();
        String refreshpage = "MainPanel";
        ViewHandler viewHandler = facesContext.getApplication().getViewHandler();
        UIViewRoot viewroot = viewHandler.createView(facesContext, refreshpage);
        viewroot.setViewId(refreshpage);
        facesContext.setViewRoot(viewroot);
    }

    public String getBuildVersion() {
        String strBuildVer = "";
        Manifest p;
        File manifestFile = null;
        String home = FacesContext.getCurrentInstance().getExternalContext().getRealPath("/");
        manifestFile = new File(home, "META-INF/MANIFEST.MF");
        try (FileInputStream fis = new FileInputStream(manifestFile)) {
            p = new Manifest();
            p.read(fis);
            Attributes a = p.getMainAttributes();
            strBuildVer = a.getValue("Implementation-Build");
        } catch (IOException ex) {

        }
        return strBuildVer;
    }
    
    public void exportLookupsWithPasswords(){        
        msedLookups.exportLookups(new File(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR)), true);
    }
    public void exportLookupsWithNoPasswords(){        
        msedLookups.exportLookups(new File(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR)), false);
    }
}
