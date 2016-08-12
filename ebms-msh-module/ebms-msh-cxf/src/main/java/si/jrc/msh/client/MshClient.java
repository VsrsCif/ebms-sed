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
package si.jrc.msh.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.svev.pmode.Leg;
import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.Protocol;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cert.SEDCertificate;
import si.jrc.msh.exception.MSHException;
import si.jrc.msh.exception.MSHExceptionCode;
import si.jrc.msh.interceptor.EBMSInFaultInterceptor;
import si.jrc.msh.interceptor.EBMSInInterceptor;
import si.jrc.msh.interceptor.EBMSLogInInterceptor;
import si.jrc.msh.interceptor.EBMSLogOutInterceptor;
import si.jrc.msh.interceptor.EBMSOutFaultInterceptor;
import si.jrc.msh.interceptor.EBMSOutInterceptor;
import si.jrc.msh.interceptor.MSHPluginInInterceptor;
import si.jrc.msh.interceptor.MSHPluginOutInterceptor;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.sec.KeystoreUtils;

/**
 * Sets up MSH client and submits message.
 *
 * @author Jože Rihtaršič
 */
public class MshClient extends EJBContainer {

  /**
   * Logger for MshClient class
   */
  protected final SEDLogger LOG = new SEDLogger(MshClient.class);

  /**
   * Keystore tools
   */
  private final KeystoreUtils mKSUtis = new KeystoreUtils();

  /**
   * Method sets up client according given pmode configuration. pmode.getLegs().get(0).getProtocol()
   *
   *
   * @param prt: transport definition object frompmode
   * @return Dispatch client for submitting message
   * @throws si.jrc.msh.exception.MSHException (Error creating client)
   */
  public Dispatch<SOAPMessage> getClient(final Protocol prt) throws MSHException {

    // --------------------------------------------------------------------
    // validate parameters
    if (prt == null) {
      throw new MSHException(MSHExceptionCode.ErrorCreatingClient, "Missing Protocol element");
    }

    if (prt.getAddress() == null) {
      throw new MSHException(MSHExceptionCode.ErrorCreatingClient, "Missing Address element");
    }
    if (prt.getAddress().getValue() == null || prt.getAddress().getValue().trim().isEmpty()) {
      throw new MSHException(MSHExceptionCode.ErrorCreatingClient, "Missing address");
    }

    // --------------------------------------------------------------------
    // create MTOM service
    String url = prt.getAddress().getValue();
    QName serviceName1 = new QName("", "");
    QName portName1 = new QName("", "");
    Service s = Service.create(serviceName1);
    s.addPort(portName1, SOAPBinding.SOAP12HTTP_MTOM_BINDING, url);
    MTOMFeature mtomFt = new MTOMFeature(true);

    Dispatch<SOAPMessage> dispSOAPMsg =
        s.createDispatch(portName1, SOAPMessage.class, Service.Mode.MESSAGE, mtomFt);
    DispatchImpl dimpl = (org.apache.cxf.jaxws.DispatchImpl) dispSOAPMsg;
    SOAPBinding sb = (SOAPBinding) dispSOAPMsg.getBinding();
    sb.setMTOMEnabled(true);

    // --------------------------------------------------------------------
    // configure interceptors (log, ebms and plugin interceptors)
    Client cxfClient = dimpl.getClient();
    cxfClient.getInInterceptors().add(new EBMSLogInInterceptor());
    cxfClient.getInInterceptors().add(new EBMSInInterceptor());
    cxfClient.getInInterceptors().add(new MSHPluginInInterceptor());
    
    cxfClient.getInFaultInterceptors().add(new EBMSInFaultInterceptor());

    cxfClient.getOutInterceptors().add(new MSHPluginOutInterceptor());
    cxfClient.getOutInterceptors().add(new EBMSOutInterceptor());
    cxfClient.getOutInterceptors().add(new EBMSLogOutInterceptor());
    cxfClient.getOutFaultInterceptors().add(new EBMSOutFaultInterceptor());

    HTTPConduit http = (HTTPConduit) cxfClient.getConduit();
    // --------------------------------------------------------------------
    // set TLS
    if (prt.getTLS() != null) {
      try {
        setupTLS(http, prt.getTLS());
      } catch (IOException | SEDSecurityException ex) {
        throw new MSHException(MSHExceptionCode.ErrorCreatingClient, ex,
            "Error occured while configuring TLS: " + ex.getMessage());
      }
    }
    // --------------------------------------------------------------------
    // set http client policy
    HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
    if (prt.getProxy() != null) {
      httpClientPolicy.setProxyServer(prt.getProxy().getHost());
      httpClientPolicy.setProxyServerPort(prt.getProxy().getPort());
      httpClientPolicy.setProxyServerType(ProxyServerType.HTTP);
    }

    httpClientPolicy.setConnectionTimeout(prt.getAddress().getConnectionTimeout());
    httpClientPolicy.setReceiveTimeout(prt.getAddress().getReceiveTimeout());
    httpClientPolicy.setAllowChunking(prt.getAddress().getChunked());
    // todo:
    httpClientPolicy.setChunkingThreshold(4096);
    httpClientPolicy.setChunkLength(-1);

    // set http Policy
    http.setClient(httpClientPolicy);

    return dispSOAPMsg;
  }



  /**
   * Method submits message according pmode configuration
   * 
   * @param mail
   * @param pmode
   * @throws si.jrc.msh.exception.MSHException
   */
  public void sendMessage(MSHOutMail mail, PMode pmode) throws MSHException {

    long l = LOG.logStart(mail);
    // validate data
    if (mail == null) {
      MSHException me = new MSHException(MSHExceptionCode.EmptyMail);
      LOG.logError(l, me.getMessage(), null);
      throw me;
    }
    if (pmode == null) {
      MSHException me = new MSHException(MSHExceptionCode.MissingPMode, mail.getId() + "");
      LOG.logError(l, me.getMessage(), null);
      throw me;
    }
    if (pmode.getLegs().isEmpty()) {
      MSHException me =
          new MSHException(MSHExceptionCode.InvalidPMode, pmode.getId(),
              "Configuration does not have defined legs!");
      LOG.logError(l, me.getMessage(), null);
      throw me;
    }
    Leg lg = pmode.getLegs().get(0);
    if (lg.getProtocol() == null) {
      MSHException me =
          new MSHException(MSHExceptionCode.InvalidPMode, pmode.getId(),
              "Fist leg does not have defined protocol!");
      LOG.logError(l, me.getMessage(), null);
      throw me;
    }

    // create client (define bus)
    Dispatch<SOAPMessage> client = getClient(lg.getProtocol());

    // set context
    DispatchImpl dimpl = (org.apache.cxf.jaxws.DispatchImpl) client;
    client.getRequestContext().put(PMode.class.getName(), pmode);
    client.getRequestContext().put(MSHOutMail.class.getName(), mail);

    // create empty soap mesage
    MessageFactory mf;
    SOAPMessage soapReq;
    try {
      mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
      soapReq = mf.createMessage();
    } catch (SOAPException ex) {
      throw new MSHException(MSHExceptionCode.ErrorCreatingSOAPMessage, ex, mail.getId() + "");
    }

    // if two-way - response in expected!
    long st = LOG.getTime();
    LOG.log("Start submiting mail");
    Object obj = dimpl.invoke(soapReq);
    LOG.log("Submiting mail finished in (" + (LOG.getTime() - st) + " ms). Got response " + obj);
    LOG.logEnd(l);
  }

  /**
   * Method sets Truststore and key (if needed) to https client for TLS
   * 
   * @param client - http(s) client
   * @param tls - pmode tls configuration
   * @throws FileNotFoundException
   * @throws IOException
   * @throws SEDSecurityException
   */
  private void setupTLS(HTTPConduit httpConduit, Protocol.TLS tls) throws IOException,
      SEDSecurityException {
    long l = LOG.logStart();


    TLSClientParameters tlsCP = null;
    // set client's key cert for mutual identification
    if (tls.getClientKeyAlias() != null && !tls.getClientKeyAlias().trim().isEmpty()) {

      // key alias
      String keyAlias = tls.getClientKeyAlias().trim();
      SEDCertStore scsKey = getLookups().getSEDCertStoreByCertAlias(keyAlias, true);
      SEDCertificate aliasKey = null;
      if (scsKey != null) {
        for (SEDCertificate crt : scsKey.getSEDCertificates()) {
          if (crt.isKeyEntry() && keyAlias.equals(crt.getAlias())) {
            aliasKey = crt;
            break;
          }
        }
      }

      if (scsKey == null || aliasKey == null) {
        String msg = "Key for alias: '" + keyAlias + "' do not exists!";
        LOG.logError(l, msg, null);
        throw new SEDSecurityException(
            SEDSecurityException.SEDSecurityExceptionCode.KeyForAliasNotExists, keyAlias);
      }

      // get key managers
      KeyManager[] myKeyManagers = mKSUtis.getKeyManagers(scsKey);
      tlsCP = new TLSClientParameters();
      tlsCP.setKeyManagers(myKeyManagers);
    }

    // set trustore cert
    if (tls.getTrustCertAlias() != null && !tls.getTrustCertAlias().trim().isEmpty()) {
      String trustAlias = tls.getTrustCertAlias().trim();
      LOG.log("\t Set truststore:" + trustAlias);

      SEDCertStore scs = getLookups().getSEDCertStoreByCertAlias(trustAlias, false);
      TrustManager[] myTrustStoreKeyManagers = mKSUtis.getTrustManagers(scs);


      tlsCP = tlsCP == null ? new TLSClientParameters() : tlsCP;
      tlsCP.setTrustManagers(myTrustStoreKeyManagers);
      tlsCP.setDisableCNCheck(true);
    }

    if (tlsCP != null) {
      LOG.log("\t TLS is setted:");
      httpConduit.setTlsClientParameters(tlsCP);
    }
  }

}
