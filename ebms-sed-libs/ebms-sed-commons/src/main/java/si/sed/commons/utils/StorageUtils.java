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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import si.sed.commons.MimeValues;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.exception.StorageException;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class StorageUtils {

    private static final SimpleDateFormat msdfFolderDateFormat = new SimpleDateFormat("yyyyMMdd");
    private static final String S_ROOT_FOLDER = "storage";

    public static final String S_OUT_PREFIX = "out_";
    public static final String S_IN_PREFIX = "in_";

    public File storeInFile(String mimeType, byte[] buffer) throws StorageException {
        return storeFile(S_IN_PREFIX, MimeValues.getSuffixBYMimeType(mimeType), buffer);
    }

    public File storeInFile(String mimeType, File fIn) throws StorageException {
        if (fIn.exists()) {
            throw new StorageException(String.format("File in message: '%s' not exists ", fIn.getAbsolutePath()));
        }
        File fStore = getNewStorageFile(S_IN_PREFIX, MimeValues.getSuffixBYMimeType(mimeType));

        try {
            copyFile(fIn, fStore);
        } catch (IOException ex) {
            throw new StorageException(String.format("Error occurred while copying file: '%s' to file: %s", fIn.getAbsolutePath(), fStore.getAbsolutePath()));
        }

        return fStore;
    }

    public File storeInFile(String mimeType, InputStream is) throws StorageException {
        return storeFile(S_IN_PREFIX, MimeValues.getSuffixBYMimeType(mimeType), is);
    }

    public File storeOutFile(String mimeType, byte[] buffer) throws StorageException {
        return storeFile(S_OUT_PREFIX, MimeValues.getSuffixBYMimeType(mimeType), buffer);
    }

    public File storeOutFile(String mimeType, File fIn) throws StorageException {
        if (fIn.exists()) {
            throw new StorageException(String.format("File in message: '%s' not exists ", fIn.getAbsolutePath()));
        }
        File fStore = getNewStorageFile(S_OUT_PREFIX, MimeValues.getSuffixBYMimeType(mimeType));

        try {
            copyFile(fIn, fStore);
        } catch (IOException ex) {
            throw new StorageException(String.format("Error occurred while copying file: '%s' to file: %s", fIn.getAbsolutePath(), fStore.getAbsolutePath()));
        }

        return fStore;
    }

    public File storeFile(String prefix, String suffix, byte[] buffer) throws StorageException {
        return storeFile(prefix, suffix, new ByteArrayInputStream(buffer));
    }

    public File storeFile(String prefix, String suffix, InputStream inStream) throws StorageException {
        File fStore = getNewStorageFile(suffix, prefix);

        try (FileOutputStream fos = new FileOutputStream(fStore)) {

            byte[] buffer = new byte[1024];
            int len = inStream.read(buffer);
            while (len != -1) {
                fos.write(buffer, 0, len);
                len = inStream.read(buffer);
            }

        } catch (IOException ex) {
            throw new StorageException(String.format("Error occurred while writing to file: '%s'", fStore.getAbsolutePath()));
        };
        return fStore;
    }

    public void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        try (FileChannel source = new FileInputStream(sourceFile).getChannel();
                FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
    }

    public static File getNewStorageFile(String suffix, String prefix) throws StorageException {
        File fStore;
        try {
            fStore = File.createTempFile(prefix, "." + suffix, currentStorageFolder());
        } catch (IOException ex) {
            throw new StorageException("Error occurred while creating storage file", ex);
        }
        return fStore;
    }

    public static synchronized File currentStorageFolder() throws StorageException {

        File f = new File(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator + S_ROOT_FOLDER + File.separator + currentStorageFolderName());
        if (!f.exists() && !f.mkdirs()) {
            throw new StorageException(String.format("Error occurred while creating storage folder: '%s'", f.getAbsolutePath()));
        }
        return f;
    }

    public static synchronized File getFile(String storagePath) throws StorageException {
        File f = new File(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator + storagePath);
        return f;
    }

    public static synchronized String currentStorageFolderName() {
        return msdfFolderDateFormat.format(Calendar.getInstance().getTime());
    }

    public static String getRelativePath(File path) {
        File hdir = new File(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR));
        if (path.getAbsolutePath().startsWith(hdir.getAbsolutePath())) {
            String rp = path.getAbsolutePath().substring(hdir.getAbsolutePath().length());
            rp = rp.startsWith(File.separator) ? rp.substring(1) : rp;
            return rp;
        }

        String base = System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR);

        String[] basePaths = base.split(File.separator);
        String[] otherPaths = path.getParent().split(File.separator);
        int n = 0;
        for (; n < basePaths.length && n < otherPaths.length; n++) {
            if (basePaths[n].equals(otherPaths[n]) == false) {
                break;
            }
        }
        StringBuilder tmp = new StringBuilder();
        for (int m = n; m < basePaths.length - 1; m++) {
            tmp.append("..");
            tmp.append(File.separator);
        }

        for (int m = n; m < otherPaths.length; m++) {
            tmp.append(otherPaths[m]);
            tmp.append(File.separator);
        }
        // add filename
        tmp.append(path.getName());
        System.out.println("GET relative path:" + tmp.toString());
        return tmp.toString();
    }

    public byte[] getByteArray(String storagePath) throws StorageException {
        byte[] bin = null;
        File f = getFile(storagePath);

        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                bin = new byte[fis.available()];
                fis.read(bin);
            } catch (IOException ex) {
                throw new StorageException("Error occurred while creating storage file", ex);
            }
        }
        return bin;
    }

}
