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

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.event.TabChangeEvent;
 
/**
 *
 * @author Jože Rihtaršič
 */

@ViewScoped
@ManagedBean(name = "MainWindow" )
public class MainWindow {
   
     
    String mstrWindowShow = AppConstant.S_PANEL_INBOX;
    
     public void onToolbarTabChange(TabChangeEvent event) {
         mstrWindowShow=event.getTab().getId();
         System.out.println("SEt render: " + event.getTab().getId()) ;
        FacesMessage msg = new FacesMessage("Tab Changed", "Active Tab: " + event.getTab().getId());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }
     
    public void onToolbarButtonAction(ActionEvent event) {
         String res = (String) event.getComponent().getAttributes().get("panel");
         System.out.println("Res:" + res);
          mstrWindowShow = res;
    }
     
     public String currentPanel(){
        return mstrWindowShow;
     }
     
   
     
    public void addMessage(String summary, String detail) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, detail);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }
}