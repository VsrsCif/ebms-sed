/*
* Copyright 2015, Supreme Court Republic of Slovenia 
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
package si.jrc.msh.interceptor;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.EJB;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.msh.ebms.inbox.event.MSHInEvent;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.outbox.event.MSHOutEvent;
import org.msh.ebms.outbox.mail.MSHOutMail;
import si.sed.commons.SEDJNDI;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.interfaces.DBSettingsInterface;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public abstract class AbstractEBMSInterceptor extends AbstractSoapInterceptor {

    String LOADED_CLASSES = "hibernate.ejb.loaded.classes";

    SEDLogger mlog = new SEDLogger(AbstractEBMSInterceptor.class);
     
    DBSettingsInterface mDBSettings;
    SEDDaoInterface mSedDao;

    public AbstractEBMSInterceptor(String p) {
        super(p);
    }

    public AbstractEBMSInterceptor(String i, String p) {
        super(i, p);

    }


   

    public DBSettingsInterface getSettings() {
        long l = mlog.logStart();
        if (mDBSettings== null) {
            try {
                mDBSettings=  InitialContext.doLookup(SEDJNDI.JNDI_DBSETTINGS);
                mlog.logEnd(l);
            } catch (NamingException ex) {
                mlog.logError(l, ex);
            }
        }
        return mDBSettings;
    }
    
     public SEDDaoInterface getDAO() {
        long l = mlog.logStart();
        if (mSedDao== null) {
            try {
                mSedDao=  InitialContext.doLookup(SEDJNDI.JNDI_SEDDAO);
                mlog.logEnd(l);
            } catch (NamingException ex) {
                mlog.logError(l, ex);
            }            
        }
      
        return mSedDao;
    }

    @Override
    public abstract void handleMessage(SoapMessage t) throws Fault;

    public void serializeMail(MSHOutMail mail, String userID, String applicationId, String pmodeId) {
        
        getDAO().serializeOutMail(mail, userID, applicationId, pmodeId);
        

    }

    public void serializeInMail(MSHInMail mail) {
        getDAO().serializeInMail(mail);
    }

}
