/*
 * Copyright 2015, Supreme Court Republic of Slovenia
 * 
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package si.jrc.msh.interceptor;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.DBSettingsInterface;
import si.sed.commons.interfaces.PModeInterface;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.utils.SEDLogger;

/**
 * Abstract class extends from AbstractSoapInterceptor with access to apliation EJB resources as:
 *  - ejb reference to signleton application settings 
 *  - ejb reference to signleton application lookups 
 *  - ejb reference to DAO services.
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public abstract class AbstractEBMSInterceptor extends AbstractSoapInterceptor {
  

  String LOADED_CLASSES = "hibernate.ejb.loaded.classes";
  // ejb reference to signleton application settings 
  protected DBSettingsInterface mDBSettings;
  // ejb reference to signleton application lookups 
  protected SEDLookupsInterface mSedLookups;
  // ejb reference to DAO services
  protected SEDDaoInterface mSedDao;
  
  // ejb reference to PModeManager services
  protected PModeInterface mPMode;
  
  protected static SEDLogger A_LOG = new SEDLogger(AbstractEBMSInterceptor.class);

  /**
   * Constructor. 
   * @param p - CXF bus Phase. Values are defined in  org.apache.cxf.phase.Phase 
   */
  public AbstractEBMSInterceptor(String p) {
    super(p);
  }

  /**
   * constructor
   * @param i - Instantiates the interceptor with a specified id.
   * @param p - CXF bus Phase. Values are defined in  org.apache.cxf.phase.Phase 
   */
  public AbstractEBMSInterceptor(String i, String p) {
    super(i, p);

  }

  /**
   * Methods lookups SEDDaoInterface.
   * @return SEDDaoInterface or null if bad application configuration.
   */
  public SEDDaoInterface getDAO() {
    long l = A_LOG.logStart();
    if (mSedDao == null) {
      try {
        mSedDao = InitialContext.doLookup(SEDJNDI.JNDI_SEDDAO);
        A_LOG.logEnd(l);
      } catch (NamingException ex) {
        A_LOG.logError(l, ex);
      }
    }

    return mSedDao;
  }

  /**
   * Methods lookups SEDLookupsInterface.
   * @return SEDLookupsInterface or null if bad application configuration.
   */
  public SEDLookupsInterface getLookups() {
    long l = A_LOG.logStart();
    if (mSedLookups == null) {
      try {
        mSedLookups = InitialContext.doLookup(SEDJNDI.JNDI_SEDLOOKUPS);
        A_LOG.logEnd(l);
      } catch (NamingException ex) {
        A_LOG.logError(l, ex);
      }
    }

    return mSedLookups;
  }

/**
   * Methods lookups DBSettingsInterface.
   * @return DBSettingsInterface or null if bad application configuration.
   */
  public DBSettingsInterface getSettings() {
    long l = A_LOG.logStart();
    if (mDBSettings == null) {
      try {
        mDBSettings = InitialContext.doLookup(SEDJNDI.JNDI_DBSETTINGS);
        A_LOG.logEnd(l);
      } catch (NamingException ex) {
        A_LOG.logError(l, ex);
      }
    }
    return mDBSettings;
  }
  
  /**
   * Methods lookups PModeInterface.
   * @return PModeInterface or null if bad application configuration.
   */
  public PModeInterface getPModeManager() {
    long l = A_LOG.logStart();
    if (mPMode == null) {
      try {
        mPMode = InitialContext.doLookup(SEDJNDI.JNDI_PMODE);
        A_LOG.logEnd(l);
      } catch (NamingException ex) {
        A_LOG.logError(l, ex);
      }
    }
    return mPMode;
  }

  /**
   * Abstract method for handling SoapMessage.
   * @param t - soap messsage
   */
  @Override
  public abstract void handleMessage(SoapMessage t) throws Fault;

}
