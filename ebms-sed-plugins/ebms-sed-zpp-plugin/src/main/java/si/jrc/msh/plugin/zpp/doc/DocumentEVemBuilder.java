/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.jrc.msh.plugin.zpp.doc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.msh.ebms.outbox.mail.MSHOutMail;
import org.msh.ebms.outbox.payload.MSHOutPart;
import si.crea._2004.eppr.message.AddressType;
import si.crea._2004.eppr.message.AddressesType;
import si.crea._2004.eppr.message.Message;
import si.crea._2004.eppr.message.PhysicalAddressType;
import si.crea.schemas._2004.document.AttachmentType;
import si.crea.schemas._2004.document.AttachmentsType;
import si.crea.schemas._2004.document.DataObjectFormatType;
import si.crea.schemas._2004.document.DataType;
import si.crea.schemas._2004.document.Document;
import si.crea.schemas._2004.document.SignaturesType;
import si.crea.schemas._2004.document.VisualisationType;
import si.crea.schemas._2004.document.VisualisationsType;
import si.crea.schemas._2004.document.ContentType;
import si.crea.schemas._2004.document.EmbeddedDataType;
import static si.jrc.msh.plugin.zpp.doc.DocumentBuilder.IDPFX_VIS;
import si.sed.commons.exception.SEDSecurityException;
import si.sed.commons.exception.StorageException;
import si.sed.commons.utils.Utils;

/**
 *
 * @author logos
 */
public class DocumentEVemBuilder extends DocumentBuilder {

    private static final String NM_DATA = "http://www.crea.si/Schemas/2004/Document/ObjectType/Data";
    private static final String NM_VIS = "http://www.crea.si/Schemas/2004/Document/ObjectType/Visualisation";

    @Override
    public void createMail(MSHOutMail dce, String alias, FileOutputStream fos) throws SEDSecurityException {
        long t = getTime();
        mlgLogger.info("DocumentBuilder.createMail: begin ");

        /*
         * - Message/To - PoBoxId - Message/From - PoBoxId - Message/Subject -
         * Message/MessageId - Message/SenderDocumentId - Message/DeliveryType
         * ki je vedno Legal-ZPP - Message/Content
         */
        //String strNotfMail = Settings.getInstance().getNotificationMail();
        // send document
        Document document = new Document();
        List<String[]> lstSignatureIDS = new ArrayList<String[]>();
        // --------------------- create  message ----------------------
        Message mt = new Message();
        // --------------------- ADDRESSES ----------------------
        AddressesType lstto = new AddressesType();
        // receiver address
        mlgLogger.info("DocumentBuilder.createMail: 1 ");
        AddressType ato = new AddressType();
        ato.setPoBoxId(dce.getReceiverEBox());
        ato.setPhysicalAddress(new PhysicalAddressType());
        ato.getPhysicalAddress().setName(dce.getReceiverName());
        lstto.getAddresses().add(ato);
        mt.setTo(lstto);
        // sender address
        AddressType aFrom = new AddressType();
        aFrom.setPoBoxId(dce.getSenderEBox());
        aFrom.setPhysicalAddress(new PhysicalAddressType());
        aFrom.getPhysicalAddress().setName(dce.getSenderName());
        /*
         * / Notification mail ?? if (strNotfMail != null &&
         * strNotfMail.length() > 0) { AddressType.NotificationEmails nfce = new
         * AddressType.NotificationEmails();
         * nfce.getNotificationEmail().add(strNotfMail);
         * aFrom.setNotificationEmails(nfce);
         }
         */
        // set data for invoice billing
        //aFrom.setURI(dce.getSodiSif() + ":" + dce.getAplikSif());
        mt.setFrom(aFrom);
        mt.setMessageId(dce.getId().toString());
        mt.setSenderDocumentId(dce.getId().toString());
        mt.setDeliveryType(DELIVERY_TYPE);
        mt.setDocumentType(DOCUMENT_TYPE);

        mt.setSubject(dce.getSubject());
        DataType dt = new DataType();
        dt.setId(Utils.getInstance().getGuidString());
        lstSignatureIDS.add(new String[]{dt.getId(), NM_DATA});
        DataObjectFormatType doft = new DataObjectFormatType();
        doft.setIdentifier(" "); // obvezen element
        doft.setMimeType(MIME_TXT); // obvezen element
        doft.setEncoding(ENC_TYPE_UTF8); // obvezen element
        ContentType ct = new ContentType();
        EmbeddedDataType emb = new EmbeddedDataType();
        emb.getContent().add(mt);
        ct.setEmbeddedData(emb);
        dt.setDataFormat(doft);
        dt.setContent(ct);
        dt.setDescription(" ");
        // --------------------- end create  message ----------------------
        // --------------------- end create  visualization  ----------------------
        VisualisationsType lstVst = new VisualisationsType();
        mlgLogger.info("DocumentBuilder.createMail: 2 ");
        List<MSHOutPart> lst = dce.getMSHOutPayload().getMSHOutParts();
        if (lst.size() > 0) {

            try {
                MSHOutPart d = lst.get(0);
                VisualisationType vst = new VisualisationType();
                vst.setDescription(d.getDescription());
                vst.setId(Utils.getInstance().getGuidString());
                lstSignatureIDS.add(new String[]{vst.getId(), NM_VIS});
                doft = new DataObjectFormatType();
                doft.setIdentifier(" ");
                doft.setMimeType(d.getMimeType() == null ? "application/pdf" : d.getMimeType());
                //doft.setMimeType(MIME_PDF);
                doft.setEncoding(ENC_TYPE_B64);
                ct = new ContentType();
                emb = new EmbeddedDataType();
                emb.getContent().add(Base64.getEncoder().encodeToString(msuStorageUtils.getByteArray(d.getFilepath())));
                ct.setEmbeddedData(emb);
                vst.setDataFormat(doft);
                vst.setContent(ct);
                vst.setDescription(d.getDescription() + " ");

                document.setData(dt);
                document.setVisualisations(lstVst);
                lstVst.getVisualisations().add(vst);
                String encId = dce.getMessageId();
                if (encId == null) {
                    encId = dce.getId().toString();
                }

                if (lst.size() > 1) {
                    AttachmentsType ats = new AttachmentsType();
                    for (int id = 1; id < lst.size(); id++) {
                        d = lst.get(id);
                        AttachmentType at = new AttachmentType();
                        at.setDescription(d.getDescription());
                        at.setId(Utils.getInstance().getGuidString());
                        at.setFileName("sod_" + encId + "_" + id + "." + getFilePrefixForMimeType(d.getMimeType()));

                        lstSignatureIDS.add(new String[]{at.getId(), NM_VIS});
                        DataObjectFormatType atft = new DataObjectFormatType();
                        atft.setIdentifier(Utils.getInstance().getGuidString());
                        atft.setMimeType(d.getMimeType() == null ? "application/pdf" : d.getMimeType());

                        //doft.setMimeType(MIME_PDF);
                        atft.setEncoding(ENC_TYPE_B64);
                        ContentType atct = new ContentType();
                        EmbeddedDataType atemb = new EmbeddedDataType();
                        emb.getContent().add(Base64.getEncoder().encodeToString(msuStorageUtils.getByteArray(d.getFilepath())));

                        atct.setEmbeddedData(atemb);

                        at.setDataFormat(atft);
                        at.setContent(atct);
                        at.setDescription(d.getDescription());
                        ats.getAttachments().add(at);
                        document.setAttachments(ats);
                    }
                }
            } catch (StorageException ex) {
                String strMsg = "DocumentCreaBuilder.createMail: error reading file'" + ex.getMessage() + "'.";
                mlgLogger.error(strMsg, ex);
                throw new SEDSecurityException(SEDSecurityException.SEDSecurityExceptionCode.CreateSignatureException, ex);
            }
        }

        mlgLogger.info("DocumentBuilder.createMail: 3 ");
        // --------------------- sign data  ----------------------
        document.setSignatures(new SignaturesType());
        // convert to w3c document
        org.w3c.dom.Document dw3c = convertEpDoc2W3cDoc(document, new Class[]{Document.class, Message.class
        });
        // sign document and return value
        
        singDocument(dw3c, lstSignatureIDS, alias,  fos);

        mlgLogger.info(
                "DocumentBuilder.DocumentCreaBuilder: - end (" + (getTime() - t) + "ms)");
    }
    static Properties mstrMimeTypes = null;

    static public synchronized String getFilePrefixForMimeType(String mimetype) {
        String strRes = null;

        if (mstrMimeTypes == null) {
            mstrMimeTypes = new Properties();
            try {
                mstrMimeTypes.load(DocumentEVemBuilder.class.getResourceAsStream("/mimetypes.properties"));
            } catch (IOException ex) {
                Logger.getLogger(DocumentEVemBuilder.class.getName()).error("DocumentCreaBuilder.getFilePrefixForMimeType: Error reading resource /mimetypes.properties" + ex);
            }
        }

        if (mimetype != null && mstrMimeTypes.containsKey(mimetype.trim().toLowerCase())) {
            strRes = mstrMimeTypes.getProperty(mimetype.trim().toLowerCase());
        }
        return strRes != null ? strRes : "bin";
    }

}
