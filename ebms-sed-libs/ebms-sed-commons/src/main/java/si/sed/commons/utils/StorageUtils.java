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
package si.sed.commons.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import static java.io.File.createTempFile;
import static java.io.File.separator;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import static java.util.Calendar.getInstance;
import static si.sed.commons.MimeValues.getSuffixBYMimeType;
import static si.sed.commons.SEDSystemProperties.SYS_PROP_FOLDER_STORAGE_DEF;
import static si.sed.commons.SEDSystemProperties.SYS_PROP_HOME_DIR;
import si.sed.commons.exception.StorageException;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class StorageUtils {

    /**
     *
     */
    public static final String S_IN_PREFIX = "in_";

    /**
     *
     */
    public static final String S_OUT_PREFIX = "out_";
    private static final SimpleDateFormat msdfFolderDateFormat =
            new SimpleDateFormat("yyyyMMdd");

    /**
     *
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    public static synchronized void copyFile(File sourceFile, File destFile)
            throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
                FileChannel destination =
                new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
    }

    /**
     *
     * @param storageFilePath
     * @param folder
     * @throws IOException
     * @throws StorageException
     */
    public static synchronized void copyFileToFolder(String storageFilePath,
            File folder)
            throws IOException,
            StorageException {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not create export folder: " +
                    folder.getAbsolutePath());
        }
        File destFile = new File(folder, storageFilePath);
        File pf = destFile.getParentFile();
        if (!pf.exists() && !pf.mkdirs()) {
            throw new IOException("Could not create folder: " +
                    pf.getAbsolutePath());
        }

        copyFile(getFile(storageFilePath), destFile);
    }

    /**
     *
     * @param strInFileName
     * @param destFile
     * @throws IOException
     * @throws StorageException
     */
    public static synchronized void copyInFile(String strInFileName,
            File destFile)
            throws IOException, StorageException {
        copyFile(getFile(strInFileName), destFile);
    }

    /**
     *
     * @return @throws StorageException
     */
    public static synchronized File currentStorageFolder()
            throws StorageException {

        File f = new File(
                getProperty(SYS_PROP_HOME_DIR) + separator +
                SYS_PROP_FOLDER_STORAGE_DEF + separator +
                currentStorageFolderName());
        if (!f.exists() && !f.mkdirs()) {
            throw new StorageException(format(
                    "Error occurred while creating storage folder: '%s'",
                    f.getAbsolutePath()));
        }
        return f;
    }

    /**
     *
     * @return
     */
    public static synchronized String currentStorageFolderName() {
        return msdfFolderDateFormat.format(getInstance().getTime());
    }

    /**
     *
     * @param storagePath
     * @return
     * @throws StorageException
     */
    public static synchronized File getFile(String storagePath)
            throws StorageException {
        File f = new File(getProperty(SYS_PROP_HOME_DIR) + separator +
                storagePath);
        return f;
    }

    /**
     *
     * @param suffix
     * @param prefix
     * @return
     * @throws StorageException
     */
    public static File getNewStorageFile(String suffix, String prefix)
            throws StorageException {
        File fStore;
        try {
            fStore =
                    createTempFile(prefix, "." + suffix, currentStorageFolder());
        } catch (IOException ex) {
            throw new StorageException(
                    "Error occurred while creating storage file", ex);
        }
        return fStore;
    }

    /**
     *
     * @param path
     * @return
     */
    public static String getRelativePath(File path) {
        File hdir = new File(getProperty(SYS_PROP_HOME_DIR));
        if (path.getAbsolutePath().startsWith(hdir.getAbsolutePath())) {
            String rp = path.getAbsolutePath().substring(
                    hdir.getAbsolutePath().length());
            rp = rp.startsWith(separator) ? rp.substring(1) : rp;
            return rp;
        }

        String base = getProperty(SYS_PROP_HOME_DIR);

        String[] basePaths = base.split(separator);
        String[] otherPaths = path.getParent().split(separator);
        int n = 0;
        for (; n < basePaths.length && n < otherPaths.length; n++) {
            if (basePaths[n].equals(otherPaths[n]) == false) {
                break;
            }
        }
        StringBuilder tmp = new StringBuilder();
        for (int m = n; m < basePaths.length - 1; m++) {
            tmp.append("..");
            tmp.append(separator);
        }

        for (int m = n; m < otherPaths.length; m++) {
            tmp.append(otherPaths[m]);
            tmp.append(separator);
        }
        // add filename
        tmp.append(path.getName());
        return tmp.toString();
    }

    /**
     *
     * @param strInFileName
     * @throws IOException
     * @throws StorageException
     */
    public static synchronized void removeFile(String strInFileName)
            throws IOException, StorageException {
        File f = getFile(strInFileName);
        f.delete();
    }

    /**
     *
     * @param storagePath
     * @return
     * @throws StorageException
     */
    public byte[] getByteArray(String storagePath)
            throws StorageException {
        byte[] bin = null;
        File f = getFile(storagePath);

        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                bin = new byte[fis.available()];
                fis.read(bin);
            } catch (IOException ex) {
                throw new StorageException(
                        "Error occurred while creating storage file", ex);
            }
        }
        return bin;
    }

    /**
     *
     * @param prefix
     * @param suffix
     * @param buffer
     * @return
     * @throws StorageException
     */
    public File storeFile(String prefix, String suffix, byte[] buffer)
            throws StorageException {
        return storeFile(prefix, suffix, new ByteArrayInputStream(buffer));
    }

    /**
     *
     * @param prefix
     * @param suffix
     * @param inStream
     * @return
     * @throws StorageException
     */
    public File storeFile(String prefix, String suffix, InputStream inStream)
            throws StorageException {
        File fStore = getNewStorageFile(suffix, prefix);

        try (FileOutputStream fos = new FileOutputStream(fStore)) {

            byte[] buffer = new byte[1024];
            int len = inStream.read(buffer);
            while (len != -1) {
                fos.write(buffer, 0, len);
                len = inStream.read(buffer);
            }

        } catch (IOException ex) {
            throw new StorageException(format(
                    "Error occurred while writing to file: '%s'",
                    fStore.getAbsolutePath()));
        }
        return fStore;
    }

    /**
     *
     * @param mimeType
     * @param buffer
     * @return
     * @throws StorageException
     */
    public File storeInFile(String mimeType, byte[] buffer)
            throws StorageException {
        return storeFile(S_IN_PREFIX, getSuffixBYMimeType(mimeType), buffer);
    }

    /**
     *
     * @param mimeType
     * @param fIn
     * @return
     * @throws StorageException
     */
    public File storeInFile(String mimeType, File fIn)
            throws StorageException {
        if (fIn.exists()) {
            throw new StorageException(format(
                    "File in message: '%s' not exists ", fIn.getAbsolutePath()));
        }
        File fStore = getNewStorageFile(S_IN_PREFIX, getSuffixBYMimeType(
                mimeType));

        try {
            copyFile(fIn, fStore);
        } catch (IOException ex) {
            throw new StorageException(format(
                    "Error occurred while copying file: '%s' to file: %s", fIn.
                    getAbsolutePath(), fStore.getAbsolutePath()));
        }

        return fStore;
    }

    /**
     *
     * @param mimeType
     * @param is
     * @return
     * @throws StorageException
     */
    public File storeInFile(String mimeType, InputStream is)
            throws StorageException {
        return storeFile(S_IN_PREFIX, getSuffixBYMimeType(mimeType), is);
    }

    /**
     *
     * @param mimeType
     * @param buffer
     * @return
     * @throws StorageException
     */
    public File storeOutFile(String mimeType, byte[] buffer)
            throws StorageException {
        return storeFile(S_OUT_PREFIX, getSuffixBYMimeType(mimeType), buffer);
    }

    /**
     *
     * @param mimeType
     * @param fIn
     * @return
     * @throws StorageException
     */
    public File storeOutFile(String mimeType, File fIn)
            throws StorageException {
        if (!fIn.exists()) {
            throw new StorageException(format(
                    "File in message: '%s' not exists ", fIn.getAbsolutePath()));
        }
        File fStore = getNewStorageFile(S_OUT_PREFIX, getSuffixBYMimeType(
                mimeType));

        try {
            copyFile(fIn, fStore);
        } catch (IOException ex) {
            throw new StorageException(format(
                    "Error occurred while copying file: '%s' to file: %s", fIn.
                    getAbsolutePath(), fStore.getAbsolutePath()));
        }

        return fStore;
    }

}
