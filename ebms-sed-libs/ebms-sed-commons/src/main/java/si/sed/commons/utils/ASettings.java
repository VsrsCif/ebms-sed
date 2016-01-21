/*
* Copyright 2015, Supreme Court Republic of Slovenia 
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
package si.sed.commons.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;


/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
abstract public class ASettings {

    SEDLogger mlog = new SEDLogger(ASettings.class);

    protected Properties mprpProperties = null;
    private long mlLastChagedTime = 0;

    public ASettings() {
        init();
    }

    public abstract File getConfigFile();

    public abstract void initialize();

    public abstract void createIniFile();

    private void init() {
        long l = mlog.logStart();
        File fPropFile = getConfigFile();
        if (mprpProperties != null && fPropFile.lastModified() == mlLastChagedTime) {
            return; // initialization is done..
        }
        mprpProperties = mprpProperties == null ? newProperties() : mprpProperties;
        if (fPropFile.exists()) {
            try (final FileInputStream fis = new FileInputStream(fPropFile)) {
                mprpProperties.load(fis);
            } catch (IOException ex) {
                mlog.logError(l, "Error init file: '" + fPropFile.getAbsolutePath() + "'", ex);
            }
        }
        // initialize def values if key not exists!
        mlLastChagedTime = fPropFile.lastModified(); // prevent cycling (reinit in createIniFile)
        createIniFile();
        mlLastChagedTime = fPropFile.lastModified();
        initialize();
        // create folders
    }

    public Properties newProperties() {
        return new Properties() {
            @Override
            public synchronized Enumeration<Object> keys() {
                return Collections.enumeration(new TreeSet<Object>(super.keySet()));
            }
        };
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
        
        init();
        // check if system property exists 
        if (System.getProperties().containsKey(strKey)){
            return System.getProperty(strKey);
        }
        // check if properties 
        if (mprpProperties != null) {
            return  mprpProperties.getProperty(strKey, defVal);
        } 
        return defVal;
    }

    public void initData(String key, String value) {
        if (!mprpProperties.containsKey(key)) {
            mprpProperties.setProperty(key, value);
        }
    }

    public void setData(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        String strKey = key.trim();
        String strValue = value != null ? value.trim() : null;
        init();
        mprpProperties = mprpProperties != null ? mprpProperties : newProperties();

        if (mprpProperties.contains(strValue)) {
            if (strValue == null) {
                mprpProperties.remove(strValue);
                storeProperties();
            } else if (mprpProperties.get(strKey)==null ||  !mprpProperties.get(strKey).equals(strValue)) {
                mprpProperties.setProperty(strKey, strValue);
                storeProperties();
            }
        } else if (strValue != null) {
            mprpProperties.setProperty(strKey, strValue);
            storeProperties();
        }

    }

    public void setPropertiesFromString(String strProperties) throws IOException {
        mprpProperties = mprpProperties == null ? newProperties() : mprpProperties;
        mprpProperties.clear();
        mprpProperties.load(new ByteArrayInputStream(strProperties.getBytes()));
        // initialize def values if key not exists!
        createIniFile();
        storeProperties();
    }

    public void storeProperties() {
        long l = mlog.logStart();
        if (mprpProperties != null) {
            try (final FileOutputStream fos = new FileOutputStream(getConfigFile())) {
                mprpProperties.store(fos, "standalone properties");
            } catch (IOException ex) {
                mlog.logError(l, "Error saving priperties to file: '" + getConfigFile().getAbsolutePath() + "'", ex);
            }
        }
    }

    public void testFolder(File f) {
        if (!f.exists()) {
            f.mkdirs();
        }
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

    public String propertiesToString() {
        StringWriter sw = new StringWriter();
        if (mprpProperties != null) {

            try {
                mprpProperties.store(sw, null);
            } catch (IOException ex) {
                // ignore
            }
        }
        return sw.toString();
    }

}
