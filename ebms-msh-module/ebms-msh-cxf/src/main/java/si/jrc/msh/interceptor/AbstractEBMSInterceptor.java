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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.DBSettingsInterface;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public abstract class AbstractEBMSInterceptor extends AbstractSoapInterceptor {

    String LOADED_CLASSES = "hibernate.ejb.loaded.classes";

    DBSettingsInterface mDBSettings;
    SEDDaoInterface mSedDao;
    SEDLookupsInterface mSedLookups;
    SEDLogger mlog = new SEDLogger(AbstractEBMSInterceptor.class);

    /**
     *
     * @param p
     */
    public AbstractEBMSInterceptor(String p) {
        super(p);
    }

    /**
     *
     * @param i
     * @param p
     */
    public AbstractEBMSInterceptor(String i, String p) {
        super(i, p);

    }

    /**
     *
     * @return
     */
    public SEDDaoInterface getDAO() {
        long l = mlog.logStart();
        if (mSedDao == null) {
            try {
                mSedDao = InitialContext.doLookup(SEDJNDI.JNDI_SEDDAO);
                mlog.logEnd(l);
            } catch (NamingException ex) {
                mlog.logError(l, ex);
            }
        }

        return mSedDao;
    }

    /**
     *
     * @return
     */
    public SEDLookupsInterface getLookups() {
        long l = mlog.logStart();
        if (mSedLookups == null) {
            try {
                mSedLookups = InitialContext.doLookup(SEDJNDI.JNDI_SEDLOOKUPS);
                mlog.logEnd(l);
            } catch (NamingException ex) {
                mlog.logError(l, ex);
            }
        }

        return mSedLookups;
    }

    /**
     *
     * @return
     */
    public DBSettingsInterface getSettings() {
        long l = mlog.logStart();
        if (mDBSettings == null) {
            try {
                mDBSettings = InitialContext.doLookup(SEDJNDI.JNDI_DBSETTINGS);
                mlog.logEnd(l);
            } catch (NamingException ex) {
                mlog.logError(l, ex);
            }
        }
        return mDBSettings;
    }

    /**
     *
     * @param t
     * @throws Fault
     */
    @Override
    public abstract void handleMessage(SoapMessage t)
            throws Fault;

}
