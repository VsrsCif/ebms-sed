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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.msh.svev.pmode.PMode;
import org.msh.svev.pmode.PModes;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.exception.PModeException;
import si.sed.commons.utils.xml.XMLUtils;

/**
 *
 * @author Joze Rihtarsic <joze.rihtarsic@sodisce.si>
 */
public class PModeManager {

    public static SEDLogger LOG = new SEDLogger(PModeManager.class);

    PModes pmodes = null;

    public void PModeManager() {

    }

    public PMode getPModeById(String pModeId) throws PModeException {
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

    public boolean removePMode(PMode pmr) {
        boolean suc = false;
        if (pmr == null) {
            suc = pmodes.getPModes().remove(pmr);
        }
        return suc;

    }

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

    public boolean add(PMode pmrNew) {
        return pmodes.getPModes().add(pmrNew);
    }

    public void add(int i, PMode pmrNew) {
        pmodes.getPModes().add(i, pmrNew);
    }

    public void savePMode() throws PModeException {
        long l = LOG.logStart();
        try {

            File pModeFile = new File(getPModeFilePath());
            int i = 1;
            String fileFormat = getPModeFilePath() + ".%03d";
            File pModeFileTarget = new File(String.format(fileFormat, i++));

            while (pModeFileTarget.exists()) {
                pModeFileTarget = new File(String.format(fileFormat, i++));
            }

            Files.move(pModeFile.toPath(), pModeFileTarget.toPath(), StandardCopyOption.REPLACE_EXISTING);

            try (PrintWriter out = new PrintWriter(pModeFile)) {
                XMLUtils.serialize(pmodes, out);
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

    public void reloadPModes() throws PModeException {
        long l = LOG.logStart();
        File pModeFile = new File(getPModeFilePath());
        try (FileInputStream fis = new FileInputStream(pModeFile)) {
            reloadPModes(fis);
        } catch (IOException ex) {
            String msg = "Error init PModes from file '" + pModeFile.getAbsolutePath() + "'";
            throw new PModeException(msg, ex);
        }
        LOG.logEnd(l);
    }

    public void reloadPModes(InputStream is) throws PModeException {
        long l = LOG.logStart();

        try {
            pmodes = (PModes) XMLUtils.deserialize(is, PModes.class);
        } catch (JAXBException ex) {
            String msg = "Error init PModes!";
            throw new PModeException(msg, ex);
        }
        LOG.logEnd(l);
    }

    public String getPModeFilePath() {
        return System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator
                + System.getProperty(SEDSystemProperties.SYS_PROP_PMODE, SEDSystemProperties.SYS_PROP_PMODE_DEF);
    }

    public PModes getPModes() throws PModeException {
        reloadPModes();
        return pmodes;
    }

    public List<PMode> getPModeList() throws PModeException {
        reloadPModes();
        return pmodes.getPModes();
    }

}
