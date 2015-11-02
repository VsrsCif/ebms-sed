/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package si.jrc.msh.exception;

import javax.xml.namespace.QName;

/**
 *
 * @author sluzba
 */
public enum SOAPExceptionCode {    
    SoapVersionMismatch("SoapVersionMismatch", new QName("http://ebms.sed,org/", "0001"), "Soap version does not match init soap version.", 0),
    InvalidSoapVersion("InvalidSoapVersion", new QName("http://ebms.sed.org/", "0002"),"Invalid soap message: %s", 1),
    SoapParseFailure("SoapParseFailure", new QName("http://ebms.sed.org/", "0003"), "Invalid Soap message: %s",1),
    ConfigurationFailure("ConfigurationFailure", new QName("http://ebms.sed.org/svev-msh", "0100"), "Configuration failure: %s", 1),
    InternalFailure("InternalFailure", new QName("http://ebms.sed.org/svev-msh", "0101"), "Internal failure: %s", 1),    
    StoreInboundMailFailure("StoreInboundMailFailure", new QName("http://ebms.sed.org/svev-msh", "0102"), "Internal error: %s", 1)
    
    ;
    
    String name;
    QName code;
    String desc;
    int iParams; 

    private SOAPExceptionCode(String name, QName code, String desc, int iCnt) {
        this.name = name;
        this.code = code;
        this.desc = desc;
        this.iParams = iCnt;
    }

    public String getName() {
        return name;
    }


    public QName getCode() {
        return code;
    }
    public int getDescParamCount(){
        return iParams;
    }
    public String getDescriptionFormat(){
        return desc;
    }
    
   public String getDesc(String ... messageParams){
        if (messageParams == null){
            messageParams = new String[iParams];
        }
        
        if (messageParams.length != getDescParamCount()){
            String[] newMP = new String[getDescParamCount()];
            for (int i =0; i< newMP.length; i++){
                newMP[i] = i<messageParams.length?messageParams[i]:"";
            }
            messageParams = newMP;
            
        }
        return String.format(getDescriptionFormat(), (Object[]) messageParams);
   }
    
    
}
