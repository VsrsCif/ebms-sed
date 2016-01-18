/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui;

import java.security.Key;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.utils.sec.CertificateUtils;
import si.sed.msh.web.gui.entities.SEDCertificate;

@ViewScoped
@ManagedBean(name = "TrustStoreModel")
public class TrustStoreModel {

    CertificateUtils mce = CertificateUtils.getInstance();

    String mstrSecFile = "/sluzba/code/SVEV2.0/sed-home-A-1/security-conf.properties";

    Properties msecProp = null;

    public String getTrustStoreFilepath() {
        return mce.getTrustStoreFilepath();
    }

    public String getTrustStoreType() {
        return mce.getTrustStoreType();
    }

    public String getTrustStorePassword() {
        return mce.getTrustStorePassword();
    }

    public String getKeyStoreFilepath() {
        return mce.getKeyStoreFilepath();
    }

    public String getKeyStoreType() {
        return mce.getKeyStoreType();
    }

    public String getKeyStorePassword() {
        return mce.getKeyStorePassword();
    }

    public List<SEDCertificate> getTrustCertificates() {
        List<SEDCertificate> lstCrts = new ArrayList<>();

        Enumeration<String> e;
        try {
            e = mce.getTrustStore().aliases();
            while (e.hasMoreElements()) {
                String as = e.nextElement();
                X509Certificate rsaCert = (X509Certificate) mce.getTrustStore().getCertificate(as);
                lstCrts.add(new SEDCertificate(as, rsaCert));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return lstCrts;

    }

    public List<SEDCertificate> getKeyCertificates() {
        List<SEDCertificate> lstCrts = new ArrayList<>();

        Enumeration<String> e;
        try {
            e = mce.getKeyStore().aliases();
            while (e.hasMoreElements()) {
                String as = e.nextElement();
                X509Certificate rsaCert = (X509Certificate) mce.getKeyStore().getCertificate(as);
                lstCrts.add(new SEDCertificate(as, rsaCert));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return lstCrts;

    }

    // file 
    // list
}
