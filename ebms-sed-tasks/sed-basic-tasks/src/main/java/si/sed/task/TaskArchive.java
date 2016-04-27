/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.xml.bind.JAXBException;
import org.msh.ebms.inbox.event.MSHInEvent;
import org.msh.ebms.inbox.mail.MSHInMail;
import org.msh.ebms.inbox.mail.MSHInMailList;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.mail.MSHOutMailList;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.SEDDaoInterface;
import si.sed.commons.interfaces.TaskExecutionInterface;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.Utils;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author sluzba
 */
@Stateless
@Local(TaskExecutionInterface.class)
public class TaskArchive implements TaskExecutionInterface {

    public static String KEY_EXPORT_FOLDER = "export_folder";
    public static String KEY_DATE_OFFSET = "date_offset";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyMMdd_HHMMss");
    String outFileFormat = "%s_%s.xml";

    private static final SEDLogger LOG = new SEDLogger(TaskArchive.class);

    @EJB(mappedName = SEDJNDI.JNDI_SEDDAO)
    SEDDaoInterface mdao;

    @Override
    public String executeTask(Properties p) throws Exception {
        long l = LOG.logStart();

        int i = Integer.parseInt(p.getProperty(KEY_DATE_OFFSET));
        String sfolder = p.getProperty(KEY_EXPORT_FOLDER);
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -1 * i);


        File f = new File(Utils.replaceProperties(sfolder));
        if (!f.exists()) {
            f.mkdirs();
        }
        
        archiveOutMails(c.getTime(), f);
        archiveInMails(c.getTime(), f);

        LOG.logEnd(l);
        return "suc";
    }

    public void archiveOutMails(Date to, File f) throws JAXBException, FileNotFoundException {
        MSHOutMailList noList = new MSHOutMailList();

        SearchParameters sp = new SearchParameters();
        sp.setSubmittedDateTo(to);

        List<MSHOutMail> lst = mdao.getDataList(MSHOutMail.class, -1, -1, "Id", "ASC", sp);
        LOG.log("got: " + lst);
        if (!lst.isEmpty()) {
            
            noList.setCount(lst.size());
            noList.getMSHOutMails().addAll(lst);
            File fout =new File( f, String.format(outFileFormat, sdf.format(Calendar.getInstance().getTime()), "MSHOutMail"));
            XMLUtils.serialize(noList, fout);
            LOG.log("Exported " +lst.size() + " to " +fout.getAbsolutePath() );
            // delete lists

            for (MSHOutMail mo : lst) {
                // move folder to new 
            }
            
            mdao.removeMail(MSHOutMail.class, lst);
        }

    }

    public void archiveInMails(Date to, File f) throws JAXBException, FileNotFoundException {
        MSHInMailList noList = new MSHInMailList();

        SearchParameters sp = new SearchParameters();
        sp.setReceivedDateTo(to);

        List<MSHInMail> lst = mdao.getDataList(MSHInMail.class, -1, -1, "Id", "ASC", sp);
        LOG.log("got: " + lst);
        
        if (!lst.isEmpty()) {
            
            
            noList.setCount(lst.size());
            noList.getMSHInMails().addAll(lst);
            File fout =new File( f, String.format(outFileFormat, sdf.format(Calendar.getInstance().getTime()), "MSHInMail"));
            XMLUtils.serialize(noList, fout);
            
            LOG.log("Exported " +lst.size() + " to " +fout.getAbsolutePath() );
            // delete lists
            mdao.removeMail(MSHInMail.class, lst);
            
        }
    }

    @Override
    public String getType() {
        return "archive";
    }

    @Override
    public String getName() {
        return "Archive data";
    }

    @Override
    public String getDesc() {
        return "Archive data and blobs";
    }

    @Override
    public Properties getProperties() {
        Properties p = new Properties();
        p.setProperty("Folder", "Archive folder");
        p.setProperty("Offset", "Archive mail older than [offset] days");
        
        return p;
    }

}
