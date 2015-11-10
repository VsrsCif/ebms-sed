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
package si.sed.commons.utils.sec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.exception.SEDSecurityException;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class CertificateUtils {

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

    private static CertificateUtils instance = null;
    protected final SEDLogger mlog = new SEDLogger(CertificateUtils.class);
    Properties mSecProp = new Properties();
    protected KeyStore mksKeyStore = null;
    protected KeyStore mtsTrustStore = null;

    private CertificateUtils() {
        long l = mlog.logStart();
        // load properties
        String fileProperty = System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR, "") + File.separator + SEDSystemProperties.SYS_PROP_CERT_DEF;
        try (FileInputStream fis = new FileInputStream(fileProperty)) {
            mSecProp.load(fis);

            mSecProp.stringPropertyNames().stream().filter((key)
                    -> (mSecProp.getProperty(key) != null)).forEach((key)
                    -> {
                mSecProp.put(key, mSecProp.getProperty(key).trim());
            }
            );
        } catch (IOException ex) {
            mlog.logError(l, "Error reading property file: " + fileProperty, ex);
        }

    }

    public static CertificateUtils getInstance() {
        if (instance == null) {
            instance = new CertificateUtils();
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

    public X509Certificate getTrustedCertForAlias(String alias) throws SEDSecurityException {

        X509Certificate cert = null;
        KeyStore ks = getTrustStore();
        try {
            if (ks.isCertificateEntry(alias)) {
                cert = (X509Certificate) ks.getCertificate(alias);
            }
        } catch (KeyStoreException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, ex, "Exception occured when retrieving: '" + alias + "' cert!");
        }
        return cert;
    }

    public Key getPrivateKeyForX509Cert(X509Certificate cert) throws SEDSecurityException {

        // find alias
        String alias = null;
        Enumeration<String> e;
        try {
            e = getKeyStore().aliases();
            while (e.hasMoreElements()) {
                String as = e.nextElement();
                X509Certificate rsaCert = (X509Certificate) getKeyStore().getCertificate(as);                
                if (cert.equals(rsaCert)) {
                    alias = as;
                    break;
                }
            }
            
        } catch (KeyStoreException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, ex, ex.getMessage());
        }
        return getPrivateKeyForAlias(alias);
        
    }
    public Key getPrivateKeyForAlias(String alias) throws SEDSecurityException {


        if (alias == null) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, "x.509 cert not found in keystore");
        }

        String passwd = KeyPasswordManager.getInstance().getPasswordForAlias(alias);
        Key rsaKey;
        try {
            rsaKey = getKeyStore().getKey(alias, passwd.toCharArray());            
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.KeyStoreException, ex, ex.getMessage());
        }
        return rsaKey;
    }
    
    public KeyStore.PrivateKeyEntry getPrivateKeyEntryForAlias(String alias) throws SEDSecurityException {


        if (alias == null) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, "x.509 cert not found in keystore");
        }

        String passwd = KeyPasswordManager.getInstance().getPasswordForAlias(alias);
        KeyStore.PrivateKeyEntry rsaKey;
        try {
            rsaKey = (KeyStore.PrivateKeyEntry) getKeyStore().getEntry(alias, new KeyStore.PasswordProtection(passwd.toCharArray()));

        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.KeyStoreException, ex, ex.getMessage());
        } catch (UnrecoverableEntryException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.KeyStoreException, ex, ex.getMessage());
        }
        return rsaKey;
    }

    public KeyStore getKeyStore() throws SEDSecurityException {
        if (mksKeyStore == null) {
            try (FileInputStream fis = new FileInputStream(getKeyStoreFilepath())) {
                mksKeyStore = getKeystore(fis, getKeyStoreType(), getKeyStorePassword());
            } catch (IOException ex) {
                throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.ReadWriteFileException, ex, "Read keystore: '" + getKeyStoreFilepath() + "!");
            }
        }
        return mksKeyStore;
    }

    public KeyStore getTrustStore() throws SEDSecurityException {
        if (mtsTrustStore == null) {
            try (FileInputStream fis = new FileInputStream(getTrustStoreFilepath())) {
                mtsTrustStore = getKeystore(fis, getTrustStoreType(), getTrustStorePassword());
            } catch (IOException ex) {
                throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.ReadWriteFileException, ex, "Read keystore: '" + getTrustStoreFilepath() + "!");
            }
        }
        return mtsTrustStore;
    }

    public KeyStore getKeystore(InputStream isTrustStore, String trustStoreType, String trustStorePassword) throws SEDSecurityException {
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(trustStoreType);

            keyStore.load(isTrustStore, trustStorePassword.toCharArray());
        } catch (KeyStoreException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.KeyStoreException, ex, ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.NoSuchAlgorithm, ex, ex.getMessage());
        } catch (CertificateException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, ex, ex.getMessage());
        } catch (IOException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.ReadWriteFileException, ex, "Read keystore: '" + getKeyStoreFilepath() + "!" + ex.getMessage());
        }
        return keyStore;
    }

}
