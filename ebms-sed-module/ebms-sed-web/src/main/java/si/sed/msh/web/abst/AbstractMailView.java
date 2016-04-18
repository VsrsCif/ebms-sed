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
package si.sed.msh.web.abst;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.faces.event.ActionEvent;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Jože Rihtaršič
 */
public abstract class AbstractMailView<T, S> {
    
    
    protected static final SimpleDateFormat SDF_DDMMYYY_HH_MM_SS = new SimpleDateFormat("dd.MM.YYYY HH:mm:ss");
    protected int mTabActiveIndex = 0;

    protected T mMail;    
    protected LazyDataModel<T> mMailModel = null;
    protected List<S> mlstMailEvents = null;

   
    
    abstract public String getStatusColor(String status);
    abstract public void updateEventList() ;
    abstract public StreamedContent getFile(BigInteger bi);
    
     public LazyDataModel<T> getMailList(){
         return mMailModel;
    }

    public T getCurrentMail() {
        return mMail;
    }

    public void setCurrentMail(T mail) {
        this.mMail = mail;
        updateEventList();
    }

    public List<S> getMailEvents() {
        return mlstMailEvents;
    }

    public void onRowSelect(SelectEvent event) {
        if (event!=null) {
            setCurrentMail((T) event.getObject());
        }else {
            setCurrentMail(null);
        }
    }

    public void onRowUnselect(UnselectEvent event) {
        setCurrentMail(null);
    }

    public String formatDate(Date date) {
        return SDF_DDMMYYY_HH_MM_SS.format(date);
    }

    public int rowIndex(T om) {
        return mMailModel.getRowIndex();
    }


    public void search(ActionEvent event) {
        String res = (String) event.getComponent().getAttributes().get("status");
        System.out.println("Res:" + res);
    }

    public void setTabActiveIndex(int itindex) {
        mTabActiveIndex = itindex;
    }

    public int getTabActiveIndex() {
        return mTabActiveIndex;
    }
    
    public void onTabChange(TabChangeEvent event) {
        if (event!=null) {
            TabView tv = (TabView) event.getComponent();
            mTabActiveIndex = tv.getActiveIndex();
        } else {
            mTabActiveIndex = 0;
        }
    }



 
}
