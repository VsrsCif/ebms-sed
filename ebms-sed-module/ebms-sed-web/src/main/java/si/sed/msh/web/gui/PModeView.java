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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.msh.svev.pmode.Leg;
import org.msh.svev.pmode.PMode;
import org.primefaces.event.SelectEvent;
import si.sed.commons.utils.PModeManager;

/**
 *
 * @author Jože Rihtaršič
 */
@ViewScoped
@ManagedBean(name = "PModeView")
public class PModeView {
    
    private PMode currentPMode;
    PModeManager pm = new  PModeManager();
    
    private Map<String,String> mLookupMep;
    private Map<String,String> mLookupMepBinding;
    
     @PostConstruct
    public void init() {
        mLookupMep  = new HashMap<>();
        mLookupMep.put("One-Way MEP", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay");
        mLookupMep.put("Two-Way MEP", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/twoWay");        
        mLookupMepBinding  = new HashMap<>();                                       
        mLookupMepBinding.put("Push", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/push");
        mLookupMepBinding.put("Pull", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pull");
        mLookupMepBinding.put("Sync", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sync");
        mLookupMepBinding.put("PushAndPush", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPush");
        mLookupMepBinding.put("PushAndPull", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPull");
        mLookupMepBinding.put("PullAndPush", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pullAndPush");
    }
    
    
    public List<PMode> getPModes() {
        return pm.getPModes().getPModes();
    }

    public PMode getCurrentPMode() {
        return currentPMode;
    }

    public void setCurrentPMode(PMode currentPMode) {
        this.currentPMode = currentPMode;
    }
    
     public Leg getCurrentPModeForeChannel() {
         return this.currentPMode != null?this.currentPMode.getLegs().get(0): null;
    }
    
     public void onRowSelect(SelectEvent event) {
        setCurrentPMode((PMode) event.getObject());      
    }


     public Map<String, String> getLookupMEP() {
        return mLookupMep;
     }
      public Map<String, String> getLookupMEPBinding() {
        return mLookupMepBinding;
     }
}
