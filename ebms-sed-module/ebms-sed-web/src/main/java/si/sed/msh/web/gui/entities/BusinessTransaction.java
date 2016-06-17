/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui.entities;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sluzba
 */
public class BusinessTransaction {

    String description;
    String initiatorRole;
    List<BTAction> mlstActionList = new ArrayList<>();
    String name;
    String responderRole;

    public BusinessTransaction(String name, String initiatorRole, String responderRole) {
        this.name = name;
        this.initiatorRole = initiatorRole;
        this.responderRole = responderRole;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
