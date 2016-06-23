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
package si.sed.msh.ws.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.sed.ebms.SEDException;
import org.sed.ebms.SEDExceptionCode;
import org.sed.ebms.SEDException_Exception;
import org.sed.ebms.control.Control;
import org.sed.ebms.outbox.mail.OutMail;
import org.sed.ebms.outbox.payload.OutPart;
import si.sed.commons.utils.Utils;

/**
 *
 * @author Jože Rihtaršič
 */
public class SEDRequestUtils {

    private static final Pattern EMAIL_PATTEREN = Pattern.compile(
            "^.+@.+(\\\\.[^\\\\.]+)+$");

    /**
     * Methods validate OutMail for missing data
     *
     * @param mail
     * @throws SEDException_Exception
     */
    public static void checkOutMailForMissingData(OutMail mail)
            throws SEDException_Exception {
        List<String> errLst = new ArrayList<>();
        if (Utils.isEmptyString(mail.getSenderMessageId())) {
            errLst.add("SenderMessageId");
        }
        if (mail.getOutPayload() == null ||
                mail.getOutPayload().getOutParts().isEmpty()) {
            errLst.add("No content in mail (Attachment is empty)!");
        }
        int iMP = 0;
        for (OutPart mp : mail.getOutPayload().getOutParts()) {
            iMP++;
            if (Utils.isEmptyString(mp.getMimeType())) {
                errLst.add("Mimetype (index:'" + iMP + "')!");
            }
            // check payload
            if (mp.getValue() == null &&
                    (Utils.isEmptyString(mp.getFilepath()) || !(new File(
                    mp.getFilepath()).exists()))) {
                errLst.add(
                        "No payload content. Add value or existing file (index:'" +
                        iMP + "')!");
            }
        }
        if (Utils.isEmptyString(mail.getReceiverName())) {
            errLst.add("ReceiverName");
        }
        if (Utils.isEmptyString(mail.getReceiverEBox())) {
            errLst.add("ReceiverEBox");
        }
        if (Utils.isEmptyString(mail.getSenderName())) {
            errLst.add("SenderName");
        }
        if (Utils.isEmptyString(mail.getSenderEBox())) {
            errLst.add("SenderEBox");
        }
        if (Utils.isEmptyString(mail.getService())) {
            errLst.add("Missing service!");
        }
        if (Utils.isEmptyString(mail.getAction())) {
            errLst.add("Missing Action!");
        }
        if (Utils.isEmptyString(mail.getConversationId())) {
            errLst.add("ConversationId!");
        }
        if (!errLst.isEmpty()) {
            throw createSEDException("Missing data (" + errLst.size() + "):" +
                    String.join(", ", errLst),
                    SEDExceptionCode.MISSING_DATA);
        }

    }

    /**
     *
     * @param message
     * @param cd
     * @return
     */
    public static SEDException_Exception createSEDException(String message,
            SEDExceptionCode cd) {
        return createSEDException(message, cd, null);
    }

    /**
     *
     * @param message
     * @param cd
     * @param tw
     * @return
     */
    public static SEDException_Exception createSEDException(String message,
            SEDExceptionCode cd, Throwable tw) {
        SEDException se = new SEDException();
        se.setErrorCode(cd);
        se.setMessage(message);
        return new SEDException_Exception(cd.name() + ":" + message, se, tw);
    }

    /**
     * Method checks format of ebox address
     *
     * @param address
     * @return
     */
    public static boolean isValidMailAddress(String address) {
        return address != null && EMAIL_PATTEREN.matcher(address).matches();

    }

    /**
     * Method checks input control parameters
     *
     * @param c
     * @throws SEDException_Exception
     */
    public static void validateControl(Control c)
            throws SEDException_Exception {

        if (c == null) {
            throw SEDRequestUtils.createSEDException("SubmitMailRequest/Control",
                    SEDExceptionCode.MISSING_DATA);
        }
        if (c.getApplicationId() == null) {
            throw SEDRequestUtils.createSEDException(
                    "SubmitMailRequest/Control/@userId",
                    SEDExceptionCode.MISSING_DATA);
        }
        if (c.getUserId() == null) {
            throw SEDRequestUtils.createSEDException(
                    "SubmitMailRequest/Control/@getApplicationId",
                    SEDExceptionCode.MISSING_DATA);
        }
    }
}
