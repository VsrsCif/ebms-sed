/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.task;

import java.util.Date;

/**
 *
 * @author sluzba
 */
public class SearchParameters {

    Date receivedDateTo;

    Date submittedDateTo;

    public Date getReceivedDateTo() {
        return receivedDateTo;
    }

    public Date getSubmittedDateTo() {
        return submittedDateTo;
    }

    public void setReceivedDateTo(Date receivedDateTo) {
        this.receivedDateTo = receivedDateTo;
    }

    public void setSubmittedDateTo(Date submittedDateTo) {
        this.submittedDateTo = submittedDateTo;
    }
}