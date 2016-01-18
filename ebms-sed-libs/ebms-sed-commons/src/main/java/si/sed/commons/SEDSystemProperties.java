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
package si.sed.commons;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class SEDSystemProperties {

    /**
     * System property for SED home directory.
     *
     * <p>
     * System property define home directory which contains pmode.configuration
     * </p>
     */
    public static final String SYS_PROP_HOME_DIR = "sed.home.dir";

    /**
     * System property for pmode configuration file.
     *
     * <p>
     * System property define pmode configuration file. File is relative to SED
     * home directory.
     * </p>
     */
    public static final String SYS_PROP_PMODE = "org.sed.pmode";
    /**
     * Default value for pmode configuration file name.
     *
     * <p>
     * If system property is not given, absolute file to pmode file
     * ${SYS_PROP_HOME_DIR}/pmode-conf.xml
     * </p>
     */
    public static final String SYS_PROP_PMODE_DEF = "pmode-conf.xml";
    
    
    /**
     * Default value for plugin folder name.
     *
     * <p>
     * Def plugin folder name form plugins;
     * ${SYS_PROP_HOME_DIR}/plugins
     * </p>
     */
    public static final String SYS_PROP_FOLDER_PLUGINS_DEF = "plugins";
    
    /**
     * Default value for plugin folder name.
     *
     * <p>
     * Def plugin folder name form plugins;
     * ${SYS_PROP_HOME_DIR}/plugins
     * </p>
     */
    public static final String SYS_PROP_FOLDER_STORAGE_DEF = "storage";

    /**
     * Default value for keystore and trustore configuration file name.
     *
     * <p>
     * If system property is not given, absolute file to pmode file
     * ${SYS_PROP_HOME_DIR}/pmode-conf.xml
     * </p>
     */
    public static final String SYS_PROP_CERT_DEF = "security-conf.properties";

    /**
     * Default value for keystore and trustore pasword file name.
     *
     * <p>
     * If system property is not given, absolute file to pmode file
     * ${SYS_PROP_HOME_DIR}/pmode-conf.xml
     * </p>
     */
    public static final String SYS_KEY_PASSWD_DEF = "key-passwords.properties";

    /**
     * System property for out qeue workers.
     *
     * <p>
     * If system property is not given, max 5 outgoing workers are initiated.
     * Workers handle outbox messages.
     * </p>
     */
    public static final String SYS_PROP_MAX_QUEUE_WORKERS = "org.sed.msh.maxWorkers";

    /**
     * System property for database dialect
     *
     * <p>
     * If system property is not given, max 5 outgoing workers are initiated.
     * Workers handle outbox messages.
     * </p>
     */
    public static final String SYS_PROP_DB_DIALECT = "org.sed.msh.hibernate.dialect";

    /**
     * System property for database dialect
     *
     * <p>
     * If system property is not given, max 5 outgoing workers are initiated.
     * Workers handle outbox messages.
     * </p>
     */
    public static final String SYS_PROP_DB_HBM2DLL = "org.sed.msh.hibernate.hbm2ddl";

    /**
     * System property for JNID prefix: wildfly: java:/jms/ jetty:
     * java:comp/env/ junit test: ''
     *
     * <p>
     * If system property is not given, max 5 outgoing workers are initiated.
     * Workers handle outbox messages.
     * </p>
     */
    public static final String SYS_PROP_JNDI_PREFIX = "org.sed.jndi.prefix";
    public static final String SYS_PROP_JNDI_JMS_PREFIX = "org.sed.jndi.jms.prefix";

}
