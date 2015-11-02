/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package si.jrc.msh.utils;

import si.sed.commons.utils.PModeManager;
import java.util.ArrayList;
import java.util.List;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;

import org.msh.svev.pmode.PMode;
import si.jrc.msh.exception.MSHException;
import si.jrc.msh.exception.MSHExceptionCode;




/**
 *
 * @author sluzba
 */
public class SvevUtils {
    
    public static final String S_LEGAL_DELIVERY_SERVICE = "legal-delivery";
    
    PModeManager mpmd = new PModeManager();
   
    public void vaildateMail(MSHOutMail mail) throws MSHException {
        List<String> merrLst = new ArrayList<>();
        
        if (mail.getConversationId() == null || mail.getConversationId().isEmpty()) {
            merrLst.add("Missing mail id!");
        }

        if (mail.getMSHOutPayload() == null || mail.getMSHOutPayload().getMSHOutParts().isEmpty() ) {
            merrLst.add("No content in mail (Attachment is empty)!");
        }

        int iMP = 0;
        for (MSHOutPart mp:  mail.getMSHOutPayload().getMSHOutParts()){
            iMP++;
            if (mp.getMimeType()==null || mp.getMimeType().isEmpty()){
                merrLst.add("Missing payload mimetype (index:'"+iMP+"')!");
            }
//            if (mp.getBin()==null ){
//                merrLst.add("Missing payload data (index:'"+iMP+"')!");
//            }
        }
        
        
        if (mail.getReceiverEBox() == null || mail.getReceiverEBox().trim().isEmpty()) {
            merrLst.add( "Missing ReceiverEBox!");
        }else if (!mail.getReceiverEBox().contains("@")) {
            merrLst.add("Receiver address: '" + mail.getReceiverEBox()  + "' is invalid!");
        }
        
        if (mail.getSenderEBox() == null || mail.getSenderEBox().trim().isEmpty()) {
            merrLst.add( "Missing SenderEBox!");
        }else if (!mail.getSenderEBox().contains("@")) {
            merrLst.add("Receiver address: '" + mail.getSenderEBox()  + "' is invalid!");
        }
        
        if (mail.getService() == null || mail.getService().trim().isEmpty()) {
            merrLst.add( "Missing service (DeliveryType)!");
        }
        if (mail.getAction() == null || mail.getAction().trim().isEmpty()) {
            merrLst.add( "Missing action!");
        }
        
         if (mail.getReceiverEBox() == null || mail.getReceiverEBox().trim().isEmpty()) {
            merrLst.add( "Missing receiver EBox!");
        }
         if (mail.getSenderEBox() == null || mail.getSenderEBox().trim().isEmpty()) {
            merrLst.add( "Missing sender EBox!");
        }
       
        
        
        /*if (mail.getMailType() != null  &&  mail.getMailType().equals(MSHOutOutboxMailType.DeliveryAdvice)
            &&  (mail.getRefToMessageId() == null || mail.getRefToMessageId().trim().isEmpty()) ){
                throw new MSHException(MSHExceptionCode.MissingDataInMail, "DeliveryAdvice must have RefToMessageId!");
         }*/
        
    
         if (!merrLst.isEmpty()){
             throw new MSHException(MSHExceptionCode.InvalidMail, String.join(", ", merrLst ));
        }
    }
    


    public PMode getPModeForMail(MSHOutMail mail) throws MSHException {
        PMode pmd;

        String targetDomain = mail.getReceiverEBox().substring(mail.getReceiverEBox().indexOf("@") + 1);
        String sourceDomain = mail.getSenderEBox().substring(mail.getSenderEBox().indexOf("@") + 1);
        String pmode =mail.getService()+":"+ sourceDomain + ":" + targetDomain;
        mpmd.reloadPModes();
        // check if exists targeted MSHOut with legal-delivery service
        pmd = mpmd.getPModeById(pmode);       
        
        if (pmd == null){
            throw new MSHException(MSHExceptionCode.InvalidPModeId, pmode);
        }
        return pmd;
    }
    
}
