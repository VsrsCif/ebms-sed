/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.interfaces;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import javax.ejb.Local;
import org.sed.ebms.cert.SEDCertStore;
import org.sed.ebms.cron.SEDCronJob;
import org.sed.ebms.cron.SEDTaskType;

import org.sed.ebms.ebox.SEDBox;
import org.sed.ebms.plugin.SEDPlugin;
import org.sed.ebms.user.SEDUser;

/**
 *
 * @author sluzba
 */
@Local
public interface SEDLookupsInterface {
    boolean addSEDBox(SEDBox sb);
    boolean addSEDCertStore(SEDCertStore sb);
    boolean addSEDCronJob(SEDCronJob sb);
    boolean addSEDPlugin(SEDPlugin sb);
    boolean addSEDTaskType(SEDTaskType sb);
    boolean addSEDUser(SEDUser sb);
    
    void exportLookups(File f);
    
    SEDBox getSEDBoxByName(String strname);
    SEDCertStore getSEDCertStoreByCertAlias(String alias, boolean isKey);
    SEDCronJob getSEDCronJobById(BigInteger id);
    SEDTaskType getSEDTaskTypeByType(String type);
    SEDUser getSEDUserByUserId(String userId);
    SEDPlugin getSEDPluginByType(String type);
    
    List<SEDBox> getSEDBoxes();
    List<SEDCertStore> getSEDCertStore();    
    List<SEDCronJob> getSEDCronJobs();
    List<SEDPlugin> getSEDPlugin();    
    List<SEDTaskType> getSEDTaskTypes();    
    List<SEDUser> getSEDUsers();
    
    boolean removeSEDBox(SEDBox sb);
    boolean removeEDCertStore(SEDCertStore sb);
    boolean removeSEDCronJob(SEDCronJob sb);    
    boolean removeSEDPlugin(SEDPlugin sb);
    boolean removeSEDTaskType(SEDTaskType sb);
    boolean removeSEDUser(SEDUser sb);
    
    boolean updateSEDBox(SEDBox sb);
    boolean updateSEDCertStore(SEDCertStore sb);
    boolean updateSEDCronJob(SEDCronJob sb);
    boolean updateSEDPlugin(SEDPlugin sb);
    boolean updateSEDTaskType(SEDTaskType sb);
    boolean updateSEDUser(SEDUser sb);
}
