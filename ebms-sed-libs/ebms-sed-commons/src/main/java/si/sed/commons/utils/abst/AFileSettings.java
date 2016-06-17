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
package si.sed.commons.utils.abst;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
abstract public class AFileSettings extends ASettings {

    /**
     *
     */
    public AFileSettings() {
        init();
    }

    /**
     *
     */
    public abstract void createIniFile();

    /**
     *
     * @return
     */
    public abstract File getConfigFile();

    /**
     *
     */
    @Override
    final protected void init() {
        long l = mlog.logStart();
        File fPropFile = getConfigFile();
        if (mprpProperties != null && fPropFile.lastModified() == mlLastChagedTime) {
            return; // initialization is done..
        }

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

    /**
     *
     * @return
     */
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

    /**
     *
     * @param key
     */
    @Override
    protected void removeProperty(String key) {
        storeProperties();
    }

    /**
     *
     * @param key
     * @param value
     * @param group
     */
    @Override
    protected void replaceProperty(String key, String value, String group) {
        storeProperties();
    }

    /**
     *
     * @param strProperties
     * @throws IOException
     */
    public void setPropertiesFromString(String strProperties) throws IOException {
        mprpProperties.clear();
        mprpProperties.load(new ByteArrayInputStream(strProperties.getBytes()));
        // initialize def values if key not exists!
        createIniFile();
        storeProperties();
    }

    /**
     *
     */
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

    /**
     *
     * @param key
     * @param value
     * @param group
     */
    @Override
    protected void storeProperty(String key, String value, String group) {
        storeProperties();
    }
}
