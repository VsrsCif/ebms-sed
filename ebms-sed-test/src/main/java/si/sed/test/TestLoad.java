/*
* Copyright 2016, Supreme Court Republic of Slovenia 
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
package si.sed.test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.sed.ebms.SEDException_Exception;
import org.sed.ebms.SEDMailBoxWS;
import org.sed.ebms.SedMailbox;
import org.sed.ebms.SubmitMailRequest;
import org.sed.ebms.SubmitMailResponse;
import org.sed.ebms.control.Control;
import org.sed.ebms.outbox.mail.OutMail;
import org.sed.ebms.outbox.payload.OutPart;
import org.sed.ebms.outbox.payload.OutPayload;
import org.sed.ebms.outbox.property.OutProperties;
import org.sed.ebms.outbox.property.OutProperty;
import si.sed.commons.MimeValues;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author sluzba
 */
public class TestLoad {

    public static String MAILBOX_ADDRESS = "http://10.48.0.101:8580/ebms-sed-ws/sed-mailbox?wsdl";
    public static String BLOB_FOLDER = "/sluzba/code/data/test-pdf/";

    public static String[] SENDER_NAMES = new String[]{"Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Trebnjem", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Mariboru", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Krškem", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Postojni", "Okrajno sodišče v Postojni, Izvršilni oddelek ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Sevnici", "Okrajno sodišče v Trbovljah, Izvršilni oddelek ", "Okrajno sodišče v Slovenj Gradcu, Izvršilni oddelek ", "Okrajno sodišče v Domžalah, Izvršilni oddelek ", "Okrajno sodišče v Radovljici ", "Okrajno sodišče v Postojni ", "Okrajno sodišče v Šmarju pri Jelšah, Izvršilni oddelek ", "Okrajno sodišče v Trebnjem ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Kočevju", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Šentjurju ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Litiji", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Novi Gorici", "Okrajno sodišče v Krškem, Izvršilni oddelek ", "Okrajno sodišče v Črnomlju, Izvršilni oddelek ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Brežicah", "Okrajno sodišče v Mariboru ", "Okrajno sodišče v Ajdovščini, Izvršilni oddelek ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Piranu", "Okrajno sodišče v Trbovljah ", "Okrajno sodišče na Ptuju ", "Okrajno sodišče v Piranu, Izvršilni oddelek ", "Okrajno sodišče v Žalcu, Izvršilni oddelek ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče na Jesenicah", "Okrajno sodišče v Šentjurju ", "Okrajno sodišče v Škofji Loki ", "Okrajno sodišče v Kočevju ", "Okrajno sodišče v Slovenski Bistrici, Izvršilni oddelek ", "Okrajno sodišče v Gornji Radgoni, Izvršilni oddelek ", "Okrajno sodišče v Kočevju, Izvršilni oddelek ", "Okrajno sodišče v Brežicah ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Ajdovščini", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Kopru", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Ljubljani", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Kranju", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Lendavi", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Cerknici", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Sežani", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Slovenj Gradcu", "Okrajno sodišče na Ptuju, Izvršilni oddelek ", "Okrajno sodišče v Lendavi, Izvršilni oddelek ", "Okrajno sodišče v Slovenj Gradcu ", "Okrajno sodišče v Mariboru, Izvršilni oddelek ", "Okrajno sodišče v Grosupljem ", "Okrajno sodišče v Murski Soboti, Izvršilni oddelek ", "Okrajno sodišče v Ljutomeru, Izvršilni oddelek ", "Okrajno sodišče v Radovljici, Izvršilni oddelek ", "Okrajno sodišče v Sevnici ", "Okrajno sodišče v Ilirski Bistrici ", "Okrajno sodišče v Trebnjem, Izvršilni oddelek ", "Okrajno sodišče v Tolminu ", "Okrajno sodišče v Šentjurju  ", "Okrajno sodišče v Gornji Radgoni ", "Okrajno sodišče v Brežicah, Izvršilni oddelek ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Celju", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Slovenski Bistrici", "Okrajno sodišče v Kamniku ", "Okrajno sodišče v Lenartu ", "Okrajno sodišče v Novi Gorici, Izvršilni oddelek ", "Okrajno sodišče na Jesenicah, Izvršilni oddelek ", "Okrajno sodišče v Slovenski Bistrici ", "Okrajno sodišče v Kopru ", "Okrajno sodišče v Škofji Loki, Izvršilni oddelek ", "Okrajno sodišče v Sežani, Izvršilni oddelek ", "Okrajno sodišče v Kopru, Izvršilni oddelek ", "Okrajno sodišče v Piranu ", "Okrajno sodišče v Lenartu, Izvršilni oddelek ", "Okrajno sodišče na Vrhniki ", "Okrajno sodišče v Novi Gorici ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Gornji Radgoni", "Okrajno sodišče v Domžalah ", "Okrajno sodišče v Slovenskih Konjicah ", "Okrajno sodišče v Ilirski Bistrici, Izvršilni oddelek ", "Okrajno sodišče v Sežani ", "Okrajno sodišče v Cerknici, Izvršilni oddelek ", "Okrajno sodišče v Idriji ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Velenju", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Kamniku", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Šmarju pri Jelšah", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Murski Soboti", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Škofji Loki", "Okrajno sodišče v Novem mestu, Izvršilni oddelek ", "Okrajno sodišče v Celju, Izvršilni oddelek ", "Okrajno sodišče v Murski Soboti ", "Okrajno sodišče v Litiji, Izvršilni oddelek ", "Okrajno sodišče v Slovenskih Konjicah, Izvršilni oddelek ", "Okrajno sodišče na Vrhniki, Izvršilni oddelek ", "Okrajno sodišče v Ormožu, Izvršilni oddelek ", "Okrajno sodišče v Cerknici ", "Okrajno sodišče v Litiji ", "Okrajno sodišče v Lendavi ", "Okrajno sodišče v Novem mestu ", "Okrajno sodišče v Tolminu, Izvršilni oddelek ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče na Ptuju", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Žalcu", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Domžalah", "Okrajno sodišče v Velenju, Izvršilni oddelek ", "Okrajno sodišče v Šmarju pri Jelšah ", "Okrajno sodišče v Velenju ", "Okrajno sodišče v Celju ", "Okrajno sodišče v Krškem ", "Okrajno sodišče v Ljutomeru ", "Okrajno sodišče v Ajdovščini ", "Okrajno sodišče v Idriji, Izvršilni oddelek ", "Okrajno sodišče v Ormožu ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Novem mestu", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Radovljici", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Lenartu", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Črnomlju", "Okrajno sodišče v Ljubljani, Izvršilni oddelek ", "Okrajno sod. v Ljubljani - centralni oddelek za verodostojno listino ", "Okrajno sodišče v Kranju ", "Okrajno sodišče na Jesenicah ", "Okrajno sodišče v Šentjurju, Izvršilni oddelek ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče na Vrhniki", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Šentjurju", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Trbovljah", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Slovenskih Konjicah", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sod. v Ljubljani - centralni oddelek za verodostojno listino", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Ljutomeru", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Idriji", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Grosupljem", "Okrajno sodišče v Kranju, Izvršilni oddelek ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Ormožu", "Okrajno sodišče v Črnomlju ", "Okrajno sodišče v Kamniku, Izvršilni oddelek ", "Okrajno sodišče v Ljubljani ", "Okrajno sodišče v Sevnici, Izvršilni oddelek ", "Okrajno sodišče v Žalcu ", "Okrajno sodišče v Grosupljem, Izvršilni oddelek ", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Ilirski Bistrici", "Vrhovno sodišče - Informacijski sistem eIzvršba Okrajno sodišče v Tolminu"};
    public static String[] SUBJECTS = new String[]{"dopis z dne 29.3.2016, PRAVN. SKLEP O ZVRŠBI", "sklep 23.03.2016,dgp.odg.na ug.s pril.z dne 21.12.2015", "P60_23, sklep št.III D 459/13 (priloga C8)", "r.št. 25 s kl.prav.", "dopis o poplačilu, dgp. vloge r. 12", "sklep o nadaljevanju izvršbe-86+predlog-82+pril.", "pozivni sklep U z drugop. r. št. 12", "Stroški IZV z r.št. 11 s pril. ", "dopis  25.3.2016 z r.št. 6", "Sklep z dne 18.3.2016,ftc.u.z. z dne 2.10.2014", "skl. str. izv. z dne 21.3.16 z vl. z dne 3.3.16 s strošk.", "sklep  24.3.2016 s predl., dopis 24.3.16", "sklep 23.3.2016 + dgp. vl. 21.1.16", "Dopis z dne 30. 03. 2016 s pril. ", "S59_S590452__23326 odgovor na ugovor s prilogami", "Sklep o izvršbi s pred. in pril., dopol. pred. s pril.", "skl. nis opp. + str.,pi,prnm si,ftk.predlog za nis ", "Sklep 21.3.2016+vloga 12.2.2016+14.3.2016", "sklep z dne 29. 3. 2016 s prilogami", "sklep z dne 29. 3. 2016 s prilogo", "sklep 23.3.2016, vl. 16.9.2015, rtr", "sklep NIS OPP s pril.", "Dopis upniku + VL. IZVR. Z DNE 19.2.16", "obv.pnm.  Skl.NIS.pl.  dopis,ftk.l.50,pnm.IS+IP,skl.l.41-,43-", "ftk.l.56 v vednost", "SKLEP 17.3.16 + FTK. SPISA", "Predlog-izvršba VL,SO sklep o izvršbi", "sklep- sprememba upnika  in vlogau pnika 15.12.2015", "prm.ID+RTRR", "Sklep N.I.S.-23 S00_0120vl. 19, prav. skl. o izv. s pr., r. 9,11", "Predložitveno poročilo, spis I 291/2014", "sklep o izvršbi in predlog s pril.", "dopis z dne 30.3.2016, vrač. skl. o del.ust.z dne 21.12.2015", "prav. skl. 21", "poziv izv ", "Odredba 29.03.2016 (VROČ.2), Sklep 29.03.2016 (VROČ.3) + Sodno pismo", "S00_0197", "dopis 18.3.16+skl. 15.3.16+vl. 26.2.16+ZZZS", "skl. 15.3.16+vl. 26.2.16+RTR+ZZZS", "Vloga 7.3.2016", "dopis 25.3.16  obv. o pravn. 25.3.16", "obvestilo o popl. dolga", "dopis z dne 29.3.16, drug.ug. z dne 1.3.16 s pril.", "Vloga", "sklep, obrazec ZST-1", "prav. skl. o izv. s pr., r. 5,19", "odredba z dne 29.3.2016", "sklep z dne 25.3.2016", "Sklep 21.3.16, sklep 18.11.15 s prav., 23.6.15 s prav.", "soi z dne 25.3.16, ip z dne 9.2.16 s pril.", "dopis z dne 29.3.2016, fot.vloge z dne 28.1.2016 - v vednost", "Sklep z dne 24. 3. 2016 +l. 13 (2)", "prm.skl. 9.2.16", "dopis prim.b., ftk. l. 13,22", "dopis 24.3.16+vl. 22.+25.2.+17.3.16", "PNM sklep z dne 4.2.2016 (1)", "dopis z dne 25.3.2016, vl. OPP 23.7.2015", "2xodr., orig. rač. ", "sklep z dne 25.3.2016 + pravn. sklep o izvršbi in predlog", "sklep o umiku z dne 25.3.2016", "pravn. sklep z dne 2.3.16, ftk. spisa", "skl. o um. 21.3.", "Dopis z dne 25.3.2016 +l. 23 (2)", "sklep o določ.", "dopis z dne 25.3.2016 in vloga z dne 10.2.2016", "sklep o novem upniku+dgp.r.št.6+pril.A1", "Sklep z dne 23. 3. 2016 (1)", "pi,si,", "sklep o ug. ", "Poziv z dne 29.3.2016, pr. sklep 7.5.2013", "sklep ustavitev premičnine   odredba seznam premoženja +sklep stroški 4.12.2015 s prilogo", "2x vloga 16.2.2016 (Ab) in vloga 16.2.2016"};

    public static String SENDER_BOX = "izvrsba@sed-court.si";

    public static final SEDLogger LOG = new SEDLogger(TestLoad.class);

    public File[] testFiles;
    SEDMailBoxWS mTestInstance = null;

    public TestLoad() {
        File f = new File(BLOB_FOLDER);
        testFiles = f.listFiles((File pathname) -> {
            return pathname.isFile() && pathname.getName().toLowerCase().endsWith(".pdf");
        });
    }

    public SEDMailBoxWS getService() {
        if (mTestInstance == null) {
            try {
                SedMailbox msb = new SedMailbox(new URL(MAILBOX_ADDRESS));
                mTestInstance = msb.getSEDMailBoxWSPort();
            } catch (MalformedURLException ex) {
                Logger.getLogger(TestLoad.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return mTestInstance;

    }

    public static void main(String... args) {

        System.out.println("test: " + Integer.parseInt("IICSR", 32));

        /*
        
        TestLoad tl = new TestLoad();
        try {
            tl.testLoad_a(50);
        } catch (SEDException_Exception ex) {
            Logger.getLogger(TestLoad.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }

    public long getDeltaTime(long l) {
        return LOG.getTime() - l;
    }

    public void testLoad_a(int imsgs) throws SEDException_Exception {

        String recBox = "izvrsba@sed-bk.si";
        String recName = "Banka Koper, Izvršilni oddelek";
        String service = "DeliveryWithReceipt";
        String action = "Delivery";
        //String service = "LegalDelivery_ZPP";
        //String action = "DeliveryNotification";

        SubmitMailRequest smr = new SubmitMailRequest();
        smr.setControl(createControl());
        smr.setData(new SubmitMailRequest.Data());

        Random rnd = new Random(Calendar.getInstance().getTimeInMillis());
        long startl = LOG.getTime();
        for (int i = 0; i < imsgs; i++) {
            long st = LOG.getTime();
            smr.getData().setOutMail(createOutMail(recBox, recName, SENDER_BOX, SENDER_NAMES[rnd.nextInt(SENDER_NAMES.length)],
                    service, action,
                    SUBJECTS[rnd.nextInt(SUBJECTS.length)], getRandomFiles(1, 5, rnd), String.format("VL %d/2016", i + 1)));
            // submit request
            SubmitMailResponse mr = getService().submitMail(smr);
            LOG.log(i + ". submited in: '" + getDeltaTime(st) + "' whole: '" + getDeltaTime(startl) + "'");

        }

    }

    public String serialize(Object o) throws JAXBException {

        StringWriter sw = new StringWriter();

        JAXBContext carContext = JAXBContext.newInstance(o.getClass());
        Marshaller carMarshaller = carContext.createMarshaller();
        carMarshaller.marshal(o, sw);

        return sw.toString();
    }

    public List<File> getRandomFiles(int imin, int iMax, Random rnd) {

        int i = imin == iMax || imin > iMax ? imin : rnd.nextInt(iMax - imin) + imin;
        i = i > 0 ? i : 1;
        List<File> lst = new ArrayList<>();
        while (i-- > 0) {
            lst.add(testFiles[rnd.nextInt(testFiles.length)]);
        }
        return lst;
    }

    private OutMail createOutMail(String rcBox, String rcName, String sndBox, String sndName,
            String service, String action, String contentDesc, List<File> fls, String oprst) {

        OutMail om = new OutMail();

        om.setSenderMessageId("SM_ID-" + UUID.randomUUID().toString());
        om.setConversationId(UUID.randomUUID().toString());
        om.setAction(action);
        om.setService(service);
        om.setReceiverName(rcName);
        om.setReceiverEBox(rcBox);
        om.setSenderName(sndName);
        om.setSenderEBox(sndBox);
        om.setSubject(oprst + " " + contentDesc);
        om.setOutProperties(new OutProperties());
        OutProperty opr = new OutProperty();
        opr.setName("oprst");
        opr.setValue(oprst);
        om.getOutProperties().getOutProperties().add(opr);

        om.setOutPayload(new OutPayload());
        int i = 0;
        for (File f : fls) {
            try {
                OutPart op = new OutPart();
                op.setFilename(f.getName());
                op.setDescription(i++ == 0 ? "Sklep" : "Priloga");
                op.setValue(Files.readAllBytes(f.toPath()));
                op.setMimeType(MimeValues.MIME_PDF.getMimeType());
                om.getOutPayload().getOutParts().add(op);
            } catch (IOException ex) {
                Logger.getLogger(TestLoad.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return om;

    }

    private Control createControl() {

        Control c = new Control();
        c.setApplicationId("ApplicationId");
        c.setUserId("UserId");
        return c;

    }
}
