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

import si.sed.msh.web.abst.AbstractAdminJSFView;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.sed.ebms.cron.SEDTaskType;
import org.sed.ebms.cron.SEDTaskTypeProperty;

import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.interfaces.SEDSchedulerInterface;

import si.sed.commons.utils.SEDLogger;

/**
 *
 * @author Jože Rihtaršič
 */
@SessionScoped
@ManagedBean(name = "adminSEDTaskTypeView")
public class AdminSEDTaskTypeView extends AbstractAdminJSFView<SEDTaskType> {

    private static final SEDLogger LOG = new SEDLogger(AdminSEDTaskTypeView.class);

    @EJB(mappedName = SEDJNDI.JNDI_SEDLOOKUPS)
    private SEDLookupsInterface mdbLookups;

    SEDTaskTypeProperty mSelTaksProp;

    

    @Override
    public void createEditable() {
        SEDTaskType ecj = new SEDTaskType();
        
        
        String type = "type_%03d";
        int i = 1;
        while (mdbLookups.getSEDTaskTypeByType(String.format(type, i)) != null) {
            i++;
        }

        ecj.setType(String.format(type, i));
        ecj.setName("emailReport");
        ecj.setDescription("Posiljanje porocil");
        ecj.getSEDTaskTypeProperties().add(createTypeProperty("sedbox", "SED-Predal", true));
        ecj.getSEDTaskTypeProperties().add(createTypeProperty("email", "Prejemnikov naslov", true));
        ecj.getSEDTaskTypeProperties().add(createTypeProperty("send-mail", "pošiljateljev email", true));
        ecj.getSEDTaskTypeProperties().add(createTypeProperty("subject", "[EMBS-SED] sedpredal", true));

        setNew(ecj);
    }

    @Override
    public void removeSelected() {
        if (getSelected() != null) {
            mdbLookups.removeSEDTaskType(getSelected());
            setSelected(null);

        }
    }
    @Override
    public void persistEditable() {
        SEDTaskType ecj = getEditable();
        if (ecj != null) {
            mdbLookups.addSEDTaskType(ecj);
        }
    }

    @Override
    public void updateEditable() {
        SEDTaskType ecj = getEditable();
        if (ecj != null) {
            mdbLookups.updateSEDTaskType(ecj);

        }
    }

    @Override
    public List<SEDTaskType> getList() {
        long l = LOG.logStart();
        List<SEDTaskType> lst = mdbLookups.getSEDTaskTypes();
        LOG.logEnd(l, lst != null ? lst.size() : "null");
        return lst;
    }
    
    
    public SEDTaskTypeProperty createTypeProperty(String key, String name, boolean mandatory){
        SEDTaskTypeProperty sp = new SEDTaskTypeProperty();        
        sp.setKey(key);
        sp.setDescription(name);
        sp.setMandatory(mandatory);
        return sp;
    }
    
    public void addTypeProperty(){
        
        if (getEditable()!=null) {
            System.out.println("ADD property");
            SEDTaskTypeProperty sp = createTypeProperty("newProp","", true);
            getEditable().getSEDTaskTypeProperties().add(sp);
            setSelectedTaskProperty(sp);
            System.out.println("set selected ");
        }

    }
     public void removeSelectedTypeProperty(){
        if (getEditable()!=null && getSelectedTaskProperty()!=null)  {
            getEditable().getSEDTaskTypeProperties().remove(getSelectedTaskProperty());
        }
    }

    
    public SEDTaskTypeProperty getSelectedTaskProperty(){
        return mSelTaksProp;
    }
    
    public void setSelectedTaskProperty(SEDTaskTypeProperty prop){
        this.mSelTaksProp = prop;
    }
    
}
