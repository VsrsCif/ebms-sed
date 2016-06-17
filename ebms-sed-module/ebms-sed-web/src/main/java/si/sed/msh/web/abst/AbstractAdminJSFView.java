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
abstract public class AbstractAdminJSFView<T> extends AbstractJSFView {

    private T mtEditable;
    private T mtNew;
    private T mtSelected;

    public void addOrUpdateEditable() {
        if (isEditableNew()) {
            persistEditable();
            setNew(null);
        } else {
            updateEditable();
            setEditable(null);
        }
    }

    abstract public void createEditable();

    public T getEditable() {
        return mtEditable;
    }

    abstract public List<T> getList();

    public T getNew() {
        return mtNew;
    }

    public T getSelected() {
        return mtSelected;
    }

    public boolean isEditableNew() {
        return getEditable() != null && getEditable() == getNew();
    }

    abstract public void persistEditable();

    abstract public void removeSelected();

    public void setEditable(T edtbl) {
        this.mtEditable = edtbl;
    }

    public void setNew(T edtbl) {
        this.mtNew = edtbl;
        setEditable(edtbl);
    }

    public void setSelected(T slct) {
        this.mtSelected = slct;
    }

    ;
    public void startEditSelected() {
        setEditable(getSelected());
    }

    abstract public void updateEditable();

}
