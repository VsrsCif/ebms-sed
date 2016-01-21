/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed;

import java.io.File;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.utils.ASettings;

/**
 *
 * @author sluzba
 */
public class StandaloneSettings extends ASettings {

    private static final String S_PROP_PORT = "sed.port";
    private static final String S_PROP_HOME = "sed.home";
    
    private static final String S_PROP_PORT_DEF = "8080";  
    private static final String S_PROP_HOME_DEF = "sed-home";

   

    protected static final String PMODE_FILE = "pmode-conf.xml";
    protected static final String KEY_PASSWD_FILE = "key-passwords.properties";
    protected static final String SEC_CONF_FILE = "security-conf.properties";
    protected static final String LOG_CONF_FILE = "sed-log4j.properties";
    public static String S_PROPERTY_FILE = "config.xml";

    private static StandaloneSettings S_INSTANCE = null;

    private StandaloneSettings() {
        
    }

    public static StandaloneSettings getInstance() {
        return S_INSTANCE == null ? S_INSTANCE = new StandaloneSettings() : S_INSTANCE;
    }

    @Override
    public File getConfigFile() {
        return new File(S_PROPERTY_FILE);

    }

    @Override
    public void initialize() {
        
        // set system properties
        System.setProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:comp/env/");
        System.setProperty(SEDSystemProperties.SYS_PROP_JNDI_JMS_PREFIX, "java:comp/env/");
        if (!System.getProperties().containsKey(SEDSystemProperties.SYS_PROP_HOME_DIR)) {
            System.setProperty(SEDSystemProperties.SYS_PROP_HOME_DIR, getData(S_PROP_HOME, S_PROP_HOME_DEF));
        }
        
        
        
    }

    @Override
    public void createIniFile() {
        boolean sf = false;

        if (getData(S_PROP_PORT) == null) {
            setData(S_PROP_PORT, S_PROP_PORT_DEF);
            sf = true;
        }

        if (getData(S_PROP_HOME) == null) {
            setData(S_PROP_HOME, S_PROP_HOME_DEF);
            sf = true;
        }

        if (sf) {
            storeProperties();
        }
    }

    public File getLogPropertiesFile() {
        return new File(getHome(), LOG_CONF_FILE);

    }

    public File getHome() {
        return getFolder(S_PROP_HOME, S_PROP_HOME_DEF);
    }
    
    public int getPort() {
        return Integer.parseInt(getData(S_PROP_PORT, S_PROP_PORT_DEF));
    }
}
