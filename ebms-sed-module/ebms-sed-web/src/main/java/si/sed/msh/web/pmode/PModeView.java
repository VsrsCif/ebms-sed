/*
* Copyright 2016, Supreme Court Republic of Slovenia 
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
package si.sed.msh.web.pmode;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import static javax.ws.rs.client.Entity.text;
import org.msh.svev.pmode.Leg;
import org.msh.svev.pmode.PMode;
import org.primefaces.event.SelectEvent;
import si.sed.commons.SEDSystemProperties;
import si.sed.commons.utils.SEDLogger;
import si.sed.commons.utils.xml.XMLUtils;
import si.sed.msh.web.abst.AbstractJSFView;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "pModeView")
public class PModeView extends AbstractJSFView {

    public static SEDLogger LOG = new SEDLogger(PModeView.class);
    
    private PMode currentPMode;
    //PModeManager pm = new  PModeManager();
    String pMode = null;

    private Map<String, String> mLookupMep;
    private Map<String, String> mLookupMepBinding;

    @PostConstruct
    public void init() {
        mLookupMep = new HashMap<>();
        mLookupMep.put("One-Way MEP", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/oneWay");
        mLookupMep.put("Two-Way MEP", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/twoWay");
        mLookupMepBinding = new HashMap<>();
        mLookupMepBinding.put("Push", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/push");
        mLookupMepBinding.put("Pull", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pull");
        mLookupMepBinding.put("Sync", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/sync");
        mLookupMepBinding.put("PushAndPush", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPush");
        mLookupMepBinding.put("PushAndPull", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pushAndPull");
        mLookupMepBinding.put("PullAndPush", "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/pullAndPush");
    }

    public List<PMode> getPModes() {
        return null;
    }

    public PMode getCurrentPMode() {
        return currentPMode;
    }

    public void setCurrentPMode(PMode currentPMode) {
        this.currentPMode = currentPMode;
    }

    public Leg getCurrentPModeForeChannel() {

        return null;
    }

    public void onRowSelect(SelectEvent event) {
        setCurrentPMode((PMode) event.getObject());
    }

    public Map<String, String> getLookupMEP() {
        return mLookupMep;
    }

    public Map<String, String> getLookupMEPBinding() {
        return mLookupMepBinding;
    }

    public String getPModeString() {
        long l = LOG.logStart();
        if (pMode == null) {
            File pModeFile = new File(getPModeFilePath());
            try {
                pMode = readFile(pModeFile, Charset.forName("UTF-8"));
                
            } catch (IOException ex) {
                LOG.logError(l, "ERROR reading file: " + getPModeFilePath(),  ex);
            }
        }
        LOG.logEnd(l);
        return pMode;
    }

    public void setPModeString(String val) {
        pMode = val;
    }

 

    public void savePMode() {
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
                out.println(getPModeString());
                
                String msg = "PMode saved!";
                LOG.log(msg + " File: " + getPModeFilePath());
                facesContext().addMessage(null, new FacesMessage(msg, pModeFile.getAbsolutePath() ));                
            }

        } catch (IOException ex) {
            String msg = "ERROR saving file: "  +   ex.getMessage();
            LOG.logError(l, msg ,  ex);
            facesContext().addMessage(null, new FacesMessage(msg,getPModeFilePath() ));                
        }
        LOG.logEnd(l);
    }

    public void formatPMode() {
        setPModeString(XMLUtils.format(getPModeString()));
    }
    
    public String getPModeFilePath(){
        return System.getProperty(SEDSystemProperties.SYS_PROP_HOME_DIR) + File.separator
                    + System.getProperty(SEDSystemProperties.SYS_PROP_PMODE, SEDSystemProperties.SYS_PROP_PMODE_DEF);
    }

    static String readFile(File file, Charset encoding)
            throws IOException {

        byte[] encoded = Files.readAllBytes(file.toPath());
        return new String(encoded, encoding);
    }
}
