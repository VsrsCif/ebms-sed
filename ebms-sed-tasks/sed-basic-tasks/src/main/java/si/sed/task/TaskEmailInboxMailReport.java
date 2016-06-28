/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package si.sed.task;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.ejb.Local;
import javax.ejb.Stateless;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.sed.ebms.cron.SEDTaskExecution;
import org.sed.ebms.cron.SEDTaskType;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.TaskExecutionInterface;
import si.sed.commons.interfaces.exception.TaskException;
import si.sed.task.filter.InMailFilter;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(TaskExecutionInterface.class)
public class TaskEmailInboxMailReport extends TaskEmailReport {

  // public static String KEY_ListLine = "mail.data";
  /**
     *
     */
  public static String KEY_MAIL_STATUS = "mail.status";

  /**
     *
     */
  public static String KEY_NoMail = "skip.on.NoMail";

  /**
     *
     */
  public static String KEY_OnlyNew = "new.only";

  /**
   *
   * @param p
   * @param sw
   * @return
   */
  @Override
  public String generateMailReport(Properties p, StringWriter sw) throws TaskException {

    String sedbox = null;
    boolean bNewOnly = true;
    boolean bSkipNoMail = true;
    String mailStatus = "RECEIVED";

    if (!p.containsKey(KEY_SEDBOX)) {
      throw new TaskException(TaskException.TaskExceptionCode.InitException,
          "Missing parameter:  '" + KEY_SEDBOX + "'!");
    } else {
      sedbox = p.getProperty(KEY_SEDBOX);
    }

    if (!p.containsKey(KEY_NoMail)) {
      throw new TaskException(TaskException.TaskExceptionCode.InitException,
          "Missing parameter:  '" + KEY_NoMail + "'!");
    } else {
      bSkipNoMail = p.getProperty(KEY_NoMail).trim().equalsIgnoreCase("true");
    }
    if (!p.containsKey(KEY_OnlyNew)) {
      throw new TaskException(TaskException.TaskExceptionCode.InitException,
          "Missing parameter:  '" + KEY_OnlyNew + "'!");
    } else {
      bNewOnly = p.getProperty(KEY_OnlyNew).trim().equalsIgnoreCase("true");
    }
    if (!p.containsKey(KEY_MAIL_STATUS)) {
      throw new TaskException(TaskException.TaskExceptionCode.InitException,
          "Missing parameter:  '" + KEY_MAIL_STATUS + "'!");
    } else {
      mailStatus = p.getProperty(KEY_MAIL_STATUS);
    }

    sw.append("Got status report ");
    Date recTo = null;
    InMailFilter miFilter = new InMailFilter();
    miFilter.setStatus(mailStatus);
    if (bNewOnly) {
      SEDTaskExecution te = null;
      try {
        te = mdao.getLastSuccesfullTaskExecution(getTaskDefinition().getType());
        if (te != null) {
          recTo = te.getStartTimestamp();
        }
      } catch (StorageException ex) {
        LOG.logWarn(0, "ERROR reading task execution", ex);
      }
      if (te != null) {
        miFilter.setReceivedDateFrom(recTo);
        miFilter.setReceivedDateTo(Calendar.getInstance().getTime());
      }
    }

    List<MSHInMail> lstInMail = mdao.getDataList(MSHInMail.class, -1, -1, "Id", "ASC", miFilter);
    if (lstInMail.isEmpty() && bSkipNoMail) {
      sw.append("In mail size: " + lstInMail.size() + "' nothing to submit - !");
      return null;
    }

    StringWriter swBody = new StringWriter();
    swBody.append("Dohodna pošta za predal: ");
    swBody.append(sedbox);
    swBody.append(System.lineSeparator());
    swBody.append("Date: ");
    swBody.append(SDF_DD_MM_YYY_HH_MI.format(Calendar.getInstance().getTime()));
    swBody.append(System.lineSeparator());
    swBody.append(System.lineSeparator());
    swBody.append("Seznam dohodne pošte za prevzem (do 500 pošiljk): ");
    swBody.append(System.lineSeparator());
    swBody.append("St pošiljk: '" + lstInMail.size() + "' ");
    swBody.append(System.lineSeparator());
    sw.append("In mail size: " + lstInMail.size());
    swBody.append("st., id, dat  prejema, transakcija ID, Storitev, Akcija, Pošiljatelj, Opis");
    swBody.append(System.lineSeparator());
    int iVal = 1;
    for (MSHInMail im : lstInMail) {
      swBody.append((iVal++) + "., ");
      swBody.append(im.getId().toString() + ", ");
      swBody.append(SDF_DD_MM_YYY_HH_MI.format(im.getReceivedDate()) + ", ");
      swBody.append(im.getConversationId() + ", ");
      swBody.append(im.getService() + ", ");
      swBody.append(im.getAction() + ", ");
      swBody.append(im.getSenderEBox() + ", ");
      swBody.append(im.getSenderName() + ", ");
      swBody.append(im.getSubject());
      swBody.append(System.lineSeparator());
    }
    return swBody.toString();
  }

  /**
   *
   * @return
   */
  @Override
  public SEDTaskType getTaskDefinition() {
    SEDTaskType tt = super.getMailTaskDefinition();
    tt.setType("inboxreport");
    tt.setName("Inbox report");
    tt.setDescription("Incomings mail list from sed box");

    tt.getSEDTaskTypeProperties().add(
        createTTProperty(KEY_NoMail, "Supress if not Mail ", true, "boolean", null, null));
    tt.getSEDTaskTypeProperties().add(
        createTTProperty(KEY_OnlyNew, "Only if new mail ", true, "boolean", null, null));
    // tt.getSEDTaskTypeProperties().add(createTTProperty(KEY_ListLine, "List line"));

    String strLst = "";
    for (SEDInboxMailStatus c : SEDInboxMailStatus.values()) {
      strLst = (strLst.isEmpty() ? "" : ",") + c.getValue();
    }

    tt.getSEDTaskTypeProperties().add(
        createTTProperty(KEY_MAIL_STATUS, "Status list", true, "list", null, strLst));

    return tt;
  }

}
