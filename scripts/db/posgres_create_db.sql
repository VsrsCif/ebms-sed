
CREATE TABLE sed_inbox (
    id numeric(19,0) NOT NULL,
    msg_id character varying(64),
    sender_msg_id character varying(64),
    service character varying(64),
    action character varying(64),
    conv_id character varying(64),
    subject character varying(512),
    receiver_ebox character varying(64),
    receiver_name character varying(128),
    sender_ebox character varying(64),
    sender_name character varying(128),
    status character varying(32),
    date_status timestamp without time zone,
    date_submited timestamp without time zone,
    date_sent timestamp without time zone,
    date_received timestamp without time zone,
    date_delivered timestamp without time zone
);


--
-- Name: sed_inbox_event; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_inbox_event (
    id numeric(19,0) NOT NULL,
    mail_id numeric(19,0),
    status character varying(32),
    date timestamp without time zone,
    description character varying(512),
    user_id character varying(64),
    application_id character varying(128)
);


--
-- Name: sed_inbox_payload; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_inbox_payload (
    id numeric(19,0) NOT NULL,
    ebms_id character varying(128),
    name character varying(128),
    description character varying(512),
    type character varying(64),
    filename character varying(128),
    filepath character varying(1028),
    mime_type character varying(128),
    encoding character varying(128),
    md5 character varying(32),
    mail_id numeric(19,0)
);


--
-- Name: sed_inbox_property; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_inbox_property (
    id numeric(19,0) NOT NULL,
    name character varying(128),
    value character varying(512),
    mail_id numeric(19,0)
);


--
-- Name: sed_outbox; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox (
    id numeric(19,0) NOT NULL,
    msg_id character varying(64),
    sender_msg_id character varying(64),
    service character varying(64),
    action character varying(64),
    conv_id character varying(64),
    subject character varying(512),
    receiver_ebox character varying(64),
    receiver_name character varying(128),
    sender_ebox character varying(64),
    sender_name character varying(128),
    status character varying(32),
    date_status timestamp without time zone,
    date_submited timestamp without time zone,
    date_sent timestamp without time zone,
    date_received timestamp without time zone,
    date_delivered timestamp without time zone
);


--
-- Name: sed_outbox_event; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox_event (
    id numeric(19,0) NOT NULL,
    mail_id numeric(19,0),
    sender_msg_id character varying(64),
    status character varying(32),
    date timestamp without time zone,
    description character varying(512),
    user_id character varying(64),
    application_id character varying(128)
);


--
-- Name: sed_outbox_payload; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox_payload (
    id numeric(19,0) NOT NULL,
    ebms_id character varying(128),
    name character varying(128),
    description character varying(512),
    type character varying(64),
    filename character varying(128),
    filepath character varying(1028),
    mime_type character varying(128),
    encoding character varying(128),
    md5 character varying(32),
    mail_id numeric(19,0)
);


--
-- Name: sed_outbox_property; Type: TABLE; Schema: public; Owner: -; Tablespace: 
--

CREATE TABLE sed_outbox_property (
    id numeric(19,0) NOT NULL,
    name character varying(128),
    value character varying(512),
    mail_id numeric(19,0)
);


--
-- Name: seq_sed_inbox; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_inbox
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: seq_sed_inbox_event; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_inbox_event
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: seq_sed_inbox_payload; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_inbox_payload
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: seq_sed_inbox_prop; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_inbox_prop
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: seq_sed_outbox; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_outbox
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: seq_sed_outbox_event; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_outbox_event
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: seq_sed_outbox_payload; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_outbox_payload
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: seq_sed_outbox_prop; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE seq_sed_outbox_prop
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sed_inbox_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY sed_inbox_event
    ADD CONSTRAINT sed_inbox_event_pkey PRIMARY KEY (id);


--
-- Name: sed_inbox_payload_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY sed_inbox_payload
    ADD CONSTRAINT sed_inbox_payload_pkey PRIMARY KEY (id);


--
-- Name: sed_inbox_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY sed_inbox
    ADD CONSTRAINT sed_inbox_pkey PRIMARY KEY (id);


--
-- Name: sed_inbox_property_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY sed_inbox_property
    ADD CONSTRAINT sed_inbox_property_pkey PRIMARY KEY (id);


--
-- Name: sed_outbox_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY sed_outbox_event
    ADD CONSTRAINT sed_outbox_event_pkey PRIMARY KEY (id);


--
-- Name: sed_outbox_payload_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY sed_outbox_payload
    ADD CONSTRAINT sed_outbox_payload_pkey PRIMARY KEY (id);


--
-- Name: sed_outbox_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY sed_outbox
    ADD CONSTRAINT sed_outbox_pkey PRIMARY KEY (id);


--
-- Name: sed_outbox_property_pkey; Type: CONSTRAINT; Schema: public; Owner: -; Tablespace: 
--

ALTER TABLE ONLY sed_outbox_property
    ADD CONSTRAINT sed_outbox_property_pkey PRIMARY KEY (id);


--
-- Name: idx_conv_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_conv_id ON sed_inbox USING btree (conv_id);


--
-- Name: idx_date_delivered; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_date_delivered ON sed_inbox USING btree (date_delivered);


--
-- Name: idx_date_received; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_date_received ON sed_inbox USING btree (date_received);


--
-- Name: idx_date_sent; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_date_sent ON sed_inbox USING btree (date_sent);


--
-- Name: idx_date_status; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_date_status ON sed_inbox USING btree (date_status);


--
-- Name: idx_date_submit; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_date_submit ON sed_inbox USING btree (date_submited);


--
-- Name: idx_in_event_mail_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_in_event_mail_id ON sed_inbox_event USING btree (mail_id);


--
-- Name: idx_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_msg_id ON sed_inbox USING btree (msg_id);


--
-- Name: idx_out_conv_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_out_conv_id ON sed_outbox USING btree (conv_id);


--
-- Name: idx_out_date_delivered; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_out_date_delivered ON sed_outbox USING btree (date_delivered);


--
-- Name: idx_out_date_received; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_out_date_received ON sed_outbox USING btree (date_received);


--
-- Name: idx_out_date_sent; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_out_date_sent ON sed_outbox USING btree (date_sent);


--
-- Name: idx_out_date_status; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_out_date_status ON sed_outbox USING btree (date_status);


--
-- Name: idx_out_date_submit; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_out_date_submit ON sed_outbox USING btree (date_submited);


--
-- Name: idx_out_even_send_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_out_even_send_msg_id ON sed_outbox_event USING btree (sender_msg_id);


--
-- Name: idx_out_event_mail_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_out_event_mail_id ON sed_outbox_event USING btree (mail_id);


--
-- Name: idx_out_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_out_msg_id ON sed_outbox USING btree (msg_id);


--
-- Name: idx_out_sender_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_out_sender_msg_id ON sed_outbox USING btree (sender_msg_id);


--
-- Name: idx_out_status; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_out_status ON sed_outbox USING btree (status);


--
-- Name: idx_sender_msg_id; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_sender_msg_id ON sed_inbox USING btree (sender_msg_id);


--
-- Name: idx_status; Type: INDEX; Schema: public; Owner: -; Tablespace: 
--

CREATE INDEX idx_status ON sed_inbox USING btree (status);


--
-- Name: fk_77l8lvj4rsoay1qkn0e26c2nn; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY sed_inbox_payload
    ADD CONSTRAINT fk_77l8lvj4rsoay1qkn0e26c2nn FOREIGN KEY (mail_id) REFERENCES sed_inbox(id);


--
-- Name: fk_cxon1wjuesu72dpc4bfx0s4db; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY sed_outbox_payload
    ADD CONSTRAINT fk_cxon1wjuesu72dpc4bfx0s4db FOREIGN KEY (mail_id) REFERENCES sed_outbox(id);


--
-- Name: fk_lp8w54f910uugjr4riev6jdpq; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY sed_inbox_property
    ADD CONSTRAINT fk_lp8w54f910uugjr4riev6jdpq FOREIGN KEY (mail_id) REFERENCES sed_inbox(id);


--
-- Name: fk_oekx0qfyggjsoqt4ri7qkqskf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY sed_outbox_property
    ADD CONSTRAINT fk_oekx0qfyggjsoqt4ri7qkqskf FOREIGN KEY (mail_id) REFERENCES sed_outbox(id);


--

