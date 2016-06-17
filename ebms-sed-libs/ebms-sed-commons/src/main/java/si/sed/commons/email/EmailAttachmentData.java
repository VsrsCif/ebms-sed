/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.email;

import java.io.File;

/**
 *
 * @author sluzba
 */
public class EmailAttachmentData {

    File mFile;

    String mstrFileName;
    String mstrMimeType;

    public EmailAttachmentData(String mstrFileName, String mstrMimeType, File mFile) {
        this.mstrFileName = mstrFileName;
        this.mstrMimeType = mstrMimeType;
        this.mFile = mFile;
    }

    public File getFile() {
        return mFile;
    }

    public String getFileName() {
        return mstrFileName;
    }

    public String getMimeType() {
        return mstrMimeType;
    }

    public void setFile(File mFile) {
        this.mFile = mFile;
    }

    public void setFileName(String mstrFileName) {
        this.mstrFileName = mstrFileName;
    }

    public void setMimeType(String mstrMimeType) {
        this.mstrMimeType = mstrMimeType;
    }

}
