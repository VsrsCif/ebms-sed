/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui;

import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.msh.svev.pmode.Leg;
import org.msh.svev.pmode.PMode;
import org.primefaces.event.SelectEvent;
import org.sed.ebms.inbox.mail.InMail;
import si.sed.commons.utils.PModeManager;


@ViewScoped
@ManagedBean(name = "PModeView")
public class PModeView {
    
    private PMode currentPMode;
    PModeManager pm = new  PModeManager();
    
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


}
