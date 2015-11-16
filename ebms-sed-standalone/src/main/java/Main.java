
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
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.eclipse.jetty.plus.jndi.Resource;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.Configuration.ClassList;
import org.eclipse.jetty.webapp.WebAppContext;
import org.sed.msh.jms.MSHQueueBean;
import si.jrc.jetty.persistence.JettyUserTransaction;
import si.sed.commons.SEDSystemProperties;

public class Main {

    protected static final String SED_HOME_TEMPLATE = "/home_dir_template";
    protected static final String SED_HOME = "sed-home";
    protected static final String PMODE_FILE = "/pmode-conf.xml";
    protected static final String KEY_PASSWD_FILE = "/key-passwords.properties";
    protected static final String SEC_CONF_FILE = "/security-conf.properties";

    protected static final String JNDI_CONNECTION_FACTORY = "ConnectionFactory";
    protected static final String JNDI_PREFIX_VALUE = "java:comp/env";

    static Path SP_HOME_DIR = Paths.get(SED_HOME);

    public static void main(String[] args) throws Exception {

        setLogger("ebms-sed");

        initialize();

        Server server = new Server(8080);

        ClassList classList = ClassList.setServerDefault(server);
        classList.addBefore(
                "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.webapp.FragmentConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration",
                "org.eclipse.jetty.plus.webapp.EnvConfiguration",
                "org.eclipse.jetty.plus.webapp.PlusConfiguration");

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        /*SEDMailBoxWS ms = new SEDMailBox();
        String address = "http://127.0.0.1:8080/SimpleWebService";
        Endpoint.publish(address, implementor);*/

        ProtectionDomain domain = Main.class.getProtectionDomain();
        URL location = domain.getCodeSource().getLocation();
        webapp.setWar(location.toExternalForm());
        webapp.setDefaultsDescriptor("META-INF/jetty/webdefault.xml");

        MSHQueueBean mb = setJMSEnvironment(webapp);
        configureResources(webapp, mb);

        ContextHandlerCollection contexts = new ContextHandlerCollection();

        contexts.setHandlers(new Handler[]{webapp});

        server.setHandler(contexts);
        server.start();
        server.join();
    }

    private static void configureResources(WebAppContext webapp, MSHQueueBean mb) {

        try {

            Properties properties = new Properties();
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("ebMS_PU", properties);
            EntityManager em = emf.createEntityManager();
            UserTransaction ut = new JettyUserTransaction(em.getTransaction());
            org.eclipse.jetty.plus.jndi.Resource myEntityManage = new org.eclipse.jetty.plus.jndi.Resource(webapp, "ebMS_PU",
                    em);

            org.eclipse.jetty.plus.jndi.Transaction transactionMgr = new org.eclipse.jetty.plus.jndi.Transaction(ut);

            mb.memEManager = em;
            mb.mutUTransaction = ut;

        } catch (NamingException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void setLogger(String fileName) {
        // set logger
        ConsoleAppender console = new ConsoleAppender(); //create appender
        //configure the appender
        String PATTERN = "%d [%p|%c|%C{1}] %m%n";
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(org.apache.log4j.Level.WARN);
        console.activateOptions();
        //add appender to any Logger (here is root)
        org.apache.log4j.Logger.getRootLogger().addAppender(console);
        FileAppender fa = new FileAppender();
        fa.setName("FileLogger-" + fileName);
        fa.setFile("target" + File.separator + fileName + ".log");
        fa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
        fa.setThreshold(org.apache.log4j.Level.DEBUG);
        fa.setAppend(true);
        fa.activateOptions();
        //add appender to any Logger (here is root)
        org.apache.log4j.Logger.getRootLogger().addAppender(fa);
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

    public static void initialize() throws IOException, URISyntaxException {
        //---------------------------------
        // set system variables
        // create home dir in target

        copyFromJar(SED_HOME_TEMPLATE, SP_HOME_DIR);
    

        // set system prioperties
        System.setProperty(SEDSystemProperties.SYS_PROP_HOME_DIR, SED_HOME);
        System.setProperty(SEDSystemProperties.SYS_PROP_JNDI_PREFIX, "java:comp/env/");
        System.setProperty(SEDSystemProperties.SYS_PROP_JNDI_JMS_PREFIX, "java:comp/env/");
    }

    public static void copyFromJar(String source, final Path target) throws URISyntaxException, IOException {
        URI resource = Main.class.getResource(SED_HOME_TEMPLATE).toURI();
        FileSystem fileSystem = FileSystems.newFileSystem(resource,
                Collections.<String, String>emptyMap()
        );

        final Path jarPath = fileSystem.getPath(source);
        try (Stream<Path> paths = Files.walk(jarPath)) {
            paths.forEach(Main::copyPath);
        }
    }

    public static void copyPath(Path path) {
        try {
            Path target = Paths.get(SED_HOME + path.toString().substring(SED_HOME_TEMPLATE.length()));
            if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isDirectory(path)){
                    Files.createDirectories(target );
                } else {
                    Files.copy(Main.class.getResourceAsStream(path.toString()), target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

      
    }

   

}
