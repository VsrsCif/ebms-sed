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
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import si.sed.commons.SEDSystemProperties;

/**
 *
 * @author Jože Rihtaršič
 */
@ViewScoped
@ManagedBean(name = "ApplicationData")
public class ApplicationData {

    public String getHomeFolder() {
        return System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR);
    }

    public String getPModeFileName() {
        return System.getProperty(SEDSystemProperties.SYS_PROP_PMODE, SEDSystemProperties.SYS_PROP_PMODE_DEF);
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

}
