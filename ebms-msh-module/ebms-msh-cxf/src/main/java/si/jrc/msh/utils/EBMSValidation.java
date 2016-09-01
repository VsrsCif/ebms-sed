/*
 * Copyright 2015, Supreme Court Republic of Slovenia
 * 
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package si.jrc.msh.utils;

import si.sed.commons.cxf.EBMSConstants;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.apache.cxf.binding.soap.SoapMessage;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.AgreementRef;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PayloadInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import si.jrc.msh.exception.EBMSError;
import si.jrc.msh.exception.EBMSErrorCode;
import si.jrc.msh.exception.EBMSErrorMessage;
import si.jrc.msh.interceptor.EBMSInInterceptor;
import si.sed.commons.cxf.SoapUtils;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author Jože Rihtaršič
 */
public class EBMSValidation {

  protected final static SEDLogger LOG = new SEDLogger(EBMSInInterceptor.class);

  /**
   * Method validates:
   * <ul>
   * <li>SOAP Version (AS4 SOAP 1.2)</li>
   * <li>existance of Messaging header</li>
   * <li> isMessaging valid by schema</li>
   * <li> existance of at most one UserMessage or/and Signal Message</li>
   * </ul>
   *
   * @param request msginput soap message msg
   * @param sv Fault code for error if occured
   * @return Messaging: serialized messaging object
   * @throws EBMSError - if validation fault occured
   */
  public Messaging vaildateHeader_Messaging(SOAPMessage request, QName sv)
      throws EBMSError {
    long l = LOG.logStart();

    // check if soap header exists
    NodeList lstND = null;
    try {
      if (request.getSOAPHeader() == null) {
        String errmsg = EBMSErrorMessage.INVALID_HEADER_MISSING_MESSAGING;
        LOG.logError(l, errmsg, null);
        throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg, sv);
      }

      lstND =
          request.getSOAPHeader().getElementsByTagNameNS(EBMSConstants.EBMS_NS,
              EBMSConstants.EBMS_ROOT_ELEMENT_NAME);
      if (lstND == null || lstND.getLength() == 0) {
        String errmsg = EBMSErrorMessage.INVALID_HEADER_MISSING_MESSAGING;
        LOG.logError(l, errmsg, null);
        throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg, sv);
      }
    } catch (SOAPException ex) {
      String errmsg = EBMSErrorMessage.INVALID_HEADER_ERROR_PARSING_MESSAGING + ex.getMessage();
      LOG.logError(l, errmsg, ex);
      throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg, sv);
    }

    // check headers
    if (lstND.getLength() != 1) {
      String errmsg = String.format(EBMSErrorMessage.INVALID_HEADER_MULTIPLE_MESSAGING + " found: ",
          lstND.getLength());
      LOG.logError(l, errmsg, null);
      throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg, sv);
    }

    Element elmn = (Element) lstND.item(0); // expected only one
    Messaging msgHeader = null;
    try {
      msgHeader = (Messaging) XMLUtils.deserialize(elmn, Messaging.class);
    } catch (JAXBException ex) {
      String errmsg = EBMSErrorMessage.INVALID_HEADER_ERROR_PARSING_MESSAGING + ex.getMessage();
      LOG.logError(l, errmsg, ex);
      throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg, sv);
    }

    if (msgHeader == null) {
      String errmsg = EBMSErrorMessage.INVALID_HEADER_MISSING_MESSAGING;
      LOG.logError(l, errmsg, null);
      throw new EBMSError(EBMSErrorCode.InvalidHeader, null, errmsg, sv);
    }

    String lstErrors =
        XMLUtils
        .validateBySchema(msgHeader,
            Messaging.class.getResourceAsStream("/schemas/ebms-header-3_0-200704.xsd"),
            "/schemas/");
    if (!lstErrors.isEmpty()) {
      String errmsg = EBMSErrorMessage.INVALID_HEADER_MESSAGING + lstErrors;
      LOG.logError(l, errmsg, null);
      throw new EBMSError(EBMSErrorCode.InvalidHeader, EBMSBuilder.getFirstMessageId(msgHeader),
          errmsg, sv);
    }

    // zero signal or usermessage is expected
    if (msgHeader.getUserMessages().isEmpty() && msgHeader.getSignalMessages().isEmpty()) {
      String errmsg = EBMSErrorMessage.INVALID_HEADER_MESSAGING_EMPTY;
      LOG.logError(l, errmsg, null);
      throw new EBMSError(EBMSErrorCode.InvalidHeader, EBMSBuilder.getFirstMessageId(msgHeader),
          errmsg, sv);
    }
    // only one user message is expected
    if (msgHeader.getUserMessages().size() > 1) {
      String errmsg = EBMSErrorMessage.INVALID_HEADER_USER_MESSAGE_COUNT + "found: " +
          msgHeader.getUserMessages().size();
      LOG.logError(l, errmsg, null);
      throw new EBMSError(EBMSErrorCode.InvalidHeader, EBMSBuilder.getFirstMessageId(msgHeader),
          errmsg, sv);
    }

    return msgHeader;
  }

  /**
   * Method validates UserMessageUnit: validate payload validate properties validate signature
   *    * <ul>
   * <li>Payload (if referenced payloads exists)</li>
   * <li>Existence and type of value: aggrement, service</li>
   * </ul>
   * Method validates: validate payload validate properties validate signature
   *
   * @param soap
   * @param um - user message
   * @param sv
   * @throws EBMSError
   */
  public void vaildateUserMessage(SoapMessage soap, UserMessage um, QName sv) {
    long l = LOG.logStart();
    String msgId = EBMSBuilder.getUserMessageId(um);
    if (um.getMpc() == null) {
      LOG.logWarn(l, "Null MPC for inbound user message: '" + msgId + "'.", null);

    }

    CollaborationInfo ci = um.getCollaborationInfo();
    // check Agreement ref
    AgreementRef ar = ci.getAgreementRef();
    if (ar != null && ar.getValue() != null &&
        (ar.getType() == null || ar.getType().trim().isEmpty()) &&
        !SoapUtils.isValidURI(ar.getValue())) {
      // If the type attribute is not present, the content of the eb:AgreementRef element
      // MUST be a URI. 
      String msg = EBMSErrorMessage.INVALID_AGR_REF_URI + ". Message agreement reference: '" +
          ar.getValue() + "'";
      throw new EBMSError(EBMSErrorCode.ValueInconsistent, msgId,
          msg, sv);
    }

    // If the type attribute is not present, the content of the Service element MUST be a URI 
    if (!SoapUtils.isValidURI(ci.getService().getValue())) {

      String msg = "Service : '" + ci.getService().getValue() + "' for 'null' type MUST be a URI" +
          " - check ebms specifications!";
      throw new EBMSError(EBMSErrorCode.ValueInconsistent, msgId,
          msg, sv);
    }

    //When the value of this element is
    //http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/test, then the eb:Service
    //element MUST have the value 
    //http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/service
    if (ci.getAction() != null && ci.getAction().equals(EBMSConstants.TEST_ACTION) &&
        !ci.getService().getValue().equals(EBMSConstants.TEST_SERVICE)) {
      String msg = "Service value for action '" + EBMSConstants.TEST_ACTION + "' must be: '" +
          EBMSConstants.TEST_SERVICE + "'! - check ebms specifications!";
      throw new EBMSError(EBMSErrorCode.ValueInconsistent, msgId,
          msg, sv);

    }

    // validate payload
    PayloadInfo pi = um.getPayloadInfo();
    if (pi == null || pi.getPartInfos().isEmpty()) {
      SOAPMessage sm = soap.getContent(SOAPMessage.class);
      try {
        if (sm.getSOAPBody().hasChildNodes()) {
          String msg = "If there is no application payload within the Message Package, then the " +
              "SOAP Body MUST be empty";
          throw new EBMSError(EBMSErrorCode.ValueInconsistent, msgId,
              msg, sv);
        }
      } catch (SOAPException ex) {
        throw new EBMSError(EBMSErrorCode.ApplicationError, msgId,
            "Error retrieving soap body" + ex.getMessage(), sv);
      }
      if (soap.getAttachments() != null && soap.getAttachments().size() > 0) {
        LOG.logWarn(l, "Message: '" + msgId +
            "' has empty PayloadInfo, but contains soap attachments. " +
            "Attachments will be ignored." +
            "Attachment count: " + soap.getAttachments().size(), null);
      }

    } else {
      List<String> attIDS = new ArrayList<>();
      if (soap.getAttachments() != null) {
        soap.getAttachments().stream().forEach((at) -> {
          attIDS.add(at.getId());
        });
      }

      for (PartInfo part : pi.getPartInfos()) {
        String href = part.getHref();
        if (href != null) {
          if (href.toLowerCase().startsWith(EBMSConstants.ATT_CID_PREFIX) &&
              !attIDS.contains(href.substring(4))) {
            throw new EBMSError(EBMSErrorCode.ValueInconsistent, msgId,
                "Attachment: '" + href.substring(4) + "' not in soap request (found " + String.join(
                ",",
                attIDS) + ")", sv);
          } else if (href.startsWith("#")) { // validate # after encryption                      
            throw new EBMSError(EBMSErrorCode.FeatureNotSupportedFailure, msgId,
                "eb:Messaging/eb:UserMessage/eb:PayloadInfo/eb:PartInfo/@href for # not supported",
                sv);
          }
        }
      }
    }
    LOG.logEnd(l, msgId);
  }

  /**
   * Method validates: - SOAP Version (AS4 SOAP 1.2), - existance of Messaging header. -
   *
   * @param soap
   * @param sm
   * @param sv
   * @throws EBMSError
   */
  public void vaildateSignalMessage(SoapMessage soap, SignalMessage sm, QName sv) {
    long l = LOG.logStart();
    String msgId = EBMSBuilder.getSignalMessageId(sm);
    for (Element el : sm.getAnies()) {
      if (!el.getLocalName().equals("EncryptedKey")) {
        throw new EBMSError(EBMSErrorCode.FeatureNotSupportedFailure, msgId,
            "Signal type " + el.getTagName() + " not suppored!", sv);

      }else {
        // add anies to exchange
        soap.getExchange().put("SIGNAL_ELEMENTS", sm.getAnies()); // TODO
      }
    }
    


    if (sm.getPullRequest() != null) {
      throw new EBMSError(EBMSErrorCode.FeatureNotSupportedFailure, msgId,
          "Signal PullRequest not suppored!", sv);
    }

    LOG.logEnd(l, msgId);
  }

}
