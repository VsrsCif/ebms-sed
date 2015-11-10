CREATE TABLE zpp_plugin_out_keys (
    mail_id numeric(19,0) NOT NULL,
    secret_key character varying(512),
    algorithm character varying(128)
);

ALTER TABLE ONLY  zpp_plugin_out_keys
    ADD CONSTRAINT zpp_plugin_out_keys_pkey PRIMARY KEY (mail_id );
