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
import java.math.BigInteger;
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
import org.sed.ebms.cert.SEDCertificate;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.Utils;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class KeystoreUtils {

    protected final SEDLogger mlog = new SEDLogger(KeystoreUtils.class);

    public KeyStore getKeystore(InputStream isTrustStore, String trustStoreType, char[] password) throws SEDSecurityException {
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
            if (ks.isCertificateEntry(alias)) {
                cert = (X509Certificate) ks.getCertificate(alias);
            }
        } catch (KeyStoreException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CertificateException, ex, "Exception occured when retrieving: '" + alias + "' cert!");
        }
        return cert;
    }

    public KeyStore openKeyStore(String filepath, String type, char[] password) throws SEDSecurityException {
        try (FileInputStream fis = new FileInputStream(Utils.replaceProperties(filepath) )) {
            return getKeystore(fis, type, password);
        } catch (IOException ex) {
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.ReadWriteFileException, ex, "Read keystore: '" + filepath + "!");
        }
    }

    public List<String> getKeyStoreAliases(KeyStore ks) throws SEDSecurityException {
        List<String> lst = new ArrayList<>();
        try {
            Enumeration<String> e =  ks.aliases();            
            while (e.hasMoreElements()) {
                String alias =e.nextElement();
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
            Enumeration<String> e =  ks.aliases();            
            while (e.hasMoreElements()) {
                SEDCertificate ec = new SEDCertificate();
                
                String alias =e.nextElement();
                Certificate c = ks.getCertificate(alias);
                ec.setKeyEntry(ks.isKeyEntry(alias));
                ec.setAlias(alias);

                ec.setType(c.getType());
                if (c instanceof X509Certificate){
                    X509Certificate xc = (X509Certificate)c;
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

}
