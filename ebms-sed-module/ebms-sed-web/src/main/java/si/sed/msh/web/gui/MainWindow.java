/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.event.TabChangeEvent;
 
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