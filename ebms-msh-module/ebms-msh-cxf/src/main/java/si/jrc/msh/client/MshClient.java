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
package si.jrc.msh.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
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
import org.apache.cxf.frontend.ClientProxy;

import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.svev.pmode.PMode;
import si.jrc.msh.exception.MSHException;
import si.jrc.msh.exception.MSHExceptionCode;

import si.jrc.msh.interceptor.EBMSInInterceptor;
import si.jrc.msh.interceptor.EBMSLogInInterceptor;
import si.jrc.msh.interceptor.EBMSLogOutInterceptor;
import si.jrc.msh.interceptor.EBMSOutInterceptor;
import si.sed.commons.utils.SEDLogger;

import si.jrc.msh.utils.SvevUtils;
import si.sed.msh.plugin.MSHPluginInInterceptor;
import si.sed.msh.plugin.MSHPluginOutInterceptor;

/**
 *
 * @author Jože Rihtaršič
 */
public class MshClient {

    protected final SEDLogger mlog = new SEDLogger(MshClient.class);
    SvevUtils msvevUtils = new SvevUtils();

    public void sendMessage(MSHOutMail mail, PMode pmode) throws MSHException {

        long l = mlog.logStart(mail);

        if (mail == null) {
            MSHException me = new MSHException(MSHExceptionCode.EmptyMail);
            mlog.logError(l, me.getMessage(), null);
            throw me;
        }

        // create client (define bus)
        Dispatch<SOAPMessage> client = getClient(pmode);

        DispatchImpl dimpl = (org.apache.cxf.jaxws.DispatchImpl) client;

        // configure svev-msh transport
        client.getRequestContext().put(PMode.class.getName(), pmode);
        client.getRequestContext().put(MSHOutMail.class.getName(), mail);

        // create empty soap mesage
        MessageFactory mf;
        SOAPMessage soapReq;
        try {
            mf = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
            soapReq = mf.createMessage();

        } catch (SOAPException ex) {
            throw new MSHException(MSHExceptionCode.EmptyMail, ex);
        }
        /*
        try {
            for (MSHOutPart p : mail.getMSHOutPayload().getMSHOutParts()) {
                String id = UUID.randomUUID().toString();
                p.setEbmsId(id);
                AttachmentPart apr = soapReq.createAttachmentPart();
                apr.setContentId(id);
                apr.setMimeHeader("id", id);
                DataHandler dh = new DataHandler(new FileDataSource(StorageUtils.getFile(p.getFilepath())));
                apr.setDataHandler(dh);
                soapReq.addAttachmentPart(apr);
            }

        } catch (StorageException ex) {
            throw new MSHException(MSHExceptionCode.InvalidMail, ex);
        }*/
        dimpl.invoke(soapReq);

        mlog.logEnd(l);
    }

    public Dispatch<SOAPMessage> getClient(PMode pmode) throws MSHException {

        if (pmode.getLegs().isEmpty() || pmode.getLegs().get(0).getProtocol() == null
                || pmode.getLegs().get(0).getProtocol().getAddress() == null
                || pmode.getLegs().get(0).getProtocol().getAddress().trim().isEmpty()) {
            throw new MSHException(MSHExceptionCode.InvalidPMode, pmode.getId(), "Missing Protocol/Address value");
        }

        // get sending leg!
        String url = pmode.getLegs().get(0).getProtocol().getAddress();

        QName serviceName1 = new QName("", "");
        QName portName1 = new QName("", "");
        Service s = Service.create(serviceName1);

        s.addPort(portName1, SOAPBinding.SOAP12HTTP_MTOM_BINDING, url);
        // AddressingFeature addrFt = new AddressingFeature(true, true);
        MTOMFeature mtomFt = new MTOMFeature(true);

        Dispatch<SOAPMessage> dispSOAPMsg = s.createDispatch(portName1, SOAPMessage.class, Service.Mode.MESSAGE, mtomFt);
        DispatchImpl dimpl = (org.apache.cxf.jaxws.DispatchImpl) dispSOAPMsg;
        SOAPBinding sb = (SOAPBinding) dispSOAPMsg.getBinding();
        sb.setMTOMEnabled(true);
        // configure interceptors

        // check plugins for interceptor
        // load jar (check if already loaded)
        // create interceptpr
        // install inteceptor
        try {

            Client cxfClient = dimpl.getClient();

            //cxfClient.getInInterceptors().add(new LoggingInInterceptor());
            cxfClient.getInInterceptors().add(new EBMSLogInInterceptor());
            cxfClient.getInInterceptors().add(new EBMSInInterceptor());
            cxfClient.getInInterceptors().add(new MSHPluginInInterceptor());

            cxfClient.getOutInterceptors().add(new MSHPluginOutInterceptor());
            cxfClient.getOutInterceptors().add(new EBMSOutInterceptor());
            cxfClient.getOutInterceptors().add(new EBMSLogOutInterceptor());
            setupTLS(cxfClient);

            HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();

            httpClientPolicy.setConnectionTimeout(36000);
            httpClientPolicy.setAllowChunking(false);
            httpClientPolicy.setReceiveTimeout(32000);

            HTTPConduit http = (HTTPConduit) cxfClient.getConduit();
            http.setClient(httpClientPolicy);

            //cxfClient.getOutInterceptors().add(new LoggingOutInterceptor());
        } catch (Throwable th) {
            th.printStackTrace();
            throw new MSHException(MSHExceptionCode.InvalidPMode, pmode.getId(), "Missing Protocol/Address value");
        }

        return dispSOAPMsg;
    }

    private static void setupTLS(Client client)
            throws FileNotFoundException, IOException, GeneralSecurityException {
        String keyStoreLoc = "sed-home/security/ssl-keystore.jks";
        String trustStoreLoc = "sed-home/security/sed-truststore.jks";
        // HTTPConduit httpConduit = (HTTPConduit) ClientProxy.getClient(port).getConduit();
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();

        TLSClientParameters tlsCP = new TLSClientParameters();
        String keyPassword = "cifadmin";
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(new FileInputStream(keyStoreLoc), "cifadmin".toCharArray());
        KeyManager[] myKeyManagers = getKeyManagers(keyStore, keyPassword);
        tlsCP.setKeyManagers(myKeyManagers);

        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(new FileInputStream(trustStoreLoc), "sed1234".toCharArray());
        TrustManager[] myTrustStoreKeyManagers = getTrustManagers(trustStore);
        tlsCP.setTrustManagers(myTrustStoreKeyManagers);

        httpConduit.setTlsClientParameters(tlsCP);
    }

    private static TrustManager[] getTrustManagers(KeyStore trustStore)
            throws NoSuchAlgorithmException, KeyStoreException {
        String alg = KeyManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory fac = TrustManagerFactory.getInstance(alg);
        fac.init(trustStore);
        return fac.getTrustManagers();
    }

    private static KeyManager[] getKeyManagers(KeyStore keyStore, String keyPassword)
            throws GeneralSecurityException, IOException {
        String alg = KeyManagerFactory.getDefaultAlgorithm();
        char[] keyPass = keyPassword != null
                ? keyPassword.toCharArray()
                : null;
        KeyManagerFactory fac = KeyManagerFactory.getInstance(alg);
        fac.init(keyStore, keyPass);
        return fac.getKeyManagers();
    }

}
