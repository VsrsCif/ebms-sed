/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.plugin.zpp.doc;

import com.sun.scenario.Settings;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.sec.CertificateUtils;
import si.sed.commons.utils.sec.XMLSignatureUtils;

/**
 *
 * @author logos
 */
public abstract class DocumentBuilder {

    // schema type
    public static final String SOD_V1 = "SOD_V1";
    public static final String CREA_V1 = "CREA_V1";

    protected static final String MIME_TXT = "text/xml";
    protected static final String MIME_PDF = "application/pdf";
    protected static final String IDPFX_VIS = "vis-test";
    protected static final String IDPFX_DATA = "dat-test";
    protected static final String ENC_TYPE_B64 = "base64";
    protected static final String ENC_TYPE_UTF8 = "UTF-8";
    protected static final String DELIVERY_TYPE = "Legal-ZPP2";
    protected static final String DOCUMENT_TYPE = "Message";

    private static final String SIGNATURE_ELEMENT_NAME = "Signatures";
    private static final String IDPFX_SIG = "sig-test";
    private static final String IDPFX_SIG_PROP = "sigprop-test";
    private static final String HLSSDK_JKSPATH = "JKSPATH";
    private static final String XAdESCertificateDigestAlgorithm = "http://www.w3.org/2000/09/xmldsig#sha1";
    private static final String XMLHEADER = "<?";
    private static final String XAdESignatureProductionPlace = "Ljubljana";

    //private static ESignDocImpl medSigJDK = null;
    private XMLSignatureUtils mssuSignUtils;
    StorageUtils msuStorageUtils = new StorageUtils();

    Logger mlgLogger = Logger.getLogger(DocumentBuilder.class.getName());

    public static void writeToFile(String strVal, String strFile) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(strFile);
            fos.write(strVal.getBytes(ENC_TYPE_UTF8));
        } catch (IOException ex) {
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
            }
        }
    }

    public XMLSignatureUtils getSignUtils() {
        if (mssuSignUtils == null) {
            mssuSignUtils = new XMLSignatureUtils();
            //mssuSignUtils.setTimeStampServerUrl(Settings.getInstance().getTimestampUrl());
        }
        return mssuSignUtils;
    }

    protected Document convertEpDoc2W3cDoc(Object jaxBDoc, Class[] cls) throws SEDSecurityException {
        Document xDoc = null;
        try {
            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
            xDoc = db.newDocument();

            JAXBContext jc = JAXBContext.newInstance(cls);
            // Marshal the Object to a Document
            Marshaller marshaller = jc.createMarshaller();
            marshaller.marshal(jaxBDoc, xDoc);
        } catch (JAXBException ex) {
            String strMsg = "DocumentBuilder.convertEpDoc2W3cDoc: could marshal Document: JAXBException: '" + ex.getMessage() + "'.";
            mlgLogger.error(strMsg, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CreateSignatureException, ex);
        } catch (ParserConfigurationException ex) {
            String strMsg = "DocumentBuilder.convertEpDoc2W3cDoc: could not create w3c document: ParserConfigurationException: '" + ex.getMessage() + "'.";
            mlgLogger.error(strMsg, ex);
            throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CreateSignatureException, ex);
        }

        return xDoc;
    }

    public abstract void createMail(MSHOutMail dce, String alias,  FileOutputStream fos) throws SEDSecurityException;

    /*
    protected static synchronized ESignDocImpl getSigJDK() throws SEDSecurityException {
        if (medSigJDK == null) {
            medSigJDK = new ESignDocImpl();
            try {
                System.out.println("INIT: KEY" + Settings.getInstance().getKeystorePath());
                ESignDocImpl.initProfile(Settings.getInstance().getKeystorePath(),HLSSDK_JKSPATH);
            } catch (GeneralSecurityException ex) {
                throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.ApplicationError, "APPLICATION EXCEPTION occurred while validating signature! Msg: '" + ex.getMessage() + "'", ex);
            }
        }
        return medSigJDK;
    }
     */
    protected long getTime() {
        return Calendar.getInstance().getTimeInMillis();
    }

    protected synchronized void singDocument(Document xDoc, List<String[]> strIds,
            String alias, FileOutputStream fos) throws SEDSecurityException {
        long t = getTime();
        mlgLogger.info("DocumentBuilder.singDocument: begin ");

        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) CertificateUtils.getInstance().getPrivateKeyEntryForAlias(alias);

        NodeList lst = xDoc.getDocumentElement().getElementsByTagName(SIGNATURE_ELEMENT_NAME);
        Element eltSignature = (Element) lst.item(0);
        getSignUtils().singDocument(entry, eltSignature, strIds, fos);

        mlgLogger.info("DocumentBuilder.singDocument: - end (" + (getTime() - t) + "ms)");
    }

}
