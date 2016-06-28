/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package si.jrc.msh.utils;

/**
 *
 * @author sluzba
 */
public class EbMSConstants {

  /**
     *
     */
  public static final String EBMS_CP_BASE_LOG_SOAP_MESSAGE_FILE = "si.jrc.base.soap.message.file";

  /**
     *
     */
  public static final String EBMS_CP_IN_LOG_SOAP_MESSAGE_FILE = "si.jrc.incoming.soap.message.file";

  /**
     *
     */
  public static final String EBMS_CP_OUT_LOG_SOAP_MESSAGE_FILE =
      "si.jrc.outgoing.soap.message.file";

  /**
     *
     */
  public static final String EBMS_FILE_PROPERTY_ENCODING = "encoding";

  /**
     *
     */
  public static final String EBMS_FILE_PROPERTY_FILENAME = "filename";

  /**
     *
     */
  public static final String EBMS_FILE_PROPERTY_IS_ENCRYPTED = "isEnrypted";

  /**
     *
     */
  public static final String EBMS_FILE_PROPERTY_MIME = "mime";
  // part properties

  /**
     *
     */
  public static final String EBMS_FILE_PROPERTY_NAME = "name";

  /**
     *
     */
  public static final String EBMS_FILE_PROPERTY_TYPE = "type";

  // schema constats
  /**
     *
     */
  public static final String EBMS_NS =
      "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";

  // party info
  /**
     *
     */
  public static final String EBMS_PARTY_TYPE_EBOX =
      "urn:oasis:names:tc:ebcore:partyid-type:unregistered:si-svev:e-box";

  /**
     *
     */
  public static final String EBMS_PARTY_TYPE_NAME =
      "urn:oasis:names:tc:ebcore:partyid-type:unregistered:si-svev:name";
  // message properties

  /**
     *
     */
  public static final String EBMS_PROPERTY_DESC = "description";

  /**
     *
     */
  public static final String EBMS_PROPERTY_SUBMIT_DATE = "submitDate";

  /**
     *
     */
  public static final String EBMS_ROOT_ELEMENT_NAME = "Messaging";

  /**
     *
     */
  public static final String File_PREFIX_SOAP_Message = "EBMS-";

  // log soap file prefix/suffix
  /**
     *
     */
  public static final String File_Suffix_SOAP_Message_Request = "-Request.xml";

  /**
     *
     */
  public static final String File_Suffix_SOAP_Message_Response = "-Response.xml";

}
