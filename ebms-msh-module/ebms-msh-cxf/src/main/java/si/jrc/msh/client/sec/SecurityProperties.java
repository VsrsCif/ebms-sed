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
package si.jrc.msh.client.sec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class SecurityProperties {

    public static final String SEC_PROVIDER = "org.apache.ws.security.crypto.provider";
    public static final String SEC_PROIDER_MERLIN = "org.apache.wss4j.common.crypto.Merlin";
    public static final String SEC_MERLIN_KEYSTORE_FILE = "org.apache.ws.security.crypto.merlin.keystore.file";
    public static final String SEC_MERLIN_KEYSTORE_TYPE = "org.apache.ws.security.crypto.merlin.keystore.type";
    public static final String SEC_MERLIN_KEYSTORE_PASS = "org.apache.ws.security.crypto.merlin.keystore.password";
    public static final String SEC_MERLIN_KEYSTORE_ALIAS = "org.apache.ws.security.crypto.merlin.keystore.alias";

    public static final String SEC_MERLIN_TRUSTSTORE_FILE = "org.apache.ws.security.crypto.merlin.truststore.file";
    public static final String SEC_MERLIN_TRUSTSTORE_TYPE = "org.apache.ws.security.crypto.merlin.truststore.type";
    public static final String SEC_MERLIN_TRUSTSTORE_PASS = "org.apache.ws.security.crypto.merlin.truststore.password";
    public static final String SEC_MERLIN_TRUSTSTORE_ALIAS = "org.apache.ws.security.crypto.merlin.truststore.alias";

    private static final String SEC_PROP_TRUSTSTORE_FILE = "org.sed.trustore.file";
    private static final String SEC_PROP_TRUSTSTORE_TYPE = "org.sed.trustore.type";
    private static final String SEC_PROP_TRUSTSTORE_PASS = "org.sed.trustore.password";
    private static final String SEC_PROP_KEYSTORE_FILE = "org.sed.keystore.file";
    private static final String SEC_PROP_KEYSTORE_TYPE = "org.sed.keystore.type";
    private static final String SEC_PROP_KEYSTORE_PASS = "org.sed.keystore.password";

    private static SecurityProperties instance = null;
    protected final SEDLogger mlog = new SEDLogger(SecurityProperties.class);

    Properties mSecProp = new Properties();

    private SecurityProperties() {
        long l = mlog.logStart();
        // load properties
        String fileProperty = System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR, "") + File.separator + SEDSystemProperties.SYS_PROP_CERT_DEF;
        try (FileInputStream fis = new FileInputStream(fileProperty)) {
            mSecProp.load(fis);
        } catch (IOException ex) {
            mlog.logError(l, "Error reading property file: " + fileProperty, ex);
        }

    }

    public static SecurityProperties getInstance() {
        if (instance == null) {
            instance = new SecurityProperties();
        }
        return instance;
    }

    public String getTrustStoreFilepath() {
        return mSecProp.getProperty(SEC_PROP_TRUSTSTORE_FILE);
    }

    public String getTrustStoreType() {
        return mSecProp.getProperty(SEC_PROP_TRUSTSTORE_TYPE);
    }

    public String getTrustStorePassword() {
        return mSecProp.getProperty(SEC_PROP_TRUSTSTORE_PASS);
    }

    public String getKeyStoreFilepath() {
        return mSecProp.getProperty(SEC_PROP_KEYSTORE_FILE);
    }

    public String getKeyStoreType() {
        return mSecProp.getProperty(SEC_PROP_KEYSTORE_TYPE, "JKS");
    }

    public String getKeyStorePassword() {
        return mSecProp.getProperty(SEC_PROP_KEYSTORE_PASS);
    }

    public Properties getSignProperties(String alias) {
        Properties signProperties = new Properties();
        signProperties.put(SEC_PROVIDER, SEC_PROIDER_MERLIN);
        signProperties.put(SEC_MERLIN_KEYSTORE_ALIAS, alias);
        signProperties.put(SEC_MERLIN_KEYSTORE_PASS, getKeyStorePassword());
        signProperties.put(SEC_MERLIN_KEYSTORE_FILE, getKeyStoreFilepath());
        signProperties.put(SEC_MERLIN_KEYSTORE_TYPE, getKeyStoreType());
        return signProperties;
    }

    public Properties getVerifySignProperties(String alias) {
        Properties signVerProperties = new Properties();
        signVerProperties.put(SEC_PROVIDER, SEC_PROIDER_MERLIN);
        signVerProperties.put(SEC_MERLIN_KEYSTORE_ALIAS, alias);
        signVerProperties.put(SEC_MERLIN_KEYSTORE_PASS, getTrustStorePassword());
        signVerProperties.put(SEC_MERLIN_KEYSTORE_FILE, getTrustStoreFilepath());
        signVerProperties.put(SEC_MERLIN_KEYSTORE_TYPE, getTrustStoreType());
        return signVerProperties;
    }

}
