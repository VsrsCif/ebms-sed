/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;


/**
 *
 * @author sluzba
 */
@ApplicationScoped
@ManagedBean(name = "AppConstant" )
public class AppConstant {
    public static final String S_PANEL_INBOX = "PANEL_INBOX";
    public static final String S_PANEL_OUTBOX = "PANEL_OUTBOX";
    public static final String S_PANEL_SETT_CUSTOM = "PANEL_SETT_CUSTOM";
    public static final String S_PANEL_SETT_CERTS = "PANEL_SETT_CERTS";
    public static final String S_PANEL_SETT_PMODE = "PANEL_SETT_PMODE";

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
    
    
    
}
