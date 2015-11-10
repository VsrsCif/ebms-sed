/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.commons.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.MimeConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.msh.ebms.outbox.mail.MSHOutMail;
import si.sed.commons.exception.FOPException;

/**
 *
 * @author sluzba
 */
public class FOPUtilsTest {
    
    public FOPUtilsTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

   /**
     * Test of generateVisualization method, of class FOPUtils.
     */
    @Test
    public void testGenerateVisualizationFromMSHOutMail() throws JAXBException, FileNotFoundException, FOPException, IOException  {
     /*   
        String fopConfigFile = "src/test/resources/fop/fop.xconf";
        String xsltFolder = "src/test/resources/fop/xslt/LegalDelivery_ZPP-DeliveryNotification.fo";
        
        MSHOutMail mout  = new  MSHOutMail();
        mout.setAction("DeliveryNotification");
        mout.setId(BigInteger.valueOf(1234));
        mout.setConversationId("ConversationId");
        mout.setMessageId("MessageId");
        mout.setReceiverEBox("receiver.box@ebox.si");
        mout.setReceiverName("receiver Box");
        mout.setSenderEBox("sender.box@ebox.si");
        mout.setSenderName("Sender Box");
        mout.setSenderMessageId("SenderMessageID");
        mout.setSentDate(Calendar.getInstance().getTime());
        
        JAXBSource source = new JAXBSource(JAXBContext.newInstance(mout.getClass()), mout );
        
        FOPUtils instance = new FOPUtils(new File(fopConfigFile),xsltFolder );
        
        try (FileOutputStream fos = new FileOutputStream("test.txt")){
            instance.generateVisualization(source, fos,  new StreamSource(xsltFolder), MimeConstants.MIME_PLAIN_TEXT);
        }
        try (FileOutputStream fos = new FileOutputStream("test.pdf")){
            instance.generateVisualization(source, fos,  new StreamSource(xsltFolder), MimeConstants.MIME_PDF);
        }
       */
        
        
        
    }

    
}
