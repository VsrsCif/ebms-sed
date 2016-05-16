/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.task;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Properties;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.mail.MessagingException;
import javax.naming.NamingException;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.sed.ebms.report.SEDReportBoxStatus;
import org.sed.ebms.report.Status;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDJNDI;
import si.sed.commons.email.EmailData;
import si.sed.commons.email.EmailUtils;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.interfaces.SEDReportInterface;
import si.sed.commons.interfaces.TaskExecutionInterface;
import si.sed.commons.interfaces.exception.TaskException;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(TaskExecutionInterface.class)
public class TaskEmailReport implements TaskExecutionInterface {

    private static final SEDLogger LOG = new SEDLogger(TaskEmailReport.class);
    static final SimpleDateFormat SDF_DD_MM_YYY_HH_MI = new SimpleDateFormat("dd.MM.yyyy HH24:mm");

    public static String KEY_SEDBOX = "sedbox";
    public static String KEY_EMAIL_TO = "email.to";
    public static String KEY_EMAIL_FROM = "email.from";
    public static String KEY_EMAIL_SUBJECT = "email.subject";
    public static String KEY_MAIL_CONFIG_JNDI = "mail.config.jndi";
    
    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mdao;
    
    @EJB(mappedName = SEDJNDI.JNDI_SEDREPORTS)
    SEDReportInterface mdaoReports;
    
    

    @Override
    public String executeTask(Properties p) throws TaskException {

        long l = LOG.logStart();
        EmailUtils memailUtil = new EmailUtils();

        StringWriter sw = new StringWriter();
        sw.append("Start report task: ");

        String sedbox = null;
        String emailTo = null;
        String emailFrom = null;
        String emailSubject = null;
        String smtpConf = null;

        if (!p.containsKey(KEY_SEDBOX)) {
            throw new TaskException(TaskException.TaskExceptionCode.InitException,
                    "Missing parameter:  '" + KEY_SEDBOX + "'!");
        } else {
            sedbox = p.getProperty(KEY_SEDBOX);
        }

        if (!p.containsKey(KEY_EMAIL_TO)) {
            throw new TaskException(TaskException.TaskExceptionCode.InitException,
                    "Missing parameter:  '" + KEY_EMAIL_TO + "'!");
        } else {
            emailTo = p.getProperty(KEY_EMAIL_TO);
        }

        if (!p.containsKey(KEY_EMAIL_FROM)) {
            throw new TaskException(TaskException.TaskExceptionCode.InitException,
                    "Missing parameter:  '" + KEY_EMAIL_FROM + "'!");
        } else {
            emailFrom = p.getProperty(KEY_EMAIL_FROM);
        }

        if (!p.containsKey(KEY_EMAIL_SUBJECT)) {
            throw new TaskException(TaskException.TaskExceptionCode.InitException,
                    "Missing parameter:  '" + KEY_EMAIL_SUBJECT + "'!");
        } else {
            emailSubject = p.getProperty(KEY_EMAIL_SUBJECT) + " " +sedbox ;
        }
        
        if (p.containsKey(KEY_MAIL_CONFIG_JNDI)) {           
            smtpConf = p.getProperty(KEY_MAIL_CONFIG_JNDI);            
        }
        
        if ( smtpConf== null || smtpConf.trim().isEmpty()) {
            smtpConf = "java:jboss/mail/Default";
        }
        SEDReportBoxStatus sr =  mdaoReports.getStatusReport(sedbox);
        sw.append("Got status report " );
        MSHInMail mi = new MSHInMail();
        mi.setStatus(SEDInboxMailStatus.RECEIVED.getValue());
        List<MSHInMail> lstInMail = mdao.getDataList(MSHInMail.class, 0,500, "Id", "ASC", mi);
        
        StringWriter swBody = new StringWriter();
        swBody.append("SED-Predal eBOX: " );
        swBody.append(sr.getSedbox());
        swBody.append("\nDate: " );
        swBody.append(SDF_DD_MM_YYY_HH_MI.format(sr.getReportDate() ) );
        
        if (sr.getInMail()!=null && !sr.getInMail().getStatuses().isEmpty() ) {
            sw.append("in mail: " + sr.getInMail().getStatuses().size() );
            swBody.append("\n\nStatusi dohodne pošte: \n");
            for (Status s:  sr.getInMail().getStatuses()){
                swBody.append(String.format("\t%s: %d\n", s.getStatus(), s.getCount()) );
            }
        } else {
            swBody.append("\n\nZa predal '"+sedbox+"' ni dohodne pošte \n");
        }
        if (sr.getOutMail()!=null && !sr.getOutMail().getStatuses().isEmpty() ) {
            sw.append(", out mail: " + sr.getInMail().getStatuses().size() );
            swBody.append("\n\nStatusi izhodne pošte: \n");
            for (Status s:  sr.getOutMail().getStatuses()){
                swBody.append(String.format("\t%s: %d\n", s.getStatus(), s.getCount()) );
            }
        } else {
            swBody.append("\n\nZa predal '"+sedbox+"' ni izhodne pošte \n");
        }
        
        swBody.append("\n\nSeznam dohodne pošte za prevzem (do 500 pošiljk): \n");
        swBody.append("St pošiljk: '"+lstInMail.size()+"' \n");
        sw.append("In mail size: " +lstInMail.size() );
        swBody.append("st., id, dat  prejema, transakcija ID, Storitev, Akcija, Pošiljatelj, Opis\n");
        int iVal = 1;
        for (MSHInMail im: lstInMail){
            
            swBody.append((iVal++) +"., ");
            swBody.append(im.getId().toString() + ", ");
            swBody.append(SDF_DD_MM_YYY_HH_MI.format(im.getReceivedDate()) + ", ");
            swBody.append(im.getConversationId()+ ", ");
            swBody.append(im.getService() + ", ");
            swBody.append(im.getAction()+ ", ");
            swBody.append(im.getSenderEBox()+ ", ");
            swBody.append(im.getSenderName()+ ", ");
            swBody.append(im.getSubject());
            swBody.append("\n");
        }
        
        
        
        EmailData ed = new EmailData(emailTo, null, emailSubject, swBody.toString());
        ed.setEmailSenderAddress(emailFrom);
        
        try {
            memailUtil.sendMailMessage(ed, smtpConf);
        } catch (MessagingException | NamingException | IOException ex) {
            LOG.logError(l, "Error submitting report", ex);
             throw new TaskException(TaskException.TaskExceptionCode.ProcessException,
                    "Error submitting report: " + ex.getMessage(), ex);
        }

        return sw.toString();
    }

    @Override
    public String getType() {
        return "sedboxreport";
    }

    @Override
    public String getName() {
        return "Report";
    }

    @Override
    public String getDesc() {
        return "Incoming outcomming mail report from sed box";
    }

    @Override
    public Properties getProperties() {
        Properties p = new Properties();
        p.setProperty(KEY_SEDBOX, "Sedbox");
        p.setProperty(KEY_EMAIL_TO, "Email address to.");
        p.setProperty(KEY_EMAIL_FROM, "Email address from");
        p.setProperty(KEY_EMAIL_SUBJECT, "Email subject");
        p.setProperty(KEY_MAIL_CONFIG_JNDI, "Mail config jndi(def: java:jboss/mail/Default)");

        return p;
    }
}
