/*
* Copyright 2016, Supreme Court Republic of Slovenia 
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
package si.sed.msh.web.gui;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.sed.ebms.cert.SEDCertStore;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.DBCertStoresInterface;
import si.sed.commons.utils.sec.CertificateUtils;
import si.sed.msh.web.gui.entities.SEDCertificate;


/**
 *
 * @author Jože Rihtaršič
 */
@ViewScoped
@ManagedBean(name = "TrustStoreModel")
public class TrustStoreModel {
    
    @EJB (mappedName = SEDJNDI.JNDI_DBCERTSTORE)
    DBCertStoresInterface mCertStores; 
    
    SEDCertStore currentCertStore = null;

    CertificateUtils mce = CertificateUtils.getInstance();

    
    public List<SEDCertStore> getDBCertStores(){
        return mCertStores.getCertStores();
    }
    
    public SEDCertStore getCurrentCertStore(){
        return currentCertStore;
    }
    public void setCurrentCertStore(SEDCertStore certStore){
        currentCertStore = certStore;
    }
    
     public void onRowSelect(SelectEvent event) {
        setCurrentCertStore((SEDCertStore) event.getObject());
    }

    public void onRowUnselect(UnselectEvent event) {
        setCurrentCertStore(null);
    }
    

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
