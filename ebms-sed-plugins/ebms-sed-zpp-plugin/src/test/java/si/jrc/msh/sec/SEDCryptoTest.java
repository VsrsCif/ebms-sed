/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.sec;

import si.jrc.msh.sec.SEDCrypto;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import org.junit.Test;
import static org.junit.Assert.*;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.utils.sec.CertificateUtils;

/**
 *
 * @author sluzba
 */
public class SEDCryptoTest {

    private static final String TEST_DATA = "This is a SECRET NOTE!";
    private File mfSecretFile;
    private static final String KEYSTORE_TYPE = "JKS";
    private static final String KEYSTORE_PASSWORD = "test1234";
    private static final String KEYSTORE = "/certs/msh.e-box-b-keystore.jks";
    private static final String TRUSTSTORE_TYPE = "JKS";
    private static final String TRUSTSORE_PASSWORD = "test1234";
    private static final String TRUSTSORE = "/certs/msh.e-box-a-truststore.jks";
    private static final String SIGN_KEY_ALIAS = "msh.e-box-b.si";

    public SEDCryptoTest() {
        try {
            mfSecretFile = File.createTempFile("secret_test", ".dat");
            mfSecretFile.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(mfSecretFile)) {
                fos.write(TEST_DATA.getBytes("UTF-8"));
            }
        } catch (IOException ex) {
            mfSecretFile = null;
            Logger.getLogger(SEDCryptoTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        // set store key password parameters
        System.getProperties().setProperty(SEDSystemProperties.SYS_PROP_HOME_DIR, "src/test/resources/certs");
        System.getProperties().setProperty(SEDSystemProperties.SYS_KEY_PASSWD_DEF, "key-passwords.properties");

    }

    /**
     * Test of encrypt and decrypt file with class SEDCrypto.
     */
    @Test
    public void testAESEncryptDecryptFile() throws IOException, SEDSecurityException {
        assertNotNull("Initialize error while creating temp test file", mfSecretFile);
        SEDCrypto instance = new SEDCrypto();
        for (SEDCrypto.SymEncAlgorithms alg : SEDCrypto.SymEncAlgorithms.values()) {
            // create test files
            File fEnc = File.createTempFile("secret_test", ".enc");
            File fDec = File.createTempFile("secret_test", ".dec");
            fEnc.deleteOnExit();
            fDec.deleteOnExit();
            // generate key
            SecretKey skey = instance.getKey(alg);
            // encrypt file
            instance.encryptFile(mfSecretFile, fEnc, skey);
            instance.decryptFile(fEnc, fDec, skey);
            String result = new String(Files.readAllBytes(fDec.toPath()), "UTF-8");
            assertEquals(TEST_DATA, result);
        }
    }

    /**
     * Test of encrypt and decrypt file with class SEDCrypto.
     * @throws java.io.IOException
     * @throws si.sed.commons.exception.SEDSecurityException
     */
    @Test
    public void testEncryptAndDecryptKey() throws IOException, SEDSecurityException {
        assertNotNull("Initialize error while creating temp test file", mfSecretFile);
        SEDCrypto.SymEncAlgorithms alg = SEDCrypto.SymEncAlgorithms.AES128_CBC;

        // create test files
        File fEnc = File.createTempFile("secret_test", ".enc");
        File fDec = File.createTempFile("secret_test", ".dec");
        fEnc.deleteOnExit();
        fDec.deleteOnExit();

        SEDCrypto instance = new SEDCrypto();
        CertificateUtils cu = CertificateUtils.getInstance();

        // generate key
        SecretKey skey = instance.getKey(alg);
        // encrypt file
        instance.encryptFile(mfSecretFile, fEnc, skey);
        //encrypt key
        // sign key cert
        X509Certificate ca = cu.getTrustedCertForAlias(SIGN_KEY_ALIAS);
        assertNotNull("Initialize error: cert with alias: '" + SIGN_KEY_ALIAS + "' not found in trustore: '" + TRUSTSORE + "'!", ca);
        // enc key
        String encKey = instance.encryptKeyWithReceiverPublicKey(skey, ca, "receiver@test.sign.com", "key-id");
        assertNotNull("Encrypting key not succeded!", encKey);

        // Decrypting key
        KeyStore.PrivateKeyEntry ke = cu.getPrivateKeyEntryForAlias(SIGN_KEY_ALIAS);
        Key decKey = instance.decryptKey(encKey, ke.getPrivateKey(), alg);
        assertNotNull("Decrypting key not succeded!", decKey);

        // decrypt file
        instance.decryptFile(fEnc, fDec, decKey);
        String result = new String(Files.readAllBytes(fDec.toPath()), "UTF-8");
        // test data
        assertEquals(TEST_DATA, result);

    }

}
