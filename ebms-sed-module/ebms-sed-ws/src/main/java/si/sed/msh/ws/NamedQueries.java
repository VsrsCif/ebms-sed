/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.ws;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class NamedQueries {
    public static final String SED_NQ_OUTMAIL_getByMessageIdAndSenderBox ="org.sed.ebms.outbox.mail.OutMail.getByMessageIdAndSenderBox";
    public static final String SED_NQ_INMAIL_GET_BY_ID_AND_RECBOX ="org.sed.ebms.inbox.mail.InMail.getByIdAndReceiverBox";
    
    public static final String SED_NQ_OUTMAIL_GET_LIST ="org.sed.ebms.outbox.mail.OutMail.getList";
    public static final String SED_NQ_INMAIL_GET_LIST ="org.sed.ebms.inbox.mail.InMail.getList";
    
    public static final String SED_NQ_INMAIL_GET_EVENTS ="org.sed.ebms.inbox.event.InEvent.getList";
    public static final String SED_NQ_OUTMAIL_GET_EVENTS ="org.sed.ebms.outbox.event.OutEvent.getList";
       
    
    
    public static final String NQ_PARAM_MAIL_ID = "mailId";
    public static final String NQ_PARAM_SENDER_MAIL_ID = "senderMessageId";
    public static final String NQ_PARAM_RECEIVER_EBOX = "receiverEBox";
    public static final String NQ_PARAM_SENDER_EBOX = "senderEBox";
    
    
}
