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

import java.util.Objects;
import org.msh.sed.pmode.AgreementRef;
import org.msh.sed.pmode.MEPLegType;
import org.msh.sed.pmode.MEPTransportType;
import org.msh.sed.pmode.MEPType;

import org.msh.sed.pmode.PMode;
import org.msh.sed.pmode.Service;
import org.msh.sed.pmode.Service.Action;
import org.msh.sed.pmode.TransportChannelType;
import si.sed.commons.exception.PModeException;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author Jože Rihtaršič
 */
public class PModeUtils {

  public static final SEDLogger LOG = new SEDLogger(FilePModeManager.class);

  /**
   * Method returns MEP for action in PMode
   *
   * @param ctx
   * @param action
   * @param pm
   * @throws si.sed.commons.exception.PModeException
   */
  public static void fillTransportMEPForAction(EBMSMessageContext ctx, String action, PMode pm)
      throws PModeException {

    for (MEPType mt : pm.getMEPS()) {
      TransportChannelType tct = null;
      MEPLegType mepLeg= null;
      boolean foreChannel = true;
      for (MEPLegType mlt : mt.getLegs()) {
        MEPTransportType tr = mlt.getTransport();
        if (tr == null) {
          LOG.formatedWarning("Bad MEP definition leg with no Transport element in PMode: '%s' !",
              pm.getId());
          continue;
        }
        if (tr.getForeChannelMessage() != null && Objects.equals(
            tr.getForeChannelMessage().getAction(), action)) {
          mepLeg = mlt;
          tct = tr.getForeChannelMessage();
          foreChannel = true;
          break;
        } else if (tr.getBackChannelMessage() != null && Objects.equals(
            tr.getBackChannelMessage().getAction(), action)) {
          mepLeg = mlt;
          tct = tr.getBackChannelMessage();
          foreChannel = false;
          break;
        }
      }
      if (tct!=null) {
        ctx.setMEPType(mt);
        ctx.setMEPLegType(mepLeg);
        ctx.setTransportChannelType(tct);
        ctx.setPushTransfrer(foreChannel);
        break;
      }
    }
    if (ctx.getTransportChannelType() == null) {   
      throw new PModeException(
          String.format("PMode '%s' does not have MEP for action '%s'.", pm.getId(), action));
    }
  }

  /**
   * Method extract AgreementRef from pmode for exchange partyId:
   *
   * @param partyId - party
   * @param pm - PMode objects
   * @return AgreementRef object or null if not exists
   */
  public static AgreementRef getAgreementRefForExchangePartyId(String partyId, PMode pm) {
    if (pm.getExchangeParties() != null && !pm.getExchangeParties().getPartyInfos().isEmpty()) {
      for (PMode.ExchangeParties.PartyInfo pf : pm.getExchangeParties().getPartyInfos()) {
        if (Objects.equals(pf.getPartyIdentitySetIdRef(), partyId)) {
          return pf.getAgreementRef();
        }
      }
    }
    return null;
  }

  /**
   * Method returs action from service. If action not exists Exception is thrown
   *
   * @param action
   * @param srv - service
   * @return Service.Action - if exists else PModeException is thrown
   * @throws PModeException
   */
  public static Action getActionFromService(String action, Service srv)
      throws PModeException {
    Service.Action act = null;
    for (Service.Action a : srv.getActions()) {
      if (Objects.equals(a.getName(), action)) {
        act = a;
        break;
      }
    }
    if (act == null) {
      throw new PModeException(String.format("No action '%s' in service '%s'.",
          action, srv.getId()));
    }
    return act;
  }
  

}
