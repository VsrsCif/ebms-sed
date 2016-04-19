/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.interfaces;

import java.math.BigInteger;
import java.util.List;
import javax.ejb.Local;
import org.msh.ebms.cron.MSHCronJob;
import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.user.SEDUser;

/**
 *
 * @author sluzba
 */
@Local
public interface SEDLookupsInterface {

    boolean addMSHCronJob(MSHCronJob sb);

    boolean addSEDBox(SEDBox sb);

    boolean addSEDUser(SEDUser sb);
    
    public MSHCronJob getMSHCronJobById(BigInteger id);

    List<MSHCronJob> getMSHCronJobs();

    SEDBox getSEDBoxByName(String strname);

    List<SEDBox> getSEDBoxes();

    List<SEDUser> getSEDUsers();

    boolean removeMSHCronJob(MSHCronJob sb);

    boolean removeSEDBox(SEDBox sb);

    boolean removeSEDUser(SEDUser sb);

    boolean updateMSHCronJob(MSHCronJob sb);

    boolean updateSEDBox(SEDBox sb);

    boolean updateSEDUser(SEDUser sb);
    
}
