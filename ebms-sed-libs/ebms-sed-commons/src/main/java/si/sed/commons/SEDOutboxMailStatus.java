/*
* Copyright 2015, Supreme Court Republic of Slovenia 
*
* Licensed under the EUPL, Version 1.1 or – as soon they will be approved by 
* the European Commission - subsequent versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the Licence.
* You may obtain a copy of the Licence at:
*
* https://joinup.ec.europa.eu/software/page/eupl
*
* Unless required by applicable law or agreed to in writing, software 
* distributed under the Licence is distributed on an "AS IS" basis, WITHOUT 
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the Licence for the specific language governing permissions and  
* limitations under the Licence.
*/
package si.sed.commons;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */

public enum SEDOutboxMailStatus { 
    SUBMITED ("SUBMITED", "Message is sucessfuly added to SED for transmition."),
    SENDING ("SENDING", "Message is pushing/pulling to receiving MSH"),
    SEND_SHEDULE ("SEND_SHEDULE", "Shedule for resend"),
    SENT ("SENT", "Message is  sent receiving MSH"),
    SEND_ERROR ("SEND_ERROR", "Error occured pushing/pulling to receiving MSH"),
    
    ; 
    
   String mstrVal;
   String mstrDesc;
   private SEDOutboxMailStatus(String val, String strDesc){
       mstrVal = val;
       mstrDesc = strDesc;
   }

    public String getValue() {
        return mstrVal;
    }

    public String getDesc() {
        return mstrDesc;
    }
    
}
