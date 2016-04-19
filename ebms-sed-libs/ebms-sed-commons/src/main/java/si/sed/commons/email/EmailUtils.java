/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.email;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import org.apache.log4j.Logger;


/**
 *
 * @author sluzba
 */
public class EmailUtils {

    private static final String SMAIL_JNDI = "sed-mail";
    
    private static final String S_MIME_TXT = "text/plain";
    private static final String S_OUTMAIL_ADDRESS = "nobody@sodisce.si";
    //do no use DCLogger -> cyclic dependecy.. 
    private static final Logger mlgLogger = Logger.getLogger(EmailUtils.class.getName());

    static CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder();

    public void sendMailMessage(String emailAddress, String subject, String body) throws MessagingException, NamingException, IOException {
        
        sendMailMessage(new EmailData(emailAddress,null, subject, body));
       
    }

    public void sendMailMessage(EmailData eml) throws MessagingException, NamingException, IOException {
        mlgLogger.info("EmailUtils.sendMailMessage: " + eml.toString());
        long l = Calendar.getInstance().getTimeInMillis();
        Session session = (Session) PortableRemoteObject.narrow(new InitialContext().lookup(SMAIL_JNDI), Session.class);
        String emailid = "sed-" + UUID.randomUUID().toString();
        MimeMessage m = new EVIPMimeMessage(session, emailid);

        m.setContentID(emailid);
        m.setContentID(emailid);
        m.addHeader("sed-id", emailid);

        Address[] to = getAddresses(eml.getEmailAddresses());
        m.setFrom(new InternetAddress(S_OUTMAIL_ADDRESS));
        m.setSender(new InternetAddress(S_OUTMAIL_ADDRESS));
        m.setRecipients(javax.mail.Message.RecipientType.TO, to);
        if (eml.getEmailCCAddresses() != null) {
            Address[] toCC = getAddresses(eml.getEmailCCAddresses());
            m.setRecipients(javax.mail.Message.RecipientType.CC, toCC);
        }

        String subject = eml.getSubject();

        m.setSubject(subject.length() > 160 ? subject.substring(0, 160) : subject);
        m.setSentDate(new Date());
        Multipart multipart = new MimeMultipart();
        StringWriter swrBody = new StringWriter();
        swrBody.append(eml.getBody());

        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setHeader("Content-Type", S_MIME_TXT + "; charset=\"utf-8\"");
        messageBodyPart.setContent(swrBody.toString(), S_MIME_TXT + "; charset=\"utf-8\"");
        multipart.addBodyPart(messageBodyPart);
        
         for (EmailAttachmentData d : eml.getAttachments()) {
            mlgLogger.info("EmailUtils.sendMailMessage: - add attachments doc  " + d.getFile().getAbsolutePath());
            MimeBodyPart messageattachmentPart = new MimeBodyPart();
            DataSource source = new FileDataSource(d.getFile());
            messageattachmentPart.setDataHandler(new DataHandler(source));
            messageattachmentPart.setFileName(MimeUtility.encodeText(d.getFileName()));
            multipart.addBodyPart(messageattachmentPart);
        }
        // Put parts in message
        m.setContent(multipart);

        Transport.send(m);
        mlgLogger.info("EmailUtils.sendMailMessage: " + eml.toString() + " - END ( " + (Calendar.getInstance().getTimeInMillis() - l) + " ms)");

    }

    private Address[] getAddresses(String strAddrString) throws AddressException, UnsupportedEncodingException {
        Address[] to = null;
        if (strAddrString == null || strAddrString.trim().isEmpty()) {
            return to;
        }
        String[] lstAdr = strAddrString.split(",");
        to = new InternetAddress[lstAdr.length];
        for (int i = 0; i < lstAdr.length; i++) {
            String adr = lstAdr[i].trim();
            if (asciiEncoder.canEncode(adr)) {
                to[i] = new InternetAddress(adr);
            } else { // suppose non asci char in name !!
                String[] lst = adr.split("<");
                if (lst.length == 2) {
                    to[i] = new InternetAddress(lst[1].replaceAll(">", ""), lst[0].trim(), "UTF-8");
                }

            }
        }

        return to;

    }
}
