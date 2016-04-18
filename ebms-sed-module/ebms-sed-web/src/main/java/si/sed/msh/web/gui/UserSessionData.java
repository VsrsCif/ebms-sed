package si.sed.msh.web.gui;


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
import si.sed.msh.web.abst.AbstractJSFView;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.List;
import javax.faces.application.FacesMessage;

import javax.faces.bean.SessionScoped;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TransferEvent;
import org.primefaces.event.UnselectEvent;
import org.sed.ebms.user.SEDUser;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.SEDGUIConstants;

@SessionScoped
@ManagedBean(name = "userSessionData")
public class UserSessionData extends AbstractJSFView {

    private static final SEDLogger LOG = new SEDLogger(UserSessionData.class);
    private String mstrCurrentSEDBox;

    public SEDUser getUser() {
        FacesContext context = facesContext();
        ExternalContext externalContext = context.getExternalContext();
        return (SEDUser) externalContext.getSessionMap().get(SEDGUIConstants.SESSION_USER_VARIABLE_NAME);
    }

    public List<String> getUserEBoxes() {
        List<String> lst = new ArrayList<>();
        SEDUser usr = getUser();
        if (usr != null) {
            getUser().getSEDBoxes().stream().forEach((sb) -> {
                lst.add(sb.getBoxName());
            });
        }
        return lst;
    }

    public void setCurrentSEDBox(String strCurrBox) {
        mstrCurrentSEDBox = strCurrBox;
    }

    public String getCurrentSEDBox() {
        return mstrCurrentSEDBox == null&&
                getUserEBoxes()!=null &&  !getUserEBoxes().isEmpty()? getUserEBoxes().get(0):mstrCurrentSEDBox;
    }
    
     public void onTransfer(TransferEvent event) {
       /* StringBuilder builder = new StringBuilder();
        for(Object item : event.getItems()) {
            builder.append(((Theme) item).getName()).append("<br />");
        }
         
        FacesMessage msg = new FacesMessage();
        msg.setSeverity(FacesMessage.SEVERITY_INFO);
        msg.setSummary("Items Transferred");
        msg.setDetail(builder.toString());
         
        FacesContext.getCurrentInstance().addMessage(null, msg);*/
    } 
 
    public void onSelect(SelectEvent event) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Item Selected", event.getObject().toString()));
    }
     
    public void onUnselect(UnselectEvent event) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Item Unselected", event.getObject().toString()));
    }
     
    public void onReorder() {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "List Reordered", null));
    } 

}
