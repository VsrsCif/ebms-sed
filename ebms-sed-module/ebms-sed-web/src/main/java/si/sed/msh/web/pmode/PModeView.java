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
package si.sed.msh.web.pmode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.xml.bind.JAXBException;
import org.msh.svev.pmode.Leg;
import org.msh.svev.pmode.PMode;
import si.sed.commons.exception.PModeException;
import si.sed.commons.utils.PModeManager;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.xml.XMLUtils;
import si.sed.msh.web.abst.AbstractAdminJSFView;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "pModeView")
public class PModeView extends AbstractAdminJSFView<PMode> {

    /**
     *
     */
    public static SEDLogger LOG = new SEDLogger(PModeView.class);

    PModeManager pm = new PModeManager();
    String curre = null;

    private Map<String, String> mLookupMep;
    private Map<String, String> mLookupMepBinding;

    /**
     *
     */
    @PostConstruct
    public void init() {
        mLookupMep = new HashMap<>();
        mLookupMep.put("One-Way MEP", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay");
        mLookupMep.put("Two-Way MEP", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/twoWay");
        mLookupMepBinding = new HashMap<>();
        mLookupMepBinding.put("Push", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/push");
        mLookupMepBinding.put("Pull", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pull");
        mLookupMepBinding.put("Sync", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sync");
        mLookupMepBinding.put("PushAndPush", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPush");
        mLookupMepBinding.put("PushAndPull", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPull");
        mLookupMepBinding.put("PullAndPush", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pullAndPush");
    }

    /**
     *
     */
    @Override
    public void createEditable() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     */
    @Override
    public void removeSelected() {
        if (getSelected() != null) {
            pm.removePMode(getSelected());
        }
    }

    /**
     *
     */
    @Override
    public void persistEditable() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     */
    @Override
    public void updateEditable() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @return
     */
    @Override
    public List<PMode> getList() {
        long l = LOG.logStart();

        try {
            return pm.getPModeList();
        } catch (PModeException ex) {
            LOG.logError(l, null, ex);
        }
        return null;
    }

    /**
     *
     * @return
     */
    public String getCurrentPModeAsString() {
        long l = LOG.logStart();
        String pmrs = "";
        PMode pmed = getEditable();
        if (pmed != null) {
            try {
                pmrs = XMLUtils.serializeToString(pmed);
            } catch (JAXBException ex) {
                LOG.logError(l, null, ex);
            }
        }
        return pmrs;
    }

    /**
     *
     * @param strPMode
     */
    public void setCurrentPModeAsString(String strPMode) {
        long l = LOG.logStart();

        PMode pmed = getEditable();
        if (pmed != null) {
            try {
                PMode pmdNew = (PMode) XMLUtils.deserialize(strPMode, PMode.class);
                setEditable(pmdNew);
                pm.replace(pmdNew, pmed.getId());
            } catch (JAXBException ex) {
                LOG.logError(l, null, ex);
            }
        }

    }

    /**
     *
     * @return
     */
    public Leg getCurrentPModeForeChannel() {

        return null;
    }

    /**
     *
     * @return
     */
    public Map<String, String> getLookupMEP() {
        return mLookupMep;
    }

    /**
     *
     * @return
     */
    public Map<String, String> getLookupMEPBinding() {
        return mLookupMepBinding;
    }

    /**
     *
     */
    public void formatPMode() {
        ///      setPModeString(XMLUtils.format(getPModeString()));
    }

}
