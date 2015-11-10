drop index IDX_SED_INBOX_CONV_ID;
drop index IDX_SED_INBOX_DATE_DELIVERED;
drop index IDX_SED_INBOX_DATE_RECEIVED;
drop index IDX_SED_INBOX_DATE_SENT;
drop index IDX_SED_INBOX_MSG_ID;
drop index IDX_SED_INBOX_STATUS;
drop index IDX_SED_OUTBOX_CONV_ID;
drop index IDX_SED_OUTBOX_DATE_DELIVERED;
drop index IDX_SED_OUTBOX_DATE_RECEIVED;
drop index IDX_SED_OUTBOX_DATE_SENT;
drop index IDX_SED_OUTBOX_DATE_STATUS;
drop index IDX_SED_OUTBOX_DATE_SUBMITED;
drop index IDX_SED_OUTBOX_EVENT_MAIL_ID;
drop index IDX_SED_OUTBOX_MSG_ID;
drop index IDX_SED_OUTBOX_SENDER_MSG_ID;
drop index IDX_SED_OUTBOX_STATUS;


drop table SED_INBOX ;
drop table SED_INBOX_EVENT;
drop table SED_INBOX_PAYLOAD;
drop table SED_INBOX_PROPERTY;
drop table SED_OUTBOX;
drop table SED_OUTBOX_EVENT;
drop table SED_OUTBOX_PAYLOAD;
drop table SED_OUTBOX_PROPERTY;

drop sequence SEQ_SED_INBOX;
drop sequence SEQ_SED_INBOX_EVENT;
drop sequence SEQ_SED_INBOX_PAYLOAD;
drop sequence SEQ_SED_INBOX_PROP;
drop sequence SEQ_SED_OUTBOX;
drop sequence SEQ_SED_OUTBOX_EVENT;
drop sequence SEQ_SED_OUTBOX_PAYLOAD;
drop sequence SEQ_SED_OUTBOX_PROP;


CREATE TABLE sed_inbox (
    id NUMBER(*,0) NOT NULL ENABLE,
    msg_id VARCHAR2(64 BYTE),
    sender_msg_id VARCHAR2(64 BYTE),
    service VARCHAR2(64 BYTE) NOT NULL ENABLE,
    action VARCHAR2(64 BYTE) NOT NULL ENABLE,
    conv_id VARCHAR2(64 BYTE) NOT NULL ENABLE,
    subject VARCHAR2(512 BYTE),
    receiver_ebox VARCHAR2(64 BYTE) NOT NULL ENABLE,
    receiver_name VARCHAR2(128 BYTE) NOT NULL ENABLE,
    sender_ebox VARCHAR2(64 BYTE) NOT NULL ENABLE,
    sender_name VARCHAR2(128 BYTE) NOT NULL ENABLE,
    status VARCHAR2(32 BYTE) NOT NULL ENABLE,
    date_status DATE NOT NULL ENABLE,
    date_submited DATE NOT NULL ENABLE,
    date_sent DATE,
    date_received DATE,
    date_delivered DATE,
    CONSTRAINT sed_inbox_pk PRIMARY KEY (id)
);


--
-- Name: sed_inbox_event; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_inbox_event (
    id NUMBER(*,0) NOT NULL ENABLE,
    mail_id NUMBER(*,0),
    status VARCHAR2(32 BYTE) NOT NULL ENABLE,
    "date" DATE NOT NULL ENABLE,
    description VARCHAR2(512 BYTE),
    user_id VARCHAR2(64 BYTE),
    application_id VARCHAR2(128 BYTE),
    CONSTRAINT sed_inbox_event_pk PRIMARY KEY (id)
);


--
-- Name: sed_inbox_payload; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_inbox_payload (
    id NUMBER(*,0) NOT NULL ENABLE,
    ebms_id VARCHAR2(128 BYTE),
    name VARCHAR2(128 BYTE),
    description VARCHAR2(512 BYTE),
    type VARCHAR2(64 BYTE),
    filename VARCHAR2(128 BYTE),
    filepath VARCHAR2(1028),
    mime_type VARCHAR2(128 BYTE),
    encoding VARCHAR2(128 BYTE),
    md5 VARCHAR2(32 BYTE) NOT NULL ENABLE,
    mail_id NUMBER(*,0) NOT NULL ENABLE,
    CONSTRAINT sed_inbox_payload_pk PRIMARY KEY (id)
);


--
-- Name: sed_inbox_property; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_inbox_property (
    id NUMBER(*,0) NOT NULL ENABLE,
    name VARCHAR2(128 BYTE),
    value VARCHAR2(512 BYTE),
    mail_id NUMBER(*,0) NOT NULL ENABLE,
    CONSTRAINT sed_inbox_property_pk PRIMARY KEY (id)
);


--
-- Name: sed_outbox; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox (
    id NUMBER(*,0) NOT NULL ENABLE,
    msg_id VARCHAR2(64 BYTE),
    sender_msg_id VARCHAR2(64 BYTE),
    service VARCHAR2(64 BYTE) NOT NULL ENABLE,
    action VARCHAR2(64 BYTE) NOT NULL ENABLE,
    conv_id VARCHAR2(64 BYTE),
    subject VARCHAR2(512 BYTE),
    receiver_ebox VARCHAR2(64 BYTE) NOT NULL ENABLE,
    receiver_name VARCHAR2(128 BYTE) NOT NULL ENABLE,
    sender_ebox VARCHAR2(64 BYTE) NOT NULL ENABLE,
    sender_name VARCHAR2(128 BYTE) NOT NULL ENABLE,
    status VARCHAR2(32 BYTE) NOT NULL ENABLE,
    date_status DATE NOT NULL ENABLE,
    date_submited DATE NOT NULL ENABLE,
    date_sent DATE,
    date_received DATE,
    date_delivered DATE,
    CONSTRAINT sed_outbox_pk PRIMARY KEY (id)
);


--
-- Name: sed_outbox_event; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox_event (
    id NUMBER(*,0) NOT NULL ENABLE,
    mail_id NUMBER(*,0) NOT NULL ENABLE,
    sender_msg_id VARCHAR2(64 BYTE),
    status VARCHAR2(32 BYTE) NOT NULL ENABLE,
    "date" DATE NOT NULL ENABLE,
    description VARCHAR2(512 BYTE),
    user_id VARCHAR2(64 BYTE),
    application_id VARCHAR2(128 BYTE),
    CONSTRAINT sed_outbox_event_pk PRIMARY KEY (id)
);


--
-- Name: sed_outbox_payload; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox_payload (
    id NUMBER(*,0) NOT NULL ENABLE,
    ebms_id VARCHAR2(128 BYTE),
    name VARCHAR2(128 BYTE),
    description VARCHAR2(512 BYTE),
    type VARCHAR2(64 BYTE),
    filename VARCHAR2(128 BYTE),
    filepath VARCHAR2(1028),
    mime_type VARCHAR2(128 BYTE),
    encoding VARCHAR2(128 BYTE),
    md5 VARCHAR2(32 BYTE) NOT NULL ENABLE,
    mail_id NUMBER(*,0) NOT NULL ENABLE,
    CONSTRAINT sed_outbox_payload_pk PRIMARY KEY (id)
);


--
-- Name: sed_outbox_property; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox_property (
    id NUMBER(*,0) NOT NULL ENABLE,
    name VARCHAR2(128 BYTE),
    value VARCHAR2(512 BYTE),
    mail_id NUMBER(*,0) NOT NULL ENABLE,
    CONSTRAINT sed_outbox_property_pk PRIMARY KEY (id)
);


--
-- Name: seq_sed_inbox; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_inbox INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;


--
-- Name: seq_sed_inbox_event; Type: SEQUENCE; Schema: public; Owner: -
--
CREATE SEQUENCE seq_sed_inbox_event INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;

--
-- Name: seq_sed_inbox_payload; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_inbox_payload INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;

--
-- Name: seq_sed_inbox_prop; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_inbox_prop INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;

--
-- Name: seq_sed_outbox; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_outbox INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;


--
-- Name: seq_sed_outbox_event; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_outbox_event INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;

--
-- Name: seq_sed_outbox_payload; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_outbox_payload INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;


--
-- Name: seq_sed_outbox_prop; Type: SEQUENCE; Schema: public; Owner: -
--
CREATE SEQUENCE seq_sed_outbox_prop INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;


--
-- Name: idx_conv_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_conv_id ON sed_inbox (conv_id);


--
-- Name: idx_date_delivered; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_date_delivered ON sed_inbox (date_delivered);

--
-- Name: idx_date_received; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_date_received ON sed_inbox (date_received);



--
-- Name: idx_date_sent; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_date_sent ON sed_inbox (date_sent);



--
-- Name: idx_date_status; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_status ON sed_inbox (status);



--
-- Name: idx_date_submit; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_date_submited ON sed_inbox (date_submited);


--
-- Name: idx_in_event_mail_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_event_mail_id ON sed_inbox_event (mail_id);


--
-- Name: idx_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_msg_id ON sed_inbox (msg_id);

--
-- Name: idx_out_conv_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_conv_id ON sed_outbox (conv_id);

--
-- Name: idx_out_date_delivered; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_date_delivered ON sed_outbox (date_delivered);


--
-- Name: idx_out_date_received; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_date_received ON sed_outbox (date_received);

--
-- Name: idx_out_date_sent; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_date_sent ON sed_outbox (date_sent);

--
-- Name: idx_out_date_status; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_date_status ON sed_outbox (date_status);

--
-- Name: idx_out_date_submit; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_date_submited ON sed_outbox (date_submited);

--
-- Name: idx_out_even_send_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX sed_outbox_event_sender_msg_id ON sed_outbox_event (sender_msg_id);


--
-- Name: idx_out_event_mail_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_event_mail_id ON sed_outbox_event (mail_id);

--
-- Name: idx_out_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_msg_id ON sed_outbox (msg_id);

--
-- Name: idx_out_sender_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_sender_msg_id ON sed_outbox (sender_msg_id);


--
-- Name: idx_out_status; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_status ON sed_outbox (status);




drop index IDX_SED_INBOX_CONV_ID;
drop index IDX_SED_INBOX_DATE_DELIVERED;
drop index IDX_SED_INBOX_DATE_RECEIVED;
drop index IDX_SED_INBOX_DATE_SENT;
drop index IDX_SED_INBOX_MSG_ID;
drop index IDX_SED_INBOX_STATUS;
drop index IDX_SED_OUTBOX_CONV_ID;
drop index IDX_SED_OUTBOX_DATE_DELIVERED;
drop index IDX_SED_OUTBOX_DATE_RECEIVED;
drop index IDX_SED_OUTBOX_DATE_SENT;
drop index IDX_SED_OUTBOX_DATE_STATUS;
drop index IDX_SED_OUTBOX_DATE_SUBMITED;
drop index IDX_SED_OUTBOX_EVENT_MAIL_ID;
drop index IDX_SED_OUTBOX_MSG_ID;
drop index IDX_SED_OUTBOX_SENDER_MSG_ID;
drop index IDX_SED_OUTBOX_STATUS;


drop table SED_INBOX ;
drop table SED_INBOX_EVENT;
drop table SED_INBOX_PAYLOAD;
drop table SED_INBOX_PROPERTY;
drop table SED_OUTBOX;
drop table SED_OUTBOX_EVENT;
drop table SED_OUTBOX_PAYLOAD;
drop table SED_OUTBOX_PROPERTY;

drop sequence SEQ_SED_INBOX;
drop sequence SEQ_SED_INBOX_EVENT;
drop sequence SEQ_SED_INBOX_PAYLOAD;
drop sequence SEQ_SED_INBOX_PROP;
drop sequence SEQ_SED_OUTBOX;
drop sequence SEQ_SED_OUTBOX_EVENT;
drop sequence SEQ_SED_OUTBOX_PAYLOAD;
drop sequence SEQ_SED_OUTBOX_PROP;


CREATE TABLE sed_inbox (
    id NUMBER(*,0) NOT NULL ENABLE,
    msg_id VARCHAR2(64 BYTE),
    sender_msg_id VARCHAR2(64 BYTE),
    service VARCHAR2(64 BYTE) NOT NULL ENABLE,
    action VARCHAR2(64 BYTE) NOT NULL ENABLE,
    conv_id VARCHAR2(64 BYTE) NOT NULL ENABLE,
    subject VARCHAR2(512 BYTE),
    receiver_ebox VARCHAR2(64 BYTE) NOT NULL ENABLE,
    receiver_name VARCHAR2(128 BYTE) NOT NULL ENABLE,
    sender_ebox VARCHAR2(64 BYTE) NOT NULL ENABLE,
    sender_name VARCHAR2(128 BYTE) NOT NULL ENABLE,
    status VARCHAR2(32 BYTE) NOT NULL ENABLE,
    date_status DATE NOT NULL ENABLE,
    date_submited DATE NOT NULL ENABLE,
    date_sent DATE,
    date_received DATE,
    date_delivered DATE,
    CONSTRAINT sed_inbox_pk PRIMARY KEY (id)
);


--
-- Name: sed_inbox_event; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_inbox_event (
    id NUMBER(*,0) NOT NULL ENABLE,
    mail_id NUMBER(*,0),
    status VARCHAR2(32 BYTE) NOT NULL ENABLE,
    "date" DATE NOT NULL ENABLE,
    description VARCHAR2(512 BYTE),
    user_id VARCHAR2(64 BYTE),
    application_id VARCHAR2(128 BYTE),
    CONSTRAINT sed_inbox_event_pk PRIMARY KEY (id)
);


--
-- Name: sed_inbox_payload; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_inbox_payload (
    id NUMBER(*,0) NOT NULL ENABLE,
    ebms_id VARCHAR2(128 BYTE),
    name VARCHAR2(128 BYTE),
    description VARCHAR2(512 BYTE),
    type VARCHAR2(64 BYTE),
    filename VARCHAR2(128 BYTE),
    filepath VARCHAR2(1028),
    mime_type VARCHAR2(128 BYTE),
    encoding VARCHAR2(128 BYTE),
    md5 VARCHAR2(32 BYTE) NOT NULL ENABLE,
    mail_id NUMBER(*,0) NOT NULL ENABLE,
    CONSTRAINT sed_inbox_payload_pk PRIMARY KEY (id)
);


--
-- Name: sed_inbox_property; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_inbox_property (
    id NUMBER(*,0) NOT NULL ENABLE,
    name VARCHAR2(128 BYTE),
    value VARCHAR2(512 BYTE),
    mail_id NUMBER(*,0) NOT NULL ENABLE,
    CONSTRAINT sed_inbox_property_pk PRIMARY KEY (id)
);


--
-- Name: sed_outbox; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox (
    id NUMBER(*,0) NOT NULL ENABLE,
    msg_id VARCHAR2(64 BYTE),
    sender_msg_id VARCHAR2(64 BYTE),
    service VARCHAR2(64 BYTE) NOT NULL ENABLE,
    action VARCHAR2(64 BYTE) NOT NULL ENABLE,
    conv_id VARCHAR2(64 BYTE),
    subject VARCHAR2(512 BYTE),
    receiver_ebox VARCHAR2(64 BYTE) NOT NULL ENABLE,
    receiver_name VARCHAR2(128 BYTE) NOT NULL ENABLE,
    sender_ebox VARCHAR2(64 BYTE) NOT NULL ENABLE,
    sender_name VARCHAR2(128 BYTE) NOT NULL ENABLE,
    status VARCHAR2(32 BYTE) NOT NULL ENABLE,
    date_status DATE NOT NULL ENABLE,
    date_submited DATE NOT NULL ENABLE,
    date_sent DATE,
    date_received DATE,
    date_delivered DATE,
    CONSTRAINT sed_outbox_pk PRIMARY KEY (id)
);


--
-- Name: sed_outbox_event; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox_event (
    id NUMBER(*,0) NOT NULL ENABLE,
    mail_id NUMBER(*,0) NOT NULL ENABLE,
    sender_msg_id VARCHAR2(64 BYTE),
    status VARCHAR2(32 BYTE) NOT NULL ENABLE,
    "date" DATE NOT NULL ENABLE,
    description VARCHAR2(512 BYTE),
    user_id VARCHAR2(64 BYTE),
    application_id VARCHAR2(128 BYTE),
    CONSTRAINT sed_outbox_event_pk PRIMARY KEY (id)
);


--
-- Name: sed_outbox_payload; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox_payload (
    id NUMBER(*,0) NOT NULL ENABLE,
    ebms_id VARCHAR2(128 BYTE),
    name VARCHAR2(128 BYTE),
    description VARCHAR2(512 BYTE),
    type VARCHAR2(64 BYTE),
    filename VARCHAR2(128 BYTE),
    filepath VARCHAR2(1028),
    mime_type VARCHAR2(128 BYTE),
    encoding VARCHAR2(128 BYTE),
    md5 VARCHAR2(32 BYTE) NOT NULL ENABLE,
    mail_id NUMBER(*,0) NOT NULL ENABLE,
    CONSTRAINT sed_outbox_payload_pk PRIMARY KEY (id)
);


--
-- Name: sed_outbox_property; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox_property (
    id NUMBER(*,0) NOT NULL ENABLE,
    name VARCHAR2(128 BYTE),
    value VARCHAR2(512 BYTE),
    mail_id NUMBER(*,0) NOT NULL ENABLE,
    CONSTRAINT sed_outbox_property_pk PRIMARY KEY (id)
);


--
-- Name: seq_sed_inbox; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_inbox INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;


--
-- Name: seq_sed_inbox_event; Type: SEQUENCE; Schema: public; Owner: -
--
CREATE SEQUENCE seq_sed_inbox_event INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;

--
-- Name: seq_sed_inbox_payload; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_inbox_payload INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;

--
-- Name: seq_sed_inbox_prop; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_inbox_prop INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;

--
-- Name: seq_sed_outbox; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_outbox INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;


--
-- Name: seq_sed_outbox_event; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_outbox_event INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;

--
-- Name: seq_sed_outbox_payload; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_outbox_payload INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;


--
-- Name: seq_sed_outbox_prop; Type: SEQUENCE; Schema: public; Owner: -
--
CREATE SEQUENCE seq_sed_outbox_prop INCREMENT BY 1 START WITH 1 MINVALUE 1 NOCACHE ORDER;


--
-- Name: idx_conv_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_conv_id ON sed_inbox (conv_id);


--
-- Name: idx_date_delivered; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_date_delivered ON sed_inbox (date_delivered);

--
-- Name: idx_date_received; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_date_received ON sed_inbox (date_received);



--
-- Name: idx_date_sent; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_date_sent ON sed_inbox (date_sent);



--
-- Name: idx_date_status; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_status ON sed_inbox (status);



--
-- Name: idx_date_submit; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_date_submited ON sed_inbox (date_submited);


--
-- Name: idx_in_event_mail_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_event_mail_id ON sed_inbox_event (mail_id);


--
-- Name: idx_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_inbox_msg_id ON sed_inbox (msg_id);

--
-- Name: idx_out_conv_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_conv_id ON sed_outbox (conv_id);

--
-- Name: idx_out_date_delivered; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_date_delivered ON sed_outbox (date_delivered);


--
-- Name: idx_out_date_received; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_date_received ON sed_outbox (date_received);

--
-- Name: idx_out_date_sent; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_date_sent ON sed_outbox (date_sent);

--
-- Name: idx_out_date_status; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_date_status ON sed_outbox (date_status);

--
-- Name: idx_out_date_submit; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_date_submited ON sed_outbox (date_submited);

--
-- Name: idx_out_even_send_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX sed_outbox_event_sender_msg_id ON sed_outbox_event (sender_msg_id);


--
-- Name: idx_out_event_mail_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_event_mail_id ON sed_outbox_event (mail_id);

--
-- Name: idx_out_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_msg_id ON sed_outbox (msg_id);

--
-- Name: idx_out_sender_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_sender_msg_id ON sed_outbox (sender_msg_id);


--
-- Name: idx_out_status; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sed_outbox_status ON sed_outbox (status);





