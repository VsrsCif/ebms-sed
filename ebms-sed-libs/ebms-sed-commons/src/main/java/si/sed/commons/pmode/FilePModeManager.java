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
package si.sed.commons.pmode;

import java.io.File;
import static java.io.File.separator;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.bind.JAXBException;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.sed.pmode.AgreementRef;
import org.msh.sed.pmode.MSHSetings;
import org.msh.sed.pmode.PMode;
import org.msh.sed.pmode.PartyIdentitySet;
import org.msh.sed.pmode.PartyIdentitySetType;
import org.msh.sed.pmode.ReceptionAwareness;
import org.msh.sed.pmode.Security;
import org.msh.sed.pmode.Service;
import si.sed.commons.PModeConstants;
import static si.sed.commons.SEDSystemProperties.SYS_PROP_HOME_DIR;
import static si.sed.commons.SEDSystemProperties.SYS_PROP_PMODE;
import static si.sed.commons.SEDSystemProperties.SYS_PROP_PMODE_DEF;
import si.sed.commons.exception.PModeException;
import si.sed.commons.interfaces.PModeInterface;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.Utils;
import static si.sed.commons.utils.xml.XMLUtils.deserialize;
import static si.sed.commons.utils.xml.XMLUtils.serialize;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class FilePModeManager implements PModeInterface {

  /**
   *
   */
  public static final SEDLogger LOG = new SEDLogger(FilePModeManager.class);

  List<PMode> mlstPModes = new ArrayList<>();
  Map<String, PartyIdentitySet> mmpPartyIdentites = new HashMap<>();
  Map<String, ReceptionAwareness> mmpReceptionAwareness = new HashMap<>();
  Map<String, Security> mmpSecurity = new HashMap<>();
  Map<String, Service> mmpServiceDef = new HashMap<>();

  MSHSetings mshSettings = null;

  public FilePModeManager() {
  }

  public FilePModeManager(InputStream is)
      throws PModeException {
    reloadPModes(is);
  }

  /**
   *
   * @param pmrNew
   * @return
   */
  public boolean add(PMode pmrNew) {
    return mshSettings.getPModes().add(pmrNew);

  }

  /**
   *
   * @param i
   * @param pmrNew
   */
  public void add(int i, PMode pmrNew) {
    mshSettings.getPModes().add(i, pmrNew);
  }

  public void clear() {
    mlstPModes.clear();
    mmpServiceDef.clear();
    mmpPartyIdentites.clear();
    mmpReceptionAwareness.clear();
    mmpSecurity.clear();
  }

  @Override
  public EBMSMessageContext createMessageContextForOutMail(MSHOutMail mail)
      throws PModeException {
    EBMSMessageContext emc = new EBMSMessageContext();
    // retrieve data
    PartyIdentitySet sPID = getPartyIdentitySetForSEDAddress(mail.getSenderEBox());
    if (!sPID.getIsLocalIdentity()) {
      String msg = String.format(
          "Sender '%s' (identityId '%s') for mail '%d' is not local identity and can not send messages!",
          mail.getSenderEBox(), sPID.getId(), mail.getId());
      LOG.logWarn(msg, null);
      throw new PModeException(msg);
    }

    PartyIdentitySet rPID = getPartyIdentitySetForSEDAddress(mail.getReceiverEBox());
    Service srv = getServiceById(mail.getService());
    Service.Action act = PModeUtils.getActionFromService(mail.getAction(), srv);

    PMode pMode = getPModeForLocalPartyAsSender(sPID.getId(), act.getSendingRole(),
        rPID.getId(),
        mail.getService());

    AgreementRef ar = PModeUtils.getAgreementRefForExchangePartyId(rPID.getId(), pMode);
    // fill transprot type, transportchanneltype
    PModeUtils.fillTransportMEPForAction(emc, act.getName(), pMode);
    //  get security
    Security security = null;
    if (!Utils.isEmptyString(emc.getTransportChannelType().getSecurityIdRef())) {
      security = getSecurityById(emc.getTransportChannelType().getSecurityIdRef());
    }
    // set getReceptionAwareness
    ReceptionAwareness ra = null;
    if (emc.getTransportChannelType().getReceptionAwareness() != null &&
        !Utils.isEmptyString(
            emc.getTransportChannelType().getReceptionAwareness().getRaPatternIdRef())) {
      ra = getReceptionAwarenessById(
          emc.getTransportChannelType().getReceptionAwareness().getRaPatternIdRef());
    }

    // get transport data for push!
    PartyIdentitySetType.TransportProtocol transport = null;
    if (emc.isPushTransfrer()) {
      String transportId = null;
      for (PMode.ExchangeParties.PartyInfo epi : pMode.getExchangeParties().getPartyInfos()) {
        // get def transport id
        if (Objects.equals(epi.getPartyIdentitySetIdRef(), rPID.getId())) {
          transportId = epi.getPartyDefTransportIdRef();
          break;
        }
      }
      if (transportId == null) {
        if (rPID.getTransportProtocols().size() > 0) {
          transport = rPID.getTransportProtocols().get(0);
          LOG.formatedWarning(
              "PMode: '%s' does not have defined transport for exchange party '%s'. First transport '%s' will be used!",
              pMode.getId(), rPID.getId(), transport.getId());
        } else {
          throw new PModeException(
              String.format(
                  "PMode: '%s' does not have defined transport for exchange party '%s'. Action %s in MEP is pushed!  ",
                  pMode.getId(), rPID.getId(), act.getName()));
          
        }
      } else {
        for (PartyIdentitySetType.TransportProtocol tp : rPID.getTransportProtocols()) {
          if (Objects.equals(tp.getId(), transportId)) {
            transport = tp;
            break;
          }
        }
      }
    }
    //receiving role
    String recRole = Objects.equals(srv.getInitiator().getRole(), act.getSendingRole()) ?
        srv.getExecutor().getRole() : srv.getInitiator().getRole();

    // set context
    emc.setAction(act);
    emc.setExchangeAgreementRef(ar);
    emc.setPMode(pMode);
    emc.setReceiverPartyIdentitySet(rPID);
    emc.setSenderPartyIdentitySet(sPID);
    emc.setSecurity(security);
    emc.setSendingRole(act.getSendingRole());
    emc.setReceivingRole(recRole);
    emc.setService(srv);
    emc.setTransportProtocol(transport);
    emc.setReceptionAwareness(ra);

    return emc;
  }

  /**
   * Method returs domain by domain and service
   *
   * @param agrRef aggrement ref
   * @param agrRefType aggrement type
   * @param agrPMode
   * @return
   * @throws PModeException
   */
  @Override
  public PMode getByAgreementRef(String agrRef, String agrRefType, String agrPMode)
      throws PModeException {

    for (PMode pm : mshSettings.getPModes()) {
      for (PMode.ExchangeParties.PartyInfo pi : pm.getExchangeParties().getPartyInfos()) {
        if (pi.getAgreementRef() != null && pi.getAgreementRef().getValue().equals(agrRef)) {
          return pm;
        }

      }
    }
    throw new PModeException(String.format(
        "AgreementRef for value: '%s', type: '%s' pmode: '%s' not exist.", agrRef, agrRefType,
        agrPMode));

  }

  /**
   *
   * @param pModeId
   * @return
   * @throws PModeException
   */
  @Override
  public PMode getPModeById(String pModeId)
      throws PModeException {

    for (PMode pm : mlstPModes) {
      if (Objects.equals(pm.getId(), pModeId)) {
        return pm;
      }
    }
    throw new PModeException(String.format("No pmode for id: '%s'.", pModeId));
  }

  /**
   *
   * @return
   */
  public String getPModeFilePath() {
    return getProperty(SYS_PROP_HOME_DIR) + separator +
        getProperty(SYS_PROP_PMODE, SYS_PROP_PMODE_DEF);
  }

  /**
   * Method returs PMODE for given exchange sender Party. If pmode not exists or more than one PMode
   * is defined for given parameters PModeException is thrown.
   *
   * @param senderRefId - exchange sender party identity set id
   * @param actionSendingRole - bussines role sender must have in current action
   * @param receiverRefId - local receiver party idetity set id
   * @param serviceId - bussines service
   * @return PMode
   * @throws PModeException
   */
  @Override
  public PMode getPModeForExchangePartyAsSender(String senderRefId, String actionSendingRole,
      String receiverRefId, String serviceId)
      throws PModeException {

    List<PMode> lstResult = new ArrayList<>();
    for (PMode pm : getPModeList()) {
      // check if service match
      if (pm.getServiceIdRef() == null ||
          !Objects.equals(pm.getServiceIdRef().getValue(), serviceId)) {
        continue;
      }
      // check if local party match as receiver
      if (pm.getLocalPartyInfo() == null ||
          !Objects.equals(pm.getLocalPartyInfo().getPartyIdentitySetIdRef(), receiverRefId)) {
        continue;
      }
      // check if exchange party list is not null
      if (pm.getExchangeParties() == null || pm.getExchangeParties().getPartyInfos().isEmpty()) {
        continue;
      }

      // check if exchange party match
      boolean epmatch = false;
      for (PMode.ExchangeParties.PartyInfo ep : pm.getExchangeParties().getPartyInfos()) {
        if (Objects.equals(ep.getPartyIdentitySetIdRef(), senderRefId) // if role match if role is null all roles are ok.
            &&
             (Utils.isEmptyString(actionSendingRole) ||
            ep.getRoles().contains(actionSendingRole))) {
          epmatch = true;
          break;
        }
      }
      if (epmatch) {
        lstResult.add(pm);
      }

    }

    if (lstResult.size() > 1) {
      throw new PModeException(String.format("more than one PMODE for exchange" +
          " senderRefId '%s',actionSendingRole '%s',  receiverRefId '%s', serviceRefId: '%s' ",
          senderRefId, actionSendingRole, receiverRefId, serviceId));
    } else if (lstResult.isEmpty()) {
      throw new PModeException(String.format("No PMODE for exchange" +
          " senderRefId '%s',actionSendingRole '%s',  receiverRefId '%s', serviceRefId: '%s' ",
          senderRefId, actionSendingRole, receiverRefId, serviceId));
    }
    return lstResult.get(0);
  }

  /**
   * Method returs PMODE for given local sender Party. If pmode not exists or more than one PMode is
   * defined for given parameters PModeException is thrown.
   *
   * @param senderRefId - local sender party identity set id
   * @param actionSendingRole - bussines role sender must have in current action
   * @param receiverRefId - exchange receiver party idetity set id
   * @param serviceId - bussines service
   * @return PMode
   * @throws PModeException
   */
  @Override
  public PMode getPModeForLocalPartyAsSender(String senderRefId, String actionSendingRole,
      String receiverRefId, String serviceId)
      throws PModeException {

    List<PMode> lstResult = new ArrayList<>();
    for (PMode pm : getPModeList()) {
      // check if service match
      if (pm.getServiceIdRef() == null ||
          !Objects.equals(pm.getServiceIdRef().getValue(), serviceId)) {
        continue;
      }
      // check if local party match
      if (pm.getLocalPartyInfo() == null ||
          !Objects.equals(pm.getLocalPartyInfo().getPartyIdentitySetIdRef(), senderRefId)) {
        continue;
      }
      // check if sending party contains sending role for action
      if (!Utils.isEmptyString(actionSendingRole) &&
          !pm.getLocalPartyInfo().getRoles().contains(actionSendingRole)) {
        continue;

      }
      // check if exchange party list is not null
      if (pm.getExchangeParties() == null || pm.getExchangeParties().getPartyInfos().isEmpty()) {
        continue;
      }
      // check if exchange party match
      boolean epmatch = false;
      for (PMode.ExchangeParties.PartyInfo ep : pm.getExchangeParties().getPartyInfos()) {
        if (Objects.equals(ep.getPartyIdentitySetIdRef(), receiverRefId)) {
          epmatch = true;
          break;
        }
      }
      if (!epmatch) {
        continue;
      }
      lstResult.add(pm);
    }

    if (lstResult.size() > 1) {
      throw new PModeException(String.format("more than one PMODE for local" +
          " senderRefId '%s',actionSendingRole '%s',  receiverRefId '%s', serviceRefId: '%s' ",
          senderRefId, actionSendingRole, receiverRefId, serviceId));
    } else if (lstResult.isEmpty()) {
      throw new PModeException(String.format("No PMODE for local" +
          " senderRefId '%s',actionSendingRole '%s',  receiverRefId '%s', serviceRefId: '%s' ",
          senderRefId, actionSendingRole, receiverRefId, serviceId));
    }
    return lstResult.get(0);
  }

  /**
   *
   * @return @throws PModeException
   */
  public List<PMode> getPModeList()
      throws PModeException {
    if (mshSettings == null) {
      reloadPModes();
    }
    return mshSettings.getPModes();
  }

  /**
   *
   * @return @throws PModeException
   */
  public MSHSetings getPModes()
      throws PModeException {
    reloadPModes();
    return mshSettings;
  }

  @Override
  public PartyIdentitySet getPartyIdentitySetById(String id) {
    return mmpPartyIdentites.get(id);
  }

  /**
   * Method returs PartyIdentitySet for Authorization. Method is used for pull signal
   *
   * @param username
   * @param password
   * @return first PartyIdentitySet or null if not found
   */
  public PartyIdentitySet getPartyIdentitySetForAuthorization(String username, String password) {
    for (PartyIdentitySet pis : mmpPartyIdentites.values()) {
      if (pis.getAuthorization() != null && username != null && password != null &&
          Objects.equals(pis.getAuthorization().getUsername(), username) &&
          Objects.equals(pis.getAuthorization().getPassword(), password)) {
        return pis;
      }
    }
    return null;
  }

  /**
   * Method returns PartyIdentitySet for partyId and type. Method returs PartyIdentitySet if one of
   * partyID has matches with given type and partyIdValue ebmsMessage
   * <ns2:PartyId type="urn:oasis:names:tc:ebcore:partyid-type:unregistered">test</ns2:PartyId>
   *
   * @param partyType - type of party - urn:oasis:names:tc:ebcore:partyid-type:unregistered
   * @param partyIdValue - partyId value: test
   * @return first PartyIdentitySet or null if not found
   */
  @Override
  public PartyIdentitySet getPartyIdentitySetForPartyId(String partyType, String partyIdValue)
      throws PModeException {

    if (Utils.isEmptyString(partyIdValue)) {
      LOG.logWarn("Empty partyIdValue", null);
      return null;
    }
    List<PartyIdentitySet> candidates = new ArrayList<>();
    for (PartyIdentitySet pis : mmpPartyIdentites.values()) {

      for (PartyIdentitySet.PartyId pid : pis.getPartyIds()) {

        //check if partyType match
        if (!Objects.equals(partyType, pid.getType())) {
          continue;
        }
        String srcType = pid.getValueSource();
        // if pid has source IGNORE -than is not intended for submiting over ebms.
        if (srcType.equals(PModeConstants.PARTY_ID_SOURCE_TYPE_IGNORE)) {
          continue;
        }
        // check if ID "fixed value" than this is target identiy set
        if (!Utils.isEmptyString(pid.getFixValue()) &&
            Objects.equals(pid.getFixValue(), partyIdValue)) {
          return pis;
        }

        // check if ID from list of identifiers
        if (srcType.equals(PModeConstants.PARTY_ID_SOURCE_TYPE_IDENTIFIER) &&
            pid.getIdentifiers().contains(partyIdValue)) {
          return pis;

        }
        // check if ID is SED 
        if (srcType.equals(PModeConstants.PARTY_ID_SOURCE_TYPE_ADDRESS) &&
            partyIdValue.contains("@")) {
          String domain = partyIdValue.substring(partyIdValue.indexOf("@") + 1);

          if (Objects.equals(pis.getDomain(), domain)) {
            candidates.add(pis);
          }
        }
        // ignore other sources
      }
    }

    if (candidates.size() > 1) {
      throw new PModeException(String.format(
          "More than one (%d) for partyType '%s' and value %s!",
          candidates.size(), partyType, partyIdValue));
    }
    return candidates.isEmpty() ? null : candidates.get(0);
  }

  @Override
  public PartyIdentitySet getPartyIdentitySetForSEDAddress(String address)
      throws PModeException {
    if (Utils.isEmptyString(address) || !address.contains("@")) {
      throw new IllegalArgumentException(String.format("SED Address must be " +
          "composed with [localpart]@[domain]. Address '%s'", address));
    }

    String[] addrTb = address.split("@");
    String localPart = addrTb[0];
    String domainPart = addrTb[1];

    int iDomainCount = 0;
    List<PartyIdentitySet> candidates = new ArrayList<>();
    for (PartyIdentitySet pis : mmpPartyIdentites.values()) {
      // check domain
      if (Objects.equals(domainPart, pis.getDomain())) {
        iDomainCount++;
        boolean bContaisIdetifierId = false;
        for (PartyIdentitySetType.PartyId pi : pis.getPartyIds()) {
          if (pi.getValueSource().equals(PModeConstants.PARTY_ID_SOURCE_TYPE_IDENTIFIER)) {
            bContaisIdetifierId = true;
            // if contains with identifier return this Etity-set
            if (pi.getIdentifiers().contains(localPart)) {
              return pis;
            }
          }
        }
        // add only identifier  with no idetifiers
        if (!bContaisIdetifierId) {
          candidates.add(pis);
        }
      }
    }
    if (candidates.isEmpty()) {
      throw new PModeException(String.format(
          "No PartyIdentitySet for address '%s'. Count identitySets for domain %d with no matchin identifiers!",
          address, iDomainCount));
    }

    if (candidates.size() > 1) {
      throw new PModeException(String.format(
          "More than one (%d) PartyIdentitySet found for address '%s'!",
          iDomainCount, address));
    }
    return candidates.get(0);
  }

  @Override
  public ReceptionAwareness getReceptionAwarenessById(String id) {
    return mmpReceptionAwareness.get(id);
  }

  @Override
  public Security getSecurityById(String securityId)
      throws PModeException {
    if (!mmpSecurity.containsKey(securityId)) {
      throw new PModeException(String.format("Security for id: '%s' not exists.", securityId));
    }
    return mmpSecurity.get(securityId);
  }

  public Security getSecuritySetById(String id) {
    return mmpSecurity.get(id);
  }

  @Override
  public Service getServiceById(String serviceId)
      throws PModeException {
    if (!mmpServiceDef.containsKey(serviceId)) {
      throw new PModeException(String.format("Service for id: '%s' not exists.", serviceId));
    }
    return mmpServiceDef.get(serviceId);
  }

  @Override
  public Service getServiceByNameAndTypeAndAction(String serviceName, String serviceType,
      String action)
      throws PModeException {

    for (Service srv : mmpServiceDef.values()) {
      if (!Objects.equals(srv.getServiceName(), serviceName) ||
          !Objects.equals(srv.getServiceType(), serviceType)) {
        continue;
      }

      for (Service.Action act : srv.getActions()) {
        if (Objects.equals(act.getName(), action)) {
          return srv;
        }
      }
    }

    throw new PModeException(String.format(
        "Service with name '%s', serviceType: '%s' and action '%s' not exists!", serviceName,
        serviceType, action));

  }

  /**
   *
   * @throws PModeException
   */
  public void reloadPModes()
      throws PModeException {
    long l = LOG.logStart();
    File pModeFile = new File(getPModeFilePath());
    try (FileInputStream fis = new FileInputStream(pModeFile)) {
      reloadPModes(fis);
    } catch (IOException ex) {
      String msg = "Error init PModes from file '" + pModeFile.getAbsolutePath() + "'";
      throw new PModeException(msg, ex);
    }
    LOG.logEnd(l);
  }

  /**
   * Reload pmodes from input stream. If parse error occurred PModeException is thrown
   *
   * @param is
   * @throws PModeException
   */
  public final void reloadPModes(InputStream is)
      throws PModeException {
    long l = LOG.logStart();
    clear();
    try {

      mshSettings = (MSHSetings) deserialize(is, MSHSetings.class);
      if (mshSettings.getServices() != null) {
        mshSettings.getServices().getServices().forEach((bt) -> {
          mmpServiceDef.put(bt.getId(), bt);
        });
      }
      if (mshSettings.getParties() != null) {
        mshSettings.getParties().getPartyIdentitySets().forEach((party) -> {
          mmpPartyIdentites.put(party.getId(), party);
        });
      }
      if (mshSettings.getReceptionAwarenessPatterns() != null) {
        mshSettings.getReceptionAwarenessPatterns().getReceptionAwarenesses().forEach((ra) -> {
          mmpReceptionAwareness.put(ra.getId(), ra);
        });
      }

      if (mshSettings.getSecurityPatterns() != null) {
        mshSettings.getSecurityPatterns().getSecurities().forEach((sc) -> {
          mmpSecurity.put(sc.getId(), sc);
        });
      }
      mlstPModes.addAll(mshSettings.getPModes());

    } catch (JAXBException ex) {
      String msg = "Error init MSH Settings!";
      throw new PModeException(msg, ex);
    }
    LOG.logEnd(l);
  }

  /**
   *
   * @param pmr
   * @return
   */
  public boolean removePMode(PMode pmr) {
    boolean suc = false;
    if (pmr == null) {
      suc = mshSettings.getPModes().remove(pmr);
    }
    return suc;

  }

  /**
   *
   * @param pModeId
   * @return
   */
  public PMode removePModeById(String pModeId) {
    PMode removed = null;
    for (PMode pm : mshSettings.getPModes()) {
      if (pm.getId() != null && pm.getId().equals(pModeId)) {
        mshSettings.getPModes().remove(pm);
        removed = pm;
        break;
      }
    }
    return removed;

  }

  /**
   *
   * @param pmrNew
   * @param pModeIdOld
   * @return
   */
  public boolean replace(PMode pmrNew, String pModeIdOld) {
    boolean suc = false;
    for (PMode pm : mshSettings.getPModes()) {
      if (pm.getId() != null && pm.getId().equals(pModeIdOld)) {
        int i = mshSettings.getPModes().indexOf(pm);
        mshSettings.getPModes().remove(pm);
        mshSettings.getPModes().add(i, pmrNew);
        suc = true;
        break;
      }
    }
    return suc;
  }

  /**
   *
   * @throws PModeException
   */
  public void savePMode()
      throws PModeException {
    long l = LOG.logStart();
    try {

      File pModeFile = new File(getPModeFilePath());
      int i = 1;
      String fileFormat = getPModeFilePath() + ".%03d";
      File pModeFileTarget = new File(format(fileFormat, i++));

      while (pModeFileTarget.exists()) {
        pModeFileTarget = new File(format(fileFormat, i++));
      }

      move(pModeFile.toPath(), pModeFileTarget.toPath(), REPLACE_EXISTING);

      try (PrintWriter out = new PrintWriter(pModeFile)) {
        serialize(mshSettings, out);
      } catch (JAXBException | FileNotFoundException ex) {
        String msg = "ERROR serialize PMODE: " + ex.getMessage();
        throw new PModeException(msg, ex);
      }

    } catch (IOException ex) {
      String msg = "ERROR saving file: " + ex.getMessage();
      throw new PModeException(msg, ex);
    }
    LOG.logEnd(l);
  }

}
