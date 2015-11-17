/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.db.entities;

import si.jrc.msh.exception.EBMSError;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author sluzba
 */
public class EBMSSignal {

    enum SignalType {
        SvevKeySignal,
        ErrorSignal,
        AS4ResponseSignal
    }

    Date signalReceivedDate;
    Date signalDate;
    String refToMessageId;
    boolean AS4ResponseValid;

    List<EBMSError> errors = new ArrayList<>();

    public Date getSignalReceivedDate() {
        return signalReceivedDate;
    }

    public void setSignalReceivedDate(Date signalReceivedDate) {
        this.signalReceivedDate = signalReceivedDate;
    }

    public Date getSignalDate() {
        return signalDate;
    }

    public void setSignalDate(Date signalDate) {
        this.signalDate = signalDate;
    }

    public String getRefToMessageId() {
        return refToMessageId;
    }

    public void setRefToMessageId(String refToMessageId) {
        this.refToMessageId = refToMessageId;
    }

    public boolean isAS4ResponseValid() {
        return AS4ResponseValid;
    }

    public void setAS4ResponseValid(boolean isAS4ResponseValid) {
        this.AS4ResponseValid = isAS4ResponseValid;
    }

    public List<EBMSError> getErrors() {
        return errors;
    }

}
