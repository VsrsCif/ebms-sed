/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui.entities;

import java.util.Date;

/**
 *
 * @author sluzba
 */
public class OutMailTableFilter {
    protected String service;
    protected String action;
    protected String conversationId;
    protected String subject;
    protected String receiverEBox;
    protected String senderEBox;
    protected String status;    
    protected Date submittedDateFrom;
    protected Date submittedDateTo;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getReceiverEBox() {
        return receiverEBox;
    }

    public void setReceiverEBox(String receiverEBox) {
        this.receiverEBox = receiverEBox;
    }

    public String getSenderEBox() {
        return senderEBox;
    }

    public void setSenderEBox(String senderEBox) {
        this.senderEBox = senderEBox;
    }

    public Date getSubmittedDateFrom() {
        return submittedDateFrom;
    }

    public void setSubmittedDateFrom(Date submittedDateFrom) {
        this.submittedDateFrom = submittedDateFrom;
    }

    public Date getSubmittedDateTo() {
        return submittedDateTo;
    }

    public void setSubmittedDateTo(Date submittedDateTo) {
        this.submittedDateTo = submittedDateTo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String st) {
        this.status = st; 
    }
    
    
}
