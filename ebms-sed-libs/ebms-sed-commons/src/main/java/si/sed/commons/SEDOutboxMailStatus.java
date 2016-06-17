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
    SUBMITTED("SUBMITTED", "Message is sucessfuly added to SED for transmition.", "orange"),
    SENDING("SENDING", "Message is pushing/pulling to receiving MSH", "gray"),
    SCHEDULE("SCHEDULE", "Shedule for resend", "green"),
    SENT("SENT", "Message is  sent to receiving MSH", "blue"),
    ERROR("ERROR", "Error occured pushing/pulling to receiving MSH", "red"),
    EBMSERROR("EBMSERROR", "EBMS Error occured pushing/pulling to receiving MSH", "darkred"),
    DELETED("DELETED", "Pošiljka je izbrisana", "black"),
    CANCELED("CANCELED", "Pošiljnje je preklicano", "yellow"),
    CANCELING("CANCELING", "Pošiljnje je v postopku preklica", "darkgray");

    String mstrVal;
    String mstrDesc;
    String mstrColor;

    private SEDOutboxMailStatus(String val, String strDesc, String strColor) {
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

        for (SEDOutboxMailStatus st : values()) {
            if (st.getValue().equals(strName)) {
                return st.getColor();
            }
        }
        return strName;
    }
}
