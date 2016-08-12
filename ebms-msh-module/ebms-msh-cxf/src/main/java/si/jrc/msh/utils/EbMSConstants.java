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
     *  http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/profiles/AS4-profile/v1.0/AS4-profile-v1.0.html
     * For XML payloads, an eb:PartInfo/eb:PartProperties/eb:Property/@name="CharacterSet" value is
     * RECOMMENDED to identify the character set of the payload before compression was applied. 
     * The values of this property MUST conform to the values defined in section 4.3.3 of 
     *   Extensible Markup Language (XML) 1.0. W3C Recommendation 26 November 2008.
     * http://www.w3.org/TR/REC-xml/
     */
  public static final String EBMS_PAYLOAD_PROPERTY_ENCODING = "CharacterSet";

  /**
     * Filename as defined by sender.
     */
  public static final String EBMS_PAYLOAD_PROPERTY_FILENAME = "Filename";

  /**
     * Is document SVEV ecrypted
     */
  public static final String EBMS_PAYLOAD_PROPERTY_IS_ENCRYPTED = "IsEncrypted";

  /**
     * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/profiles/AS4-profile/v1.0/AS4-profile-v1.0.html
     * An eb:PartInfo/eb:PartProperties/eb:Property/@name="MimeType" value is REQUIRED  to identify 
     * the MIME type of the payload before compression was applied.
     */
  public static final String EBMS_PAYLOAD_PROPERTY_MIME = "MimeType";
  // part properties

  /**
     * Custom name or short desc for payload
    */
  public static final String EBMS_PAYLOAD_PROPERTY_NAME = "Name";

  /**
     * Custom type defined by sender and receiver.   
     */
  public static final String EBMS_PAYLOAD_PROPERTY_TYPE = "Type";
  
   /**
     * Document custom type defined by sender and receiver. 
     */
  public static final String EBMS_PAYLOAD_COMPRESSION_TYPE = "CompressionType";

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