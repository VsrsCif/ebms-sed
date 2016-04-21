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
package si.sed.msh.web.admin;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.primefaces.model.DualListModel;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cert.SEDCertificate;
import org.sed.ebms.cron.SEDTaskType;
import si.sed.commons.SEDJNDI;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.sec.KeystoreUtils;
import si.sed.msh.web.abst.AbstractAdminJSFView;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "adminSEDKeyStores")
public class AdminSEDKeyStores extends AbstractAdminJSFView<SEDCertStore> {

    private static final SEDLogger LOG = new SEDLogger(AdminSEDKeyStores.class);

    KeystoreUtils mku = new KeystoreUtils();

    @EJB(mappedName = SEDJNDI.JNDI_SEDLOOKUPS)
    private SEDLookupsInterface mdbLookups;

    @Override
    public void createEditable() {
        SEDCertStore cs = new SEDCertStore();
        
        setNew(cs);

    }

    private DualListModel<SEDCertificate> msbCBDualList = new DualListModel<>();

    public DualListModel<SEDCertificate> getCurrentPickupDualSEDCertList() {
        long l = LOG.logStart();

        List<String> sbIDs = new ArrayList<>();
        if (getEditable() != null) {
            getEditable().getSEDCertificates().stream().forEach((sb) -> {
                sbIDs.add(sb.getAlias());
            });
        }
        List<SEDCertificate> src = getEditable().getSEDCertificates();
        List<SEDCertificate> trg = new ArrayList<>();

        try {
            KeyStore ks = mku.openKeyStore(getEditable().getFilePath(), getEditable().getType(), getEditable().getPassword().toCharArray());
            List<SEDCertificate> lstals = mku.getKeyStoreSEDCertificates(ks);
            for (SEDCertificate als: lstals) {
                if (!sbIDs.contains(als.getAlias())){
                    src.add(als);
                }
            }

        } catch (SEDSecurityException ex) {
            LOG.logWarn(l,getEditable().getFilePath(), ex);            
        }

        LOG.logEnd(l);
        return msbCBDualList = new DualListModel<>(src, trg);
    }

    public void setCurrentPickupDualSEDCertList(DualListModel<SEDCertificate> dl) {
        msbCBDualList = dl;
    }

    @Override
    public List<SEDCertStore> getList() {
        long l = LOG.logStart();
        List<SEDCertStore> lst = mdbLookups.getSEDCertStore();
        LOG.logEnd(l, lst != null ? lst.size() : "null");
        return lst;
    }

    public List<String> getTaskTypeList() {
        long l = LOG.logStart();
        List<String> rstLst = new ArrayList<>();
        List<SEDTaskType> lst = mdbLookups.getSEDTaskTypes();
        lst.stream().forEach((tsk) -> {
            rstLst.add(tsk.getType());
        });
        LOG.logEnd(l, lst != null ? lst.size() : "null");
        return rstLst;
    }

    @Override
    public void persistEditable() {
        SEDCertStore ecj = getEditable();
        if (ecj != null) {
            mdbLookups.addSEDCertStore(ecj);
            ecj.getSEDCertificates().clear();
             if (msbCBDualList.getTarget() != null && !msbCBDualList.getTarget().isEmpty()) {                 
                ecj.getSEDCertificates().addAll(msbCBDualList.getTarget());
            }
        }
    }

    @Override
    public void removeSelected() {
        if (getSelected() != null) {
            mdbLookups.removeEDCertStore(getSelected());
            setSelected(null);

        }
    }

    @Override
    public void updateEditable() {
        SEDCertStore ecj = getEditable();
        if (ecj != null) {
            mdbLookups.updateSEDCertStore(ecj);
        }
    }

}
