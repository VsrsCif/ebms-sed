/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.test;

import java.math.BigInteger;
import java.util.List;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.sed.ebms.cron.SEDTaskExecution;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.exception.StorageException;
import si.sed.commons.interfaces.SEDDaoInterface;

/**
 *
 * @author sluzba
 */
public class SEDTestDao implements SEDDaoInterface{

  @Override
  public boolean addExecutionTask(SEDTaskExecution ad)
      throws StorageException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public <T> List<T> getDataList(Class<T> type, int startingAt, int maxResultCnt, String sortField,
      String sortOrder, Object filters) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public <T> long getDataListCount(Class<T> type, Object filters) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<MSHInMail> getInMailConvIdAndAction(String action, String convId) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public SEDTaskExecution getLastSuccesfullTaskExecution(String type)
      throws StorageException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public <T> T getMailById(Class<T> type, BigInteger mailId) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public <T> List<T> getMailEventList(Class<T> type, BigInteger mailId) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void removeInMail(BigInteger bi)
      throws StorageException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void removeOutMail(BigInteger bi)
      throws StorageException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void serializeInMail(MSHInMail mail, String applicationId)
      throws StorageException {
    
  }

  @Override
  public void serializeOutMail(MSHOutMail mail, String userID, String applicationId, String pmodeId)
      throws StorageException {
    // no implementation
  }

  @Override
  public void setStatusToInMail(MSHInMail mail, SEDInboxMailStatus status, String desc)
      throws StorageException {
    // no implementation
  }

  @Override
  public void setStatusToInMail(MSHInMail mail, SEDInboxMailStatus status, String desc,
      String userID, String applicationId)
      throws StorageException {
    // no implementation
  }

  @Override
  public void setStatusToInMail(MSHInMail mail, SEDInboxMailStatus status, String desc,
      String userID, String applicationId, String filePath, String mime)
      throws StorageException {
  // no implementation
  }

  @Override
  public void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc)
      throws StorageException {
    // no implementation
  }

  @Override
  public void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc,
      String userID, String applicationId)
      throws StorageException {
    // no implementation
  }

  @Override
  public void setStatusToOutMail(MSHOutMail mail, SEDOutboxMailStatus status, String desc,
      String userID, String applicationId, String filePath, String mime)
      throws StorageException {
    // no implementation
  }

  @Override
  public boolean updateExecutionTask(SEDTaskExecution ad)
      throws StorageException {
    // no implementation
    return false;
  }

  @Override
  public void updateInMail(MSHInMail mail, String statusDesc, String userID)
      throws StorageException {
    
  }
  
}
