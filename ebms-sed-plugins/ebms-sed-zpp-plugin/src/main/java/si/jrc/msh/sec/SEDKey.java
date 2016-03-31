/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.sec;

import java.math.BigInteger;
import java.security.Key;

public class SEDKey  implements Key {
    BigInteger id;
    byte[] secretKey;
    String algorithm;
    String format;

    public SEDKey(BigInteger id, byte[] secretKey, String algorithm, String format) {
        this.id = id;
        this.secretKey = secretKey;
        this.algorithm = algorithm;
        this.format = format;
    }

    
    
    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public byte[] getEncoded() {
        return secretKey;
    }

   
}
