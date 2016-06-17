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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import si.sed.commons.exception.HashException;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class HashUtils {

    /**
     *
     */
    public static String MessageDigest_MD5 = "MD5";

    MessageDigest mdMD5 = null;

    /**
     *
     * @param file
     * @return
     * @throws HashException
     */
    public String getMD5Hash(File file) throws HashException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return getMD5Hash(fis);
        } catch (IOException ex) {
            throw new HashException("Error reading file '" + file.getAbsolutePath() + "'.", ex);
        }
    }

    /**
     *
     * @param filePath
     * @return
     * @throws HashException
     */
    public String getMD5Hash(String filePath) throws HashException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return getMD5Hash(fis);
        } catch (IOException ex) {
            throw new HashException("Error reading file '" + filePath + "'.", ex);
        }
    }

    /**
     *
     * @param is
     * @return
     * @throws HashException
     */
    public String getMD5Hash(InputStream is) throws HashException {
        String strHash = null;
        try {
            MessageDigest md5 = getMD5MessageDigest();
            md5.reset();
            byte[] buffer = new byte[1024];
            int len = is.read(buffer);
            while (len != -1) {
                md5.update(buffer, 0, len); // calculate MD5Digest
                len = is.read(buffer);
            }
            byte[] hash = md5.digest();

            //converting byte array to Hexadecimal String
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            strHash = sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new HashException("System error. Check deployment, missing MD5 (MessageDigest) algoritem.", ex);
        } catch (IOException ex) {
            throw new HashException("Error reading inputstream", ex);
        }
        return strHash;
    }

    private MessageDigest getMD5MessageDigest() throws NoSuchAlgorithmException {
        return mdMD5 == null ? (mdMD5 = MessageDigest.getInstance(MessageDigest_MD5)) : mdMD5;
    }

}
