package si.jrc.msh.interceptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.msh.ebms.outbox.mail.MSHOutMail;
import si.jrc.msh.utils.EBMSLogUtils;
import si.jrc.msh.utils.EbMSConstants;
import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author sluzba
 */
public class EBMSLogOutInterceptor extends AbstractLoggingInterceptor {

    private static final Logger LOG = LogUtils.getLogger(
            EBMSLogOutInterceptor.class);
    private static final String LOG_SETUP =
            EBMSLogOutInterceptor.class.getName() + ".log-setup";
    SEDLogger mlog = new SEDLogger(EBMSLogOutInterceptor.class);

    /**
     *
     * @param phase
     */
    public EBMSLogOutInterceptor(String phase) {
        super(phase);
        addBefore(StaxOutInterceptor.class.getName());
    }

    /**
     *
     */
    public EBMSLogOutInterceptor() {
        this(Phase.PRE_STREAM);
    }

    /**
     *
     * @param lim
     */
    public EBMSLogOutInterceptor(int lim) {
        this();
        limit = lim;
    }

    /**
     *
     * @param w
     */
    public EBMSLogOutInterceptor(PrintWriter w) {
        this();
        this.writer = w;
    }

    /**
     *
     * @param buffer
     * @return
     */
    protected String formatLoggingMessage(LoggingMessage buffer) {
        return buffer.toString();
    }

    /**
     *
     * @return
     */
    @Override
    protected Logger getLogger() {
        return LOG;

    }

    /**
     *
     * @param message
     * @throws Fault
     */
    @Override
    public void handleMessage(Message message)
            throws Fault {
        long l = mlog.logStart();

        final OutputStream os = message.getContent(OutputStream.class);
        final Writer iowriter = message.getContent(Writer.class);
        boolean isRequestor = MessageUtils.isRequestor(message);
        if (os == null && iowriter == null) {
            return;
        }
        File fStore = null;

        if (isRequestor) {

            MSHOutMail rq = message.getExchange().get(MSHOutMail.class);
            fStore = EBMSLogUtils.getOutboundFileName(true, rq.getId(), null);
        } else {
            // get base from input log file

            String base = (String) message.getExchange().get(
                    EbMSConstants.EBMS_CP_BASE_LOG_SOAP_MESSAGE_FILE);
            fStore = EBMSLogUtils.getOutboundFileName(false, null, base);

        }
        mlog.log(
                "Out " + (isRequestor ? "request" : "response") + " stored to:" +
                fStore.getName());
        message.getExchange().
                put(EbMSConstants.EBMS_CP_BASE_LOG_SOAP_MESSAGE_FILE,
                        EBMSLogUtils.getBaseFileName(fStore));
        message.getExchange().put(
                EbMSConstants.EBMS_CP_OUT_LOG_SOAP_MESSAGE_FILE, fStore);

        try {
            writer = new PrintWriter(fStore);
        } catch (FileNotFoundException ex) {
            String errmsg =
                    "Application error store outbound message to file: '" +
                    fStore.getAbsolutePath() + "'! ";
            mlog.logError(l, errmsg, ex);
        }

        Logger logger = LOG;
        boolean hasLogged = message.containsKey(LOG_SETUP);
        if (!hasLogged) {
            message.put(LOG_SETUP, Boolean.TRUE);
            if (os != null) {
                final CacheAndWriteOutputStream newOut =
                        new CacheAndWriteOutputStream(os);
                if (threshold > 0) {
                    newOut.setThreshold(threshold);
                }
                if (limit > 0) {
                    newOut.setCacheLimit(limit);
                }
                message.setContent(OutputStream.class, newOut);
                newOut.registerCallback(new LoggingCallback(logger, message, os));
            } else {
                message.setContent(Writer.class, new LogWriter(logger, message,
                        iowriter));
            }
        }
        mlog.logEnd(l);
    }

    private LoggingMessage setupBuffer(Message message) {
        String id = (String) message.getExchange().get(LoggingMessage.ID_KEY);
        if (id == null) {
            id = LoggingMessage.nextId();
            message.getExchange().put(LoggingMessage.ID_KEY, id);
        }
        final LoggingMessage buffer =
                new LoggingMessage(
                        "Outbound Message\n---------------------------",
                        id);

        Integer responseCode = (Integer) message.get(Message.RESPONSE_CODE);
        if (responseCode != null) {
            buffer.getResponseCode().append(responseCode);
        }

        String encoding = (String) message.get(Message.ENCODING);
        if (encoding != null) {
            buffer.getEncoding().append(encoding);
        }
        String httpMethod = (String) message.get(Message.HTTP_REQUEST_METHOD);
        if (httpMethod != null) {
            buffer.getHttpMethod().append(httpMethod);
        }
        String address = (String) message.get(Message.ENDPOINT_ADDRESS);
        if (address != null) {
            buffer.getAddress().append(address);
            String uri = (String) message.get(Message.REQUEST_URI);
            if (uri != null && !address.startsWith(uri)) {
                if (!address.endsWith("/") && !uri.startsWith("/")) {
                    buffer.getAddress().append("/");
                }
                buffer.getAddress().append(uri);
            }
        }
        String ct = (String) message.get(Message.CONTENT_TYPE);
        if (ct != null) {
            buffer.getContentType().append(ct);
        }
        Object headers = message.get(Message.PROTOCOL_HEADERS);
        if (headers != null) {
            buffer.getHeader().append(headers);
        }
        return buffer;
    }

    private class LogWriter extends FilterWriter {

        int count;
        final int lim;
        Logger logger; //NOPMD
        Message message;
        StringWriter out2;

        public LogWriter(Logger logger, Message message, Writer writer) {
            super(writer);
            this.logger = logger;
            this.message = message;
            if (!(writer instanceof StringWriter)) {
                out2 = new StringWriter();
            }
            lim = limit == -1 ? Integer.MAX_VALUE : limit;
        }

        public void close()
                throws IOException {
            LoggingMessage buffer = setupBuffer(message);
            if (count >= lim) {
                buffer.getMessage().append("(message truncated to ").append(lim).append(
                        " bytes)\n");
            }
            StringWriter w2 = out2;
            if (w2 == null) {
                w2 = (StringWriter) out;
            }
            String ct = (String) message.get(Message.CONTENT_TYPE);
            try {
                writePayload(buffer.getPayload(), w2, ct);
            } catch (Exception ex) {
                //ignore
            }
            log(logger, buffer.toString());
            message.setContent(Writer.class, out);
            super.close();
        }

        public void write(int c)
                throws IOException {
            super.write(c);
            if (out2 != null && count < lim) {
                out2.write(c);
            }
            count++;
        }

        public void write(char[] cbuf, int off, int len)
                throws IOException {
            super.write(cbuf, off, len);
            if (out2 != null && count < lim) {
                out2.write(cbuf, off, len);
            }
            count += len;
        }

        public void write(String str, int off, int len)
                throws IOException {
            super.write(str, off, len);
            if (out2 != null && count < lim) {
                out2.write(str, off, len);
            }
            count += len;
        }

    }

    class LoggingCallback implements CachedOutputStreamCallback {

        private final int lim;
        private final Logger logger; //NOPMD

        private final Message message;
        private final OutputStream origStream;

        public LoggingCallback(final Logger logger, final Message msg,
                final OutputStream os) {
            this.logger = logger;
            this.message = msg;
            this.origStream = os;
            this.lim = limit == -1 ? Integer.MAX_VALUE : limit;
        }

        public void onClose(CachedOutputStream cos) {
            LoggingMessage buffer = setupBuffer(message);
            String ct = (String) message.get(Message.CONTENT_TYPE);
            if (!isShowBinaryContent() && isBinaryContent(ct)) {
                buffer.getMessage().append(BINARY_CONTENT_MESSAGE).append('\n');
                log(logger, formatLoggingMessage(buffer));
                return;
            }
            if (cos.getTempFile() == null) {
                //buffer.append("Outbound Message:\n");
                if (cos.size() >= lim) {
                    buffer.getMessage().append("(message truncated to ").append(
                            lim).append(" bytes)\n");
                }
            } else {
                buffer.getMessage().append(
                        "Outbound Message (saved to tmp file):\n");
                buffer.getMessage().append("Filename: ").append(
                        cos.getTempFile().getAbsolutePath()).append("\n");
                if (cos.size() >= lim) {
                    buffer.getMessage().append("(message truncated to ").append(
                            lim).append(" bytes)\n");
                }
            }
            try {
                String encoding = (String) message.get(Message.ENCODING);
                writePayload(buffer.getPayload(), cos, encoding, ct);
            } catch (Exception ex) {
                //ignore
            }
            log(logger, formatLoggingMessage(buffer));
            try {
                //empty out the cache
                cos.lockOutputStream();
                cos.resetOut(null, false);
            } catch (Exception ex) {
                //ignore
            }
            message.setContent(OutputStream.class,
                    origStream);
        }

        public void onFlush(CachedOutputStream cos) {

        }

    }

}
