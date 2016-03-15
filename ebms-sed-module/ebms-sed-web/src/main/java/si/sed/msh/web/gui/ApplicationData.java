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

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.user.SEDUser;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.utils.DBSettings;

/**
 *
 * @author Jože Rihtaršič
 */
@ViewScoped
@ManagedBean(name = "ApplicationData")
public class ApplicationData {

    @EJB
    private DBSettings mdbSettings;

    public String getHomeFolder() {
        return System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR);
    }

    public String getPModeFileName() {
        return mdbSettings.getPModeFileName();
    }

    public String getSecurityFileName() {
        return SEDSystemProperties.SYS_PROP_CERT_DEF;
    }

    public String getKeyPasswordFilename() {
        return SEDSystemProperties.SYS_KEY_PASSWD_DEF;
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
    
    public List<SEDBox> getSEDBoxes(){
        return mdbSettings.getSEDBoxes();
    }
     public List<SEDUser> getSEDUsers(){
        return mdbSettings.getSEDUsers();
    }

}
