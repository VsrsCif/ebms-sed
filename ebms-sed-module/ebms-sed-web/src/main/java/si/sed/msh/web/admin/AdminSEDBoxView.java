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
package si.sed.msh.web.admin;

import java.util.Calendar;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.sed.ebms.ebox.Execute;
import org.sed.ebms.ebox.Export;
import org.sed.ebms.ebox.SEDBox;
import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.DBSettingsInterface;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.utils.SEDLogger;
import si.sed.msh.web.abst.AbstractAdminJSFView;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "adminSEDBoxView")
public class AdminSEDBoxView extends AbstractAdminJSFView<SEDBox> {

    private static final SEDLogger LOG = new SEDLogger(AdminSEDBoxView.class);

    @EJB(mappedName = SEDJNDI.JNDI_DBSETTINGS)
    private DBSettingsInterface mdbSettings;

    @EJB(mappedName = SEDJNDI.JNDI_SEDLOOKUPS)
    private SEDLookupsInterface mdbLookups;

    /**
     *
     * @param sedBox
     * @return
     */
    public SEDBox getSEDBoxByName(String sedBox) {
        return mdbLookups.getSEDBoxByName(sedBox);
    }

    /**
     *
     */
    @Override
    public void createEditable() {
        long l = LOG.logStart();

        String domain = mdbSettings.getDomain();
        String sbname = "name.%03d@%s";
        int i = 1;
        while (getSEDBoxByName(String.format(sbname, i, domain)) != null) {
            i++;
        }
        SEDBox sbx = new SEDBox();
        sbx.setBoxName(String.format(sbname, i, domain));
        sbx.setActiveFromDate(Calendar.getInstance().getTime());
        sbx.setExport(new Export());
        sbx.setExecute(new Execute());
        setNew(sbx);
        LOG.logEnd(l);
    }

    /**
     *
     */
    @Override
    public void removeSelected() {
        SEDBox sb = getSelected();
        if (sb != null) {

            mdbLookups.removeSEDBox(sb);
            setSelected(null);

        }

    }

    /**
     *
     */
    @Override
    public void startEditSelected() {
        if (getSelected() != null && getSelected().getExport() == null) {
            getSelected().setExport(new Export());
        }
        if (getSelected() != null && getSelected().getExecute() == null) {
            getSelected().setExecute(new Execute());
        }
        super.startEditSelected(); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     */
    @Override
    public void persistEditable() {
        SEDBox sb = getEditable();
        if (sb != null) {
            mdbLookups.addSEDBox(sb);
            setEditable(null);
        }
    }

    /**
     *
     */
    @Override
    public void updateEditable() {
        SEDBox sb = getEditable();
        if (sb != null) {
            mdbLookups.updateSEDBox(sb);
            setEditable(null);
        }
    }

    /**
     *
     * @return
     */
    @Override
    public List<SEDBox> getList() {
        return mdbLookups.getSEDBoxes();
    }

}
