/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.exception;

import javax.xml.namespace.QName;
import org.apache.cxf.binding.soap.SoapFault;

/**
 *
 * @author sluzba
 */
public class ExceptionUtils {

    public static SoapFault createSoapFault(SOAPExceptionCode sc, QName soapCode) {
        SoapFault sf = new SoapFault(sc.getDesc(), soapCode);

        sf.setSubCode(sc.getCode());
        return sf;
    }

    public static SoapFault createSoapFault(SOAPExceptionCode sc, QName soapCode, String... msg) {
        SoapFault sf = new SoapFault(sc.getDesc(msg), soapCode);
        sf.setSubCode(sc.getCode());
        return sf;
    }

}
