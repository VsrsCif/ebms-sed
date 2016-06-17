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
public class InMailTableFilter {

    protected String action;
    protected String conversationId;
    protected Date receivedDateFrom;
    protected Date receivedDateTo;

    protected List<String> receiverEBoxList = new ArrayList<>();
    protected String senderEBox;
    protected String service;
    protected String status;
    protected String subject;

    public String getAction() {
        return action;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Date getReceivedDateFrom() {
        return receivedDateFrom;
    }

    public Date getReceivedDateTo() {
        return receivedDateTo;
    }

    public List<String> getReceiverEBoxList() {
        return receiverEBoxList;
    }

    /* public void setReceiverEBox(String receiverEBox) {
        this.receiverEBox = receiverEBox;
    }*/
    public String getSenderEBox() {
        return senderEBox;
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

    public void setAction(String action) {
        this.action = action;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setReceivedDateFrom(Date receivedDateFrom) {
        this.receivedDateFrom = receivedDateFrom;
    }

    public void setReceivedDateTo(Date receivedDateTo) {
        this.receivedDateTo = receivedDateTo;
    }

    public void setSenderEBox(String senderEBox) {
        this.senderEBox = senderEBox;
    }

    public void setService(String service) {
        this.service = service;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

}
