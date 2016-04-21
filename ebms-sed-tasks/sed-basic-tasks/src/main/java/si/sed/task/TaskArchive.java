/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.task;

import java.io.File;
import java.util.List;
import java.util.Properties;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.mail.MSHOutMailList;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.interfaces.TaskExecutionInterface;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(TaskExecutionInterface.class)
public class TaskArchive implements TaskExecutionInterface {

    private static final SEDLogger LOG = new SEDLogger(TaskArchive.class);
    
    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mdao;

    @Override
    public String executeTask(Properties p) throws Exception {
        long l = LOG.logStart();
        MSHOutMailList noList = new  MSHOutMailList();
        MSHOutMail moSearchParam = new MSHOutMail();
        List<MSHOutMail> lst =  mdao.getDataList(MSHOutMail.class, -1, -1, "Id", "ASC", null);
        if (!lst.isEmpty()){
            noList.setCount(lst.size());
            noList.getMSHOutMails().addAll(lst);
            
            XMLUtils.serialize(noList, new File("MSHOutMail.xml"));
            // delete lists
        }
        LOG.logEnd(l, "Exported: " + lst.size());
        return "suc";
    }

}
