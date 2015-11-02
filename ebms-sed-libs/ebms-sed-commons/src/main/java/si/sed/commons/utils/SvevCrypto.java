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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import org.w3._2000._09.xmldsig_.KeyInfo;
import org.w3._2001._04.xmlenc_.CipherData;
import org.w3._2001._04.xmlenc_.EncryptedData;
import org.w3._2001._04.xmlenc_.EncryptionMethodType;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class SvevCrypto {

    public enum SymEncAlgorithms {

        AES128_CBC("http://www.w3.org/2001/04/xmlenc#aes128-cbc", "AES/CBC/PKCS5Padding", 128),
        AES192_CBC("http://www.w3.org/2001/04/xmlenc#aes192-cbc", "AES/CBC/PKCS5Padding", 192),
        AES256_CBC("http://www.w3.org/2001/04/xmlenc#aes256-cbc", "AES/CBC/PKCS5Padding", 256),
        AES_ECB_NP("http://xmlns.webpki.org/keygen2/1.0#algorithm.aes.ecb.nopad", "AES/ECB/NoPadding", -1),
        AES_ECB_P5("http://xmlns.webpki.org/keygen2/1.0#algorithm.aes.ecb.pkcs5", "AES/ECB/PKCS5Padding", -1),
        AES_CBC_NP("internal:AES/CBC/NoPadding", "AES/CBC/NoPadding", -1),
        AES_CBC_P5("http://xmlns.webpki.org/keygen2/1.0#algorithm.aes.cbc.pkcs5", "AES/CBC/PKCS5Padding", -1);

        private final String mstrW3_uri;
        private final String mstrJce_name;
        private final int miKey_len;

        private SymEncAlgorithms(String uri, String name, int iKeyLen) {
            this.mstrW3_uri = uri;
            this.mstrJce_name = name;
            this.miKey_len = iKeyLen;
        }

        public String getJCEName() {
            return mstrJce_name;
        }

        public String getURI() {
            return mstrW3_uri;
        }

        public int getKeyLength() {
            return miKey_len;
        }

    }

    public SvevCrypto() {
    }

    public SecretKey getKey(int iKeyLen) throws NoSuchAlgorithmException {

        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(iKeyLen); 
        // Generate the secret key specs.
        return kgen.generateKey();
    }
    
    public EncryptedData encryptData(byte[] buff, SecretKey skey, String mimeType, String encoding, SymEncAlgorithms alg) {
        EncryptedData enc = null;
        try {
            
            
            int binLen =buff.length;
            // encrypt data
            Cipher cipher = Cipher.getInstance(alg.getJCEName());
            cipher.init(Cipher.ENCRYPT_MODE, skey);
            byte[] cipherText = new byte[cipher.getOutputSize(binLen)];
            int ctLength = cipher.update(buff, 0, binLen, cipherText, 0);
            ctLength += cipher.doFinal(cipherText, ctLength);
            
            // strore key 
            enc = new EncryptedData();
            enc.setMimeType(mimeType);
            enc.setEncoding(encoding);
            enc.setEncryptionMethod(new EncryptionMethodType());
            enc.getEncryptionMethod().setAlgorithm(alg.getURI());
            enc.setKeyInfo(new KeyInfo());

            enc.setCipherData(new CipherData());
            enc.getCipherData().setCipherValue(cipherText);
            
            
            
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | ShortBufferException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(SvevCrypto.class.getName()).log(Level.SEVERE, null, ex);
        }
        return enc;
    
    }

}
