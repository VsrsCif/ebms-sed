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
public class EmailAttachmentData{
    String mstrFileName;
    String mstrMimeType;
    File mFile;

    public EmailAttachmentData(String mstrFileName, String mstrMimeType, File mFile) {
        this.mstrFileName = mstrFileName;
        this.mstrMimeType = mstrMimeType;
        this.mFile = mFile;
    }

    
    public String getFileName() {
        return mstrFileName;
    }

    public void setFileName(String mstrFileName) {
        this.mstrFileName = mstrFileName;
    }

    public String getMimeType() {
        return mstrMimeType;
    }

    public void setMimeType(String mstrMimeType) {
        this.mstrMimeType = mstrMimeType;
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File mFile) {
        this.mFile = mFile;
    }
    
}
