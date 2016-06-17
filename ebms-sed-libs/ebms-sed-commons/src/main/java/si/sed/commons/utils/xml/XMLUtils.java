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
package si.sed.commons.utils.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.util.JAXBSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class XMLUtils {

    /**
     *
     * @param xml
     * @return
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    public static Document bytesToDom(byte[] xml)
            throws SAXException, ParserConfigurationException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml));

    }

    /**
     *
     * @param fXMLFilePath
     * @param cls
     * @return
     * @throws JAXBException
     */
    public static Object deserialize(File fXMLFilePath, Class cls) throws JAXBException {
        final Unmarshaller um = JAXBContext.newInstance(cls).createUnmarshaller();
        return um.unmarshal(fXMLFilePath);
    }

    /**
     *
     * @param xml
     * @param cls
     * @return
     * @throws JAXBException
     */
    public static Object deserialize(String xml, Class cls) throws JAXBException {
        final Unmarshaller um = JAXBContext.newInstance(cls).createUnmarshaller();
        return um.unmarshal(new ByteArrayInputStream(xml.getBytes()));
    }

    /**
     *
     * @param io
     * @param cls
     * @return
     * @throws JAXBException
     */
    public static Object deserialize(InputStream io, Class cls) throws JAXBException {
        final Unmarshaller um = JAXBContext.newInstance(cls).createUnmarshaller();
        return um.unmarshal(io);
    }

    /**
     *
     * @param elmnt
     * @param cls
     * @return
     * @throws JAXBException
     */
    public static Object deserialize(Element elmnt, Class cls) throws JAXBException {
        final Unmarshaller um = JAXBContext.newInstance(cls).createUnmarshaller();
        return um.unmarshal(elmnt);
    }

    /**
     *
     * @param source
     * @param xsltSource
     * @param cls
     * @return
     * @throws TransformerConfigurationException
     * @throws JAXBException
     * @throws TransformerException
     */
    public static synchronized Object deserialize(InputStream source, InputStream xsltSource, Class cls) throws TransformerConfigurationException, JAXBException, TransformerException {
        Object obj = null;
        JAXBContext jc = JAXBContext.newInstance(cls);

        if (xsltSource != null) {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer;
            transformer = factory.newTransformer(new StreamSource(xsltSource));

            JAXBResult result = new JAXBResult(jc);
            transformer.transform(new StreamSource(source), result);
            obj = result.getResult();
        } else {
            obj = jc.createUnmarshaller().unmarshal(source);
        }
        return obj;
    }

    /**
     *
     * @param xmlIS
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static Document deserializeToDom(InputStream xmlIS) throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(xmlIS);
    }

    /**
     *
     * @param xmlFile
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static Document deserializeToDom(File xmlFile) throws IOException, ParserConfigurationException, SAXException {
        Document doc = null;
        try (FileInputStream fis = new FileInputStream(xmlFile)) {
            doc = deserializeToDom(fis);
        }
        return doc;
    }

    /**
     *
     * @param xml
     * @return
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static Document deserializeToDom(String xml) throws IOException, ParserConfigurationException, SAXException {
        return deserializeToDom(new ByteArrayInputStream(xml.getBytes()));
    }

    /**
     *
     * @param source
     * @param xsltSource
     * @return
     * @throws TransformerConfigurationException
     * @throws JAXBException
     * @throws TransformerException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static synchronized Document deserializeToDom(InputStream source, InputStream xsltSource) throws TransformerConfigurationException, JAXBException, TransformerException, ParserConfigurationException, SAXException, IOException {
        Document obj;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        if (xsltSource != null) {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer;
            transformer = factory.newTransformer(new StreamSource(xsltSource));
            obj = dbf.newDocumentBuilder().newDocument();
            Result result = new DOMResult(obj);
            transformer.transform(new StreamSource(source), result);
        } else {
            obj = dbf.newDocumentBuilder().parse(source);

        }
        return obj;
    }

    /**
     *
     * @param unformattedXml
     * @return
     */
    public static String format(String unformattedXml) {
        String xmlString = null;
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(new StringWriter());

            DOMSource source = new DOMSource(deserializeToDom(unformattedXml));
            transformer.transform(source, result);
            xmlString = result.getWriter().toString();
        } catch (IOException | ParserConfigurationException | SAXException | TransformerException ex) {
            Logger.getLogger(XMLUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return xmlString;
    }

    /**
     *
     * @param source
     * @param xsltSource
     * @return
     * @throws TransformerConfigurationException
     * @throws JAXBException
     * @throws TransformerException
     */
    public static synchronized String getElementValue(InputStream source, InputStream xsltSource) throws TransformerConfigurationException, JAXBException, TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer;
        transformer = factory.newTransformer(new StreamSource(xsltSource));
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        transformer.transform(new StreamSource(source), result);
        return sw.toString();
    }

    /**
     *
     * @param obj
     * @return
     * @throws JAXBException
     * @throws ParserConfigurationException
     */
    public static Document jaxbToDocument(Object obj) throws JAXBException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = factory.newDocumentBuilder();
        Document doc = db.newDocument();
        // Marshal the Object to a Document
        final Marshaller m = JAXBContext.newInstance(obj.getClass()).createMarshaller();
        m.marshal(obj, doc);
        return doc;
    }

    /**
     *
     * @param obj
     * @return
     * @throws JAXBException
     */
    public static byte[] serialize(Object obj) throws JAXBException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        serialize(obj, bos);
        return bos.toByteArray();
    }

    /**
     *
     * @param obj
     * @param os
     * @throws JAXBException
     */
    public static void serialize(Object obj, OutputStream os) throws JAXBException {
        final Marshaller m = JAXBContext.newInstance(obj.getClass()).createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.marshal(obj, os);
    }

    /**
     *
     * @param obj
     * @param filename
     * @throws JAXBException
     * @throws FileNotFoundException
     */
    public static void serialize(Object obj, String filename) throws JAXBException, FileNotFoundException {
        final Marshaller m = JAXBContext.newInstance(obj.getClass()).createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(obj, new FileOutputStream(filename));
    }

    /**
     *
     * @param obj
     * @param file
     * @throws JAXBException
     * @throws FileNotFoundException
     */
    public static void serialize(Object obj, File file) throws JAXBException, FileNotFoundException {
        final Marshaller m = JAXBContext.newInstance(obj.getClass()).createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(obj, new FileOutputStream(file));
    }

    /**
     *
     * @param obj
     * @param w
     * @throws JAXBException
     * @throws FileNotFoundException
     */
    public static void serialize(Object obj, Writer w) throws JAXBException, FileNotFoundException {
        final Marshaller m = JAXBContext.newInstance(obj.getClass()).createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(obj, w);
    }

    /**
     *
     * @param obj
     * @throws JAXBException
     */
    public static void serializeToSTD(Object obj) throws JAXBException {
        final Marshaller m = JAXBContext.newInstance(obj.getClass()).createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.marshal(obj, System.out);
    }

    /**
     *
     * @param obj
     * @return
     * @throws JAXBException
     */
    public static String serializeToString(Object obj) throws JAXBException {
        java.io.StringWriter sw = new StringWriter();
        final Marshaller m = JAXBContext.newInstance(obj.getClass()).createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(obj, sw);
        return sw.toString();
    }

    /**
     *
     * @param rootElement
     * @param setXmlDecl
     * @return
     */
    public static String serializeToString(Element rootElement, boolean setXmlDecl) {
        DOMImplementationLS lsImpl = (DOMImplementationLS) rootElement.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
        LSSerializer serializer = lsImpl.createLSSerializer();
        serializer.getDomConfig().setParameter("xml-declaration", setXmlDecl); //set it to false to get String without xml-declaration
        return serializer.writeToString(rootElement);
    }

    /**
     *
     * @param source
     * @param xsltSource
     * @return
     * @throws TransformerConfigurationException
     * @throws JAXBException
     * @throws TransformerException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static synchronized Document transform(Element source, InputStream xsltSource) throws TransformerConfigurationException, JAXBException, TransformerException, ParserConfigurationException, SAXException, IOException {
        Document obj = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        if (xsltSource != null) {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer;
            transformer = factory.newTransformer(new StreamSource(xsltSource));
            obj = dbf.newDocumentBuilder().newDocument();
            Result result = new DOMResult(obj);
            transformer.transform(new DOMSource(source), result);
        }
        return obj;
    }

    /**
     *
     * @param jabxObj
     * @param schXsd
     * @param xsdResourceFolder
     * @return
     */
    public static String validateBySchema(Object jabxObj, InputStream schXsd, String xsdResourceFolder) {
        String res = "";
        try {
            JAXBContext jbc = JAXBContext.newInstance(jabxObj.getClass());
            Source xml = new JAXBSource(jbc, jabxObj);
            res = validateBySchema(xml, schXsd, xsdResourceFolder);
        } catch (JAXBException ex) {

            res += "SchemaValidationError 2:" + ex.getMessage() + "\n";
        }
        return res;

    }

    /**
     *
     * @param xml
     * @param schXsd
     * @param xsdResourceFolder
     * @return
     */
    public static String validateBySchema(Source xml, InputStream schXsd, String xsdResourceFolder) {
        String res = "";

        try {
            SchemaErrorHandler se = new SchemaErrorHandler();

            Source xsdFile = new StreamSource(schXsd);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setResourceResolver(new SchemaResourceResolver(xsdResourceFolder));
            schemaFactory.setErrorHandler(se);
            Schema schema = schemaFactory.newSchema(xsdFile);
            Validator validator = schema.newValidator();
            validator.validate(xml);
            res = se.getErrorAsString();
        } catch (SAXException | IOException ex) {

            res += "SchemaValidationError:" + ex.getMessage() + "\n";
        }

        return res;

    }

}
