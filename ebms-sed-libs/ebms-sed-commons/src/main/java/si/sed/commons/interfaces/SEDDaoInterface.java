/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.interfaces;

import java.math.BigInteger;
import java.util.List;
import javax.ejb.Local;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.sed.ebms.cron.SEDTaskExecution;
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.user.SEDUser;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDOutboxMailStatus;

/**
 *
 * @author sluzba
 */
@Local
public interface SEDDaoInterface {

    <T> List<T> getDataList(Class<T> type, int startingAt, int maxResultCnt, String sortField, String sortOrder, Object filters);
    <T> long getDataListCount(Class<T> type, Object filters);
    <T> List<T> getMailEventList(Class<T> type, BigInteger mailId);
    <T> T getMailById(Class<T> type, BigInteger mailId);
    SEDUser getSEDUser(String username);
    void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc);
    void setStatusToInMail(MSHInMail mail, SEDInboxMailStatus status, String desc);
    List<MSHInMail> getInMailConvIdAndAction(String action, String convId);
    void updateInMail(MSHInMail mail, String statusDesc);
    void serializeOutMail(MSHOutMail mail, String userID, String applicationId, String pmodeId);
    void serializeInMail(MSHInMail mail);
    SEDBox getSedBoxByName(String sbox);

    boolean addExecutionTask(SEDTaskExecution ad);
    boolean updateExecutionTask(SEDTaskExecution ad);
    <T> void removeMail(Class<T> type, List<T> lst);
    <T,E> void removeMail(Class<T> type, Class<E> typeEvent, BigInteger bi);
}
