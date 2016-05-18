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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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

import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.Protocol;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cert.SEDCertificate;
import si.jrc.msh.exception.MSHException;
import si.jrc.msh.exception.MSHExceptionCode;

import si.jrc.msh.interceptor.EBMSInInterceptor;
import si.jrc.msh.interceptor.EBMSLogInInterceptor;
import si.jrc.msh.interceptor.EBMSLogOutInterceptor;
import si.jrc.msh.interceptor.EBMSOutInterceptor;
import si.sed.commons.utils.SEDLogger;

import si.jrc.msh.utils.SvevUtils;
import si.sed.commons.utils.Utils;
import si.jrc.msh.interceptor.MSHPluginInInterceptor;
import si.jrc.msh.interceptor.MSHPluginOutInterceptor;
import si.sed.commons.SEDJNDI;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.utils.sec.KeystoreUtils;

/**
 *
 * @author Jože Rihtaršič
 */
public class MshClient {

    protected final SEDLogger mlog = new SEDLogger(MshClient.class);
    
    SEDLookupsInterface mSedLookups;
    
    
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
        
        dimpl.invoke(soapReq);

        mlog.logEnd(l);
    }

    public Dispatch<SOAPMessage> getClient(PMode pmode) throws MSHException {

        if (pmode.getLegs().isEmpty() || pmode.getLegs().get(0).getProtocol() == null
                || pmode.getLegs().get(0).getProtocol().getAddress() == null
                || pmode.getLegs().get(0).getProtocol().getAddress().getValue().trim().isEmpty()) {
            throw new MSHException(MSHExceptionCode.InvalidPMode, pmode.getId(), "Missing Protocol/Address value");
        }
        Protocol prt = pmode.getLegs().get(0).getProtocol();
        // get sending leg!
        String url = prt.getAddress().getValue();

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
            if(prt.getTLS()!=null) {
                mlog.log("Dispatching mail using pmode: "+pmode.getId()+" Set TLS" );
                setupTLS(cxfClient , prt.getTLS());
            }

            HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();

            httpClientPolicy.setConnectionTimeout(prt.getAddress().getConnectionTimeout());
            httpClientPolicy.setAllowChunking(prt.getAddress().getChunked());
            httpClientPolicy.setReceiveTimeout(prt.getAddress().getReceiveTimeout());
            
            if (prt.getProxy()!=null) {
                mlog.log("Dispatching mail using pmode: "+pmode.getId()+" Set proxy: " + prt.getProxy().getHost() +":" + prt.getProxy().getPort());
                httpClientPolicy.setProxyServer(prt.getProxy().getHost());
                httpClientPolicy.setProxyServerPort(prt.getProxy().getPort());
                httpClientPolicy.setProxyServerType(ProxyServerType.HTTP);
            }
            

            HTTPConduit http = (HTTPConduit) cxfClient.getConduit();
            http.setClient(httpClientPolicy);

            //cxfClient.getOutInterceptors().add(new LoggingOutInterceptor());
        } catch (SEDSecurityException | IOException | GeneralSecurityException th) {
            throw new MSHException(MSHExceptionCode.InvalidPMode, pmode.getId(), th.getMessage());
        } 
        

        return dispSOAPMsg;
    }

    private  void setupTLS(Client client, Protocol.TLS tls)
            throws FileNotFoundException, IOException, GeneralSecurityException, SEDSecurityException {
        long l = mlog.logStart();
        
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();
        

        TLSClientParameters tlsCP = null;

        if (tls.getKeyAlias()!= null && !tls.getKeyAlias().trim().isEmpty()) {
            String keyAlias = tls.getKeyAlias().trim();
            SEDCertStore scs =  getLookups().getSEDCertStoreByCertAlias(keyAlias, true);
            SEDCertificate aliasCrt = null;
            if( scs != null) {
                for (SEDCertificate crt: scs.getSEDCertificates()){
                    if(crt.isKeyEntry() &&  keyAlias.equals(crt.getAlias())) {
                        aliasCrt = crt;
                        break;
                    }
                }
            }
             if (scs ==null ||  aliasCrt==null){
                 String msg = "Key for alias: '"+keyAlias+"' do not exists!";
                mlog.logError( l, msg, null);
               throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.KeyForAliasNotExists, keyAlias);
            }
            
            tlsCP = tlsCP==null?new TLSClientParameters():tlsCP;
            KeyStore keyStore = KeystoreUtils.getKeystore(scs);            
            KeyManager[] myKeyManagers = getKeyManagers(keyStore, aliasCrt.getKeyPassword());
            tlsCP.setKeyManagers(myKeyManagers);
        }

        if (tls.getTrustCertAlias()!= null && !tls.getTrustCertAlias().trim().isEmpty()) {
            String trustAlias = tls.getKeyAlias().trim();
            mlog.log("\t Set truststore:" + tls.getTrustCertAlias() );
            tlsCP = tlsCP==null?new TLSClientParameters():tlsCP;
            SEDCertStore scs =  getLookups().getSEDCertStoreByCertAlias(trustAlias, false);
            
            KeyStore trustStore = KeystoreUtils.getKeystore(scs);            
            TrustManager[] myTrustStoreKeyManagers = getTrustManagers(trustStore);
            tlsCP.setTrustManagers(myTrustStoreKeyManagers);
            
            tlsCP.setDisableCNCheck(true);
        }

        if (tlsCP!= null) {
            mlog.log("\t TLS is setted:" );
            httpConduit.setTlsClientParameters(tlsCP);
        }
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
    
    
     public SEDLookupsInterface getLookups() {
        long l = mlog.logStart();
        if (mSedLookups== null) {
            try {
                mSedLookups=  InitialContext.doLookup(SEDJNDI.JNDI_SEDLOOKUPS);
                mlog.logEnd(l);
            } catch (NamingException ex) {
                mlog.logError(l, ex);
            }            
        }
      
        return mSedLookups;
    }

}
