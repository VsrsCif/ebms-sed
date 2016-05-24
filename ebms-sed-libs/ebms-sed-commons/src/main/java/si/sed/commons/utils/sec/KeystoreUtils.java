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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cert.SEDCertificate;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.Utils;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class KeystoreUtils {

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

    protected final SEDLogger mlog = new SEDLogger(KeystoreUtils.class);

    public static KeyStore getKeystore(SEDCertStore sc) throws SEDSecurityException {
        KeyStore keyStore = null;
        try (FileInputStream fis = new FileInputStream(Utils.replaceProperties(sc.getFilePath()))) {
            keyStore = getKeystore(fis, sc.getType(), sc.getPassword().toCharArray());
        } catch (IOException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.ReadWriteFileException, ex, "Read keystore from stream!" + ex.getMessage());
        }
        return keyStore;
    }

    public static KeyStore getKeystore(InputStream isTrustStore, String trustStoreType, char[] password) throws SEDSecurityException {
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(trustStoreType);

            keyStore.load(isTrustStore, password);
        } catch (KeyStoreException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.KeyStoreException, ex, ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.NoSuchAlgorithm, ex, ex.getMessage());
        } catch (CertificateException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, ex, ex.getMessage());
        } catch (IOException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.ReadWriteFileException, ex, "Read keystore from stream!" + ex.getMessage());
        }
        return keyStore;
    }

    public KeyStore.PrivateKeyEntry getPrivateKeyEntryForAlias(KeyStore ks, String alias, String passwd) throws SEDSecurityException {

        if (alias == null) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, "x.509 cert not found in keystore");
        }

        KeyStore.PrivateKeyEntry rsaKey;
        try {
            rsaKey = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, new KeyStore.PasswordProtection(passwd.toCharArray()));

        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.KeyStoreException, ex, ex.getMessage());
        } catch (UnrecoverableEntryException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.KeyStoreException, ex, ex.getMessage());
        }
        return rsaKey;
    }

    public Key getPrivateKeyForAlias(KeyStore ks, String alias, String psswd) throws SEDSecurityException {

        if (alias == null) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, "x.509 cert not found in keystore");
        }

        Key rsaKey;
        try {
            rsaKey = ks.getKey(alias, psswd.toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.KeyStoreException, ex, ex.getMessage());
        }
        return rsaKey;
    }

    public Key getPrivateKeyForX509Cert(List<SEDCertStore> lst, X509Certificate cert) throws SEDSecurityException {

        // find alias
        String alias = null;
        Key k = null;
        for (SEDCertStore cs : lst) {
            KeyStore ks = getKeystore(cs);
            // get alias for private key
            alias = getPrivateKeyAliasForX509Cert(ks, cert);
            if (alias != null) {
                // get key password 
                for (SEDCertificate c : cs.getSEDCertificates()) {
                    if (c.getAlias().equals(alias)) {
                        k = getPrivateKeyForAlias(ks, alias, c.getKeyPassword());
                        if (k != null) {
                            break;
                        }
                    }
                }
            }

        }
        return k;
    }

    public String getPrivateKeyAliasForX509Cert(KeyStore ks, X509Certificate cert) throws SEDSecurityException {

        // find alias
        String alias = null;
        Enumeration<String> e;
        try {
            e = ks.aliases();
            while (e.hasMoreElements()) {
                String as = e.nextElement();
                X509Certificate rsaCert = (X509Certificate) ks.getCertificate(as);
                if (cert.equals(rsaCert) && ks.isKeyEntry(as)) {
                    alias = as;
                    break;
                }
            }

        } catch (KeyStoreException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, ex, ex.getMessage());
        }
        return alias;

    }

    public Key getPrivateKeyForX509Cert(KeyStore ks, X509Certificate cert, String psswd) throws SEDSecurityException {

        // find alias
        String alias = null;
        Enumeration<String> e;
        try {
            e = ks.aliases();
            while (e.hasMoreElements()) {
                String as = e.nextElement();
                X509Certificate rsaCert = (X509Certificate) ks.getCertificate(as);
                if (cert.equals(rsaCert)) {
                    alias = as;
                    break;
                }
            }

        } catch (KeyStoreException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, ex, ex.getMessage());
        }
        return getPrivateKeyForAlias(ks, alias, psswd);

    }

    public X509Certificate getTrustedCertForAlias(KeyStore ks, String alias) throws SEDSecurityException {
        X509Certificate cert = null;
        try {

            cert = (X509Certificate) ks.getCertificate(alias);

        } catch (KeyStoreException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, ex, "Exception occured when retrieving: '" + alias + "' cert!");
        }
        return cert;
    }

    public KeyStore openKeyStore(String filepath, String type, char[] password) throws SEDSecurityException {
        try (FileInputStream fis = new FileInputStream(Utils.replaceProperties(filepath))) {
            return getKeystore(fis, type, password);
        } catch (IOException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.ReadWriteFileException, ex, "Read keystore: '" + filepath + "!");
        }
    }

    public List<String> getKeyStoreAliases(KeyStore ks) throws SEDSecurityException {
        List<String> lst = new ArrayList<>();
        try {
            Enumeration<String> e = ks.aliases();
            while (e.hasMoreElements()) {
                String alias = e.nextElement();
                lst.add(alias);
            }
        } catch (KeyStoreException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.ReadWriteFileException, ex, "Read keystore aliased: !");
        }
        return lst;
    }

    public List<SEDCertificate> getKeyStoreSEDCertificates(KeyStore ks) throws SEDSecurityException {
        List<SEDCertificate> lst = new ArrayList<>();
        try {
            Enumeration<String> e = ks.aliases();
            while (e.hasMoreElements()) {
                SEDCertificate ec = new SEDCertificate();

                String alias = e.nextElement();
                Certificate c = ks.getCertificate(alias);
                ec.setKeyEntry(ks.isKeyEntry(alias));
                ec.setAlias(alias);

                ec.setType(c.getType());
                if (c instanceof X509Certificate) {
                    X509Certificate xc = (X509Certificate) c;
                    ec.setValidFrom(xc.getNotBefore());
                    ec.setValidTo(xc.getNotAfter());
                    ec.setIssuerDN(xc.getIssuerDN().getName());
                    ec.setSubjectDN(xc.getSubjectDN().getName());
                    ec.setSerialNumber(xc.getSerialNumber());

                }

                lst.add(ec);
            }
        } catch (KeyStoreException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.ReadWriteFileException, ex, "Read keystore aliased: !");
        }
        return lst;
    }

    public static Properties getSignProperties(String alias, SEDCertStore cs) {
        Properties signProperties = new Properties();
        signProperties.put(SEC_PROVIDER, SEC_PROIDER_MERLIN);
        signProperties.put(SEC_MERLIN_KEYSTORE_ALIAS, alias);
        signProperties.put(SEC_MERLIN_KEYSTORE_PASS, cs.getPassword());
        signProperties.put(SEC_MERLIN_KEYSTORE_FILE, Utils.replaceProperties(cs.getFilePath()));
        signProperties.put(SEC_MERLIN_KEYSTORE_TYPE, cs.getType());
        return signProperties;
    }

    public static Properties getVerifySignProperties(String alias, SEDCertStore cs) {
        Properties signVerProperties = new Properties();
        signVerProperties.put(SEC_PROVIDER, SEC_PROIDER_MERLIN);
        signVerProperties.put(SEC_MERLIN_KEYSTORE_ALIAS, alias);
        signVerProperties.put(SEC_MERLIN_KEYSTORE_PASS, cs.getPassword());
        signVerProperties.put(SEC_MERLIN_KEYSTORE_FILE, Utils.replaceProperties(cs.getFilePath()));
        signVerProperties.put(SEC_MERLIN_KEYSTORE_TYPE, cs.getType());
        return signVerProperties;
    }

    public KeyStore.PrivateKeyEntry getPrivateKeyEntryForAlias(String alias, SEDCertStore cs) throws SEDSecurityException {

        if (alias == null) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, "x.509 cert not found in keystore");
        }

        KeyStore.PrivateKeyEntry rsaKey = null;
        try {
            for (SEDCertificate c : cs.getSEDCertificates()) {
                if (c.isKeyEntry() && c.getAlias().equalsIgnoreCase(alias)) {
                    rsaKey = (KeyStore.PrivateKeyEntry) getKeystore(cs).getEntry(alias, new KeyStore.PasswordProtection(c.getKeyPassword().toCharArray()));
                    break;
                }
            }

        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.KeyStoreException, ex, ex.getMessage());
        } catch (UnrecoverableEntryException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.KeyStoreException, ex, ex.getMessage());
        }
        return rsaKey;
    }

}
