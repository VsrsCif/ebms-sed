/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.IntrospectionException;
import si.sed.commons.SEDSystemProperties;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class PluginManager {

    public static AbstractPluginInterceptor getInterceptor(String fileName, String className) {
        AbstractPluginInterceptor authorizedPlugin = null;
        try {
            File authorizedJarFile = new File(fileName);
            // PluginManager.class.getClassLoader().
            ClassLoader cl = addURLToSystemClassLoader(authorizedJarFile);
            authorizedPlugin = (AbstractPluginInterceptor) cl.loadClass(className).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IntrospectionException ex) {
            Logger.getLogger(PluginManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return authorizedPlugin;
    }

    public static URLClassLoader addURLToSystemClassLoader(File file) throws IntrospectionException {
        try {
            URL[] urls = {new URL("jar:" + file.toURI().toURL() + "!/")};
            URLClassLoader cl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
            return cl;
        } catch (IOException ex) {
            Logger.getLogger(PluginManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }
    
    public static List<String> getPluginNames()  {
        List<String> plLSt = new ArrayList<>();
        File fldPlugins = new File(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator + SEDSystemProperties.SYS_PROP_FOLDER_PLUGINS_DEF);
        if (fldPlugins.exists() && fldPlugins.isDirectory()){
            for (File f: fldPlugins.listFiles((File dir, String name) -> name.toLowerCase().endsWith(".jar"))){
                plLSt.add(f.getName());
            }
        }
        return plLSt;       
    }
}