/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.exception;

import static java.lang.String.format;

/**
 *
 * @author sluzba
 */
public class SEDSecurityException extends Exception {

    String[] messageParams;
    SEDSecurityExceptionCode mshErrorCode;

    /**
     *
     * @param ec
     */
    public SEDSecurityException(SEDSecurityExceptionCode ec) {
        mshErrorCode = ec;

    }

    /**
     *
     * @param ec
     * @param params
     */
    public SEDSecurityException(SEDSecurityExceptionCode ec, String... params) {
        super(ec.getName());
        mshErrorCode = ec;

        messageParams = params;

    }

    /**
     *
     * @param ec
     * @param cause
     * @param params
     */
    public SEDSecurityException(SEDSecurityExceptionCode ec, Throwable cause,
            String... params) {
        super(ec.getName(), cause);
        mshErrorCode = ec;
        messageParams = params;
    }

    /**
     *
     * @param ec
     * @param cause
     */
    public SEDSecurityException(SEDSecurityExceptionCode ec, Throwable cause) {
        super(ec.getName(), cause);
        mshErrorCode = ec;
    }

    /**
     *
     * @return
     */
    public SEDSecurityExceptionCode getMSHErrorCode() {
        return mshErrorCode;
    }

    @Override
    public String getMessage() {
        if (messageParams == null) {
            messageParams = new String[mshErrorCode.getDescParamCount()];
        }

        if (messageParams.length != mshErrorCode.getDescParamCount()) {
            String[] newMP = new String[mshErrorCode.getDescParamCount()];
            for (int i = 0; i < newMP.length; i++) {
                newMP[i] = i < messageParams.length ? messageParams[i] : "";
            }
            messageParams = newMP;

        }
        return format(mshErrorCode.getDescriptionFormat(),
                (Object[]) messageParams);
    }

    /**
     *
     */
    public enum SEDSecurityExceptionCode {

        /**
         *
         */
        NoSuchAlgorithm("SEC:0001", "NoSuchAlgorithm", "No such algorithm: %s ",
                1),

        /**
         *
         */
        NoSuchPadding("SEC:0002", "NoSuchPadding",
                "No such padding: %s, msg: %s", 2),

        /**
         *
         */
        InvalidKey("SEC:0003", "InvalidKey", "Invalid key: %s, msg: %s", 2),

        /**
         *
         */
        EncryptionException("SEC:0004", "EncryptionError",
                "Encryption error: %s", 1),

        /**
         *
         */
        PasswordFileError("SEC:0005", "PasswordFileError", "Security error: %s",
                1),

        /**
         *
         */
        ReadWriteFileException("SEC:0006", "ReadWriteFileException",
                "Read write file exception: %s", 1),

        /**
         *
         */
        KeyStoreException("SEC:0007", "KeyStoreException",
                "Key store exception %s", 1),

        /**
         *
         */
        CertificateException("SEC:0008", "CertificateException",
                "Certificate exception %s", 1),

        /**
         *
         */
        InitializeException("SEC:0009", "InitializeException",
                "Initialize exception %s", 1),

        /**
         *
         */
        CreateSignatureException("SEC:0010", "CreateSignatureException",
                "Create Signature exception %s", 1),

        /**
         *
         */
        CreateTimestampException("SEC:0011", "CreateTimestampException",
                "Create Timestamp exception %s", 1),

        /**
         *
         */
        XMLParseException("SEC:0012", "XMLParseException",
                "XMLParse exception %s", 1),

        /**
         *
         */
        SignatureNotFound("SEC:0013", "SignatureNotFound",
                "Signature Not Found exception %s", 1),

        /**
         *
         */
        KeyForAliasNotExists("SEC:0014", "KeyForAliasNotExists",
                "Key for alias %s not found!", 1),;
        ;
        
        String code;
        String name;
        String description;
        int paramCount;

        SEDSecurityExceptionCode(String cd, String nm, String desc, int pc) {
            code = cd;
            name = nm;
            description = desc;
            paramCount = pc;
        }

        /**
         *
         * @return
         */
        public String getCode() {
            return code;
        }

        /**
         *
         * @return
         */
        public String getName() {
            return name;
        }

        /**
         *
         * @return
         */
        public String getDescriptionFormat() {
            return description;
        }

        /**
         *
         * @return
         */
        public int getDescParamCount() {
            return paramCount;
        }
    }

}
