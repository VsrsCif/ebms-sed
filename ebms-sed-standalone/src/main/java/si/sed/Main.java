package si.sed;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.UserTransaction;
import javax.xml.ws.Endpoint;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;

import org.sed.msh.jms.MSHQueueBean;
import si.jrc.jetty.persistence.JettyUserTransaction;
import si.sed.commons.utils.SEDLogger;

public class Main {

    protected static final String SED_HOME_TEMPLATE = "/home_dir_template";
    protected static final String JNDI_CONNECTION_FACTORY = "ConnectionFactory";
    protected static final String JNDI_PREFIX_VALUE = "java:comp/env";
    protected static final String PU_NAME = "ebMS_PU";
    protected static final String PU_MSH_NAME = "ebMS_MSH_PU";

    protected static final StandaloneSettings S_CONF;

    static {
        S_CONF = StandaloneSettings.getInstance();
        System.out.println("INIT HOME_FOLDER:" + S_CONF.getHome().getAbsolutePath());
        // init home folder
        copyFromJar(SED_HOME_TEMPLATE, S_CONF.getHome().toPath());
        // inizialize log4j
        System.out.println("INIT LOG4J");
        PropertyConfigurator.configure(S_CONF.getLogPropertiesFile().getAbsolutePath());
    }

    protected final SEDLogger mlog = new SEDLogger(Main.class);

    public static void main(String[] args) throws Exception {

        // start server
        Server server = new Server(S_CONF.getPort());

        ClassList classList = ClassList.setServerDefault(server);
        classList.addBefore(
                "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.webapp.FragmentConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration",
                "org.eclipse.jetty.plus.webapp.EnvConfiguration",
                "org.eclipse.jetty.plus.webapp.PlusConfiguration");

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");

        ProtectionDomain domain = Main.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();
        webapp.setWar(location.toExternalForm());
        webapp.setDefaultsDescriptor("META-INF/jetty/webdefault.xml");

        MSHQueueBean mb = setJMSEnvironment(webapp);
        configureResources(webapp, mb);

        ContextHandlerCollection contexts = new ContextHandlerCollection();

        contexts.setHandlers(new Handler[]{webapp});

        server.setHandler(contexts);
        /*
        CXFNonSpringServlet cxf = new CXFNonSpringServlet();
        ServletHolder servlet = new ServletHolder(cxf);
        servlet.setName("soap");
        servlet.setForcedPath("soap");
        webapp.addServlet(servlet, "/soap/*");
        
         Bus bus = cxf.getBus();
        SEDMailBox impl = new SEDMailBox();
        Endpoint.publish("/Greeter", impl);
         */
        server.start();
        server.join();
    }

    private static void configureResources(WebAppContext webapp, MSHQueueBean mb) {

        try {
            // create derby database
            String strDBPath = S_CONF.getHome().getAbsolutePath() + File.separator + "db" + File.separator + "ebms-sed";
            Properties properties = new Properties();
            properties.put("javax.persistence.jdbc.url", "jdbc:derby:" + strDBPath + ";create=true");
            properties.put("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.EmbeddedDriver");
            properties.put("hibernate.cache.provider_class", "org.hibernate.cache.NoCacheProvider");
            properties.put("hibernate.dialect", "org.hibernate.dialect.DerbyTenSevenDialect");
            properties.put("hibernate.archive.autodetection", "class");
            properties.put("hibernate.connection.autocommit", "false");
            if (Files.notExists(Paths.get(strDBPath))) {
                properties.put("hibernate.hbm2ddl.auto", "create"); // overide properties
            }

            EntityManagerFactory emf = Persistence.createEntityManagerFactory(PU_NAME, properties);
            EntityManager em = emf.createEntityManager();
            UserTransaction ut = new JettyUserTransaction(em.getTransaction());
            org.eclipse.jetty.plus.jndi.Resource myEntityManage = new org.eclipse.jetty.plus.jndi.Resource(webapp, PU_NAME,
                    em);

            org.eclipse.jetty.plus.jndi.Resource myEntityManage2 = new org.eclipse.jetty.plus.jndi.Resource(webapp, PU_MSH_NAME,
                    em);

            org.eclipse.jetty.plus.jndi.Transaction transactionMgr = new org.eclipse.jetty.plus.jndi.Transaction(ut);

        } catch (NamingException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static MSHQueueBean setJMSEnvironment(WebAppContext webapp) throws NamingException, JMSException {
        Properties p = new Properties();
        p.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        p.put("java.naming.provider.url", "vm://localhost?broker.persistent=false");
        InitialContext context = new InitialContext(p);
        ActiveMQConnectionFactory connectionFactory = (ActiveMQConnectionFactory) context.lookup(JNDI_CONNECTION_FACTORY);

        // Create a Connection
        Connection connection = connectionFactory.createConnection();
        connection.start();
        // Create a Session
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        // Create the destination Queue
        Queue mshQue = session.createQueue("queue/MSHQueue");
        // add consumer
        MessageConsumer mc = session.createConsumer(mshQue);
        MSHQueueBean mshConsumer = new MSHQueueBean();
        mc.setMessageListener(mshConsumer);
        Resource myQueue = new org.eclipse.jetty.plus.jndi.Resource(webapp, "queue/MSHQueue",
                mshQue);

        Resource mConnectionFactory = new org.eclipse.jetty.plus.jndi.Resource(webapp, JNDI_CONNECTION_FACTORY,
                connectionFactory);
        return mshConsumer;
    }

    public static void copyFromJar(String source, final Path target) {
        try {
            URI resource = Main.class.getResource(SED_HOME_TEMPLATE).toURI();
            FileSystem fileSystem = FileSystems.newFileSystem(resource,
                    Collections.<String, String>emptyMap()
            );

            final Path jarPath = fileSystem.getPath(source);

            try (Stream<Path> paths = Files.walk(jarPath)) {
                paths.forEach(Main::copyPath);
            }
        } catch (URISyntaxException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void copyPath(Path path) {
        try {
            Path target = Paths.get(S_CONF.getHome().getAbsolutePath() + path.toString().substring(SED_HOME_TEMPLATE.length()));
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(Main.class.getResourceAsStream(path.toString()), target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

}
