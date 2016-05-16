/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.interfaces;

import javax.ejb.Local;
import org.sed.ebms.report.SEDReportBoxStatus;



/**
 *
 * @author sluzba
 */
@Local
public interface SEDReportInterface {
    
    
    SEDReportBoxStatus getStatusReport(String strSedBox);
}
