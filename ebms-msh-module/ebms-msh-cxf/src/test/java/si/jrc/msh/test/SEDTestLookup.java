/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.test;

import generated.SedLookups;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cert.SEDCertificate;
import org.sed.ebms.cron.SEDCronJob;
import org.sed.ebms.cron.SEDTaskType;
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.plugin.SEDPlugin;
import org.sed.ebms.user.SEDUser;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author sluzba
 */
public class SEDTestLookup implements SEDLookupsInterface {

  public static final String INIT_LOOKUPS_RESOURCE_PATH = "/sed-lookups.xml";
  private final HashMap<Class, List<?>> mlstCacheLookup = new HashMap<>();

  public SEDTestLookup(InputStream is)
      throws IOException, JAXBException {
    init(is);
  }

  private void init(InputStream is)
      throws IOException, JAXBException {
    SedLookups cls = (SedLookups) XMLUtils.deserialize(is, SedLookups.class);

    mlstCacheLookup.put(SEDBox.class, cls.getSEDBoxes().getSEDBoxes());
    mlstCacheLookup.put(SEDCertStore.class, cls.getSEDCertStores().getSEDCertStores());
    mlstCacheLookup.put(SEDCronJob.class, cls.getSEDCronJobs().getSEDCronJobs());
    mlstCacheLookup.put(SEDPlugin.class, cls.getSEDPlugins().getSEDPlugins());
    mlstCacheLookup.put(SEDTaskType.class, cls.getSEDTaskTypes().getSEDTaskTypes());
    mlstCacheLookup.put(SEDUser.class, cls.getSEDUsers().getSEDUsers());
  }

  private <T> List<T> getLookup(Class<T> c) {
    if (!mlstCacheLookup.containsKey(c)) {
      mlstCacheLookup.put(c, new ArrayList<T>());
    }
    return (List<T>) mlstCacheLookup.get(c);
  }

  private <T> boolean add(T val) {
    List lst = getLookup(val.getClass());
    return lst.add(val);
  }

  private <T> boolean remove(T val) {
    List lst = getLookup(val.getClass());
    return lst.remove(val);
  }

  @Override
  public boolean addSEDBox(SEDBox sb) {
    return add(sb);
  }

  @Override
  public boolean addSEDCertStore(SEDCertStore sb) {
    return add(sb);
  }

  @Override
  public boolean addSEDCronJob(SEDCronJob sb) {
    return add(sb);
  }

  @Override
  public boolean addSEDPlugin(SEDPlugin sb) {
    return add(sb);
  }

  @Override
  public boolean addSEDTaskType(SEDTaskType sb) {
    return add(sb);
  }

  @Override
  public boolean addSEDUser(SEDUser sb) {
    return add(sb);
  }

  @Override
  public void exportLookups(File f, boolean saveCertPasswords) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public SEDBox getSEDBoxByName(String strname, boolean ignoreDomain) {
    if (strname != null && !strname.trim().isEmpty()) {
      String sedBox = strname.trim();
      List<SEDBox> lst = getSEDBoxes();
      for (SEDBox sb : lst) {
        if (ignoreDomain && sb.getBoxName().startsWith(sedBox + "@") ||
            sb.getBoxName().equalsIgnoreCase(sedBox)) {
          return sb;
        }
      }
    }
    return null;
  }

  @Override
  public List<SEDBox> getSEDBoxes() {
    return getLookup(SEDBox.class);
  }

  @Override
  public List<SEDCertStore> getSEDCertStore() {
    return getLookup(SEDCertStore.class);
  }

  @Override
  public SEDCertStore getSEDCertStoreById(BigInteger id) {
    if (id == null) {
      throw new IllegalArgumentException(String.format("KeyStore id is null"));
    }
    List<SEDCertStore> lst = getSEDCertStore();
    for (SEDCertStore cs : lst) {
      if (id.equals(cs.getId())) {
        return cs;
      }
    }
    return null;
  }

  @Override
  public SEDCertStore getSEDCertStoreByName(String storeName) {
    if (Utils.isEmptyString(storeName)) {
      throw new IllegalArgumentException(String.format("KeyStore name is null"));
    }

    List<SEDCertStore> lst = getSEDCertStore();
    for (SEDCertStore cs : lst) {
      if (storeName.equals(cs.getName())) {
        return cs;
      }
    }

    return null;
  }

  @Override
  public SEDCertificate getSEDCertificatForAlias(String alias, SEDCertStore cs, boolean isKey) {
    if (alias == null) {
      return null;
    }

    for (SEDCertificate c : cs.getSEDCertificates()) {
      if (c.getAlias().equalsIgnoreCase(alias)) {
        if (!isKey || c.isKeyEntry() == isKey) {
          return c;
        }
      }
    }
    return null;
  }

  @Override
  public SEDCertificate getSEDCertificatForAlias(String alias, String storeName, boolean isKey) {
    if (Utils.isEmptyString(alias) || Utils.isEmptyString(storeName)) {
      return null;
    }
    return getSEDCertificatForAlias(alias, getSEDCertStoreByName(storeName), isKey);
  }

  @Override
  public SEDCronJob getSEDCronJobById(BigInteger id) {
    if (id != null) {

      List<SEDCronJob> lst = getSEDCronJobs();
      for (SEDCronJob sb : lst) {
        if (id.equals(sb.getId())) {
          return sb;
        }
      }
    }
    return null;
  }

  @Override
  public List<SEDCronJob> getSEDCronJobs() {
    return getLookup(SEDCronJob.class);
  }

  @Override
  public List<SEDPlugin> getSEDPlugin() {
    return getLookup(SEDPlugin.class);
  }

  @Override
  public SEDPlugin getSEDPluginByType(String type) {
    if (type != null) {

      List<SEDPlugin> lst = getSEDPlugin();
      for (SEDPlugin sb : lst) {
        if (type.equals(sb.getType())) {
          return sb;
        }
      }
    }
    return null;
  }

  @Override
  public SEDTaskType getSEDTaskTypeByType(String type) {
    if (type != null) {

      List<SEDTaskType> lst = getSEDTaskTypes();
      for (SEDTaskType sb : lst) {
        if (type.equals(sb.getType())) {
          return sb;
        }
      }
    }
    return null;
  }

  @Override
  public List<SEDTaskType> getSEDTaskTypes() {
    return getLookup(SEDTaskType.class);
  }

  @Override
  public SEDUser getSEDUserByUserId(String userId) {
    if (userId != null && !userId.trim().isEmpty()) {
      String ui = userId.trim();
      List<SEDUser> lst = getSEDUsers();
      for (SEDUser sb : lst) {
        if (sb.getUserId().equalsIgnoreCase(ui)) {
          return sb;
        }
      }
    }
    return null;
  }

  @Override
  public List<SEDUser> getSEDUsers() {
    return getLookup(SEDUser.class);
  }

  @Override
  public boolean removeEDCertStore(SEDCertStore sb) {
    return remove(sb);
  }

  @Override
  public boolean removeSEDBox(SEDBox sb) {
    return remove(sb);
  }

  @Override
  public boolean removeSEDCronJob(SEDCronJob sb) {
    return remove(sb);
  }

  @Override
  public boolean removeSEDPlugin(SEDPlugin sb) {
    return remove(sb);
  }

  @Override
  public boolean removeSEDTaskType(SEDTaskType sb) {
    return remove(sb);
  }

  @Override
  public boolean removeSEDUser(SEDUser sb) {
    return remove(sb);
  }

  @Override
  public boolean updateSEDBox(SEDBox sb) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean updateSEDCertStore(SEDCertStore sb) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean updateSEDCronJob(SEDCronJob sb) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean updateSEDPlugin(SEDPlugin sb) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean updateSEDTaskType(SEDTaskType sb) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public boolean updateSEDUser(SEDUser sb) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
