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
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.exception.StorageException;

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

    List<MSHInMail> getInMailConvIdAndAction(String action, String convId);

    void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc) throws StorageException;

    void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc, String userID, String applicationId) throws StorageException;

    void setStatusToInMail(MSHInMail mail, SEDInboxMailStatus status, String desc) throws StorageException;

    void setStatusToInMail(MSHInMail mail, SEDInboxMailStatus status, String desc, String userID, String applicationId) throws StorageException;

    void updateInMail(MSHInMail mail, String statusDesc, String userID) throws StorageException;

    void serializeOutMail(MSHOutMail mail, String userID, String applicationId, String pmodeId) throws StorageException;

    void serializeInMail(MSHInMail mail, String applicationId) throws StorageException;

    void removeInMail(BigInteger bi) throws StorageException;

    void removeOutMail(BigInteger bi) throws StorageException;

    boolean addExecutionTask(SEDTaskExecution ad) throws StorageException;

    boolean updateExecutionTask(SEDTaskExecution ad) throws StorageException;

    SEDTaskExecution getLastSuccesfullTaskExecution(String type) throws StorageException;
}
