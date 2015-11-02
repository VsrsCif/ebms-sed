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
public class MSHException extends Exception{
    
    MSHExceptionCode mshErrorCode;
    String[] messageParams;
    
    
    public MSHException(MSHExceptionCode ec) {
        mshErrorCode = ec;
        
    }

    public MSHException(MSHExceptionCode ec,  String... params) {
        super(ec.getName());
        mshErrorCode = ec;
        
        messageParams =  params;
        
    }

    public MSHException(MSHExceptionCode ec,   Throwable cause,String... params) {
        super(ec.getName(), cause);
        mshErrorCode = ec;
        messageParams =  params;
    }

    public MSHException(MSHExceptionCode ec, Throwable cause) {
        super(ec.getName(), cause);
        mshErrorCode = ec;        
    }

    @Override
    public String getMessage() {
        if (messageParams == null){
            messageParams = new String[mshErrorCode.getDescParamCount()];
        }
        
        if (messageParams.length != mshErrorCode.getDescParamCount()){
            String[] newMP = new String[mshErrorCode.getDescParamCount()];
            for (int i =0; i< newMP.length; i++){
                newMP[i] = i<messageParams.length?messageParams[i]:"";
            }
            messageParams = newMP;
            
        }
        return String.format(mshErrorCode.getDescriptionFormat(), (Object[]) messageParams);
    }

    public MSHExceptionCode getMSHErrorCode() {
        return mshErrorCode;
    }

    
}
