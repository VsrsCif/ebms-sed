/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author sluzba
 */
public class AbstractJSFView {
    
    public String getClientIP() {
        return ((HttpServletRequest) externalContext().getRequest()).getRemoteAddr();
    }

    protected ExternalContext externalContext() {
        return facesContext().getExternalContext();
    }

    protected FacesContext facesContext() {
        return FacesContext.getCurrentInstance();
    }
    
}
