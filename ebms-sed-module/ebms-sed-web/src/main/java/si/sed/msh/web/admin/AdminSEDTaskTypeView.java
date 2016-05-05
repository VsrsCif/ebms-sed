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
import si.sed.msh.web.abst.AbstractAdminJSFView;
import java.util.List;
import java.util.Properties;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.sed.ebms.cron.SEDTaskType;
import org.sed.ebms.cron.SEDTaskTypeProperty;

import si.sed.commons.SEDJNDI;
import si.sed.commons.interfaces.SEDLookupsInterface;
import si.sed.commons.interfaces.SEDSchedulerInterface;
import si.sed.commons.interfaces.TaskExecutionInterface;

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
        ecj.setJndi("java:global[/application name]/module name/enterprise bean name[/interface name]");
        ecj.setType(String.format(type, i));
        ecj.setName("");
        ecj.setDescription("Enter JNDI and refresh data from EJB task!");
     

        setNew(ecj);
    }
    
    public void refreshDataFromEJB(){
        if (getEditable() ==null || getEditable().getJndi()==null || getEditable().getJndi().isEmpty())  {
            return;
        }
        
         try {
            TaskExecutionInterface tproc = InitialContext.doLookup(getEditable().getJndi());
            getEditable().setDescription(tproc.getDesc());
            getEditable().setName(tproc.getName());
            getEditable().setType(tproc.getType());
            getEditable().getSEDTaskTypeProperties().clear();
            if (tproc.getProperties()!= null){
                Properties p = tproc.getProperties();
                for (String  key: p.stringPropertyNames()) {
                    SEDTaskTypeProperty tp = createTypeProperty(key, p.getProperty(key), true);
                    getEditable().getSEDTaskTypeProperties().add(tp);
                }
            }
        } catch (NamingException ex) {
        
            return;
        }
        
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