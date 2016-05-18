/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.task;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Properties;
import javax.ejb.EJB;
import javax.mail.MessagingException;
import javax.naming.NamingException;
import org.sed.ebms.cron.SEDTaskType;
import org.sed.ebms.cron.SEDTaskTypeProperty;
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
public abstract class TaskEmailReport implements TaskExecutionInterface {

    public static String KEY_EMAIL_FROM = "email.from";
    public static String KEY_EMAIL_SUBJECT = "email.subject";
    public static String KEY_EMAIL_TO = "email.to";
    public static String KEY_MAIL_CONFIG_JNDI = "mail.config.jndi";
    public static String KEY_SEDBOX = "sedbox";

    protected static final SEDLogger LOG = new SEDLogger(TaskEmailStatusReport.class);
    static final SimpleDateFormat SDF_DD_MM_YYY_HH_MI = new SimpleDateFormat("dd.MM.yyyy HH24:mm");
    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mdao;
    @EJB(mappedName = SEDJNDI.JNDI_SEDREPORTS)
    SEDReportInterface mdaoReports;

    public EmailData validateMailParameters(Properties p) throws TaskException {

        String emailTo = null;
        String emailFrom = null;
        String emailSubject = null;

        if (!p.containsKey(KEY_EMAIL_TO)) {
            throw new TaskException(TaskException.TaskExceptionCode.InitException, "Missing parameter:  '" + KEY_EMAIL_TO + "'!");
        } else {
            emailTo = p.getProperty(KEY_EMAIL_TO);
        }
        if (!p.containsKey(KEY_EMAIL_FROM)) {
            throw new TaskException(TaskException.TaskExceptionCode.InitException, "Missing parameter:  '" + KEY_EMAIL_FROM + "'!");
        } else {
            emailFrom = p.getProperty(KEY_EMAIL_FROM);
        }
        if (!p.containsKey(KEY_EMAIL_SUBJECT)) {
            throw new TaskException(TaskException.TaskExceptionCode.InitException, "Missing parameter:  '" + KEY_EMAIL_SUBJECT + "'!");
        } else {
            emailSubject = p.getProperty(KEY_EMAIL_SUBJECT);
        }

        EmailData emd = new EmailData(emailTo, null, emailSubject, null);
        emd.setEmailSenderAddress(emailFrom);
        return emd;

    }

    @Override
    public String executeTask(Properties p) throws TaskException {
        long l = LOG.logStart();
        EmailUtils memailUtil = new EmailUtils();
        StringWriter sw = new StringWriter();
        sw.append("Start report task: ");

        String smtpConf = null;
        if (p.containsKey(KEY_MAIL_CONFIG_JNDI)) {
            smtpConf = p.getProperty(KEY_MAIL_CONFIG_JNDI);
        }
        if (smtpConf == null || smtpConf.trim().isEmpty()) {
            smtpConf = "java:jboss/mail/Default";
        }

        EmailData ed = validateMailParameters(p);
        String strBody = generateMailReport(p, sw);
        if (strBody != null) {

            ed.setBody(strBody);

            try {
                sw.append("Submit mail\n");
                memailUtil.sendMailMessage(ed, smtpConf);
            } catch (MessagingException | NamingException | IOException ex) {
                LOG.logError(l, "Error submitting report", ex);
                throw new TaskException(TaskException.TaskExceptionCode.ProcessException, "Error submitting report: " + ex.getMessage(), ex);
            }
        } else {
            sw.append("Mail not submitted - nothing to submit\n");
        }
        return sw.toString();
    }

    abstract String generateMailReport(Properties p, StringWriter sw) throws TaskException;

    /*
    public Properties getMailProperties() {
        Properties p = new Properties();
        p.setProperty(KEY_SEDBOX, "Sedbox");
        p.setProperty(KEY_EMAIL_TO, "Email address to.");
        p.setProperty(KEY_EMAIL_FROM, "Email address from");
        p.setProperty(KEY_EMAIL_SUBJECT, "Email subject");
        p.setProperty(KEY_MAIL_CONFIG_JNDI, "Mail config jndi(def: java:jboss/mail/Default)");
        return p;
    }*/
    public SEDTaskType getMailTaskDefinition() {
        SEDTaskType tt = new SEDTaskType();
        tt.setType("");
        tt.setName("");
        tt.setDescription("");
        tt.getSEDTaskTypeProperties().add(createTTProperty(KEY_SEDBOX, "SED-BOX"));
        tt.getSEDTaskTypeProperties().add(createTTProperty(KEY_EMAIL_TO, "Receiver email addresses, separated by comma."));
        tt.getSEDTaskTypeProperties().add(createTTProperty(KEY_EMAIL_FROM, "Sender email address."));
        tt.getSEDTaskTypeProperties().add(createTTProperty(KEY_EMAIL_SUBJECT, "EMail subject."));
        tt.getSEDTaskTypeProperties().add(createTTProperty(KEY_MAIL_CONFIG_JNDI, "Mail config jndi(def: java:jboss/mail/Default)"));
        return tt;
    }

    protected SEDTaskTypeProperty createTTProperty(String key, String desc, boolean mandatory, String type, String valFormat, String valList) {
        SEDTaskTypeProperty ttp = new SEDTaskTypeProperty();
        ttp.setKey(key);
        ttp.setDescription(desc);
        ttp.setMandatory(mandatory);
        ttp.setType(type);
        ttp.setValueFormat(valFormat);
        ttp.setValueList(valList);
        return ttp;
    }

    protected SEDTaskTypeProperty createTTProperty(String key, String desc) {
        return createTTProperty(key, desc, true, "string", null, null);
    }

}
