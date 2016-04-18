/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.abst;

import java.util.List;

/**
 *
 * @author sluzba
 */
abstract public class AbstractAdminJSFView <T> extends AbstractJSFView {
    
    
    private T mtNew;
    private T mtEditable;
    private T mtSelected;
    
    abstract public void createEditable();
    abstract public void removeSelected();    
    abstract public void persistEditable();
    abstract public void updateEditable();    
    abstract public List<T> getList();

    public T getSelected() {
        return mtSelected;
    }
    
    public void setSelected(T slct) {
        this.mtSelected = slct;
    }
        
    public void startEditSelected(){
        setEditable(getSelected());
    }
    
    public T getEditable() {
        return mtEditable;
    }
    
    public void setEditable(T edtbl) {
        this.mtEditable = edtbl;
    }
    
     public T getNew() {
        return mtNew;
    }
    
    public void setNew(T edtbl) {
        this.mtNew = edtbl;
        setEditable(edtbl);
    }
   
    
    public void addOrUpdateEditable(){
        if (isEditableNew()){
            persistEditable();
            setNew(null);
        } else {
            updateEditable();
            setEditable(null);
        }
    };
    
    
     public boolean isEditableNew() {
        return getEditable()!=null && getEditable() == getNew();
    }

    
    
    
    
    
    
}
