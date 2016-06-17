package si.sed.commons.email;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sluzba
 */
public class EmailData {

    List<EmailAttachmentData> mlstAttachments = new ArrayList<EmailAttachmentData>();
    String mstrBody;
    String mstrEmailAddresses;
    String mstrEmailCCAddresses;
    String mstrEmailSenderAddress;
    String mstrSubject;

    public EmailData(String mstrEmailAddresses, String mstrEmailCCAddresses, String mstrSubject, String mstrBody) {
        this.mstrEmailAddresses = mstrEmailAddresses;
        this.mstrEmailCCAddresses = mstrEmailCCAddresses;
        this.mstrSubject = mstrSubject;
        this.mstrBody = mstrBody;
    }

    public List<EmailAttachmentData> getAttachments() {
        return mlstAttachments;
    }

    public String getBody() {
        return mstrBody;
    }

    public String getEmailAddresses() {
        return mstrEmailAddresses;
    }

    public String getEmailCCAddresses() {
        return mstrEmailCCAddresses;
    }

    public String getEmailSenderAddress() {
        return mstrEmailSenderAddress;
    }

    public String getSubject() {
        return mstrSubject;
    }

    public void setBody(String mstrBody) {
        this.mstrBody = mstrBody;
    }

    public void setEmailAddresses(String mstrEmailAddresses) {
        this.mstrEmailAddresses = mstrEmailAddresses;
    }

    public void setEmailCCAddresses(String mstrEmailCCAddresses) {
        this.mstrEmailCCAddresses = mstrEmailCCAddresses;
    }

    public void setEmailSenderAddress(String mstrEmailSenderAddress) {
        this.mstrEmailSenderAddress = mstrEmailSenderAddress;
    }

    public void setSubject(String mstrSubject) {
        this.mstrSubject = mstrSubject;
    }

    @Override
    public String toString() {
        return "EmailData{" + "to=" + mstrEmailAddresses + ", cc=" + mstrEmailCCAddresses + ", subject=" + mstrSubject + ", Attachments size=" + mlstAttachments.size() + '}';
    }

}
