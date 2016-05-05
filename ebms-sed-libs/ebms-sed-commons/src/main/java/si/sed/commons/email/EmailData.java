/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.email;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sluzba
 */
public class EmailData {
    String mstrEmailSenderAddress;
    String mstrEmailAddresses;
    String mstrEmailCCAddresses;
    String mstrSubject;
    String mstrBody;
    List<EmailAttachmentData> mlstAttachments = new ArrayList<EmailAttachmentData>();

    public EmailData(String mstrEmailAddresses, String mstrEmailCCAddresses, String mstrSubject, String mstrBody) {
        this.mstrEmailAddresses = mstrEmailAddresses;
        this.mstrEmailCCAddresses = mstrEmailCCAddresses;
        this.mstrSubject = mstrSubject;
        this.mstrBody = mstrBody;
    }
    
    

    public String getEmailAddresses() {
        return mstrEmailAddresses;
    }

    public void setEmailAddresses(String mstrEmailAddresses) {
        this.mstrEmailAddresses = mstrEmailAddresses;
    }

    public String getEmailCCAddresses() {
        return mstrEmailCCAddresses;
    }

    public void setEmailCCAddresses(String mstrEmailCCAddresses) {
        this.mstrEmailCCAddresses = mstrEmailCCAddresses;
    }

    public String getEmailSenderAddress() {
        return mstrEmailSenderAddress;
    }

    public void setEmailSenderAddress(String mstrEmailSenderAddress) {
        this.mstrEmailSenderAddress = mstrEmailSenderAddress;
    }
    
    

    public String getSubject() {
        return mstrSubject;
    }

    public void setSubject(String mstrSubject) {
        this.mstrSubject = mstrSubject;
    }

    public String getBody() {
        return mstrBody;
    }

    public void setBody(String mstrBody) {
        this.mstrBody = mstrBody;
    }

    public List<EmailAttachmentData> getAttachments() {
        return mlstAttachments;
    }

    @Override
    public String toString() {
        return "EmailData{" + "to=" + mstrEmailAddresses + ", cc=" + mstrEmailCCAddresses + ", subject=" + mstrSubject + ", Attachments size=" + mlstAttachments.size() + '}';
    }

    
    
    
}
