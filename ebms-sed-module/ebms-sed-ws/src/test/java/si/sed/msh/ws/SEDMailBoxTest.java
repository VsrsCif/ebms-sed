/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package si.sed.msh.ws;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sed.ebms.GetInMailRequest;
import org.sed.ebms.GetInMailResponse;
import org.sed.ebms.InMailEventListRequest;
import org.sed.ebms.InMailEventListResponse;
import org.sed.ebms.InMailListRequest;
import org.sed.ebms.InMailListResponse;
import org.sed.ebms.ModifOutActionCode;
import org.sed.ebms.ModifyActionCode;
import org.sed.ebms.ModifyInMailRequest;
import org.sed.ebms.ModifyInMailResponse;
import org.sed.ebms.ModifyOutMailRequest;
import org.sed.ebms.ModifyOutMailResponse;
import org.sed.ebms.OutMailEventListRequest;
import org.sed.ebms.OutMailEventListResponse;
import org.sed.ebms.OutMailListRequest;
import org.sed.ebms.OutMailListResponse;
import org.sed.ebms.SEDExceptionCode;
import org.sed.ebms.SEDException_Exception;
import org.sed.ebms.SubmitMailRequest;
import org.sed.ebms.SubmitMailResponse;
import org.sed.ebms.control.Control;
import org.sed.ebms.inbox.event.InEvent;
import org.sed.ebms.inbox.mail.InMail;
import org.sed.ebms.inbox.payload.InPart;
import org.sed.ebms.inbox.payload.InPayload;
import org.sed.ebms.outbox.event.OutEvent;
import org.sed.ebms.outbox.mail.OutMail;
import org.sed.ebms.outbox.payload.OutPart;
import org.sed.ebms.outbox.payload.OutPayload;
import si.sed.commons.MimeValues;
import si.sed.commons.SEDInboxMailStatus;
import si.sed.commons.SEDOutboxMailStatus;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.exception.HashException;
import si.sed.commons.exception.SVEVReturnValue;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.HashUtils;
import si.sed.commons.utils.StorageUtils;
import si.sed.commons.utils.Utils;
import si.sed.msh.test.db.MockUserTransaction;

/**
 *
 * @author sluzba
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SEDMailBoxTest extends TestUtils {

  /**
     *
     */
  public static final Logger LOG = Logger.getLogger(SEDMailBoxTest.class);

  static SEDMailBox mTestInstance = new SEDMailBox();
  static EntityManagerFactory memfFactory = null;
  static EntityManagerFactory memfMSHFactory = null;

  /**
   *
   * @throws Exception
   */
  @BeforeClass
  public static void startClass() throws Exception {

    try {
      // ---------------------------------
      // set logger
      setLogger(SEDMailBoxTest.class.getSimpleName());

      // ---------------------------------
      // set system variables
      // create home dir in target
      Files.createDirectory(Paths.get(SED_HOME));
      Files.copy(SEDMailBoxTest.class.getResourceAsStream("/pmode-conf.xml"),
          Paths.get(SED_HOME + "/pmode-conf.xml"), StandardCopyOption.REPLACE_EXISTING);
      System.getProperties().put(SEDSystemProperties.SYS_PROP_HOME_DIR, SED_HOME);
      System.setProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "");
      System.setProperty(SEDSystemProperties.SYS_PROP_JNDI_JMS_PREFIX, "");

      // ---------------------------------
      // set jms environment
      Queue mshueue = setJMSEnvironment();
      // mTestInstance.JNDI_CONNECTION_FACTORY = JNDI_CONNECTION_FACTORY;
      // mTestInstance.JNDI_QUEUE_NAME = SEDValues.EBMS_QUEUE_JNDI;
      mTestInstance.mqMSHQueue = mshueue;
      // ---------------------------------
      // set derby database
      // SEDLookups msLookup = new SEDLookups();
      // mTestInstance.mdbLookups = msLookup;

      memfMSHFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
      memfFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
      // msLookup.memEManager = memfMSHFactory.createEntityManager();
      mTestInstance.memEManager = memfFactory.createEntityManager();
      // msLookup.mutUTransaction = new MockUserTransaction(msLookup.memEManager.getTransaction());
      mTestInstance.mutUTransaction =
          new MockUserTransaction(mTestInstance.memEManager.getTransaction());

      // set lookup
    } catch (NamingException | JMSException ex) {
      LOG.error("ERROR startClass", ex);
    }
  }

  /**
   *
   * @throws Exception
   */
  @AfterClass
  public static void tearDownClass() throws Exception {}

  HashUtils mpHU = new HashUtils();
  StorageUtils msuStorageUtils = new StorageUtils();

  private void assertModifyOutMail(OutMail om, SEDOutboxMailStatus startOMStatus,
      ModifOutActionCode oac, SEDOutboxMailStatus endOMStatus, SEDExceptionCode ecExpected)
      throws StorageException, HashException {

    storeUpdateOutMail(om, startOMStatus);

    ModifyOutMailRequest momr = new ModifyOutMailRequest();
    momr.setControl(createControl());
    momr.setData(new ModifyOutMailRequest.Data());
    momr.getData().setMailId(om.getId());
    momr.getData().setSenderEBox(om.getSenderEBox());
    momr.getData().setAction(oac);

    try {
      ModifyOutMailResponse mer = mTestInstance.modifyOutMail(momr);
      assertNotNull("Response", mer);
      assertNotNull("Response/RControl", mer.getRControl());
      assertNotNull("Response/RControl/@returnValue", mer.getRControl().getReturnValue());
      assertEquals("Response/RControl/@returnValue", mer.getRControl().getReturnValue().intValue(),
          SVEVReturnValue.OK.getValue());
      assertNotNull("Response/RData", mer.getRData());
      assertNotNull("Response/RData/OutEvent", mer.getRData().getOutEvent());
      assertEquals("Mail id", om.getId(), mer.getRData().getOutEvent().getMailId());
      assertEquals("Sender box", om.getSenderEBox(), mer.getRData().getOutEvent().getSenderEBox());

      OutMail omtest = getOutMail(om.getId());
      assertNotNull("OutMail not exists in db", omtest);
      assertEquals("Wrong modified end status", endOMStatus.getValue(), omtest.getStatus());

    } catch (SEDException_Exception ex) {
      assertNotNull("Error " + ex.getMessage() + " is not exptected to be thrown", ecExpected);
      assertNotNull("SEDException_Exception/FaultInfo", ex.getFaultInfo());
      assertNotNull("SEDException_Exception/FaultInfo/ErrorCode", ex.getFaultInfo().getErrorCode());
      assertEquals("Erro code", ecExpected, ex.getFaultInfo().getErrorCode());
    }

  }

  private void assertThrowErrorOnSubmit(SubmitMailRequest smr, String assertMessage,
      SEDExceptionCode ecExpected) {

    SEDException_Exception ex = null;
    try {
      mTestInstance.submitMail(smr);
    } catch (SEDException_Exception exRes) {
      ex = exRes;
    }
    assertNotNull(assertMessage, ex);
    assertEquals(ecExpected, ex.getFaultInfo().getErrorCode());

  }

  private Control createControl() {

    Control c = new Control();
    c.setApplicationId("ApplicationId");
    c.setUserId("UserId");
    return c;

  }

  private InMail createInMail() {

    InMail im = new InMail();

    im.setSenderMessageId("SM_ID-" + UUID.randomUUID().toString());
    im.setAction("action");
    im.setService("LegalDelivery_ZPP");
    im.setConversationId(UUID.randomUUID().toString());
    im.setReceiverName("Mr. Receiver Name");
    im.setReceiverEBox("receiver.name@sed-box.si");
    im.setSenderName("Mr. Sender Name");
    im.setSenderEBox("sender.name@sed-box.si");

    String testContent = "Test content";
    im.setInPayload(new InPayload());
    InPart ip = new InPart();
    ip.setFilename("Test.txt");
    ip.setDescription("test attachment");
    ip.setValue(testContent.getBytes());
    ip.setMimeType(MimeValues.MIME_TEXI.getMimeType());
    im.getInPayload().getInParts().add(ip);

    return im;

  }

  private OutMail createOutMail() {

    OutMail om = new OutMail();

    om.setSenderMessageId("SM_ID-" + UUID.randomUUID().toString());
    om.setAction("action");
    om.setService("LegalDelivery_ZPP");
    om.setConversationId(UUID.randomUUID().toString());
    om.setReceiverName("Mr. Receiver Name");
    om.setReceiverEBox("receiver.name@sed-box.si");
    om.setSenderName("Mr. Sender Name");
    om.setSenderEBox("sender.name@sed-box.si");

    String testContent = "Test content";
    om.setOutPayload(new OutPayload());
    OutPart op = new OutPart();
    op.setFilename("Test.txt");
    op.setDescription("test attachment");
    op.setValue(testContent.getBytes());
    op.setMimeType(MimeValues.MIME_TEXI.getMimeType());
    om.getOutPayload().getOutParts().add(op);

    return om;

  }

  private OutMail getOutMail(BigInteger iId) {
    EntityManager me = null;
    OutMail om = null;
    try {
      me = memfFactory.createEntityManager();
      om = me.find(OutMail.class, iId);
    } finally {
      if (me != null) {
        me.close();
      }
    }
    return om;
  }

  /**
     *
     */
  @Before
  public void setUp() {

  }

  private void storeInMail(InMail mail) throws StorageException, HashException {

    EntityManager me = null;
    try {
      // --------------------
      // serialize payload

      if (mail.getInPayload() != null && !mail.getInPayload().getInParts().isEmpty()) {
        for (InPart p : mail.getInPayload().getInParts()) {
          File fout = null;

          if (p.getValue() != null) {
            fout = msuStorageUtils.storeInFile(p.getMimeType(), p.getValue());
            // purge binary data
            // p.setValue(null);
          } else if (!Utils.isEmptyString(p.getFilepath())) {
            File fIn = new File(p.getFilepath());
            if (fIn.exists()) {
              fout = msuStorageUtils.storeOutFile(p.getMimeType(), fIn);
            }
          }
          // set MD5 and relative path;
          if (fout != null) {
            String strMD5 = mpHU.getMD5Hash(fout);
            String relPath = StorageUtils.getRelativePath(fout);
            p.setFilepath(relPath);
            p.setMd5(strMD5);

            if (Utils.isEmptyString(p.getFilename())) {
              p.setFilename(fout.getName());
            }
            if (Utils.isEmptyString(p.getName())) {
              p.setName(p.getFilename().substring(p.getFilename().lastIndexOf(".")));
            }
          }
        }
      }

      me = memfFactory.createEntityManager();
      mail.setReceivedDate(Calendar.getInstance().getTime());
      mail.setStatusDate(mail.getReceivedDate());
      mail.setStatus(SEDInboxMailStatus.RECEIVED.getValue());
      me.getTransaction().begin();
      me.persist(mail);

      InEvent mevent = new InEvent();

      mevent.setMailId(mail.getId());
      mevent.setReceiverEBox(mail.getReceiverEBox());
      mevent.setStatus(mail.getStatus());
      mevent.setDate(mail.getStatusDate());
      mevent.setUserId("userID");
      mevent.setApplicationId("applicationId");
      me.persist(mevent);
      me.getTransaction().commit();
    } finally {
      if (me != null) {
        me.close();
      }
    }

  }

  private void storeUpdateOutMail(OutMail mail, SEDOutboxMailStatus st) throws StorageException,
      HashException {
    if (mail.getId() != null) {
      EntityManager me = null;
      try {

        me = memfFactory.createEntityManager();
        mail.setReceivedDate(Calendar.getInstance().getTime());
        mail.setStatusDate(mail.getReceivedDate());
        mail.setStatus(st.getValue());
        me.getTransaction().begin();
        me.merge(mail);

        OutEvent mevent = new OutEvent();

        mevent.setMailId(mail.getId());
        mevent.setSenderEBox(mail.getSenderEBox());
        mevent.setStatus(mail.getStatus());
        mevent.setDate(mail.getStatusDate());
        mevent.setUserId("userID");
        mevent.setApplicationId("applicationId");
        me.persist(mevent);
        me.getTransaction().commit();
      } finally {
        if (me != null) {
          me.close();
        }
      }

    } else {

      EntityManager me = null;
      try {
        // --------------------
        // serialize payload

        if (mail.getOutPayload() != null && !mail.getOutPayload().getOutParts().isEmpty()) {
          for (OutPart p : mail.getOutPayload().getOutParts()) {
            File fout = null;

            if (p.getValue() != null) {
              fout = msuStorageUtils.storeOutFile(p.getMimeType(), p.getValue());
              // purge binary data
              // p.setValue(null);
            } else if (!Utils.isEmptyString(p.getFilepath())) {
              File fIn = new File(p.getFilepath());
              if (fIn.exists()) {
                fout = msuStorageUtils.storeOutFile(p.getMimeType(), fIn);
              }
            }
            // set MD5 and relative path;
            if (fout != null) {
              String strMD5 = mpHU.getMD5Hash(fout);
              String relPath = StorageUtils.getRelativePath(fout);
              p.setFilepath(relPath);
              p.setMd5(strMD5);

              if (Utils.isEmptyString(p.getFilename())) {
                p.setFilename(fout.getName());
              }
              if (Utils.isEmptyString(p.getName())) {
                p.setName(p.getFilename().substring(p.getFilename().lastIndexOf(".")));
              }
            }
          }
        }

        me = memfFactory.createEntityManager();
        mail.setReceivedDate(Calendar.getInstance().getTime());
        mail.setStatusDate(mail.getReceivedDate());
        mail.setStatus(st.getValue());
        me.getTransaction().begin();
        me.persist(mail);

        OutEvent mevent = new OutEvent();

        mevent.setMailId(mail.getId());
        mevent.setSenderEBox(mail.getSenderEBox());
        mevent.setStatus(mail.getStatus());
        mevent.setDate(mail.getStatusDate());
        mevent.setUserId("userID");
        mevent.setApplicationId("applicationId");
        me.persist(mevent);
        me.getTransaction().commit();
      } finally {
        if (me != null) {
          me.close();
        }
      }
    }

  }

  /**
     *
     */
  @After
  public void tearDown() {}

  /**
   * Method test SubmitMail
   *
   * @throws Exception
   */
  @Test
  public void test_A_SubmitMail() throws Exception {
    LOG.info("test_A_SubmitMail");
    // create request
    SubmitMailRequest smr = new SubmitMailRequest();
    smr.setControl(createControl());
    smr.setData(new SubmitMailRequest.Data());
    smr.getData().setOutMail(createOutMail());
    // submit request
    SubmitMailResponse mr = mTestInstance.submitMail(smr);
    assertNotNull("Response", mr);
    assertNotNull("Response/RControl", mr.getRControl());
    assertNotNull("Response/RControl/@returnValue", mr.getRControl().getReturnValue());
    assertEquals("Response/RControl/@returnValue", mr.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
    assertNotNull("Response/RData", mr.getRData());
    assertNotNull("Response/RData/@mailId", mr.getRData().getMailId());
    assertNotNull("Response/RData/@submitDate", mr.getRData().getSubmittedDate());
    assertNotNull("SenderMessageId", mr.getRData().getSenderMessageId());
    assertEquals("SenderMessageId", mr.getRData().getSenderMessageId(), smr.getData().getOutMail()
        .getSenderMessageId());

  }

  /**
   * Method test ValidationOfRequest of SubmitMail
   *
   * @throws Exception
   */
  @Test
  public void test_B_SubmitMail_ValidationOfRequest() throws Exception {
    LOG.info("test_B_SubmitMail_ValidationOfRequest");
    // create sumbmit OK mail
    SubmitMailRequest smr = new SubmitMailRequest();
    smr.setControl(createControl());
    smr.setData(new SubmitMailRequest.Data());
    smr.getData().setOutMail(createOutMail());

    // check Control
    Control c = smr.getControl();
    smr.setControl(null);
    assertThrowErrorOnSubmit(smr, "Missing Control", SEDExceptionCode.MISSING_DATA);
    smr.setControl(c);
    // check Control/@applicationId
    String aid = c.getApplicationId();
    c.setApplicationId(null);
    assertThrowErrorOnSubmit(smr, "Missing Control/@applicationId", SEDExceptionCode.MISSING_DATA);
    c.setApplicationId(aid);
    // check Control/@userId
    String userid = c.getUserId();
    c.setUserId(null);
    assertThrowErrorOnSubmit(smr, "Missing Control/@userId", SEDExceptionCode.MISSING_DATA);
    c.setUserId(userid);
    // check Data
    SubmitMailRequest.Data dt = smr.getData();
    smr.setData(null);
    assertThrowErrorOnSubmit(smr, "Missing Data", SEDExceptionCode.MISSING_DATA);
    smr.setData(dt);
    // check Data/OutMail
    OutMail om = smr.getData().getOutMail();
    smr.getData().setOutMail(null);
    assertThrowErrorOnSubmit(smr, "Missing Data/OutMail", SEDExceptionCode.MISSING_DATA);
    smr.getData().setOutMail(om);

    // check Data/OutMail/@action
    String value = om.getAction();
    om.setAction(null);
    assertThrowErrorOnSubmit(smr, "Missing Data/OutMail/@action", SEDExceptionCode.MISSING_DATA);
    om.setAction(value);
    // check Data/OutMail/@service
    value = om.getService();
    om.setService(null);
    assertThrowErrorOnSubmit(smr, "Missing Data/OutMail/@service", SEDExceptionCode.MISSING_DATA);
    om.setService(value);
    // check Data/OutMail/@ConversationId
    value = om.getConversationId();
    om.setConversationId(null);
    assertThrowErrorOnSubmit(smr, "Missing Data/OutMail/@ConversationId",
        SEDExceptionCode.MISSING_DATA);
    om.setConversationId(value);

    // check Data/OutMail/@senderMessageId
    value = om.getSenderMessageId();
    om.setSenderMessageId(null);
    assertThrowErrorOnSubmit(smr, "Missing Data/OutMail/@senderMessageId",
        SEDExceptionCode.MISSING_DATA);
    om.setSenderMessageId(value);
    // check Data/OutMail/@SenderName
    value = om.getSenderName();
    om.setSenderName(null);
    assertThrowErrorOnSubmit(smr, "Missing Data/OutMail/@SenderName", SEDExceptionCode.MISSING_DATA);
    om.setSenderName(value);
    // check Data/OutMail/@SenderEBox
    value = om.getSenderEBox();
    om.setSenderEBox(null);
    assertThrowErrorOnSubmit(smr, "Missing Data/OutMail/@SenderEBox", SEDExceptionCode.MISSING_DATA);
    om.setSenderEBox(value);

    // check Data/OutMail/@ReceiverName
    value = om.getReceiverName();
    om.setReceiverName(null);
    assertThrowErrorOnSubmit(smr, "Missing Data/OutMail/@ReceiverName",
        SEDExceptionCode.MISSING_DATA);
    om.setReceiverName(value);
    // check Data/OutMail/@SenderEBox
    value = om.getReceiverEBox();
    om.setReceiverEBox(null);
    assertThrowErrorOnSubmit(smr, "Missing Data/OutMail/@SenderEBox", SEDExceptionCode.MISSING_DATA);
    om.setReceiverEBox(value);

    SubmitMailResponse mr = mTestInstance.submitMail(smr);
    assertEquals("Response/RControl/@returnValue", mr.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
  }

  /**
   * Method test duplicate detection of second mail SubmitMail
   *
   * @throws Exception
   */
  @Test
  public void test_C_SubmitMail_ExistsMail() throws Exception {
    LOG.info("test_C_SubmitMail_ExistsMail");
    // create request
    SubmitMailRequest smr = new SubmitMailRequest();
    smr.setControl(createControl());
    smr.setData(new SubmitMailRequest.Data());
    smr.getData().setOutMail(createOutMail());

    // submit for first time,
    SubmitMailResponse mr = mTestInstance.submitMail(smr);
    assertEquals("Response/RControl/@returnValue", mr.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());

    SubmitMailResponse secmr = mTestInstance.submitMail(smr);
    assertEquals("Second submission Response/RControl/@returnValue", secmr.getRControl()
        .getReturnValue().intValue(), SVEVReturnValue.WARNING.getValue());
    assertEquals("Second submission Response/RData/@submitDate", mr.getRData().getSubmittedDate(),
        secmr.getRData().getSubmittedDate());
    assertEquals("Second submission Response/RData/@mailId", mr.getRData().getMailId(), secmr
        .getRData().getMailId());
    assertEquals("Second submission Response/RData/@senderMessageId", mr.getRData()
        .getSenderMessageId(), secmr.getRData().getSenderMessageId());

  }

  /**
   * Test of getOutMailList method, of class SEDMailBox. Method tests search parameters
   *
   * @throws java.lang.Exception
   */
  @Test
  public void test_D_GetOutMailList() throws Exception {
    LOG.info("test_D_GetOutMailList");
    OutMailListRequest omr = new OutMailListRequest();
    omr.setControl(createControl());
    omr.setData(new OutMailListRequest.Data());

    OutMailListResponse mlr = mTestInstance.getOutMailList(omr);
    assertNotNull("Response", mlr);
    assertNotNull("Response/RControl", mlr.getRControl());
    assertNotNull("Response/RControl/@returnValue", mlr.getRControl().getReturnValue());
    assertEquals("Response/RControl/@returnValue", mlr.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
    assertNotNull("Response/RControl/@resultSize", mlr.getRControl().getResultSize());
    assertNotNull("Response/RControl/@responseSize", mlr.getRControl().getResponseSize());
    assertNotNull("Response/RControl/@StartIndex", mlr.getRControl().getStartIndex());
    assertNotNull("Response/RData", mlr.getRData());
    assertEquals("Response/RControl/@resultSize -> getOutMails().size()", mlr.getRData()
        .getOutMails().size(), mlr.getRControl().getResultSize().intValue());
    assertEquals("Response/RControl/@resultSize -> getOutMails().size()", mlr.getRData()
        .getOutMails().size(), mlr.getRControl().getResponseSize().intValue());
    int iResCNt = mlr.getRData().getOutMails().size();
    // -----------------------------------------
    // / add five new objects
    int TEST_CNT = 5;

    SubmitMailRequest smr = new SubmitMailRequest();
    smr.setControl(createControl());
    smr.setData(new SubmitMailRequest.Data());
    // wait for second for testing time interval search
    long l = Calendar.getInstance().getTimeInMillis();
    while (l + 1000 > Calendar.getInstance().getTimeInMillis()) {
      // wait for second seconds.. // test
    }
    Date dt = Calendar.getInstance().getTime();

    for (int i = 0; i < TEST_CNT; i++) {
      smr.getData().setOutMail(createOutMail()); // create new mail
      SubmitMailResponse mr = mTestInstance.submitMail(smr);
      assertEquals("Response/RControl/@returnValue", mr.getRControl().getReturnValue().intValue(),
          SVEVReturnValue.OK.getValue());
    }
    // check new list
    mlr = mTestInstance.getOutMailList(omr);
    assertEquals("Response/RControl/@returnValue", mlr.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
    assertEquals("Response/RControl/@resultSize -> getOutMails().size()", mlr.getRData()
        .getOutMails().size(), mlr.getRControl().getResultSize().intValue());
    assertEquals("Response/RControl/@resultSize -> increased", TEST_CNT + iResCNt, mlr.getRData()
        .getOutMails().size());

    // wait for second for testing time interval search
    Date dto = Calendar.getInstance().getTime();
    l = Calendar.getInstance().getTimeInMillis();
    while (l + 1000 > Calendar.getInstance().getTimeInMillis()) {
      // wait for second seconds.. // test
    }

    // -----------------------------------------
    // add new mail with unique service, action, sender box, receiver box
    String valTest = "TEST_SERVICE"; // must be declared in pmod conf
    OutMail om = createOutMail();

    om.setService(valTest);
    om.setAction(UUID.randomUUID().toString());
    om.setSenderEBox(UUID.randomUUID().toString() + "@sed-box.si");
    om.setReceiverEBox(UUID.randomUUID().toString() + "@sed-box.si");
    smr.getData().setOutMail(om); // create new mail
    // add new mail
    SubmitMailResponse mr = mTestInstance.submitMail(smr);
    assertEquals("Response/RControl/@returnValue", mr.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());

    // test search for service
    omr.getData().setService(om.getService());
    mlr = mTestInstance.getOutMailList(omr);
    omr.getData().setService(null);
    assertEquals("Service Response/RControl/@returnValue", mlr.getRControl().getReturnValue()
        .intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test service parameter", 1, mlr.getRData().getOutMails().size());

    assertEquals("Test service response id", om.getId(), mlr.getRData().getOutMails().get(0)
        .getId());

    // test search for action
    omr.getData().setAction((om.getAction()));
    mlr = mTestInstance.getOutMailList(omr);
    omr.getData().setAction(null);
    assertEquals("Action Response/RControl/@returnValue", mlr.getRControl().getReturnValue()
        .intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test Action parameter", 1, mlr.getRData().getOutMails().size());
    assertEquals("Test  Action response id", om.getId(), mlr.getRData().getOutMails().get(0)
        .getId());

    // test search for ConversationId
    omr.getData().setConversationId(om.getConversationId());
    mlr = mTestInstance.getOutMailList(omr);
    omr.getData().setConversationId(null);
    assertEquals("ConversationId Response/RControl/@returnValue", mlr.getRControl()
        .getReturnValue().intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test ConversationId parameter", 1, mlr.getRData().getOutMails().size());
    assertEquals("Test ConversationId response id", om.getId(), mlr.getRData().getOutMails().get(0)
        .getId());

    // test search for SenderEBox
    omr.getData().setSenderEBox((om.getSenderEBox()));
    mlr = mTestInstance.getOutMailList(omr);
    omr.getData().setSenderEBox(null);
    assertEquals("SenderEBox Response/RControl/@returnValue", mlr.getRControl().getReturnValue()
        .intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test SenderEBox parameter", 1, mlr.getRData().getOutMails().size());
    assertEquals("Test SenderEBox response id", om.getId(), mlr.getRData().getOutMails().get(0)
        .getId());

    // test search for ReceiverEBox
    omr.getData().setReceiverEBox((om.getReceiverEBox()));
    mlr = mTestInstance.getOutMailList(omr);
    omr.getData().setReceiverEBox(null);
    assertEquals("ReceiverEBox Response/RControl/@returnValue", mlr.getRControl().getReturnValue()
        .intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test ReceiverEBox parameter", 1, mlr.getRData().getOutMails().size());
    assertEquals("Test ReceiverEBox response id", om.getId(), mlr.getRData().getOutMails().get(0)
        .getId());

    // test search for SubmittedDateFrom
    omr.getData().setSubmittedDateFrom(dt);
    mlr = mTestInstance.getOutMailList(omr);
    assertEquals("SubmittedDateFrom Response/RControl/@returnValue", mlr.getRControl()
        .getReturnValue().intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test SubmittedDateFrom parameter", TEST_CNT + 1, mlr.getRData().getOutMails()
        .size());

    // test search for SubmittedDateFrom
    omr.getData().setSubmittedDateFrom(dt);
    omr.getData().setSubmittedDateTo(dto);
    mlr = mTestInstance.getOutMailList(omr);
    assertEquals("SubmittedDateFrom Response/RControl/@returnValue", mlr.getRControl()
        .getReturnValue().intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test SubmittedDateFrom parameter", TEST_CNT, mlr.getRData().getOutMails().size());

    // ------------------------
    // TEST pagination
    omr.getData().setSubmittedDateFrom(null);
    omr.getData().setSubmittedDateTo(null);
    omr.getControl().setStartIndex(BigInteger.valueOf(iResCNt));
    omr.getControl().setResponseSize(BigInteger.valueOf(TEST_CNT));
    mlr = mTestInstance.getOutMailList(omr);
    assertEquals("SubmittedDateFrom Response/RControl/@returnValue", mlr.getRControl()
        .getReturnValue().intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("service RControl/@ResultSize", TEST_CNT + iResCNt + 1, mlr.getRControl()
        .getResultSize().intValue());
    assertEquals("service RControl/@getResponseSize", TEST_CNT, mlr.getRControl().getResponseSize()
        .intValue());
  }

  /**
   * Test of getOutMailEventList method, of class SEDMailBox.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void test_E_GetOutMailEventList() throws Exception {
    LOG.info("test_E_GetOutMailEventList");

    // submit mail
    OutMail om = createOutMail();
    SubmitMailRequest smr = new SubmitMailRequest();
    smr.setControl(createControl());
    smr.setData(new SubmitMailRequest.Data());
    smr.getData().setOutMail(om); // create new mail
    SubmitMailResponse mr = mTestInstance.submitMail(smr);
    assertEquals("Response/RControl/@returnValue", mr.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());

    // retrieve mail events by id
    OutMailEventListRequest omer = new OutMailEventListRequest();
    omer.setControl(createControl());
    omer.setData(new OutMailEventListRequest.Data());
    omer.getData().setMailId(om.getId());
    omer.getData().setSenderEBox(om.getSenderEBox());
    OutMailEventListResponse mer = mTestInstance.getOutMailEventList(omer);

    assertNotNull("Response", mer);
    assertNotNull("Response/RControl", mer.getRControl());
    assertNotNull("Response/RControl/@returnValue", mer.getRControl().getReturnValue());
    assertEquals("Response/RControl/@returnValue", mer.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
    assertNotNull("Response/RControl/@resultSize", mer.getRControl().getResultSize());
    assertNotNull("Response/RControl/@responseSize", mer.getRControl().getResponseSize());
    assertNotNull("Response/RControl/@StartIndex", mer.getRControl().getStartIndex());
    assertNotNull("Response/RData", mer.getRData());
    assertEquals("Response/RControl/@resultSize -> OutMailEvent().size()", mer.getRData()
        .getOutEvents().size(), mer.getRControl().getResultSize().intValue());
    assertEquals("Response/RControl/@resultSize -> OutMailEvent().size()", mer.getRData()
        .getOutEvents().size(), mer.getRControl().getResponseSize().intValue());
    assertTrue("Response/RControl/@resultSize -> OutMailEvent().size()", mer.getRData()
        .getOutEvents().size() > 0);
    int iResult = mer.getRData().getOutEvents().size();

    // retrieve mail events by id
    omer.getData().setMailId(null);
    omer.getData().setSenderEBox(om.getSenderEBox());
    omer.getData().setSenderMessageId(om.getSenderMessageId());

    mer = mTestInstance.getOutMailEventList(omer);
    assertEquals("Response/RControl/@returnValue", mer.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
    assertTrue("Response/RControl/@resultSize -> OutMailEvent().size()", mer.getRData()
        .getOutEvents().size() == iResult);
  }

  /**
   * Test of getInMailList method, of class SEDMailBox.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void test_F_GetInMailList() throws Exception {
    LOG.info("test_F_GetInMailList");
    // prepare
    InMail im = createInMail();
    storeInMail(im);
    //

    InMailListRequest imr = new InMailListRequest();
    imr.setControl(createControl());
    imr.setData(new InMailListRequest.Data());

    InMailListResponse mlr = mTestInstance.getInMailList(imr);
    assertNotNull("Response", mlr);
    assertNotNull("Response/RControl", mlr.getRControl());
    assertNotNull("Response/RControl/@returnValue", mlr.getRControl().getReturnValue());
    assertEquals("Response/RControl/@returnValue", mlr.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
    assertNotNull("Response/RControl/@resultSize", mlr.getRControl().getResultSize());
    assertNotNull("Response/RControl/@responseSize", mlr.getRControl().getResponseSize());
    assertNotNull("Response/RControl/@StartIndex", mlr.getRControl().getStartIndex());
    assertNotNull("Response/RData", mlr.getRData());
    assertEquals("Response/RControl/@resultSize -> getOutMails().size()", mlr.getRData()
        .getInMails().size(), mlr.getRControl().getResultSize().intValue());
    assertEquals("Response/RControl/@resultSize -> getOutMails().size()", mlr.getRData()
        .getInMails().size(), mlr.getRControl().getResponseSize().intValue());
    assertTrue("Response/RControl/@resultSize -> OutMailEvent().size()", mlr.getRData()
        .getInMails().size() > 0);
    int iResCNt = mlr.getRData().getInMails().size();

    // -----------------------------------------
    // / add five new objects
    int TEST_CNT = 5;

    // wait for second for testing time interval search
    long l = Calendar.getInstance().getTimeInMillis();
    while (l + 1000 > Calendar.getInstance().getTimeInMillis()) {
      // wait for second seconds.. // test
    }
    Date dt = Calendar.getInstance().getTime();

    for (int i = 0; i < TEST_CNT; i++) {
      storeInMail(createInMail());
    }
    // check new list
    mlr = mTestInstance.getInMailList(imr);
    assertEquals("Response/RControl/@returnValue", mlr.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
    assertEquals("Response/RControl/@resultSize -> getOutMails().size()", mlr.getRData()
        .getInMails().size(), mlr.getRControl().getResultSize().intValue());
    assertEquals("Response/RControl/@resultSize -> increased", TEST_CNT + iResCNt, mlr.getRData()
        .getInMails().size());

    // wait for second for testing time interval search
    Date dto = Calendar.getInstance().getTime();
    l = Calendar.getInstance().getTimeInMillis();
    while (l + 1000 > Calendar.getInstance().getTimeInMillis()) {
      // wait for second seconds.. // test
    }

    // -----------------------------------------
    // add new mail with unique service, action, sender box, receiver box
    String valTest = "TEST_SERVICE"; // must be declared in pmod conf
    InMail im2 = createInMail();

    im2.setService(valTest);
    im2.setAction(UUID.randomUUID().toString());
    im2.setSenderEBox(UUID.randomUUID().toString() + "@sed-box.si");
    im2.setReceiverEBox(UUID.randomUUID().toString() + "@sed-box.si");

    storeInMail(im2);

    // test search for service
    imr.getData().setService(im2.getService());
    mlr = mTestInstance.getInMailList(imr);
    imr.getData().setService(null);
    assertEquals("Service Response/RControl/@returnValue", mlr.getRControl().getReturnValue()
        .intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test service parameter", 1, mlr.getRData().getInMails().size());
    assertEquals("Test service response id", im2.getId(), mlr.getRData().getInMails().get(0)
        .getId());

    // test search for action
    imr.getData().setAction((im2.getAction()));
    mlr = mTestInstance.getInMailList(imr);
    imr.getData().setAction(null);
    assertEquals("Action Response/RControl/@returnValue", mlr.getRControl().getReturnValue()
        .intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test Action parameter", 1, mlr.getRData().getInMails().size());
    assertEquals("Test  Action response id", im2.getId(), mlr.getRData().getInMails().get(0)
        .getId());

    // test search for ConversationId
    imr.getData().setConversationId(im2.getConversationId());
    mlr = mTestInstance.getInMailList(imr);
    imr.getData().setConversationId(null);
    assertEquals("ConversationId Response/RControl/@returnValue", mlr.getRControl()
        .getReturnValue().intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test ConversationId parameter", 1, mlr.getRData().getInMails().size());
    assertEquals("Test ConversationId response id", im2.getId(), mlr.getRData().getInMails().get(0)
        .getId());

    // test search for SenderEBox
    imr.getData().setSenderEBox(im2.getSenderEBox());
    mlr = mTestInstance.getInMailList(imr);
    imr.getData().setSenderEBox(null);
    assertEquals("SenderEBox Response/RControl/@returnValue", mlr.getRControl().getReturnValue()
        .intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test SenderEBox parameter", 1, mlr.getRData().getInMails().size());
    assertEquals("Test SenderEBox response id", im2.getId(), mlr.getRData().getInMails().get(0)
        .getId());

    // test search for ReceiverEBox
    imr.getData().setReceiverEBox((im2.getReceiverEBox()));
    mlr = mTestInstance.getInMailList(imr);
    imr.getData().setReceiverEBox(null);
    assertEquals("ReceiverEBox Response/RControl/@returnValue", mlr.getRControl().getReturnValue()
        .intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test ReceiverEBox parameter", 1, mlr.getRData().getInMails().size());
    assertEquals("Test ReceiverEBox response id", im2.getId(), mlr.getRData().getInMails().get(0)
        .getId());

    // test search for SubmittedDateFrom
    imr.getData().setReceivedDateFrom(dt);
    mlr = mTestInstance.getInMailList(imr);
    assertEquals("SubmittedDateFrom Response/RControl/@returnValue", mlr.getRControl()
        .getReturnValue().intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test SubmittedDateFrom parameter", TEST_CNT + 1, mlr.getRData().getInMails()
        .size());

    // test search for SubmittedDateFrom
    imr.getData().setReceivedDateFrom(dt);
    imr.getData().setReceivedDateTo(dto);
    mlr = mTestInstance.getInMailList(imr);
    assertEquals("SubmittedDateFrom Response/RControl/@returnValue", mlr.getRControl()
        .getReturnValue().intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("Test SubmittedDateFrom parameter", TEST_CNT, mlr.getRData().getInMails().size());

    // ------------------------
    // TEST pagination
    imr.getData().setReceivedDateFrom(null);
    imr.getData().setReceivedDateTo(null);
    imr.getControl().setStartIndex(BigInteger.valueOf(iResCNt));
    imr.getControl().setResponseSize(BigInteger.valueOf(TEST_CNT));
    mlr = mTestInstance.getInMailList(imr);
    assertEquals("SubmittedDateFrom Response/RControl/@returnValue", mlr.getRControl()
        .getReturnValue().intValue(), SVEVReturnValue.OK.getValue());
    assertEquals("service RControl/@ResultSize", TEST_CNT + iResCNt + 1, mlr.getRControl()
        .getResultSize().intValue());
    assertEquals("service RControl/@getResponseSize", TEST_CNT, mlr.getRControl().getResponseSize()
        .intValue());
  }

  /**
   * Test of getInMailEventList method, of class SEDMailBox.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void test_G_GetInMailEventList() throws Exception {
    LOG.info("test_G_GetInMailEventList");

    // store mail
    InMail im = createInMail();
    storeInMail(im);

    // retrieve mail events by id
    InMailEventListRequest imer = new InMailEventListRequest();
    imer.setControl(createControl());
    imer.setData(new InMailEventListRequest.Data());
    imer.getData().setMailId(im.getId());
    imer.getData().setReceiverEBox(im.getReceiverEBox());
    InMailEventListResponse mer = mTestInstance.getInMailEventList(imer);

    assertNotNull("Response", mer);
    assertNotNull("Response/RControl", mer.getRControl());
    assertNotNull("Response/RControl/@returnValue", mer.getRControl().getReturnValue());
    assertEquals("Response/RControl/@returnValue", mer.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
    assertNotNull("Response/RControl/@resultSize", mer.getRControl().getResultSize());
    assertNotNull("Response/RControl/@responseSize", mer.getRControl().getResponseSize());
    assertNotNull("Response/RControl/@StartIndex", mer.getRControl().getStartIndex());
    assertNotNull("Response/RData", mer.getRData());
    assertEquals("Response/RControl/@resultSize -> OutMailEvent().size()", mer.getRData()
        .getInEvents().size(), mer.getRControl().getResultSize().intValue());
    assertEquals("Response/RControl/@resultSize -> OutMailEvent().size()", mer.getRData()
        .getInEvents().size(), mer.getRControl().getResponseSize().intValue());
    assertTrue("Response/RControl/@resultSize -> OutMailEvent().size()", mer.getRData()
        .getInEvents().size() > 0);

  }

  /**
   * Test of getInMail method, of class SEDMailBox.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void test_H_GetInMail() throws Exception {
    LOG.info("test_H_GetInMail");

    // store mail
    InMail im = createInMail();
    storeInMail(im);

    // retrieve in mail
    GetInMailRequest gimr = new GetInMailRequest();
    gimr.setControl(createControl());
    gimr.setData(new GetInMailRequest.Data());
    gimr.getData().setMailId(im.getId());
    gimr.getData().setReceiverEBox(im.getReceiverEBox());
    GetInMailResponse mer = mTestInstance.getInMail(gimr);

    assertNotNull("Response", mer);
    assertNotNull("Response/RControl", mer.getRControl());
    assertNotNull("Response/RControl/@returnValue", mer.getRControl().getReturnValue());
    assertEquals("Response/RControl/@returnValue", mer.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
    assertNotNull("Response/RData", mer.getRData());
    assertNotNull("Response/RData/InMail", mer.getRData().getInMail());
    assertEquals("Response/RData/InMail", im.getId(), mer.getRData().getInMail().getId());
    assertArrayEquals("Response/RData/getPyloadPart", im.getInPayload().getInParts().get(0)
        .getValue(), mer.getRData().getInMail().getInPayload().getInParts().get(0).getValue());

  }

  /**
   * Test of modifyInMail method, of class SEDMailBox.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void test_I_ModifyInMail() throws Exception {
    LOG.info("test_I_ModifyInMail");

    // store mail
    InMail im = createInMail();
    storeInMail(im);

    ModifyInMailRequest mimr = new ModifyInMailRequest();
    mimr.setControl(createControl());
    mimr.setData(new ModifyInMailRequest.Data());
    mimr.getData().setMailId(im.getId());
    mimr.getData().setReceiverEBox(im.getReceiverEBox());
    mimr.getData().setAction(ModifyActionCode.ACCEPT);
    ModifyInMailResponse mer = mTestInstance.modifyInMail(mimr);
    assertNotNull("Response", mer);
    assertNotNull("Response/RControl", mer.getRControl());
    assertNotNull("Response/RControl/@returnValue", mer.getRControl().getReturnValue());
    assertEquals("Response/RControl/@returnValue", mer.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
    assertNotNull("Response/RData", mer.getRData());
    assertNotNull("Response/RData/InEvent", mer.getRData().getInEvent());
    assertEquals("Mail id", im.getId(), mer.getRData().getInEvent().getMailId());
    assertEquals("Receiver box", im.getReceiverEBox(), mer.getRData().getInEvent()
        .getReceiverEBox());

    // get inmail and check status
    GetInMailRequest gimr = new GetInMailRequest();
    gimr.setControl(createControl());
    gimr.setData(new GetInMailRequest.Data());
    gimr.getData().setMailId(im.getId());
    gimr.getData().setReceiverEBox(im.getReceiverEBox());
    GetInMailResponse mrs = mTestInstance.getInMail(gimr);
    assertEquals("Response/RControl/@returnValue", mrs.getRControl().getReturnValue().intValue(),
        SVEVReturnValue.OK.getValue());
    assertEquals("Status", SEDInboxMailStatus.DELIVERED.getValue(), mrs.getRData().getInMail()
        .getStatus());
    assertEquals("Date", mer.getRData().getInEvent().getDate(), mrs.getRData().getInMail()
        .getStatusDate());

  }

  /**
   * Test of modifyInMail method, of class SEDMailBox.
   *
   * @throws java.lang.Exception
   */
  @Test
  public void test_J_ModifyOutMail() throws Exception {
    LOG.info("test_J_ModifyOutMail");

    // assert abort
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.SUBMITTED, ModifOutActionCode.ABORT,
        SEDOutboxMailStatus.CANCELED, null);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.ERROR, ModifOutActionCode.ABORT,
        SEDOutboxMailStatus.CANCELED, null);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.EBMSERROR, ModifOutActionCode.ABORT,
        SEDOutboxMailStatus.CANCELED, null);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.SCHEDULE, ModifOutActionCode.ABORT,
        SEDOutboxMailStatus.CANCELED, null);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.CANCELED, ModifOutActionCode.ABORT,
        SEDOutboxMailStatus.CANCELED, null);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.CANCELING, ModifOutActionCode.ABORT,
        SEDOutboxMailStatus.CANCELING, null);

    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.SENT, ModifOutActionCode.ABORT, null,
        SEDExceptionCode.INVALID_DATA);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.SENDING, ModifOutActionCode.ABORT,
        null, SEDExceptionCode.INVALID_DATA);

    // assert Delete
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.SUBMITTED, ModifOutActionCode.DELETE,
        SEDOutboxMailStatus.DELETED, null);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.ERROR, ModifOutActionCode.DELETE,
        SEDOutboxMailStatus.DELETED, null);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.EBMSERROR, ModifOutActionCode.DELETE,
        SEDOutboxMailStatus.DELETED, null);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.SCHEDULE, ModifOutActionCode.DELETE,
        SEDOutboxMailStatus.DELETED, null);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.CANCELED, ModifOutActionCode.DELETE,
        SEDOutboxMailStatus.DELETED, null);

    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.CANCELING, ModifOutActionCode.DELETE,
        null, SEDExceptionCode.INVALID_DATA);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.SENT, ModifOutActionCode.DELETE, null,
        SEDExceptionCode.INVALID_DATA);
    assertModifyOutMail(createOutMail(), SEDOutboxMailStatus.SENDING, ModifOutActionCode.DELETE,
        null, SEDExceptionCode.INVALID_DATA);

    // assert resend
    // todo
  }

}
