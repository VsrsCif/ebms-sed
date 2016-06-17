/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui.entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author sluzba
 */
public class OutMailTableFilter {

    protected String action;
    protected String conversationId;
    protected String receiverEBox;
    protected List<String> senderEBoxList = new ArrayList<>();
    protected String service;
    protected String status;
    protected String subject;
    protected Date submittedDateFrom;
    protected Date submittedDateTo;

    public String getAction() {
        return action;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getReceiverEBox() {
        return receiverEBox;
    }

    public List<String> getSenderEBoxList() {
        return senderEBoxList;
    }

    public String getService() {
        return service;
    }

    public String getStatus() {
        return status;
    }

    public String getSubject() {
        return subject;
    }

    public Date getSubmittedDateFrom() {
        return submittedDateFrom;
    }

    public Date getSubmittedDateTo() {
        return submittedDateTo;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setReceiverEBox(String receiverEBox) {
        this.receiverEBox = receiverEBox;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setStatus(String st) {
        this.status = st;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setSubmittedDateFrom(Date submittedDateFrom) {
        this.submittedDateFrom = submittedDateFrom;
    }

    public void setSubmittedDateTo(Date submittedDateTo) {
        this.submittedDateTo = submittedDateTo;
    }

}
