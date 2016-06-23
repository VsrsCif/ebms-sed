/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils.abst;

import java.io.File;
import static java.io.File.separator;
import static java.lang.System.getProperties;
import static java.lang.System.getProperty;
import static java.util.Collections.enumeration;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author sluzba
 */
public abstract class ASettings {

    /**
     *
     * @return
     */
    public static Properties newProperties() {
        return new Properties() {
            @Override
            public synchronized Enumeration<Object> keys() {
                return enumeration(new TreeSet<Object>(super.keySet()));
            }
        };
    }

    /**
     *
     */
    protected long mlLastChagedTime = 0;

    /**
     *
     */
    protected SEDLogger mlog = new SEDLogger(AFileSettings.class);

    /**
     *
     */
    final protected Properties mprpProperties = newProperties();

    /**
     *
     */
    public ASettings() {

    }

    /**
     *
     * @param strKey
     * @return
     */
    public String getData(String strKey) {
        String strVal = null;
        init();
        if (mprpProperties != null) {
            strVal = mprpProperties.getProperty(strKey);
        }
        return strVal;
    }

    /**
     *
     * @param strKey
     * @param defVal
     * @return
     */
    public String getData(String strKey, String defVal) {
        // check if system property exists
        if (getProperties().containsKey(strKey)) {
            return getProperty(strKey);
        }
        init();
        // check if properties
        if (mprpProperties != null) {
            return mprpProperties.getProperty(strKey, defVal);
        }
        return defVal;
    }

    /**
     *
     * @param strPropName
     * @param strDefProfValue
     * @param strFileName
     * @return
     */
    public File getFile(String strPropName, String strDefProfValue,
            String strFileName) {
        File f = new File(getProperty(strPropName, getData(strPropName,
                strDefProfValue)));
        if (!f.exists()) {
            f.mkdirs();
        }
        return new File((f.getAbsolutePath().endsWith(separator) ? f.getPath() :
                f.getPath() + separator) + strFileName);
    }

    /**
     *
     * @param prop
     * @param defVal
     * @return
     */
    public File getFolder(String prop, String defVal) {
        File f = new File(getProperty(prop, getData(prop, defVal)));
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }

    /**
     *
     */
    protected abstract void init();

    /**
     *
     * @param key
     * @param value
     */
    public void initData(String key, String value) {
        if (!mprpProperties.containsKey(key)) {
            mprpProperties.setProperty(key, value);
        }
    }

    /**
     *
     */
    public abstract void initialize();

    /**
     *
     * @param key
     */
    protected abstract void removeProperty(String key);

    /**
     *
     * @param key
     * @param value
     * @param group
     */
    protected abstract void replaceProperty(String key, String value,
            String group);

    /**
     *
     * @param key
     * @param value
     */
    public void setData(String key, String value) {
        setData(key, value, null);
    }

    /**
     *
     * @param key
     * @param value
     * @param group
     */
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
            } else if (mprpProperties.get(strKey) != null ||
                    !mprpProperties.get(strKey).equals(strValue)) {
                mprpProperties.setProperty(strKey, strValue);
                replaceProperty(strKey, strValue, group);
            }
        } else if (strValue != null) {
            mprpProperties.setProperty(strKey, strValue);
            storeProperty(strKey, strValue, group);
        }
    }

    /**
     *
     * @param key
     * @param value
     * @param group
     */
    protected abstract void storeProperty(String key, String value, String group);

    /**
     *
     * @param f
     */
    public void testFolder(File f) {
        if (!f.exists()) {
            f.mkdirs();
        }
    }

}
