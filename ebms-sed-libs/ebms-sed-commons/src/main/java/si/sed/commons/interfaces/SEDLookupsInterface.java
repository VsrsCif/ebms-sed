/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package si.sed.commons.interfaces;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import javax.ejb.Local;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cert.SEDCertificate;
import org.sed.ebms.cron.SEDCronJob;
import org.sed.ebms.cron.SEDTaskType;
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.plugin.SEDPlugin;
import org.sed.ebms.user.SEDUser;

/**
 *
 * @author sluzba
 */
@Local
public interface SEDLookupsInterface {

  /**
   *
   * @param sb
   * @return
   */
  boolean addSEDBox(SEDBox sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean addSEDCertStore(SEDCertStore sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean addSEDCronJob(SEDCronJob sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean addSEDPlugin(SEDPlugin sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean addSEDTaskType(SEDTaskType sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean addSEDUser(SEDUser sb);

  /**
   *
   * @param f
   * @param saveCertPasswords
   */
  void exportLookups(File f, boolean saveCertPasswords);

  /**
   *
   * @param strname
   * @param ignoreDomain
   * @return
   */
  SEDBox getSEDBoxByName(String strname, boolean ignoreDomain);

  /**
   *
   * @param alias
   * @param isKey
   * @return
   */
  SEDCertStore getSEDCertStoreByCertAlias(String alias, boolean isKey);

  /**
   * MEthod resturs SEDCertificat object for alias
   *
   * @param alias - alias of certificate
   * @param cs - key/trustostre
   * @param isKey - returned SEDCertificate must be a key
   * @return SEDCertificate or null in SEDCertificate is found givem store.
   */
   SEDCertificate getSEDCertificatForAlias(String alias,
      SEDCertStore cs, boolean isKey);
  /**
   *
   * @param id
   * @return
   */
  SEDCronJob getSEDCronJobById(BigInteger id);

  /**
   *
   * @param type
   * @return
   */
  SEDTaskType getSEDTaskTypeByType(String type);

  /**
   *
   * @param userId
   * @return
   */
  SEDUser getSEDUserByUserId(String userId);

  /**
   *
   * @param type
   * @return
   */
  SEDPlugin getSEDPluginByType(String type);

  /**
   *
   * @return
   */
  List<SEDBox> getSEDBoxes();

  /**
   *
   * @return
   */
  List<SEDCertStore> getSEDCertStore();

  /**
   *
   * @return
   */
  List<SEDCronJob> getSEDCronJobs();

  /**
   *
   * @return
   */
  List<SEDPlugin> getSEDPlugin();

  /**
   *
   * @return
   */
  List<SEDTaskType> getSEDTaskTypes();

  /**
   *
   * @return
   */
  List<SEDUser> getSEDUsers();

  /**
   *
   * @param sb
   * @return
   */
  boolean removeSEDBox(SEDBox sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean removeEDCertStore(SEDCertStore sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean removeSEDCronJob(SEDCronJob sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean removeSEDPlugin(SEDPlugin sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean removeSEDTaskType(SEDTaskType sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean removeSEDUser(SEDUser sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean updateSEDBox(SEDBox sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean updateSEDCertStore(SEDCertStore sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean updateSEDCronJob(SEDCronJob sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean updateSEDPlugin(SEDPlugin sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean updateSEDTaskType(SEDTaskType sb);

  /**
   *
   * @param sb
   * @return
   */
  boolean updateSEDUser(SEDUser sb);
}
