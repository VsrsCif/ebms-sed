/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.email;

import java.io.StringWriter;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EVIPMimeMessage extends MimeMessage {

    
    private String mStrMessageId = null;

    public EVIPMimeMessage(Session session, String messageId) {
        super(session);
        this.session = session;
        mStrMessageId = messageId;
    }

    @Override
    public void updateMessageID() throws MessagingException {
        setHeader("Message-ID", "<" + getUniqueMessageIDValue(session) + ">");
    }

    public String getUniqueMessageIDValue(Session ssn) {
        String suffix = null;

        InternetAddress addr = InternetAddress.getLocalAddress(ssn);
        if (addr != null) {
            suffix = addr.getAddress();
        } else {
            suffix = "evip@sodisce.si"; // worst-case default
        }

        StringWriter s = new StringWriter();

        // Unique string is <hashcode>.<id>.<currentTime>.JavaMail.<suffix>
        //s.append(s.hashCode()+"");
        //s.append('.');
        s.append(mStrMessageId).append('.');
        //s.append(""+System.currentTimeMillis()).append('.');
        s.append("EVIPMail.");
        s.append(suffix);
        return s.toString();
    }

}
