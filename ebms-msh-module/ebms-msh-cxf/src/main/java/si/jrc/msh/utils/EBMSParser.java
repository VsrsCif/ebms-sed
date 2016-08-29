/*
 * Copyright 2016, Supreme Court Republic of Slovenia
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

import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.inbox.payload.MSHInPart;
import org.msh.ebms.inbox.payload.MSHInPayload;
import org.msh.ebms.inbox.property.MSHInProperties;
import org.msh.ebms.inbox.property.MSHInProperty;
import org.msh.sed.pmode.PMode;
import org.msh.sed.pmode.PartyIdentitySet;
import org.msh.sed.pmode.Security;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.AgreementRef;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.From;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartInfo;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyId;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Property;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.To;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage;
import si.jrc.msh.exception.EBMSError;
import si.jrc.msh.exception.EBMSErrorCode;
import si.jrc.msh.exception.EBMSErrorMessage;
import si.sed.commons.exception.PModeException;
import si.sed.commons.interfaces.PModeInterface;
import si.sed.commons.pmode.EBMSMessageContext;
import si.sed.commons.pmode.PModeUtils;

import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.Utils;

/**
 *
 * @author Jože Rihtaršič
 */
public class EBMSParser {

  private static final String ID_PREFIX = "SED-";
  private static final SEDLogger LOG = new SEDLogger(EBMSParser.class);

  /**
   * Method parses UserMessage. Message is exptected to be valid by schema 
   * http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/core/ebms-header-3_0-200704.xsd.  else
   * nullpointer exception could be trown
   * @param um
   * @param ectx
   * @param sv
   * @return
   * @throws EBMSError
   */
  public MSHInMail parseUserMessage(UserMessage um, EBMSMessageContext ectx, QName sv)
      throws EBMSError {
    long l = LOG.logStart();

    MessageInfo mi = um.getMessageInfo();
    CollaborationInfo ca = um.getCollaborationInfo();
    

    MSHInMail mshmail = new MSHInMail();
    mshmail.setService(ca.getService().getValue());
    mshmail.setAction(ca.getAction());
    mshmail.setConversationId(ca.getConversationId());
    mshmail.setRefToMessageId(mi.getRefToMessageId());
    mshmail.setSentDate(mi.getTimestamp());
    mshmail.setMessageId(mi.getMessageId());
    
    // parse properties
    if (um.getMessageProperties() != null && !um.getMessageProperties().getProperties().isEmpty()) {
      List<MSHInProperty> lstProp = new ArrayList<>();
      for (Property p : um.getMessageProperties().getProperties()) {
        if (p.getName() != null) {
          switch (p.getName()) {
            case EBMSConstants.EBMS_PROPERTY_DESC:
              mshmail.setSubject(p.getValue());
              break;
            case EBMSConstants.EBMS_PROPERTY_SUBMIT_DATE:
              Date dt = DatatypeConverter.parseDateTime(p.getValue()).getTime();
              mshmail.setSubmittedDate(dt);
              break;
            default:
              MSHInProperty mp = new MSHInProperty();
              mp.setName(p.getName());
              mp.setValue(p.getValue());
              lstProp.add(mp);
          }
        } else {
          MSHInProperty mp = new MSHInProperty();
          mp.setName(p.getName());
          mp.setValue(p.getValue());
          lstProp.add(mp);
        }
      }
      if (!lstProp.isEmpty()) {
        MSHInProperties mop = new MSHInProperties();
        mop.getMSHInProperties().addAll(lstProp);
        mshmail.setMSHInProperties(mop);
      }
    }
    
    // receiver
     for (PartyId pi : um.getPartyInfo().getTo().getPartyIds()) {
      if (pi.getType() != null) {
        String val = pi.getValue();
        if (Utils.isEmptyString(val)){
          continue;
        }
        switch (pi.getType()) {
          case EBMSConstants.EBMS_PARTY_TYPE_EBOX:
            mshmail.setReceiverEBox(val);
            break;
          case EBMSConstants.EBMS_PARTY_TYPE_NAME:
            mshmail.setReceiverName(val);
            break;
          default:
            if (Utils.isEmptyString(mshmail.getReceiverEBox())){
              if (val.endsWith("@" +ectx.getReceiverPartyIdentitySet().getDomain())){
                mshmail.setReceiverEBox(val);
              } else {
                mshmail.setReceiverEBox(val + "@" +ectx.getReceiverPartyIdentitySet().getDomain());
              }
            }
            if (Utils.isEmptyString(mshmail.getReceiverName())){
              mshmail.setReceiverName(val);
            }
            mshmail.setReceiverEBox(pi.getValue());
            String msgwrn =
                "Unknown type '" + pi.getType() + "' for To/PartyId: with value:'" + pi.getValue() +
                "'. Value is setted as receiverId!";
            LOG.logWarn(l, msgwrn, null);
            break;
        }
      } else {
        String msgwrn =
            "Missing type for To/PartyId: with value:'" + pi.getValue() + "'. Value is ignored!";
        LOG.logWarn(l, msgwrn, null);

      }
    }
    

    for (PartyId pi : um.getPartyInfo().getFrom().getPartyIds()) {
      if (pi.getType() != null) {
        String val = pi.getValue();
        if (Utils.isEmptyString(val)){
          continue;
        }
        switch (pi.getType()) {
          case EBMSConstants.EBMS_PARTY_TYPE_EBOX:
            mshmail.setSenderEBox(pi.getValue());
            break;
          case EBMSConstants.EBMS_PARTY_TYPE_NAME:
            mshmail.setSenderName(pi.getValue());
            break;
          default:
             if (Utils.isEmptyString(mshmail.getSenderEBox())){
              if (val.endsWith("@" +ectx.getSenderPartyIdentitySet().getDomain())){
                mshmail.setSenderEBox(val);
              } else {
                mshmail.setSenderEBox(val + "@" +ectx.getSenderPartyIdentitySet().getDomain());
              }
            }
            if (Utils.isEmptyString(mshmail.getSenderName())){
              mshmail.setSenderName(val);
            }
    
            String msgwrn =
                "Unknown type '" + pi.getType() + "' for From/PartyId: with value:'" +
                pi.getValue() + "'. Value is setted as sender address!";
            LOG.logWarn(l, msgwrn, null);

            break;
        }
      } else {
        String msgwrn =
            "Missing type for From/PartyId: with value:'" + pi.getValue() + "'. Value is ignored!";
        LOG.logWarn(l, msgwrn, null);
      }
    }

 

    for (PartyId pi : um.getPartyInfo().getTo().getPartyIds()) {
      if (pi.getType() != null) {
        switch (pi.getType()) {
          case EBMSConstants.EBMS_PARTY_TYPE_EBOX:
            mshmail.setReceiverEBox(pi.getValue());
            break;
          case EBMSConstants.EBMS_PARTY_TYPE_NAME:
            mshmail.setReceiverName(pi.getValue());
            break;
          default:
            mshmail.setReceiverEBox(pi.getValue());
            String msgwrn =
                "Unknown type '" + pi.getType() + "' for To/PartyId: with value:'" + pi.getValue() +
                "'. Value is setted as receiverId!";
            LOG.logWarn(l, msgwrn, null);
            break;
        }
      } else {
        String msgwrn =
            "Missing type for To/PartyId: with value:'" + pi.getValue() + "'. Value is ignored!";
        LOG.logWarn(l, msgwrn, null);

      }
    }

    if (mshmail.getReceiverEBox() == null) {
      String errmsg = "Missing receipt type: '" + EBMSConstants.EBMS_PARTY_TYPE_EBOX + "' ";
      throw new EBMSError(EBMSErrorCode.MissingReceipt, mshmail.getMessageId(), errmsg, sv);
    }

    if (um.getPayloadInfo() != null && !um.getPayloadInfo().getPartInfos().isEmpty()) {
      List<MSHInPart> lstParts = new ArrayList<>();

      for (PartInfo pi : um.getPayloadInfo().getPartInfos()) {
        MSHInPart part = new MSHInPart();
        String href = pi.getHref();
        if (href != null) {
          if (href.startsWith("cid:")) {
            part.setEbmsId(pi.getHref().substring(4)); // remove cid
          } else if (href.startsWith("#")) {
            part.setEbmsId(pi.getHref().substring(1)); // remove hash
          } else {
            part.setEbmsId(pi.getHref());
          }
        }
        part.setDescription(pi.getDescription() != null ? pi.getDescription().getValue() : null);
        if (pi.getPartProperties() != null && !pi.getPartProperties().getProperties().isEmpty()) {
          for (Property p : pi.getPartProperties().getProperties()) {
            if (p.getName() != null) {
              switch (p.getName()) {
                case EBMSConstants.EBMS_PAYLOAD_PROPERTY_NAME:
                  part.setName(p.getValue());
                  break;
                case EBMSConstants.EBMS_PAYLOAD_PROPERTY_FILENAME:
                  part.setFilename(p.getValue());
                  break;
                case EBMSConstants.EBMS_PAYLOAD_PROPERTY_MIME:
                  part.setMimeType(p.getValue());
                  break;
                case EBMSConstants.EBMS_PAYLOAD_PROPERTY_ENCODING:
                  part.setEncoding(p.getValue());
                  break;
                case EBMSConstants.EBMS_PAYLOAD_COMPRESSION_TYPE:
                  part.setType(p.getValue());
                  break;
                case EBMSConstants.EBMS_PAYLOAD_PROPERTY_IS_ENCRYPTED:
                  part.setIsEncrypted(p.getValue() != null && p.getValue().equalsIgnoreCase("true"));
                  break;
                default:
                  LOG.logWarn(l, "Unknown part property: '" + p.getName() + "' for message: '" +
                      mshmail.getMessageId() + "' ", null);
              }
            }
          }
        }

        lstParts.add(part);
      }
      if (!lstParts.isEmpty()) {
        mshmail.setMSHInPayload(new MSHInPayload());
        mshmail.getMSHInPayload().getMSHInParts().addAll(lstParts);
      }
    }

    return mshmail;
  }

  public static EBMSMessageContext createEBMSContextFromUserMessage(final SoapMessage soap,
      final UserMessage um, final PModeInterface pmdManager) {
    long l = LOG.logStart();

    // UserMessage was validated by schema so this values should not be null
    From ebmsFromParty = um.getPartyInfo().getFrom();
    To ebmsToParty = um.getPartyInfo().getTo();
    Service ebmsService = um.getCollaborationInfo().getService();
    String ebmsAction = um.getCollaborationInfo().getAction();
    String embsMessageId = um.getMessageInfo().getMessageId();
    // get local entites
    PartyIdentitySet pisFrom = null;
    try {
      pisFrom = getPartyIdentityFromPartyId(ebmsFromParty.getPartyIds(), pmdManager);
    } catch (PModeException ex) {
      String msg = "Error reading sender (from) partyID: " + ex.getMessage();
      throw new EBMSError(EBMSErrorCode.ValueNotRecognized, embsMessageId,
          msg, ex, SoapFault.FAULT_CODE_SERVER);
    }

    if (pisFrom == null) {
      String msg = "No party found for sender (from) partyID";
      throw new EBMSError(EBMSErrorCode.ValueNotRecognized, embsMessageId,
          msg, SoapFault.FAULT_CODE_SERVER);
    }

    PartyIdentitySet pisTo;
    try {
      pisTo = getPartyIdentityFromPartyId(ebmsToParty.getPartyIds(), pmdManager);
    } catch (PModeException ex) {
      String msg = "Error reading sender (from) partyID: " + ex.getMessage();
      throw new EBMSError(EBMSErrorCode.ValueNotRecognized, embsMessageId,
          msg, ex, SoapFault.FAULT_CODE_SERVER);
    }
    if (pisTo == null) {
      String msg = EBMSErrorMessage.INVALID_HEADER_DATA + "No party found for receiver (to) partyID";
      throw new EBMSError(EBMSErrorCode.ValueNotRecognized, embsMessageId,
          msg, SoapFault.FAULT_CODE_SERVER);
    }

    EBMSMessageContext ebctx = new EBMSMessageContext();
    try {
      // get service
      org.msh.sed.pmode.Service srv =
          pmdManager.getServiceByNameAndTypeAndAction(
              ebmsService.getValue(), ebmsService.getType(), ebmsAction);

      // get pmode
      PMode pmd = pmdManager.getPModeForExchangePartyAsSender(pisFrom.getId(),
          ebmsFromParty.getRole(),
          pisTo.getId(), srv.getId());

      PModeUtils.fillTransportMEPForAction(ebctx, ebmsAction, pmd);

      Security security = null;
      // retrieve security
      if (ebctx.getTransportChannelType() != null &&
          !Utils.isEmptyString(ebctx.getTransportChannelType().getSecurityIdRef())) {

        security = pmdManager.getSecurityById(
            ebctx.getTransportChannelType().getSecurityIdRef());

      } else {
        LOG.logWarn(String.format(
            "Action '%s' for mail '%s' has no security defined  in pmode (id: '%s') configuration.",
            ebmsAction, embsMessageId, pmd.getId()), null);
      }

      ebctx.setSecurity(security);
      ebctx.setService(srv);
      ebctx.setPMode(pmd);
      ebctx.setReceiverPartyIdentitySet(pisTo);
      ebctx.setSenderPartyIdentitySet(pisFrom);
      ebctx.setSendingRole(ebmsFromParty.getRole());

    } catch (PModeException ex) {
      String msg = EBMSErrorMessage.INVALID_HEADER_DATA + ex.getMessage();
      throw new EBMSError(EBMSErrorCode.ValueNotRecognized, embsMessageId,
          msg, ex, SoapFault.FAULT_CODE_SERVER);
    }

    AgreementRef ar = um.getCollaborationInfo().getAgreementRef();
    if (ar != null && !Utils.isEmptyString(ar.getValue())) {
      PMode arl;
      try {
        arl =
            pmdManager.getByAgreementRef(ar.getValue(),
                ar.getType(), ar.getPmode());
      } catch (PModeException ex) {
        String msg = "Error reading agreement: " + ex.getMessage();
      throw new EBMSError(EBMSErrorCode.ValueNotRecognized, embsMessageId,
          msg, ex, SoapFault.FAULT_CODE_SERVER);
      }

      if (Objects.equal(arl.getId(), ebctx.getPMode().getId())) {
        String msg = String.format(
            "Agreement '%s' for message '%s' does not match agreement for sender, receiver and service.",
            ar.getValue(), embsMessageId);
        LOG.logError(0, msg + String.format(" Agrement pmode: '%s', selected pmodet '%s' ",
            arl.getId(), ebctx.getPMode().getId()), null);
        throw new EBMSError(EBMSErrorCode.ValueNotRecognized, embsMessageId,
            msg, null, SoapFault.FAULT_CODE_SERVER);
      }
    }
    return ebctx;

  }

  public static PartyIdentitySet getPartyIdentityFromPartyId(List<PartyId> lstPI,
      final PModeInterface pmdManager)
      throws PModeException {
    PartyIdentitySet pis = null;
    for (PartyId pi : lstPI) {
      LOG.formatedlog("getPartyIdentityFromPartyId for id: %s, type %s", pi.getValue(), pi.getType());
      pis = pmdManager.getPartyIdentitySetForPartyId(pi.getType(), pi.getValue());
      if (pis != null) {
        break;
      }
    }
    return pis;

  }
}
