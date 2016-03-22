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

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.ejb.EJB;
import org.sed.ebms.user.SEDUser;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.SEDPersistenceBean;
import si.sed.msh.web.utils.SEDGUIConstants;




@ViewScoped
@ManagedBean(name = "loginManager")
public class LoginManager {

    private static final String HOME_PAGE = "/";
    private static final String PAGE_AFTER_LOGOUT = HOME_PAGE; // Another good option is the login page back again
    
    
    private static final SEDLogger mLog = new SEDLogger(LoginManager.class);

    private String mstrUsername ="sed";
    private String mstrPassword = "sed1234";
    private String mstrForwardUrl;
    
    @EJB
    SEDPersistenceBean mSedDB;


    public String getUsername() {
        return mstrUsername;
    }
    public void setUsername(String username) {
        this.mstrUsername = username;
    }

    public String getPassword() {
        return mstrPassword;
    }
    public void setPassword(String password) {
        this.mstrPassword = password;
    }


    @PostConstruct
    public void init() {
        long l = mLog.logStart();
        this.mstrForwardUrl = extractRequestedUrlBeforeLogin();
        mLog.logEnd(l, mstrForwardUrl);
    }

    private String extractRequestedUrlBeforeLogin() {
        ExternalContext externalContext = externalContext();
        String requestedUrl = (String) externalContext.getRequestMap().get(RequestDispatcher.FORWARD_REQUEST_URI);
        if (requestedUrl == null) {
            return externalContext.getRequestContextPath() + HOME_PAGE;
        }
        String queryString = (String) externalContext.getRequestMap().get(RequestDispatcher.FORWARD_QUERY_STRING);
        return requestedUrl + (queryString == null ? "" : "?" + queryString);
    }

    private ExternalContext externalContext() {
        return facesContext().getExternalContext();
    }

    private FacesContext facesContext() {
        return FacesContext.getCurrentInstance();
    }


    /**
     * Performs user login accordingly to the username/password set.
     *
     * @throws IOException from {@link ExternalContext#redirect(String)}
     */
    public void login() throws IOException {
        long l = mLog.logStart(getClientIP());
        ExternalContext externalContext = externalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        
        if (getUsername()==null || getUsername().trim().isEmpty()){
            String msg = "Username must not be null or empty!";
            mLog.logWarn(l, getClientIP() +  " msg: " +msg,null);
            facesContext().addMessage(null, new FacesMessage(msg));
            return;
            
        }
        
        if (getPassword()==null || getPassword().trim().isEmpty()){
            String msg = "Password must not be null or empty!";
            mLog.logWarn(l, getClientIP() +  " msg: " +msg,null);
            facesContext().addMessage(null, new FacesMessage(msg));
            return;
        }
        try {
            String userName = getUsername().trim();
            request.login(userName, getPassword().trim());
            
            SEDUser user =  mSedDB.getSEDUser(userName);
            if(user == null){
                String msg = "User '"+userName+"' is not reqistred in ebms-sed";
                mLog.logWarn(l, getClientIP() +  " msg: " +msg,null);
                facesContext().addMessage(null, new FacesMessage(msg));
                
                externalContext.invalidateSession();
                return;
            }
            
            Date dCd = Calendar.getInstance().getTime();
            if(user.getActiveFromDate().after(dCd) || 
                    (user.getActiveToDate()!= null && user.getActiveToDate().before(dCd)) ){
                String msg = "User '"+userName+"' is not active";
                mLog.logWarn(l, getClientIP() +  " msg: " +msg,null);
                facesContext().addMessage(null, new FacesMessage(msg));
                externalContext.invalidateSession();
                return;
            }
            // get user data
            
            
            
            externalContext.getSessionMap().put(SEDGUIConstants.SESSION_USER_VARIABLE_NAME, user);
            externalContext.redirect(mstrForwardUrl);
            String msg = "Username: '"+getUsername()+"' logged in!";
            mLog.log(l, getClientIP() +  " msg: " +msg,null);
        } catch (ServletException e) {
            /*
             * The ServletException is thrown if the configured login mechanism does not support
             * username password authentication, or if a non-null caller identity had already been
             * established (prior to the call to login), or if validation of the provided username and password fails.
             */
            String loginErrorMessage = e.getLocalizedMessage();
            facesContext().addMessage(null, new FacesMessage(loginErrorMessage));
        
            String msg = "Error occured while logging user: '"+getUsername()+"'";
            mLog.log(l, getClientIP() +  " msg: " +msg,e);
        }

        mLog.logEnd(l, getClientIP(), getUsername() );
    }


    /**
     * Invalidates the current session, effectively logging out the current user.
     *
     * @throws IOException from {@link ExternalContext#redirect(String)}
     */
    public void logout() throws IOException {
        long l = mLog.logStart(getClientIP());
        ExternalContext externalContext = externalContext();
        externalContext.invalidateSession();
        externalContext.redirect(externalContext.getRequestContextPath() + PAGE_AFTER_LOGOUT);
        mLog.logEnd(l, getClientIP(), getUsername() );
    }


    /**
     * Makes the current logged in available through EL: #{loginManager.user}. Notice as the user is also placed in
     * the session map (), it also is available through #{user}.
     *
     * @return The currently logged in {@link User}, or {@code null} if no user is logged in.
     */
    public SEDUser getUser() {
        FacesContext context = facesContext();
        ExternalContext externalContext = context.getExternalContext();
        return (SEDUser) externalContext.getSessionMap().get(SEDGUIConstants.SESSION_USER_VARIABLE_NAME);
    }

    

    /**
     * Verifies if there is a currently logged in user.
     *
     * @return {@code true} if there's a logged in {@link User}, {@code false} otherwise.
     */
    public boolean isUserLoggedIn() {
        return getUser() != null;
    }


    /**
     * Verifies if the currently logged in user, if exists, is in the given ROLE.
     *
     * @param role The ROLE to verify if the user has.
     * @return {@code true} if the user is logged in and has the given ROLE. {@code false} otherwise.
     */
    public boolean isUserInRole(String role) {
        FacesContext context = facesContext();
        ExternalContext externalContext = context.getExternalContext();
        return externalContext.isUserInRole(role);
    }
    
    public String getClientIP(){
        return  ((HttpServletRequest) externalContext().getRequest()).getRemoteAddr();
    }

}