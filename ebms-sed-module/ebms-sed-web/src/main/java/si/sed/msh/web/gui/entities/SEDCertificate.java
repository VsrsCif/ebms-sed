/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui.entities;

import java.security.cert.X509Certificate;
import java.util.Date;

/**
 *
 * @author sluzba
 */
public class SEDCertificate {
    String alias;
    X509Certificate x509Certificate;

    public SEDCertificate(String alias, X509Certificate x509Certificate) {
        this.alias = alias;
        this.x509Certificate = x509Certificate;
    }

    
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate;
    }
    
    
    
    
            
}
