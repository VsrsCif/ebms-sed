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
     * Default value for keystore and trustore configuration file name.
     *
     * <p>
     * If system property is not given, absolute file to pmode file
     * ${SYS_PROP_HOME_DIR}/pmode-conf.xml
     * </p>
     */
    public static final String SYS_PROP_CERT_DEF = "security-conf.properties";
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
     * System property for out qeue workers.
     *
     * <p>
     * If system property is not given, max 5 outgoing workers are initiated.
     * Workers handle outbox messages.
     * </p>
     */
    public static final String SYS_PROP_EXECUTION_WORKERS = "org.sed.msh.execution.workers.count";
    /**
     * Default value for plugin folder name.
     *
     * <p>
     * Def plugin folder name form plugins; ${SYS_PROP_HOME_DIR}/plugins
     * </p>
     */
    public static final String SYS_PROP_FOLDER_PLUGINS_DEF = "plugins";
    /**
     * Default value for plugin folder name.
     *
     * <p>
     * Def security folder name for trunstore and keystore ;
     * ${SYS_PROP_HOME_DIR}/security
     * </p>
     */
    public static final String SYS_PROP_FOLDER_SECURITY_DEF = "security";
    /**
     * Default value for plugin folder name.
     *
     * <p>
     * Def plugin folder name for plugins; ${SYS_PROP_HOME_DIR}/plugins
     * </p>
     */
    public static final String SYS_PROP_FOLDER_STORAGE_DEF = "storage";

    /**
     * System property for SED home directory.
     *
     * <p>
     * System property define home directory which contains pmode.configuration
     * </p>
     */
    public static final String SYS_PROP_HOME_DIR = "sed.home";
    public static final String SYS_PROP_HOME_DIR_DEF = "sed-home";

    /**
     * System property for init lookups file.
     *
     * <p>
     * System property define init lookups file. File is absolute path to init
     * file home directory.
     * </p>
     */
    public static final String SYS_PROP_INIT_LOOKUPS = "org.sed.init.lookups";
    public static final String SYS_PROP_JNDI_JMS_PREFIX = "org.sed.jndi.jms.prefix";
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
     * System property for out qeue workers.
     *
     * <p>
     * If system property is not given, max 5 outgoing workers are initiated.
     * Workers handle outbox messages.
     * </p>
     */
    public static final String SYS_PROP_QUEUE_SENDER_WORKERS = "org.sed.msh.sender.workers.count";

    static {
        if (System.getProperty(SYS_PROP_QUEUE_SENDER_WORKERS) == null) {
            System.setProperty(SYS_PROP_QUEUE_SENDER_WORKERS, "5");
        }
        if (System.getProperty(SYS_PROP_EXECUTION_WORKERS) == null) {
            System.setProperty(SYS_PROP_EXECUTION_WORKERS, "5");
        }

    }

}
