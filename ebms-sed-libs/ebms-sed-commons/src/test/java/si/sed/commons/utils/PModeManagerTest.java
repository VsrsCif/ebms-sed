/*
 * Copyright 2015, Supreme Court Republic of Slovenia
 * 
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package si.sed.commons.utils;

import java.io.FileNotFoundException;
import static java.lang.System.setProperty;
import javax.xml.bind.JAXBException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import static org.apache.log4j.Level.DEBUG;
import static org.apache.log4j.Level.FATAL;
import static org.apache.log4j.Logger.getRootLogger;
import org.apache.log4j.PatternLayout;
import org.junit.After;
import org.msh.svev.pmode.PModes;
import static si.sed.commons.SEDSystemProperties.SYS_PROP_HOME_DIR;
import static si.sed.commons.SEDSystemProperties.SYS_PROP_PMODE;
import si.sed.commons.exception.PModeException;
import static si.sed.commons.utils.xml.XMLUtils.serialize;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class PModeManagerTest {

  /**
     *
     */
  public PModeManagerTest() {

    setProperty(SYS_PROP_HOME_DIR, ".");
    setProperty(SYS_PROP_PMODE, "test-pmode.xml");

    ConsoleAppender console = new ConsoleAppender(); // create appender
    // configure the appender
    String PATTERN = "%d [%p|%c|%C{1}] %m%n";
    console.setLayout(new PatternLayout(PATTERN));
    console.setThreshold(FATAL);
    console.activateOptions();
    // add appender to any Logger (here is root)
    getRootLogger().addAppender(console);

    FileAppender fa = new FileAppender();
    fa.setName("FileLogger");
    fa.setFile("test.log");
    fa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
    fa.setThreshold(DEBUG);
    fa.setAppend(true);
    fa.activateOptions();
    // add appender to any Logger (here is root)
    getRootLogger().addAppender(fa);
  }

  /**
     *
     */
  @After
  public void tearDown() {}

  /**
   *
   * @throws PModeException
   * @throws JAXBException
   * @throws FileNotFoundException
   */
  @org.junit.Test
  public void testReloadPModes() throws PModeException, JAXBException, FileNotFoundException {

    PModeManager pmd = new PModeManager();
    pmd.reloadPModes(PModeManagerTest.class.getResourceAsStream("/pmode/pmodes.xml"));
    PModes pm = pmd.getPModes();

    serialize(pm, "test-pmode.xml");

  }

}
