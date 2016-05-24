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
package si.sed.commons.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.apache.log4j.Logger;
import org.msh.svev.pmode.BusinessInfo;
import org.msh.svev.pmode.Certificate;
import org.msh.svev.pmode.Leg;
import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.PModes;
import org.msh.svev.pmode.Party;
import org.msh.svev.pmode.Protocol;
import org.msh.svev.pmode.ReceptionAwareness;
import org.msh.svev.pmode.References;
import org.msh.svev.pmode.Security;
import org.msh.svev.pmode.X509;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class PModeManager {

    protected final static Logger mlog = Logger.getLogger(PModeManager.class);

    PModes pmodes = null;

    public void PModeManager() {

    }

    public PMode getPModeById(String pModeId) {
        if (pmodes == null) {
            reloadPModes();
        }
        for (PMode pm : pmodes.getPModes()) {
            if (pm.getId() != null && pm.getId().equals(pModeId)) {
                return pm;
            }
        }
        return null;
    }

    public void reloadPModes() {
        File pModeFile = new File(System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator
                + System.getProperty(SEDSystemProperties.SYS_PROP_PMODE, SEDSystemProperties.SYS_PROP_PMODE_DEF));
        try (FileInputStream fis = new FileInputStream(pModeFile)) {
            reloadPModes(fis);
        } catch (IOException ex) {
            mlog.error("Error init PModes from file '" + pModeFile.getAbsolutePath() + "'", ex);
        }
    }

    public void reloadPModes(InputStream is) {

        File pModeFile = new File(System.getProperty(SEDSystemProperties.SYS_PROP_PMODE, SEDSystemProperties.SYS_PROP_PMODE));
        try {
            pmodes = (PModes) XMLUtils.deserialize(is, PModes.class);

            List<String> tmpIds = new ArrayList<>();
            for (PMode pmd : pmodes.getPModes()) {
                if (pmd.getId() == null || pmd.getId().isEmpty()) {
                    mlog.error("Missing ID in pmode! Pmode with null/empty id is ignored!");
                } else if (tmpIds.contains(pmd.getId())) {
                    mlog.error("Duplicate pmode/@id: '" + pmd.getId() + "'!. Second pmode is ignored");
                } else {
                    tmpIds.add(pmd.getId());
                    if (pmd.getExtends() != null && !pmd.getExtends().isEmpty()) {
                        updateExtension(pmd);
                    }
                }
            }
        } catch (JAXBException ex) {
            mlog.error("Error init PModes from file '" + pModeFile.getAbsolutePath() + "'", ex);
        }
    }

    public void updateExtension(PMode pmd) {

        if (pmd.getExtends() != null && !pmd.getExtends().isEmpty()) {
            PMode pmExtension = getPModeById(pmd.getExtends());
            if (pmExtension == null) {
                mlog.error("Extension '" + pmd.getExtends() + "'  for  pmode/@id: '" + pmd.getId() + "' do not exists!");
            } else {
                updateExtension(pmExtension);
                mergeFromTemplate(pmd, pmExtension);
            }
            pmd.setExtends(null);
        }

    }

    public void mergeFromTemplate(PMode target, PMode template) {

        if (isEmpty(target.getAgreement()) && !isEmpty(template.getAgreement())) {
            target.setAgreement(template.getAgreement());
        }

        if (isEmpty(target.getMEP()) && !isEmpty(template.getMEP())) {
            target.setMEP(template.getMEP());
        }

        if (isEmpty(target.getMEPbinding()) && !isEmpty(template.getMEPbinding())) {
            target.setMEPbinding(template.getMEPbinding());
        }

        if (target.getInitiator() == null && template.getInitiator() != null) {
            target.setInitiator(new Party());
            mergeParty(target.getInitiator(), template.getInitiator());
        }
        if (target.getResponder() == null && template.getResponder() != null) {
            target.setResponder(new Party());
            mergeParty(target.getResponder(), template.getResponder());
        }
        mergeLegs(target.getLegs(), template.getLegs());

        if (target.getReceptionAwareness() == null && template.getReceptionAwareness() != null) {
            target.setReceptionAwareness(new ReceptionAwareness());

            mergeReceptionAwareness(target.getReceptionAwareness(), template.getReceptionAwareness());

        }

    }

    public List<Leg> mergeLegs(List<Leg> target, List<Leg> template) {
        for (int i = 0; i < template.size(); i++) {
            if (target.size() <= i) {
                target.add(new Leg());
            }
            Leg ltmp = template.get(i);
            Leg ltrg = target.get(i);

            if (isEmpty(ltrg.getType()) && !isEmpty(ltmp.getType())) {
                ltrg.setType(ltmp.getType());
            }
            if (ltmp.getProtocol() != null) {
                if (ltrg.getProtocol() == null) {
                    ltrg.setProtocol(new Protocol());
                }
                if (ltmp.getProtocol().getAddress() != null) {
                    if (ltrg.getProtocol().getAddress() == null) {
                        ltrg.getProtocol().setAddress(new Protocol.Address());
                    }
                    if (isEmpty(ltrg.getProtocol().getAddress().getValue())
                            && !isEmpty(ltmp.getProtocol().getAddress().getValue())) {
                        ltrg.getProtocol().getAddress().setValue(ltmp.getProtocol().getAddress().getValue());
                    }
                    ltrg.getProtocol().getAddress().setChunked(ltmp.getProtocol().getAddress().getChunked());
                    
                    ltrg.getProtocol().getAddress().setConnectionTimeout(ltmp.getProtocol().getAddress().getConnectionTimeout());
                    ltrg.getProtocol().getAddress().setReceiveTimeout(ltmp.getProtocol().getAddress().getReceiveTimeout());
                }

                if (ltmp.getProtocol().getTLS() != null) {
                    if (ltrg.getProtocol().getTLS() == null) {
                        ltrg.getProtocol().setTLS(new Protocol.TLS());
                    }
                    if (isEmpty(ltrg.getProtocol().getTLS().getClientKeyAlias())
                            && !isEmpty(ltmp.getProtocol().getTLS().getClientKeyAlias())) {
                        ltrg.getProtocol().getTLS().setClientKeyAlias(ltmp.getProtocol().getTLS().getClientKeyAlias());
                    }
                    if (isEmpty(ltrg.getProtocol().getTLS().getTrustCertAlias())
                            && !isEmpty(ltmp.getProtocol().getTLS().getTrustCertAlias())) {
                        ltrg.getProtocol().getTLS().setTrustCertAlias(ltmp.getProtocol().getTLS().getTrustCertAlias());
                    }

                }

                if (ltrg.getProtocol().getSOAPVersion() == null && ltmp.getProtocol().getSOAPVersion() != null) {
                    ltrg.getProtocol().setSOAPVersion(ltmp.getProtocol().getSOAPVersion());
                }
            }

            if (ltmp.getBusinessInfo() != null) {
                if (ltrg.getBusinessInfo() == null) {
                    ltrg.setBusinessInfo(new BusinessInfo());
                }

                mergeBusinessInfo(ltrg.getBusinessInfo(), ltmp.getBusinessInfo());
            }

            if (ltmp.getSecurity() != null) {
                if (ltrg.getSecurity() == null) {
                    ltrg.setSecurity(new Security());
                }
                if (isEmpty(ltrg.getSecurity().getWSSVersion()) && !isEmpty(ltmp.getSecurity().getWSSVersion())) {
                    ltrg.getSecurity().setWSSVersion(ltmp.getSecurity().getWSSVersion());
                }

                if (ltmp.getSecurity().getX509() != null) {
                    if (ltrg.getSecurity().getX509() == null) {
                        ltrg.getSecurity().setX509(new X509());
                    }
                    mergeX509(ltrg.getSecurity().getX509(), ltmp.getSecurity().getX509());
                }

            }

        }
        return target;

    }

    public void mergeX509(X509 target, X509 template) {
        if (template.getSignature() != null) {
            if (target.getSignature() == null) {
                target.setSignature(new X509.Signature());
            }
            if (isEmpty(target.getSignature().getAlgorithm()) && !isEmpty(template.getSignature().getAlgorithm())) {
                target.getSignature().setAlgorithm(template.getSignature().getAlgorithm());
            }
            if (isEmpty(target.getSignature().getHashFunction()) && !isEmpty(template.getSignature().getHashFunction())) {
                target.getSignature().setHashFunction(template.getSignature().getHashFunction());
            }

            if (template.getSignature().getSign() != null) {
                if (target.getSignature().getSign() == null) {
                    target.getSignature().setSign(new X509.Signature.Sign());
                    target.getSignature().getSign().setSignAttachments(template.getSignature().getSign().getSignAttachments());
                    target.getSignature().getSign().setSignElements(template.getSignature().getSign().getSignElements());
                }

                if (template.getSignature().getSign().getElements() != null && !template.getSignature().getSign().getElements().getXPaths().isEmpty()) {
                    if (target.getSignature().getSign().getElements() == null) {
                        target.getSignature().getSign().setElements(new References.Elements());
                    }

                    if (isEmpty(target.getSignature().getSign().getSignCertAlias()) && !isEmpty(template.getSignature().getSign().getSignCertAlias())) {
                        target.getSignature().getSign().setSignCertAlias(template.getSignature().getSign().getSignCertAlias());
                    }
                    mergeXPaths(target.getSignature().getSign().getElements().getXPaths(), template.getSignature().getSign().getElements().getXPaths());
                }
            }

            if (template.getSignature().getCertificate() != null) {
                if (target.getSignature().getCertificate() == null) {
                    target.getSignature().setCertificate(new Certificate());
                }
                if (isEmpty(target.getSignature().getCertificate().getAlias()) && !isEmpty(template.getSignature().getCertificate().getAlias())) {
                    target.getSignature().getCertificate().setAlias(template.getSignature().getCertificate().getAlias());
                }
            }

        }

    }

    public void mergeXPaths(List<References.Elements.XPath> target, List<References.Elements.XPath> template) {
        for (int i = 0; i < template.size(); i++) {
            if (target.size() <= i) {
                target.add(new References.Elements.XPath());
            }
            References.Elements.XPath ltmp = template.get(i);
            References.Elements.XPath ltrg = target.get(i);

            if (isEmpty(ltrg.getXpath()) && !isEmpty(ltmp.getXpath())) {
                ltrg.setXpath(ltmp.getXpath());
            }
            mergeXPathsNamespaces(ltrg.getNamespaces(), ltmp.getNamespaces());
        }
    }

    public void mergeXPathsNamespaces(List<References.Elements.XPath.Namespace> target, List<References.Elements.XPath.Namespace> template) {
        for (int i = 0; i < template.size(); i++) {
            if (target.size() <= i) {
                target.add(new References.Elements.XPath.Namespace());
            }
            References.Elements.XPath.Namespace ltmp = template.get(i);
            References.Elements.XPath.Namespace ltrg = target.get(i);

            if (isEmpty(ltrg.getNamespace()) && !isEmpty(ltmp.getNamespace())) {
                ltrg.setNamespace(ltmp.getNamespace());
            }
            if (isEmpty(ltrg.getPrefix()) && !isEmpty(ltmp.getPrefix())) {
                ltrg.setPrefix(ltmp.getPrefix());
            }
        }
    }

    public void mergeBusinessInfo(BusinessInfo target, BusinessInfo template) {
        if (isEmpty(target.getMPC()) && !isEmpty(template.getMPC())) {
            target.setMPC(template.getMPC());
        }

        if (template.getService() != null) {
            if (target.getService() == null) {
                target.setService(new BusinessInfo.Service());
            }

            if (isEmpty(target.getService().getValue()) && !isEmpty(template.getService().getValue())) {
                target.getService().setValue(template.getService().getValue());
            }
            if (isEmpty(target.getService().getInPlugin()) && !isEmpty(template.getService().getInPlugin())) {
                target.getService().setInPlugin(template.getService().getInPlugin());
            }
            if (isEmpty(target.getService().getOutPlugin()) && !isEmpty(template.getService().getOutPlugin())) {
                target.getService().setOutPlugin(template.getService().getOutPlugin());
            }

        }
        for (BusinessInfo.Action actTmpl : template.getActions()) {
            BusinessInfo.Action actTrg = getActionByValue(target.getActions(), actTmpl.getValue());
            if (actTrg == null) {
                actTrg = new BusinessInfo.Action();
                actTrg.setValue(actTmpl.getValue());
                target.getActions().add(actTrg);
            }
            if (isEmpty(actTrg.getAfter()) && !isEmpty(actTmpl.getAfter())) {
                actTrg.setAfter(actTmpl.getAfter());
            }
            if (isEmpty(actTrg.getDirection()) && !isEmpty(actTmpl.getDirection())) {
                actTrg.setDirection(actTmpl.getDirection());
            }
        }

        for (BusinessInfo.PayloadProfiles ppTmpl : template.getPayloadProfiles()) {
            BusinessInfo.PayloadProfiles actPP = getPayloadProfilesByAction(target.getPayloadProfiles(), ppTmpl.getAction());
            if (actPP == null) {
                actPP = new BusinessInfo.PayloadProfiles();
                actPP.setAction(ppTmpl.getAction());
                target.getPayloadProfiles().add(actPP);
            }
            if (isEmpty(actPP.getMaxSize()) && !isEmpty(ppTmpl.getMaxSize())) {
                actPP.setMaxSize(ppTmpl.getMaxSize());
            }

            for (BusinessInfo.PayloadProfiles.PayloadProfile partTmpl : actPP.getPayloadProfiles()) {

                BusinessInfo.PayloadProfiles.PayloadProfile targetPart = getPayloadProfileByName(actPP.getPayloadProfiles(), partTmpl.getName());
                if (targetPart == null) {
                    targetPart = new BusinessInfo.PayloadProfiles.PayloadProfile();
                    targetPart.setName(partTmpl.getName());
                    actPP.getPayloadProfiles().add(targetPart);
                }
                if (isEmpty(targetPart.getMIME()) && !isEmpty(partTmpl.getMIME())) {
                    targetPart.setMIME(partTmpl.getMIME());
                }
                if (targetPart.getMaxSize() == null && partTmpl.getMaxSize() != null) {
                    targetPart.setMaxSize(partTmpl.getMaxSize());
                }
                if (targetPart.getRequired() == null && partTmpl.getRequired() != null) {
                    targetPart.setRequired(partTmpl.getRequired());
                }

            }
        }

    }

    public void mergeParty(Party target, Party template) {
        if (isEmpty(target.getRole()) && !isEmpty(template.getRole())) {
            target.setRole(template.getRole());
        }

        if (template.getAuthorization() != null) {
            if (target.getAuthorization() == null) {
                target.setAuthorization(new Party.Authorization());
            }

            if (isEmpty(target.getAuthorization().getUsername()) && !isEmpty(template.getAuthorization().getUsername())) {
                target.getAuthorization().setUsername(template.getAuthorization().getUsername());
            }
            if (isEmpty(target.getAuthorization().getPassword()) && !isEmpty(template.getAuthorization().getPassword())) {
                target.getAuthorization().setPassword(template.getAuthorization().getPassword());
            }
        }

        for (Party.PartyId ptTmpl : template.getPartyIds()) {
            if (ptTmpl.getType() != null) {
                Party.PartyId ptTarget = getPartyByType(target.getPartyIds(), ptTmpl.getType());
                if (ptTarget == null) {
                    ptTarget = new Party.PartyId();
                    ptTarget.setType(ptTmpl.getType());
                    target.getPartyIds().add(ptTarget);
                }
                if (isEmpty(ptTarget.getValue()) && !isEmpty(ptTmpl.getValue())) {
                    ptTarget.setValue(ptTmpl.getValue());
                }
            } else {
                Party.PartyId ptTarget = getPartyByValue(target.getPartyIds(), ptTmpl.getValue());
                if (ptTarget == null) {
                    ptTarget = new Party.PartyId();
                    ptTarget.setValue(ptTmpl.getValue());
                    target.getPartyIds().add(ptTarget);
                }

            }

        }
    }

    public void mergeReceptionAwareness(ReceptionAwareness target, ReceptionAwareness template) {
        if (template.getRetry() != null) {
            if (target.getRetry() == null) {
                target.setRetry(new ReceptionAwareness.Retry());
                target.getRetry().setMaxRetries(template.getRetry().getMaxRetries());
                target.getRetry().setMultiplyPeriod(template.getRetry().getMultiplyPeriod());
                target.getRetry().setPeriod(template.getRetry().getPeriod());
            }
        }

        if (template.getDuplicateDetection() != null) {
            if (target.getDuplicateDetection() == null) {
                target.setDuplicateDetection(new ReceptionAwareness.DuplicateDetection());
                target.getDuplicateDetection().setWindowPeriode(template.getDuplicateDetection().getWindowPeriode());
            }
        }
    }

    private BusinessInfo.PayloadProfiles getPayloadProfilesByAction(List<BusinessInfo.PayloadProfiles> lst, String action) {
        BusinessInfo.PayloadProfiles pt = null;
        for (BusinessInfo.PayloadProfiles p : lst) {
            if (isEmpty(action) && isEmpty(p.getAction()) || p.getAction() != null && p.getAction().equals(action)) {
                pt = p;
                break;
            }

        }
        return pt;
    }

    private BusinessInfo.PayloadProfiles.PayloadProfile getPayloadProfileByName(List<BusinessInfo.PayloadProfiles.PayloadProfile> lst, String name) {
        BusinessInfo.PayloadProfiles.PayloadProfile pt = null;
        for (BusinessInfo.PayloadProfiles.PayloadProfile p : lst) {
            if (isEmpty(name) && isEmpty(p.getName()) || p.getName() != null && p.getName().equals(name)) {
                pt = p;
                break;
            }

        }
        return pt;
    }

    private Party.PartyId getPartyByType(List<Party.PartyId> lst, String type) {
        Party.PartyId pt = null;

        for (Party.PartyId p : lst) {
            if (isEmpty(type) && isEmpty(p.getType()) || p.getType() != null && p.getType().equals(type)) {
                pt = p;
                break;
            }

        }
        return pt;
    }

    private BusinessInfo.Action getActionByValue(List<BusinessInfo.Action> lst, String name) {
        BusinessInfo.Action pt = null;
        for (BusinessInfo.Action p : lst) {
            if (isEmpty(name) && isEmpty(p.getValue()) || p.getValue() != null && p.getValue().equals(name)) {
                pt = p;
                break;
            }

        }
        return pt;
    }

    private Party.PartyId getPartyByValue(List<Party.PartyId> lst, String value) {
        Party.PartyId pt = null;
        for (Party.PartyId p : lst) {
            if (isEmpty(value) && isEmpty(p.getValue()) || p.getValue() != null && p.getValue().equals(value)) {
                pt = p;
                break;
            }

        }
        return pt;
    }

    public List<String> getServices() {
        List<String> lstPMSrv = new ArrayList<>();
        for (PMode pm : pmodes.getPModes()) {
            String strVal = pm.getLegs().get(0).getBusinessInfo().getService().getValue();
            if (!lstPMSrv.contains(strVal)) {
                lstPMSrv.add(strVal);
            }

        }
        return lstPMSrv;
    }

    public PModes getPModes() {
        reloadPModes();
        return pmodes;
    }

    public boolean isEmpty(String val) {
        {
            return val == null || val.trim().isEmpty();
        }

    }
    
    
}
