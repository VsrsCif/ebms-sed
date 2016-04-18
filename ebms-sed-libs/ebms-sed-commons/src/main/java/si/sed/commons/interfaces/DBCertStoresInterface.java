/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.interfaces;

import java.util.List;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import org.msh.ebms.cert.MSHCertStore;

/**
 *
 * @author sluzba
 */
@Local
public interface DBCertStoresInterface {

    @Lock(value = LockType.READ)
    List<MSHCertStore> getCertStores();
    
}
