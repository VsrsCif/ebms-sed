/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package si.jrc.msh.client;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author sluzba
 */
public class EJBContainer {

  /**
   * Logger for MshClient class
   */
  private final SEDLogger LOG = new SEDLogger(EJBContainer.class);

  /**
   * Common Lookups from database
   */
  private SEDLookupsInterface mSedLookups;

  /**
   * Method returns SEDLookupsInterface
   * 
   * @return SEDLookupsInterface: database
   */
  public SEDLookupsInterface getLookups() {
    long l = LOG.logStart();
    if (mSedLookups == null) {
      try {
        mSedLookups = InitialContext.doLookup(SEDJNDI.JNDI_SEDLOOKUPS);
        LOG.logEnd(l);
      } catch (NamingException ex) {
        LOG.logError(l, ex);
      }
    }

    return mSedLookups;
  }
}
