/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils.abst;

import java.io.File;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author sluzba
 */
public abstract class ASettings {

    public static Properties newProperties() {
        return new Properties() {
            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<Object>(super.keySet()));
            }
        };
    }
    protected long mlLastChagedTime = 0;

    protected SEDLogger mlog = new SEDLogger(AFileSettings.class);
    final protected Properties mprpProperties = newProperties();

    public ASettings() {

    }

    public String getData(String strKey) {
        String strVal = null;
        init();
        if (mprpProperties != null) {
            strVal = mprpProperties.getProperty(strKey);
        }
        return strVal;
    }

    public String getData(String strKey, String defVal) {
        // check if system property exists
        if (System.getProperties().containsKey(strKey)) {
            return System.getProperty(strKey);
        }
        init();
        // check if properties
        if (mprpProperties != null) {
            return mprpProperties.getProperty(strKey, defVal);
        }
        return defVal;
    }

    public File getFile(String strPropName, String strDefProfValue, String strFileName) {
        File f = new File(System.getProperty(strPropName, getData(strPropName, strDefProfValue)));
        if (!f.exists()) {
            f.mkdirs();
        }
        return new File((f.getAbsolutePath().endsWith(File.separator) ? f.getPath() : f.getPath() + File.separator) + strFileName);
    }

    public File getFolder(String prop, String defVal) {
        File f = new File(System.getProperty(prop, getData(prop, defVal)));
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    protected abstract void init();

    public void initData(String key, String value) {
        if (!mprpProperties.containsKey(key)) {
            mprpProperties.setProperty(key, value);
        }
    }

    public abstract void initialize();

    protected abstract void removeProperty(String key);

    protected abstract void replaceProperty(String key, String value, String group);

    public void setData(String key, String value) {
        setData(key, value, null);
    }

    public void setData(String key, String value, String group) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        String strKey = key.trim();
        String strValue = value != null ? value.trim() : null;

        init();

        if (mprpProperties.containsKey(key)) {
            if (strValue == null) {
                mprpProperties.remove(strValue);
                removeProperty(strValue);
            } else if (mprpProperties.get(strKey) != null || !mprpProperties.get(strKey).equals(strValue)) {
                mprpProperties.setProperty(strKey, strValue);
                replaceProperty(strKey, strValue, group);
            }
        } else if (strValue != null) {
            mprpProperties.setProperty(strKey, strValue);
            storeProperty(strKey, strValue, group);
        }
    }

    protected abstract void storeProperty(String key, String value, String group);

    public void testFolder(File f) {
        if (!f.exists()) {
            f.mkdirs();
        }
    }

}
