/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package si.jrc.msh.exception;



/**
 *
 * @author sluzba
 */
public class EBMSError extends Exception{
    
    EBMSErrorCode ebmsErrorCode;
    String subMessage;
    String refToMessage;
    
    public EBMSError(EBMSErrorCode ec, String refToMsg) {
        ebmsErrorCode = ec;
        refToMessage =  refToMsg;
    }

    public EBMSError(EBMSErrorCode ec, String refToMsg,  String message) {
        super(ec.getName());
        ebmsErrorCode = ec;
        refToMessage =  refToMsg;
        subMessage =  message;
        
    }

    public EBMSError(EBMSErrorCode ec,String refToMsg, String message, Throwable cause) {
        super(ec.getName(), cause);
        ebmsErrorCode = ec;
        refToMessage =  refToMsg;
        subMessage =  message;
    }

    public EBMSError(EBMSErrorCode ec,String refToMsg, Throwable cause) {
        super(ec.getName(), cause);
        ebmsErrorCode = ec;
        refToMessage =  refToMsg;
        
    }

    public String getSubMessage() {
        return subMessage;
    }

    public EBMSErrorCode getEbmsErrorCode() {
        return ebmsErrorCode;
    }

    public String getRefToMessage() {
        return refToMessage;
    }
    

    
    
}
