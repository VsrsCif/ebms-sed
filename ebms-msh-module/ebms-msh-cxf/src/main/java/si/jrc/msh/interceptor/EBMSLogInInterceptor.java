/*
* Copyright 2015, Supreme Court Republic of Slovenia 
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
package si.jrc.msh.interceptor;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedWriter;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import si.jrc.msh.utils.EBMSLogUtils;
import si.jrc.msh.utils.EbMSConstants;
import si.sed.commons.utils.SEDLogger;

public class EBMSLogInInterceptor extends AbstractLoggingInterceptor {

    private static final Logger LOG = LogUtils.getLogger(EBMSLogInInterceptor.class);
    protected final SEDLogger mlog = new SEDLogger(EBMSLogInInterceptor.class);

    public EBMSLogInInterceptor() {
        super(Phase.RECEIVE);
    }

    protected String formatLoggingMessage(LoggingMessage loggingMessage) {

        return loggingMessage.toString();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    public String getURI(Message message) {
        String uri = (String) message.get(Message.REQUEST_URL);
        if (uri == null) {
            String address = (String) message.get(Message.ENDPOINT_ADDRESS);
            uri = (String) message.get(Message.REQUEST_URI);
            if (uri != null && uri.startsWith("/")) {
                if (address != null && !address.startsWith(uri)) {
                    if (address.endsWith("/") && address.length() > 1) {
                        address = address.substring(0, address.length());
                    }
                    uri = address + uri;
                }
            } else {
                uri = address;
            }
        }
        return uri;
    }

    public void handleMessage(Message msg) throws Fault {
        long l = mlog.logStart();
        Logger logger = LOG;

        boolean isRequestor = MessageUtils.isRequestor(msg);
        String base = (String) msg.getExchange().get(EbMSConstants.EBMS_CP_BASE_LOG_SOAP_MESSAGE_FILE);
        File f = EBMSLogUtils.getInboundFileName(isRequestor, base);
        base = EBMSLogUtils.getBaseFileName(f);
        msg.getExchange().put(EbMSConstants.EBMS_CP_BASE_LOG_SOAP_MESSAGE_FILE, base);
        msg.getExchange().put(EbMSConstants.EBMS_CP_IN_LOG_SOAP_MESSAGE_FILE, f);

        mlog.log("In from: '" + getURI(msg) + "' " + (isRequestor ? "response" : "request") + " stored to:" + f.getName());
        try {
            writer = new PrintWriter(f);
        } catch (FileNotFoundException ex) {
            String errmsg = "Application error store outbound message to file: '" + f.getAbsolutePath() + "'! ";
            mlog.logError(l, errmsg, ex);
        }

        try {
            writer = new PrintWriter(f);
        } catch (FileNotFoundException ex) {
            String errmsg = "Application error store outbound message to file: '" + f.getAbsolutePath() + "'! ";
            mlog.logError(l, errmsg, ex);
        }

        if (writer != null || logger.isLoggable(Level.INFO)) {
            logging(logger, msg);
        }
    }

    protected void logInputStream(Message message, InputStream is, LoggingMessage buffer,
            String encoding, String ct) {
        CachedOutputStream bos = new CachedOutputStream();
        if (threshold > 0) {
            bos.setThreshold(threshold);
        }
        try {
            // use the appropriate input stream and restore it later
            InputStream bis = is instanceof DelegatingInputStream
                    ? ((DelegatingInputStream) is).getInputStream() : is;

            //only copy up to the limit since that's all we need to log
            //we can stream the rest
            IOUtils.copyAtLeast(bis, bos, limit == -1 ? Integer.MAX_VALUE : limit);
            bos.flush();
            bis = new SequenceInputStream(bos.getInputStream(), bis);

            // restore the delegating input stream or the input stream
            if (is instanceof DelegatingInputStream) {
                ((DelegatingInputStream) is).setInputStream(bis);
            } else {
                message.setContent(InputStream.class, bis);
            }

            if (bos.getTempFile() != null) {
                //large thing on disk...
                buffer.getMessage().append("\nMessage (saved to tmp file):\n");
                buffer.getMessage().append("Filename: ").append(bos.getTempFile().getAbsolutePath()).append("\n");
            }
            if (bos.size() > limit && limit != -1) {
                buffer.getMessage().append("(message truncated to ").append(limit).append(" bytes)\n");
            }
            writePayload(buffer.getPayload(), bos, encoding, ct);

            bos.close();
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    protected void logReader(Message message, Reader reader, LoggingMessage buffer) {
        try {
            CachedWriter writer = new CachedWriter();
            IOUtils.copyAndCloseInput(reader, writer);
            message.setContent(Reader.class, writer.getReader());

            if (writer.getTempFile() != null) {
                //large thing on disk...
                buffer.getMessage().append("\nMessage (saved to tmp file):\n");
                buffer.getMessage().append("Filename: ").append(writer.getTempFile().getAbsolutePath()).append("\n");
            }
            if (writer.size() > limit && limit != -1) {
                buffer.getMessage().append("(message truncated to ").append(limit).append(" bytes)\n");
            }
            writer.writeCacheTo(buffer.getPayload(), limit);
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    protected void logging(Logger logger, Message message) throws Fault {
        if (message.containsKey(LoggingMessage.ID_KEY)) {
            return;
        }
        String id = (String) message.getExchange().get(LoggingMessage.ID_KEY);
        if (id == null) {
            id = LoggingMessage.nextId();
            message.getExchange().put(LoggingMessage.ID_KEY, id);
        }
        message.put(LoggingMessage.ID_KEY, id);
        final LoggingMessage buffer
                = new LoggingMessage("Inbound Message\n----------------------------", id);

        if (!Boolean.TRUE.equals(message.get(Message.DECOUPLED_CHANNEL_MESSAGE))) {
            // avoid logging the default responseCode 200 for the decoupled responses
            Integer responseCode = (Integer) message.get(Message.RESPONSE_CODE);
            if (responseCode != null) {
                buffer.getResponseCode().append(responseCode);
            }
        }

        String encoding = (String) message.get(Message.ENCODING);

        if (encoding != null) {
            buffer.getEncoding().append(encoding);
        }
        String httpMethod = (String) message.get(Message.HTTP_REQUEST_METHOD);
        if (httpMethod != null) {
            buffer.getHttpMethod().append(httpMethod);
        }
        String ct = (String) message.get(Message.CONTENT_TYPE);
        if (ct != null) {
            buffer.getContentType().append(ct);
        }
        Object headers = message.get(Message.PROTOCOL_HEADERS);

        if (headers != null) {
            buffer.getHeader().append(headers);
        }
        String uri = (String) message.get(Message.REQUEST_URL);
        if (uri == null) {
            String address = (String) message.get(Message.ENDPOINT_ADDRESS);
            uri = (String) message.get(Message.REQUEST_URI);
            if (uri != null && uri.startsWith("/")) {
                if (address != null && !address.startsWith(uri)) {
                    if (address.endsWith("/") && address.length() > 1) {
                        address = address.substring(0, address.length());
                    }
                    uri = address + uri;
                }
            } else {
                uri = address;
            }
        }
        if (uri != null) {
            buffer.getAddress().append(uri);
            String query = (String) message.get(Message.QUERY_STRING);
            if (query != null) {
                buffer.getAddress().append("?").append(query);
            }
        }

        if (!isShowBinaryContent() && isBinaryContent(ct)) {
            buffer.getMessage().append(BINARY_CONTENT_MESSAGE).append('\n');
            log(logger, buffer.toString());
            return;
        }
        if (!isShowMultipartContent() && isMultipartContent(ct)) {
            buffer.getMessage().append(MULTIPART_CONTENT_MESSAGE).append('\n');
            log(logger, buffer.toString());
            return;
        }

        InputStream is = message.getContent(InputStream.class);
        if (is != null) {
            logInputStream(message, is, buffer, encoding, ct);
        } else {
            Reader reader = message.getContent(Reader.class);
            if (reader != null) {
                logReader(message, reader, buffer);
            }
        }
        log(logger, formatLoggingMessage(buffer));
    }
}
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
