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
package si.sed.commons.exception;

import javax.xml.namespace.QName;
import org.apache.cxf.binding.soap.SoapFault;

/**
 *
 * @author Jože Rihtaršič
 */
public class ExceptionUtils {

    /**
     *
     * @param sc
     * @param soapCode
     * @return
     */
    public static SoapFault createSoapFault(SOAPExceptionCode sc, QName soapCode) {
        SoapFault sf = new SoapFault(sc.getDesc(), soapCode);

        sf.setSubCode(sc.getCode());
        return sf;
    }

    /**
     *
     * @param sc
     * @param soapCode
     * @param msg
     * @return
     */
    public static SoapFault createSoapFault(SOAPExceptionCode sc, QName soapCode,
            String... msg) {
        SoapFault sf = new SoapFault(sc.getDesc(msg), soapCode);
        sf.setSubCode(sc.getCode());
        return sf;
    }

}
