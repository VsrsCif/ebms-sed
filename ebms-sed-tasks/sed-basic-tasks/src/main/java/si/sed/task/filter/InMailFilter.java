/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.task.filter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author sluzba
 */
public class InMailFilter {
    protected String service;
    protected String action;
    protected String conversationId;
    protected String subject;
    
    protected List<String> receiverEBoxList = new ArrayList<>();
    protected String senderEBox;
    protected String status;
    protected Date receivedDateFrom;
    protected Date receivedDateTo;

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

    public List<String> getReceiverEBoxList() {
        return receiverEBoxList;
    }

   /* public void setReceiverEBox(String receiverEBox) {
        this.receiverEBox = receiverEBox;
    }*/

    public String getSenderEBox() {
        return senderEBox;
    }

    public void setSenderEBox(String senderEBox) {
        this.senderEBox = senderEBox;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getReceivedDateFrom() {
        return receivedDateFrom;
    }

    public void setReceivedDateFrom(Date receivedDateFrom) {
        this.receivedDateFrom = receivedDateFrom;
    }

    public Date getReceivedDateTo() {
        return receivedDateTo;
    }

    public void setReceivedDateTo(Date receivedDateTo) {
        this.receivedDateTo = receivedDateTo;
    }


    
}
