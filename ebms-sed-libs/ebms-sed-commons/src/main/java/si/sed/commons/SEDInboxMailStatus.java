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
public enum SEDInboxMailStatus {
    RECEIVE("RECEIVE", "Receive message in process.", "orange"),
    RECEIVED("RECEIVED", "Message is sucessfuly received to MSH.", "green"),
    PROCESS("PROCESS", "Message is locked by plugin", "gray"),
    LOCKED("LOCKED", "Message is locked by consumer", "lightgray"),
    PLUGINLOCKED("PLGLOCKED", "Message is locked by plugin", "lightgray"),
    DELIVERED("DELIVERED", "Message is consumed", "Blue"),
    ERROR("ERROR", "Error occured receiving, processing MSH", "red"),
    DELETED("DELETED", "Pošiljka je izbrisana", "black");

    String mstrVal;
    String mstrDesc;
    String mstrColor;

    private SEDInboxMailStatus(String val, String strDesc, String strColor) {
        mstrVal = val;
        mstrDesc = strDesc;
        mstrColor = strColor;
    }

    public String getValue() {
        return mstrVal;
    }

    public String getDesc() {
        return mstrDesc;
    }

    public String getColor() {
        return mstrColor;
    }

    public static String getColor(String strName) {

        for (SEDInboxMailStatus st : values()) {
            if (st.getValue().equals(strName)) {
                return st.getColor();
            }
        }
        /*SEDOutboxMailStatus st = SEDOutboxMailStatus.valueOf(strName);
        if (st!=null){
            return st.getColor();
        }*/
        return strName;
    }

}
