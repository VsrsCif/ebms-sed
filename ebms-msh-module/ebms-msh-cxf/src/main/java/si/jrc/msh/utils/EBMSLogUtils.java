package si.jrc.msh.utils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import si.jrc.msh.interceptor.EBMSOutInterceptor;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.SEDLogger;
import static si.sed.commons.utils.StorageUtils.currentStorageFolderName;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class EBMSLogUtils {

  /**
     *
     */
  public static final String S_IN_PREFIX = "_in";

  /**
     *
     */
  public static final String S_OUT_PREFIX = "_out";

  /**
     *
     */
  public static final String S_PREFIX = "ebms_";

  /**
     *
     */
  public static final String S_REQUEST_SUFFIX = "-request.soap";

  /**
     *
     */
  public static final String S_RESPONSE_SUFFIX = "-response.soap";
  private static final String S_ROOT_FOLDER = "ebms_log";

  /**
     *
     */
  protected static final SEDLogger mlog = new SEDLogger(EBMSOutInterceptor.class);

  private static synchronized File currentStorageFolder() throws StorageException {

    File f =
        new File(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator
            + S_ROOT_FOLDER + File.separator + currentStorageFolderName());
    if (!f.exists() && !f.mkdirs()) {
      throw new StorageException(String.format(
          "Error occurred while creating storage folder: '%s'", f.getAbsolutePath()));
    }
    return f;
  }

  /**
   *
   * @param prefix
   * @param base
   * @param suffix
   * @return
   */
  public static File getBaseFile(String prefix, String base, String suffix) throws StorageException {
    File fStore = null;
    if (base != null) {
      fStore = new File(currentStorageFolder(), prefix + base + suffix);
    }
    if (fStore == null || fStore.exists()) {
      fStore = getNewFile(prefix, suffix);
    }
    return fStore;
  }

  /**
   *
   * @param f
   * @return
   */
  public static String getBaseFileName(File f) {
    String fn = f.getName();
    return fn.substring(fn.indexOf('_') + 1, fn.lastIndexOf('_'));
  }

  /**
   *
   * @param isRequestor
   * @param baseName
   * @return
   */
  public static File getInboundFileName(boolean isRequestor, String baseName) {
    try {
      return getBaseFile(S_PREFIX, baseName, S_IN_PREFIX
          + (isRequestor ? S_RESPONSE_SUFFIX : S_REQUEST_SUFFIX));
    } catch (StorageException ex) {
      mlog.logError(0, ex);
    }
    return null;
  }

  private static File getNewFile(String prefix, String suffix) throws StorageException {
    File fStore;
    try {
      fStore = File.createTempFile(prefix, suffix, currentStorageFolder());
    } catch (IOException ex) {
      throw new StorageException("Error occurred while creating storage file", ex);
    }
    return fStore;
  }

  /**
   *
   * @param isRequestor
   * @param id
   * @param baseName
   * @return
   */
  public static File getOutboundFileName(boolean isRequestor, BigInteger id, String baseName) {
    try {
      return getBaseFile(S_PREFIX, (baseName != null ? baseName : "")
          + (baseName != null && id != null ? "-" : "") + (id != null ? id.toString() : ""),
          S_OUT_PREFIX + (isRequestor ? S_REQUEST_SUFFIX : S_RESPONSE_SUFFIX));
    } catch (StorageException ex) {
      mlog.logError(0, ex);
    }
    return null;
  }

}
