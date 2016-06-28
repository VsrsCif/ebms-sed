/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package si.jrc.msh.db.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import si.jrc.msh.exception.EBMSError;

/**
 *
 * @author sluzba
 */
public class EBMSSignal {

  boolean AS4ResponseValid;

  List<EBMSError> errors = new ArrayList<>();
  String refToMessageId;
  Date signalDate;
  Date signalReceivedDate;

  /**
   *
   * @return
   */
  public List<EBMSError> getErrors() {
    return errors;
  }

  /**
   *
   * @return
   */
  public String getRefToMessageId() {
    return refToMessageId;
  }

  /**
   *
   * @return
   */
  public Date getSignalDate() {
    return signalDate;
  }

  /**
   *
   * @return
   */
  public Date getSignalReceivedDate() {
    return signalReceivedDate;
  }

  /**
   *
   * @return
   */
  public boolean isAS4ResponseValid() {
    return AS4ResponseValid;
  }

  /**
   *
   * @param isAS4ResponseValid
   */
  public void setAS4ResponseValid(boolean isAS4ResponseValid) {
    this.AS4ResponseValid = isAS4ResponseValid;
  }

  /**
   *
   * @param refToMessageId
   */
  public void setRefToMessageId(String refToMessageId) {
    this.refToMessageId = refToMessageId;
  }

  /**
   *
   * @param signalDate
   */
  public void setSignalDate(Date signalDate) {
    this.signalDate = signalDate;
  }

  /**
   *
   * @param signalReceivedDate
   */
  public void setSignalReceivedDate(Date signalReceivedDate) {
    this.signalReceivedDate = signalReceivedDate;
  }

  enum SignalType {
    SvevKeySignal, ErrorSignal, AS4ResponseSignal
  }

}
