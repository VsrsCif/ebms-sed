/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils.sec;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.crypto.*;
import javax.xml.crypto.dom.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.etsi.uri._01903.v1_1.CertIDType;
import org.etsi.uri._01903.v1_1.DigestAlgAndValueType;
import org.etsi.uri._01903.v1_1.HashDataInfoType;
import org.etsi.uri._01903.v1_1.QualifyingProperties;
import org.etsi.uri._01903.v1_1.SignaturePolicyIdentifier;
import org.etsi.uri._01903.v1_1.SignatureProductionPlace;
import org.etsi.uri._01903.v1_1.SignedProperties;
import org.etsi.uri._01903.v1_1.SignedSignatureProperties;
import org.etsi.uri._01903.v1_1.SigningCertificate;
import org.etsi.uri._01903.v1_1.TimeStampType;
import org.etsi.uri._01903.v1_1.UnsignedProperties;
import org.etsi.uri._01903.v1_1.UnsignedSignatureProperties;
import org.w3._2000._09.xmldsig_.X509IssuerSerialType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import si.sed.commons.exception.SEDSecurityException;

/**
 *
 * @author sluzba
 */
public class XMLSignatureUtils {

    private static final String HTTPHeader_ContentType = "Content-Type";
    private static final String HTTPHeader_ContentTypeValue = "text/xml;charset=UTF-8";
    private static final String HTTPHeader_SAOPAction = "SOAPAction";
    private static final String ID_PREFIX_Referece = "Ref";
    private static final String ID_PREFIX_Signature = "Signature";
    private static final String ID_PREFIX_SignatureValue = "SignatureValue";
    private static final String ID_PREFIX_SignedInfo = "SignedInfo";
    private static final String ID_PREFIX_SignedProperties = "SignedProperties";
    private static final String TIMESTAMP_REQUEST = "<?xml version='1.0' encoding='UTF-8'?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><SOAP-ENV:Body><tsa:service xmlns:tsa=\"urn:Entrust-TSA\"><ts:TimeStampRequest xmlns:ts=\"http://www.entrust.com/schemas/timestamp-protocol-20020207\"><ts:Digest><ds:DigestMethod xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">%s</ds:DigestValue></ts:Digest><ts:Nonce>%s</ts:Nonce></ts:TimeStampRequest></tsa:service></SOAP-ENV:Body></SOAP-ENV:Envelope>";
    private static final String XADES_NS = "http://uri.etsi.org/01903/v1.1.1#";
    private static final String XADES_XMLTimeStamp = "XMLTimeStamp";
    private static final String XML_SIGNATURE_PROVIDER_PROP = "jsr105Provider";
    private static final String XML_SIGNATURE_PROVIDER_VALUE_1 = "org.jcp.xml.dsig.internal.dom.XMLDSigRI";
    private static final String XML_SIGNATURE_PROVIDER_VALUE_2 = "org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI";

    /**
     *
     */
    protected static final Logger mlgLogger = Logger.getLogger(XMLSignatureUtils.class.getName());

    private static UnsignedProperties createUnsignedPriperties(String signUriId) {
        UnsignedProperties uns = new UnsignedProperties();
        uns.setUnsignedSignatureProperties(new UnsignedSignatureProperties());

        TimeStampType tt = new TimeStampType();
        uns.getUnsignedSignatureProperties().getSignatureTimeStamps().add(tt);
        HashDataInfoType ht = new HashDataInfoType();
        ht.setUri("#" + signUriId);
        tt.getHashDataInfos().add(ht);
        tt.setXMLTimeStamp(null);

        return uns;
    }

    /**
     *
     * @param strVal
     * @return
     */
    public static String getUUID(String strVal) {
        StringBuilder sb = new StringBuilder();
        sb.setLength(0); // clear
        sb.append(strVal);
        sb.append("-");
        sb.append(UUID.randomUUID().toString());
        return sb.toString();
    }
    //String mstrTimeStampServerUrl = "http://ts.si-tsa.sigov.si:80/verificationserver/timestamp";
    String mstrResultLogFolder = System.getProperty("java.io.tmpdir");
    String mstrTimeStampServerUrl = null;

    private String calculateSignedValueDigest(String strSigValId, XMLSignatureFactory fac, KeyInfo ki, KeyStore.PrivateKeyEntry certPrivateKey, Document oDoc) {
        String strDigest = null;

        try {

            // todo calculate signature direct!! this si bad :>
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().newDocument();
            Node n = doc.adoptNode(oDoc.getDocumentElement().cloneNode(true));
            doc.appendChild(n);
            setIdnessToElemetns(n);
            Reference ref_TS = fac.newReference("#" + strSigValId,
                    fac.newDigestMethod(DigestMethod.SHA1, null),
                    null,
                    null, null);

            List<Reference> lstRef1 = new ArrayList<Reference>();
            lstRef1.add(ref_TS);

            // Create the SignedInfo
            SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                    (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                    lstRef1, "SignedInfo1-39EB3E08-97ED-48AF-969B-ABFD697FC5FA");

            XMLSignature sig2 = fac.newXMLSignature(si, ki);

            DOMSignContext dsc = new DOMSignContext(certPrivateKey.getPrivateKey(), doc.getDocumentElement());
            dsc.setProperty("javax.xml.crypto.dsig.cacheReference", Boolean.TRUE);

            try {

                // Marshal, generate (and sign) the enveloped signature
                sig2.sign(dsc);
                strDigest = Base64.getEncoder().encodeToString(ref_TS.getDigestValue());

                InputStream is = ref_TS.getDigestInputStream();
                byte[] bf = new byte[is.available()];
                is.read(bf);

            } catch (MarshalException | XMLSignatureException ex) {
                mlgLogger.error("SvevSignatureUtils.", ex);
            } catch (Exception ex) {
                mlgLogger.error("SvevSignatureUtils.", ex);
            }

        } catch (ParserConfigurationException | NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
            mlgLogger.error("SvevSignatureUtils.", ex);
        }
        return strDigest;
    }

    private String calculateSignedValueDigest(String strSigValId, Document oDoc) {

        String strDigest = null;
        try {
            Element el = oDoc.getElementById(strSigValId);

            Canonicalizer c = Canonicalizer.getInstance(CanonicalizationMethod.INCLUSIVE);
            byte[] buff = c.canonicalizeSubtree(el);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.digest(); // reset digest
            strDigest = Base64.getEncoder().encodeToString(md.digest(buff));
        } catch (NoSuchAlgorithmException ex) {
            mlgLogger.error("NoSuchAlgorithmException.", ex);
        } catch (CanonicalizationException ex) {
            mlgLogger.error("CanonicalizationException.", ex);
        } catch (InvalidCanonicalizerException ex) {
            mlgLogger.error("InvalidCanonicalizerException.", ex);
        }
        return strDigest;
    }

    private Document callTimestampService(String ireq, String wsldLocatin, String soapActionNamespace, String soapAction) throws SEDSecurityException {
        long t = logStart("SvevSignatureUtils.callTimestampService: params: req: '" + ireq + "' url: '" + wsldLocatin + "'");
        long tCall, tReceive;
        Document respDoc = null;
        HttpURLConnection conn = null;
        try {
            String strLocation = wsldLocatin;
            int iVal = strLocation.indexOf('?');
            if (iVal > 0) {
                strLocation = strLocation.substring(0, iVal);
            }
            URL url = new URL(strLocation);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            if (soapAction == null) {
                conn.setRequestProperty(HTTPHeader_SAOPAction, soapAction == null ? "" : (soapActionNamespace + soapAction));
            }
            conn.setRequestProperty(HTTPHeader_ContentType, HTTPHeader_ContentTypeValue);
            OutputStream os = conn.getOutputStream();
            // write post  ----------------------------------------
            os.write(ireq.getBytes("UTF-8"));
            os.flush();
            tCall = getTime() - t;
            mlgLogger.info("SvevSignatureUtils.callTimestampService: send request in " + tCall + "ms");
            // start receiving  ----------------------------------------
            tReceive = getTime() - tCall;
            mlgLogger.info("SvevSignatureUtils.callTimestampService: receive response in (" + tReceive + "ms)");
            InputStream httpIS = conn.getInputStream();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            respDoc = dbf.newDocumentBuilder().parse(httpIS);
        } catch (SAXException | ParserConfigurationException ex) {
            logError("SvevSignatureUtils.callTimestampService", ex.getMessage(), t, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CreateTimestampException, ex, ex.getMessage());
        } catch (IOException ex) {
            File fout = null;
            if (conn != null && conn.getErrorStream() != null) {
                fout = writeToFile(conn.getErrorStream(), getResultLogFolder(), "TS_ERROR", ".html");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("SvevSignatureUtils.callTimestampService: ERROR\n");
                sb.append("\trequest:");
                sb.append(ireq);
                sb.append("\n\twsldLocatin:");
                sb.append(wsldLocatin);
                sb.append("\n\tsoapActionNamespace:");
                sb.append(soapActionNamespace);
                sb.append("\n\tsoapAction:");
                sb.append(soapAction);
                sb.append("\n\tmsg");
                sb.append(ex.getMessage());
                sb.append("SvevSignatureUtils.callTimestampService: ERROR\n");
                for (StackTraceElement st : ex.getStackTrace()) {
                    sb.append("\n\t\t");
                    sb.append(st.toString());
                }
                fout = writeToFile(new ByteArrayInputStream(sb.toString().getBytes()), getResultLogFolder(), "TS_ERROR", ".html");
            }

            StringWriter sw = new StringWriter();
            sw.append("SvevSoap.callService: Exception: SoapAction:'");
            sw.append(soapActionNamespace);
            sw.append(soapAction);
            sw.append("' location:'" + wsldLocatin + "' Exception message:");
            if (fout != null) {
                sw.append("\nResponse writen to: '");
                sw.append(fout.getAbsolutePath());
                sw.append("' ");
            }
            sw.append("error message:");
            sw.append(ex.getMessage());
            sw.append("SvevSoap.callService: Header values'");
            Map<String, List<String>> mp = conn.getHeaderFields();
            for (String s : mp.keySet()) {
                sw.append(s + " : " + mp.get(s));
            }
            mlgLogger.error(sw.toString(), ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CreateSignatureException, ex, ex.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.getInputStream().close();
                } catch (IOException ex) {
                    //mlgLogger.error("SvevSoap.callService: Error closing socket: " + ex.getMessage());
                }

            }
        }
        logEnd("SvevSignatureUtils.callTimestampService", t);
        return respDoc;
    }

    /**
     *
     * @param lst
     * @param fac
     * @return
     * @throws SEDSecurityException
     */
    public List<Reference> createReferenceList(List<String[]> lst, XMLSignatureFactory fac) throws SEDSecurityException {
        long t = getTime();
        List<Reference> lstRef = new ArrayList<Reference>();
        try {

            DigestMethod dm = fac.newDigestMethod(DigestMethod.SHA1, null);
            for (String[] s : lst) {
                lstRef.add(fac.newReference("#" + s[0],
                        dm,
                        null,
                        s[1], getUUID(ID_PREFIX_Referece)));
            }
        } catch (NoSuchAlgorithmException ex) {
            logError("SvevSignatureUtils.createSignedInfo", ex.getMessage(), t, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.NoSuchAlgorithm, ex, ex.getMessage());
        } catch (InvalidAlgorithmParameterException ex) {
            logError("SvevSignatureUtils.createSignedInfo", ex.getMessage(), t, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CreateSignatureException, ex, ex.getMessage());
        }
        return lstRef;
    }

    /**
     *
     * @param lst
     * @param fac
     * @return
     * @throws SEDSecurityException
     */
    public SignedInfo createSignedInfo(List<String[]> lst, XMLSignatureFactory fac) throws SEDSecurityException {
        long t = getTime();
        SignedInfo si = null;
        try {
            List<Reference> lstRef = createReferenceList(lst, fac);
            si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                    (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                    lstRef, getUUID(ID_PREFIX_SignedInfo));
        } catch (NoSuchAlgorithmException ex) {
            logError("SvevSignatureUtils.createSignedInfo", ex.getMessage(), t, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.NoSuchAlgorithm, ex, ex.getMessage());
        } catch (InvalidAlgorithmParameterException ex) {
            logError("SvevSignatureUtils.createSignedInfo", ex.getMessage(), t, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CreateSignatureException, ex, ex.getMessage());
        }
        return si;
    }

    private SignedProperties createSignedProperties(String strSigPropId, X509Certificate cert) {
        SignedProperties sp = new SignedProperties();
        try {
            sp.setId(strSigPropId);
            SigningCertificate scert = new SigningCertificate();
            CertIDType sit = new CertIDType();
            DigestAlgAndValueType dt = new DigestAlgAndValueType();
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            byte[] der = cert.getEncoded();
            md.update(der);
            dt.setDigestValue(md.digest());
            dt.setDigestMethod(new org.w3._2000._09.xmldsig_.DigestMethod());
            dt.getDigestMethod().setAlgorithm(DigestMethod.SHA1);
            sit.setCertDigest(dt);
            sit.setIssuerSerial(new X509IssuerSerialType());
            sit.getIssuerSerial().setX509IssuerName(cert.getIssuerDN().getName());
            sit.getIssuerSerial().setX509SerialNumber(cert.getSerialNumber());

            SignedSignatureProperties ssp = new SignedSignatureProperties();
            ssp.setSigningTime(Calendar.getInstance().getTime());
            ssp.setSigningCertificate(scert);
            ssp.setSignaturePolicyIdentifier(new SignaturePolicyIdentifier());
            ssp.setSignatureProductionPlace(new SignatureProductionPlace());
            ssp.getSignatureProductionPlace().setCity("Ljubljana");

            scert.getCerts().add(sit);
            sp.setSignedSignatureProperties(ssp);
        } catch (CertificateEncodingException ex) {
            mlgLogger.error("SvevSignatureUtils.", ex);
        } catch (NoSuchAlgorithmException ex) {
            mlgLogger.error("SvevSignatureUtils.", ex);
        }
        return sp;

    }

    /**
     *
     * @param cert
     * @param fac
     * @return
     */
    public KeyInfo createXAdESKeyInfo(X509Certificate cert, XMLSignatureFactory fac) {
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        // add certificate to signature:         
        X509IssuerSerial x509IssuerSerial = kif.newX509IssuerSerial(cert.getIssuerDN().getName(), cert.getSerialNumber());
        List x509 = new ArrayList();
        x509.add(cert);
        x509.add(x509IssuerSerial);
        X509Data x509Data = kif.newX509Data(x509);
        //x509Data.getContent().add(x509IssuerSerial);
        List items = new ArrayList();
        items.add(x509Data);
        return kif.newKeyInfo(items);
    }

    /**
     *
     * @param sigId
     * @param strSigValId
     * @param strSigPropId
     * @param cert
     * @param doc
     * @return
     * @throws SEDSecurityException
     */
    public XMLStructure createXAdESQualifyingProperties(String sigId, String strSigValId, String strSigPropId, X509Certificate cert, Document doc) throws SEDSecurityException {
        long t = getTime();
        XMLStructure content = null;
        try {
            QualifyingProperties qt = new QualifyingProperties();
            qt.setTarget("#" + sigId);
            qt.setSignedProperties(createSignedProperties(strSigPropId, cert));
            qt.setUnsignedProperties(createUnsignedPriperties(strSigValId));

            JAXBContext jc = JAXBContext.newInstance(QualifyingProperties.class);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            Marshaller m = jc.createMarshaller();
            Node el = doc.createElement("ROOT");
            m.marshal(qt, el);
            setIdnessToElemetns(el);

            content = new DOMStructure(el.getFirstChild());
        } catch (JAXBException ex) {
            logError("SvevSignatureUtils.createReferenceList", ex.getMessage(), t, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CreateSignatureException, ex, ex.getMessage());
        }
        return content;
    }

    /**
     *
     * @return
     */
    public String getResultLogFolder() {
        return mstrResultLogFolder;
    }

    /**
     *
     * @return
     */
    protected long getTime() {
        return Calendar.getInstance().getTimeInMillis();
    }

    /**
     *
     * @param hash
     * @return
     * @throws SEDSecurityException
     */
    public Element getTimeStamp(String hash) throws SEDSecurityException {
        String reg = String.format(TIMESTAMP_REQUEST, hash, Calendar.getInstance().getTimeInMillis());
        Document d = callTimestampService(reg, getTimeStampServerUrl(), null, null);
        setIdnessToElemetns(d.getDocumentElement());
        Element e = d.getElementById("TimeStampToken");
        if (e == null) {
            e = (Element) d.getElementsByTagName("dsig:Signature").item(0);
        }
        return e;
    }

    /**
     *
     * @return
     */
    public String getTimeStampServerUrl() {
        return mstrTimeStampServerUrl;
    }

    /**
     *
     * @return
     * @throws SEDSecurityException
     */
    public XMLSignatureFactory getXMLSignatureFactory() throws SEDSecurityException {
        long t = getTime();
        //org.jcp.xml.dsig.internal.dom.DOMXMLSignatureFactory
        XMLSignatureFactory fac = null;
        try {
            String providerName = System.getProperty(XML_SIGNATURE_PROVIDER_PROP);

            if (providerName == null) {
                providerName = XML_SIGNATURE_PROVIDER_VALUE_2;
            }
            Class c = null;
            try {
                c = Class.forName(providerName);
            } catch (ClassNotFoundException ignore) {
                providerName = XML_SIGNATURE_PROVIDER_VALUE_1.equalsIgnoreCase(providerName) ? XML_SIGNATURE_PROVIDER_VALUE_2 : XML_SIGNATURE_PROVIDER_VALUE_1;
                c = Class.forName(providerName);
            }
            mlgLogger.info("SvevSignatureUtils.getXMLSignatureFactory: user provider: '" + providerName + "'");
            fac = XMLSignatureFactory.getInstance("DOM", (Provider) c.newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            logError("SvevSignatureUtils.getXMLSignatureFactory", ex.getMessage(), t, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.InitializeException, ex, ex.getMessage());
        }
        return fac;
    }

    private boolean isSignatureTimestamp(Node sigNode) {
        return sigNode != null && sigNode.getParentNode() != null && XADES_XMLTimeStamp.equals(sigNode.getParentNode().getNodeName())
                && XADES_NS.equals(sigNode.getParentNode().getNamespaceURI());

    }

    /**
     *
     * @param strMethod
     * @param iStartTime
     */
    protected void logEnd(final String strMethod, long iStartTime) {
        mlgLogger.info(strMethod + ": - END (" + (getTime() - iStartTime) + "ms)");
    }

    /**
     *
     * @param strMethod
     * @param strMessage
     * @param iStartTime
     * @param ex
     */
    protected void logError(final String strMethod, String strMessage, long iStartTime, Exception ex) {
        mlgLogger.error(strMethod + ": - ERROR:" + strMethod + ":(" + (getTime() - iStartTime) + "ms)", ex);
    }

    /**
     *
     * @param strMethod
     * @return
     */
    protected long logStart(final String strMethod) {
        long t = getTime();
        mlgLogger.info(strMethod + ": - BEGIN");
        return t;
    }

    private void setIdnessToElemetns(Node n) {
        if (n.getNodeType() == Node.ELEMENT_NODE) {
            Element e = (Element) n;
            if (e.hasAttribute("Id")) {
                e.setIdAttribute("Id", true);
            } else if (e.hasAttribute("id")) {
                e.setIdAttribute("id", true);
            }
            if (e.hasAttribute("ID")) {
                e.setIdAttribute("ID", true);
            }
            NodeList l = e.getChildNodes();
            for (int i = 0; i < l.getLength(); i++) {
                setIdnessToElemetns(l.item(i));
            }
        }
    }

    /**
     *
     * @param sResultLogFolder
     */
    public void setResultLogFolder(String sResultLogFolder) {
        this.mstrResultLogFolder = sResultLogFolder;
    }

    /**
     *
     * @param mstrTimeStampServerUrl
     */
    public void setTimeStampServerUrl(String mstrTimeStampServerUrl) {
        this.mstrTimeStampServerUrl = mstrTimeStampServerUrl;
    }

    /**
     *
     * @param certPrivateKey
     * @param sigParentElement
     * @param strIds
     * @param os
     * @throws SEDSecurityException
     */
    public void singDocument(KeyStore.PrivateKeyEntry certPrivateKey, Element sigParentElement, List<String[]> strIds, OutputStream os) throws SEDSecurityException {
        long t = logStart("SvevSignatureUtils.singDocument");
        // get XMLSignatureFactory implemenation
        XMLSignatureFactory fac = getXMLSignatureFactory();
        // generate signature id's
        String strSigId = getUUID(ID_PREFIX_Signature);
        String strSigValId = getUUID(ID_PREFIX_SignatureValue);
        String strSigPropId = getUUID(ID_PREFIX_SignedProperties);
        strIds.add(new String[]{strSigPropId, XADES_NS + "SignedProperties"});

        X509Certificate cert = (X509Certificate) certPrivateKey.getCertificate();
        // Create the SignedInfo
        SignedInfo si = createSignedInfo(strIds, fac);
        // Create the KeyInfo
        KeyInfo ki = createXAdESKeyInfo(cert, fac);
        // Create the QualifyingProperties
        Document doc = sigParentElement.getOwnerDocument();
        XMLStructure content = createXAdESQualifyingProperties(strSigId, strSigValId, strSigPropId, cert, doc);
        XMLObject xoQualifyingProperties = fac.newXMLObject(Collections.singletonList(content), null, null, null);

        // Create the XMLSignature (but don't sign it yet)
        XMLSignature signature = fac.newXMLSignature(si, ki, Collections.singletonList(xoQualifyingProperties), strSigId, strSigValId);

        // Create the DOMSignContext
        DOMSignContext dsc = new DOMSignContext(certPrivateKey.getPrivateKey(), sigParentElement);

        try {
            // Marshal, generate (and sign) the enveloped signature
            setIdnessToElemetns(doc.getDocumentElement());
            signature.sign(dsc);
            setIdnessToElemetns(doc.getDocumentElement());
            //String strVal = calculateSignedValueDigest(strSigValId, fac, ki, certPrivateKey, sigParentElement.getOwnerDocument());
            String strVal = calculateSignedValueDigest(strSigValId, sigParentElement.getOwnerDocument());

            NodeList l = sigParentElement.getElementsByTagName("HashDataInfo");
            Element nTS = doc.createElementNS(XADES_NS, "XMLTimeStamp");
            l.item(0).getParentNode().appendChild(nTS);

            // Add timestamp
            if (getTimeStampServerUrl() != null && !getTimeStampServerUrl().trim().isEmpty()) {
                Element dSigTS = getTimeStamp(strVal);
                Node adTSig = doc.importNode(dSigTS, true);
                nTS.appendChild(adTSig);
            }
            // write it to output..
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer trans = tf.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.transform(new DOMSource(sigParentElement.getOwnerDocument()), new StreamResult(os));

        } catch (TransformerException ex) {
            mlgLogger.error("SvevSignatureUtils.", ex);
        } catch (MarshalException ex) {
            mlgLogger.error("SvevSignatureUtils.", ex);
        } catch (XMLSignatureException ex) {
            mlgLogger.error("SvevSignatureUtils.", ex);
        }
        logEnd("SvevSignatureUtils.singDocument", t);
    }

    private void validateSignature(Node sigNode) throws MarshalException, XMLSignatureException, SEDSecurityException {
        //  check if timestamp
        Node ndVal = sigNode;
        if (isSignatureTimestamp(sigNode)) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                Document doc = dbf.newDocumentBuilder().newDocument();
                Node n = doc.adoptNode(sigNode.cloneNode(true));
                doc.appendChild(n);
                setIdnessToElemetns(n);
                ndVal = doc.getDocumentElement();
            } catch (ParserConfigurationException ex) {
                java.util.logging.Logger.getLogger(XMLSignatureUtils.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        // Create a DOM XMLSignatureFactory that will be used to unmarshal the
        // document containing the XMLSignature 
        XMLSignatureFactory fac = getXMLSignatureFactory();

        // Create a DOMValidateContext and specify a KeyValue KeySelector
        // and document context
        DOMValidateContext valContext = new DOMValidateContext(new XMLSignatureX509KeySelector(), ndVal);
        //DOMValidateContext valContext = new DOMValidateContext(new X509KeySelector(ks), nl.item(0));
        // unmarshal the XMLSignature
        XMLSignature signature = fac.unmarshalXMLSignature(valContext);
        signature.getKeyInfo().getContent();

        // Validate the XMLSignature (generated above)
        boolean coreValidity = signature.validate(valContext);

        // Check core validation status
        if (coreValidity == false) {
            logError("validateSignature", "Signature failed core validation", 0, null);
            boolean sv = signature.getSignatureValue().validate(valContext);            
            // check the validation status of each Reference
            Iterator i = signature.getSignedInfo().getReferences().iterator();
            for (int j = 0; i.hasNext(); j++) {
                Reference r = ((Reference) i.next());
                boolean refValid = r.validate(valContext);
                String msg = "ref[" + j + ", id: " + r.getURI() + "] validity status: " + refValid;
                logError("validateSignature", "Signature failed core validation", 0, null);
            }
        } else {
            boolean sv = signature.getSignatureValue().validate(valContext);
            // check the validation status of each Reference
            Iterator i = signature.getSignedInfo().getReferences().iterator();
            for (int j = 0; i.hasNext(); j++) {
                Reference r = ((Reference) i.next());

                boolean refValid = r.validate(valContext);
                String msg = "ref[" + j + ", id: " + r.getURI() + "] validity status: " + refValid;
                logError("validateSignature", "Signature failed core validation", 0, null);                
            }
        }

    }

    /**
     *
     * @param fDoc
     * @throws SEDSecurityException
     * @throws XMLSignatureException
     * @throws MarshalException
     */
    public void validateXmlDSigSignature(File fDoc) throws SEDSecurityException, XMLSignatureException, MarshalException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fDoc);
            validateXmlDSigSignature(fis);
        } catch (FileNotFoundException ex) {
            logError("SvevSignatureUtils.validateXmlDSigSignature: FileNotFoundException", ex.getMessage(), getTime(), ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.XMLParseException, ex, ex.getMessage());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ingore) {
                }
            }
        }
    }

    /**
     *
     * @param is
     * @throws SEDSecurityException
     * @throws XMLSignatureException
     * @throws MarshalException
     */
    public void validateXmlDSigSignature(InputStream is) throws SEDSecurityException, XMLSignatureException, MarshalException {
        long t = logStart("SvevSignatureUtils.validateXmlDSigSignature");

        // Instantiate the document to be validated
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc;
        try {
            doc = dbf.newDocumentBuilder().parse(is);
        } catch (ParserConfigurationException ex) {
            logError("SvevSignatureUtils.validateXmlDSigSignature: ParserConfigurationException", ex.getMessage(), t, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.XMLParseException, ex, ex.getMessage());
        } catch (SAXException ex) {
            logError("SvevSignatureUtils.validateXmlDSigSignature: SAXException", ex.getMessage(), t, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.XMLParseException, ex, ex.getMessage());
        } catch (IOException ex) {
            logError("SvevSignatureUtils.validateXmlDSigSignature: IOException", ex.getMessage(), t, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.XMLParseException, ex, ex.getMessage());
        }
        setIdnessToElemetns(doc.getDocumentElement());

        // Find Signature element
        NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (nl.getLength() == 0) {
            logError("SvevSignatureUtils.validateXmlDSigSignature", "No signature found", t, null);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.SignatureNotFound, "No signature found");
        }
        for (int index = 0; index < nl.getLength(); index++) {

            validateSignature(nl.item(index));
        }

    }

    /**
     *
     * @param in
     * @param logFolder
     * @param fileNamePrefix
     * @param fileNameSuffix
     * @return
     */
    public File writeToFile(InputStream in, String logFolder, String fileNamePrefix, String fileNameSuffix) {
        FileOutputStream out = null;
        File f = null;
        try {

            f = File.createTempFile(fileNamePrefix, fileNameSuffix, new File(logFolder));
            out = new FileOutputStream(f);
            byte[] buffer = new byte[1024];
            int len = in.read(buffer);
            while (len != -1) {
                out.write(buffer, 0, len);
                len = in.read(buffer);
            }
        } catch (IOException ex) {
            String strMessage = "Error write to; '" + f.getAbsolutePath() + "' exception:" + ex.getMessage();
            mlgLogger.error(strMessage);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                mlgLogger.error("Error closing file; '" + f.getAbsolutePath() + "' exception:" + ex.getMessage());
            }
        }
        return f;
    }
}
