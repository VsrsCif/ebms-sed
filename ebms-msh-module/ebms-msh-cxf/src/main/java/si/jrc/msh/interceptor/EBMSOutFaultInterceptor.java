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
package si.jrc.msh.interceptor;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import static org.bouncycastle.asn1.x500.style.RFC4519Style.l;
import org.w3c.dom.Node;
import si.jrc.msh.exception.EBMSError;
import static si.jrc.msh.interceptor.EBMSInFaultInterceptor.LOG;
import static si.jrc.msh.interceptor.EBMSOutInterceptor.LOG;
import si.jrc.msh.utils.EBMSBuilder;
import si.sed.commons.utils.SEDLogger;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Error;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage;
import si.jrc.msh.exception.EBMSErrorCode;

/**
 * Sets up the outgoing chain to build a ebms 3.0 (AS4) form message. First it will create Messaging
 * object according pmode configuratin added as "PMode.class" param in message context. For user
 * message attachments are added (and compressed according to pmode settings ) In the end encryption
 * and security interceptors are configured.
 *
 * @author Jože Rihtaršič
 */
public class EBMSOutFaultInterceptor extends AbstractEBMSInterceptor {

  /**
   * Logger for EBMSOutFaultInterceptor class
   */
  protected final static SEDLogger LOG = new SEDLogger(EBMSOutFaultInterceptor.class);
  /**
   * ebms message tools for converting between ebms and ebms-sed message entity
   */
  protected final EBMSBuilder mEBMSUtil = new EBMSBuilder();

  /**
   * Contstructor EBMSOutFaultInterceptor for setting instance in a phase Phase.PRE_PROTOCOL
   */
  public EBMSOutFaultInterceptor() {
    super(Phase.PRE_PROTOCOL);
    getAfter().add(EBMSOutInterceptor.class.getName());
  }

  /**
   *
   *
   * @param message: SoapMessage handled in CXF bus
   */
  @Override
  public void handleMessage(SoapMessage message) {

    long l = LOG.logStart();
    SoapVersion version = message.getVersion();
    // is out mail request or response
    boolean isRequest = MessageUtils.isRequestor(message);
    QName qnFault = (isRequest ? SoapFault.FAULT_CODE_CLIENT : SoapFault.FAULT_CODE_SERVER);

    Exception exc = message.getContent(Exception.class);
    Object sm = message.getContent(SOAPMessage.class);

    LOG.formatedlog("Object %s message %s", exc, sm);
    if (exc instanceof EBMSError) {
      System.out.println("IT IS EBMS ERROR");
    }

    LOG.log("SoapMessage: ********************************************************");
    message.entrySet().stream().forEach((entry) -> {
      LOG.formatedlog("Key: %s, val: %s", entry.getKey(), entry.getValue());
    });
    LOG.log("Exchange: ********************************************************");
    Exchange map = message.getExchange();
    map.entrySet().stream().forEach((entry) -> {
      LOG.formatedlog("Key: %s, val: %s", entry.getKey(), entry.getValue());
    });

    try {
      MessageFactory mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
      SOAPMessage request = mf.createMessage();

      message.put(SOAPMessage.class, request);
      SOAPBody body = request.getSOAPBody();
      body.removeContents();
      SOAPFault soapFault = body.addFault();
      if (exc instanceof EBMSError) {

        EBMSError sf = (EBMSError) exc;

        soapFault.setFaultString(((EBMSError) exc).getSubMessage());
        soapFault.setFaultCode(SoapFault.FAULT_CODE_SERVER);

        Messaging msgHeader = mEBMSUtil.createMessaging(version);
        SignalMessage sgnl = new SignalMessage();
        sgnl.getErrors().add(mEBMSUtil.createErrorSignal(sf));
        msgHeader.getSignalMessages().add(sgnl);

        try {

          SOAPHeader sh = request.getSOAPHeader();
          Marshaller marshaller = JAXBContext.newInstance(Messaging.class).createMarshaller();
          marshaller.marshal(msgHeader, sh);
          request.saveChanges();
        } catch (JAXBException |  SOAPException ex) {
          String errMsg = "Error adding ebms header to soap: " + ex.getMessage();
          LOG.logError(l, errMsg, ex);

        }
        /*        
           
        soapFault.setFaultString(sf.getFault().getFaultString());
        soapFault.setFaultCode(sf.getFault().getFaultCodeAsQName());
        soapFault.setFaultActor(sf.getFault().getFaultActor());
        if (sf.getFault().hasDetail()) {
          Node nd = originalMsg.getSOAPPart().importNode(
              sf.getFault().getDetail()
              .getFirstChild(), true);
          soapFault.addDetail().appendChild(nd);
        }*/
      } else if (exc instanceof Fault) {
        SoapFault sf = SoapFault.createFault((Fault) exc, ((SoapMessage) message)
            .getVersion());
        soapFault.setFaultString(sf.getReason());
        soapFault.setFaultCode(sf.getFaultCode());
        if (sf.hasDetails()) {
          soapFault.addDetail();
          Node nd = request.getSOAPPart().importNode(sf.getDetail(), true);
          nd = nd.getFirstChild();
          while (nd != null) {
            soapFault.getDetail().appendChild(nd);
            nd = nd.getNextSibling();
          }
        }
      } else {
        soapFault.setFaultString(exc.getMessage());
        soapFault.setFaultCode(new QName("http://cxf.apache.org/faultcode", "HandleFault"));
      }
    } catch (SOAPException e) {
      // do nothing
      e.printStackTrace();
    }

    LOG.logEnd(l);
  }

}
