/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

    public List<EBMSError> getErrors() {
        return errors;
    }

    public String getRefToMessageId() {
        return refToMessageId;
    }

    public Date getSignalDate() {
        return signalDate;
    }

    public Date getSignalReceivedDate() {
        return signalReceivedDate;
    }

    public boolean isAS4ResponseValid() {
        return AS4ResponseValid;
    }

    public void setAS4ResponseValid(boolean isAS4ResponseValid) {
        this.AS4ResponseValid = isAS4ResponseValid;
    }

    public void setRefToMessageId(String refToMessageId) {
        this.refToMessageId = refToMessageId;
    }

    public void setSignalDate(Date signalDate) {
        this.signalDate = signalDate;
    }

    public void setSignalReceivedDate(Date signalReceivedDate) {
        this.signalReceivedDate = signalReceivedDate;
    }

    enum SignalType {
        SvevKeySignal,
        ErrorSignal,
        AS4ResponseSignal
    }

}
