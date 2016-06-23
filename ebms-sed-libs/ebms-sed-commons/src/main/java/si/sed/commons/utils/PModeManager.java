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
package si.sed.commons.utils;

import java.io.File;
import static java.io.File.separator;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.PModes;
import static si.sed.commons.SEDSystemProperties.SYS_PROP_HOME_DIR;
import static si.sed.commons.SEDSystemProperties.SYS_PROP_PMODE;
import static si.sed.commons.SEDSystemProperties.SYS_PROP_PMODE_DEF;
import si.sed.commons.exception.PModeException;
import static si.sed.commons.utils.xml.XMLUtils.deserialize;
import static si.sed.commons.utils.xml.XMLUtils.serialize;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class PModeManager {

    /**
     *
     */
    public static SEDLogger LOG = new SEDLogger(PModeManager.class);

    PModes pmodes = null;

    /**
     *
     */
    public void PModeManager() {

    }

    /**
     *
     * @param pModeId
     * @return
     * @throws PModeException
     */
    public PMode getPModeById(String pModeId)
            throws PModeException {
        if (pmodes == null) {
            reloadPModes();
        }
        for (PMode pm : pmodes.getPModes()) {
            if (pm.getId() != null && pm.getId().equals(pModeId)) {
                return pm;
            }
        }
        return null;
    }

    /**
     *
     * @param pmr
     * @return
     */
    public boolean removePMode(PMode pmr) {
        boolean suc = false;
        if (pmr == null) {
            suc = pmodes.getPModes().remove(pmr);
        }
        return suc;

    }

    /**
     *
     * @param pModeId
     * @return
     */
    public PMode removePModeById(String pModeId) {
        PMode removed = null;
        for (PMode pm : pmodes.getPModes()) {
            if (pm.getId() != null && pm.getId().equals(pModeId)) {
                pmodes.getPModes().remove(pm);
                removed = pm;
                break;
            }
        }
        return removed;

    }

    /**
     *
     * @param pmrNew
     * @param pModeIdOld
     * @return
     */
    public boolean replace(PMode pmrNew, String pModeIdOld) {
        boolean suc = false;
        for (PMode pm : pmodes.getPModes()) {
            if (pm.getId() != null && pm.getId().equals(pModeIdOld)) {
                int i = pmodes.getPModes().indexOf(pm);
                pmodes.getPModes().remove(pm);
                pmodes.getPModes().add(i, pmrNew);
                suc = true;
                break;
            }
        }
        return suc;
    }

    /**
     *
     * @param pmrNew
     * @return
     */
    public boolean add(PMode pmrNew) {
        return pmodes.getPModes().add(pmrNew);
    }

    /**
     *
     * @param i
     * @param pmrNew
     */
    public void add(int i, PMode pmrNew) {
        pmodes.getPModes().add(i, pmrNew);
    }

    /**
     *
     * @throws PModeException
     */
    public void savePMode()
            throws PModeException {
        long l = LOG.logStart();
        try {

            File pModeFile = new File(getPModeFilePath());
            int i = 1;
            String fileFormat = getPModeFilePath() + ".%03d";
            File pModeFileTarget = new File(format(fileFormat, i++));

            while (pModeFileTarget.exists()) {
                pModeFileTarget = new File(format(fileFormat, i++));
            }

            move(pModeFile.toPath(), pModeFileTarget.toPath(), REPLACE_EXISTING);

            try (PrintWriter out = new PrintWriter(pModeFile)) {
                serialize(pmodes, out);
            } catch (JAXBException | FileNotFoundException ex) {
                String msg = "ERROR serialize PMODE: " + ex.getMessage();
                throw new PModeException(msg, ex);
            }

        } catch (IOException ex) {
            String msg = "ERROR saving file: " + ex.getMessage();
            throw new PModeException(msg, ex);
        }
        LOG.logEnd(l);
    }

    /**
     *
     * @throws PModeException
     */
    public void reloadPModes()
            throws PModeException {
        long l = LOG.logStart();
        File pModeFile = new File(getPModeFilePath());
        try (FileInputStream fis = new FileInputStream(pModeFile)) {
            reloadPModes(fis);
        } catch (IOException ex) {
            String msg = "Error init PModes from file '" +
                    pModeFile.getAbsolutePath() + "'";
            throw new PModeException(msg, ex);
        }
        LOG.logEnd(l);
    }

    /**
     *
     * @param is
     * @throws PModeException
     */
    public void reloadPModes(InputStream is)
            throws PModeException {
        long l = LOG.logStart();

        try {
            pmodes = (PModes) deserialize(is, PModes.class);
        } catch (JAXBException ex) {
            String msg = "Error init PModes!";
            throw new PModeException(msg, ex);
        }
        LOG.logEnd(l);
    }

    /**
     *
     * @return
     */
    public String getPModeFilePath() {
        return getProperty(SYS_PROP_HOME_DIR) + separator +
                getProperty(SYS_PROP_PMODE, SYS_PROP_PMODE_DEF);
    }

    /**
     *
     * @return @throws PModeException
     */
    public PModes getPModes()
            throws PModeException {
        reloadPModes();
        return pmodes;
    }

    /**
     *
     * @return @throws PModeException
     */
    public List<PMode> getPModeList()
            throws PModeException {
        if (pmodes == null) {
            reloadPModes();
        }
        return pmodes.getPModes();
    }

}
