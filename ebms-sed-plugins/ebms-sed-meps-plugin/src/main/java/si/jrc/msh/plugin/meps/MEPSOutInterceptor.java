/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.plugin.meps;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import org.apache.cxf.binding.soap.SoapMessage;
import si.sed.commons.interfaces.SoapInterceptorInterface;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(SoapInterceptorInterface.class)
@TransactionManagement(TransactionManagementType.BEAN)
public class MEPSOutInterceptor implements SoapInterceptorInterface {

    @Override
    public void handleFault(SoapMessage t) {

    }

    @Override
    public void handleMessage(SoapMessage msg) {
        // create one pdf with white pages
        // file message

    }

}
