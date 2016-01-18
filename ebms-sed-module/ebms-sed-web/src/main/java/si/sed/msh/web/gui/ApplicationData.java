/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import si.sed.commons.SEDSystemProperties;


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
