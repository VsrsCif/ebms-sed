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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.log4j.Logger;

import org.xml.sax.SAXException;
import si.sed.commons.exception.FOPException;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class FOPUtils {

    public enum FopTransformations {

        DeliveryNotification("DeliveryNotification.fo"),
        AdviceOfDelivery("AdviceOfDelivery.fo"),
        AdviceOfDeliveryFiction("AdviceOfDeliveryFiction.fo");

        private final String mstrfileName;

        FopTransformations(String filename) {
            mstrfileName = filename;
        }

        public String getFileName() {
            return mstrfileName;
        }
    }
    private static final Logger mlog = Logger.getLogger(FOPUtils.class);
    FopFactory mfopFactorory = null;
    String mTransformationFolder;
    File msfConfigFile;

    public FOPUtils(File configfile, String xsltFolder) {
        msfConfigFile = configfile;
        mTransformationFolder = xsltFolder;

    }

    private FopFactory getFopFactory() throws SAXException, IOException {
        if (mfopFactorory == null) {
            mfopFactorory = FopFactory.newInstance();
            if (msfConfigFile != null) {
                mfopFactorory.setUserConfig(msfConfigFile);
            }
        }
        return mfopFactorory;

    }

    public static void main(String... args) {
        try {
            File cfile = new File("/sluzba/mag-naloga/izmenjava-podatkov/code/SVEVDemo/msh-AS4/msh/config/fop.xconf");
            File fIn = new File("/sluzba/mag-naloga/izmenjava-podatkov/code/SVEVDemo/msh-AS4/msh/inbox/msh-as4/msh--653980083958689530-Request.xml");
            File fOut = new File("/sluzba/mag-naloga/izmenjava-podatkov/code/SVEVDemo/msh-AS4/msh/inbox/msh-as4/msh--653980083958689530-Request.pdf");
            String folder = "/sluzba/mag-naloga/izmenjava-podatkov/code/SVEVDemo/msh-AS4/msh/config/xslt/";

            FOPUtils fu = new FOPUtils(cfile, folder);
            fu.generateVisualization(fIn, fOut, "", FopTransformations.DeliveryNotification);
        } catch (FOPException ex) {
            ex.printStackTrace();
        }

    }



    public byte[] generateVisualization(File inputRequest, String service, FopTransformations xslt) throws FOPException {
        // get service  + "get vssl"
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Source src = new StreamSource(inputRequest);
        generateVisualization(src, bos, new StreamSource(getTransformatinoFile(xslt, service)));
        return bos.toByteArray();
    }

    public void generateVisualization(Source src, OutputStream out, Source xslt) throws FOPException {

        try {
            Fop fop = getFopFactory().newFop(MimeConstants.MIME_PDF, out);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xslt);
            Result res = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, res);
        } catch (IOException | SAXException | TransformerException ex) {
            String msg = "Error generating visualization";
            throw new FOPException(msg, ex);
        } finally {
            try {
                out.close();
            } catch (IOException ignore) {

            }
        }
    }

    public void generateVisualization(File fIn, File fOut, String service, FopTransformations xslt) throws FOPException {

        File tmplxslt = getTransformatinoFile(xslt, service);
        try (OutputStream out = new FileOutputStream(fOut)) {
            generateVisualization(new StreamSource(fIn), out, new StreamSource(tmplxslt));
        } catch (IOException ex) {
            String msg = "Error generating visualization: Error writing to file :'" + fOut + "'!";
            throw new FOPException(msg, ex);
        }
    }

    private File getTransformatinoFile(FopTransformations xslt, String service) {
        if (mTransformationFolder == null) {
            return new File(xslt.getFileName());
        }
        return new File(mTransformationFolder, service + "-" + xslt.getFileName());

    }

}
