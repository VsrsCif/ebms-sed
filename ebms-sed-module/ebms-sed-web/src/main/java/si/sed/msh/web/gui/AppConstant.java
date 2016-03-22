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

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;


/**
 *
 * @author Jože Rihtaršič
 */
@ApplicationScoped
@ManagedBean(name = "AppConstant" )
public class AppConstant {
    public static final String S_PANEL_INBOX = "PANEL_INBOX";
    public static final String S_PANEL_OUTBOX = "PANEL_OUTBOX";
    public static final String S_PANEL_SETT_CUSTOM = "PANEL_SETT_CUSTOM";
    public static final String S_PANEL_SETT_CERTS = "PANEL_SETT_CERTS";
    public static final String S_PANEL_SETT_PMODE = "PANEL_SETT_PMODE";
    public static final String S_PANEL_ADMIN_USERS = "PANEL_ADMIN_USERS";
    public static final String S_PANEL_ADMIN_EBOXES = "PANEL_ADMIN_EBOXES";
    public static final String S_PANEL_ADMIN_CRON = "PANEL_ADMIN_CRON";
    

    public  String getS_PANEL_INBOX() {
        return S_PANEL_INBOX;
    }

    public  String getS_PANEL_OUTBOX() {
        return S_PANEL_OUTBOX;
    }

    public  String getS_PANEL_SETT_CUSTOM() {
        return S_PANEL_SETT_CUSTOM;
    }

    public  String getS_PANEL_SETT_CERTS() {
        return S_PANEL_SETT_CERTS;
    }

    public  String getS_PANEL_SETT_PMODE() {
        return S_PANEL_SETT_PMODE;
    }
    
    public  String getS_PANEL_ADMIN_USERS() {
        return S_PANEL_ADMIN_USERS;
    }
    public  String getS_PANEL_ADMIN_EBOXES() {
        return S_PANEL_ADMIN_EBOXES;
    }
    
     public  String getS_PANEL_ADMIN_CRON() {
        return S_PANEL_ADMIN_CRON;
    }
    
    
}
