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

import java.io.StringWriter;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class SchemaErrorHandler implements ErrorHandler {

    StringWriter sw = new StringWriter();

    @Override
    public void error(SAXParseException ex)
            throws SAXException {
        sw.append(ex.toString());
        sw.append("\n");
    }

    @Override
    public void fatalError(SAXParseException ex)
            throws SAXException {
        sw.append(ex.toString());
        sw.append("\n");
    }

    /**
     *
     * @return
     */
    public String getErrorAsString() {
        return sw.toString();
    }

    @Override
    public void warning(SAXParseException ex)
            throws SAXException {
        // ignore
    }
}
