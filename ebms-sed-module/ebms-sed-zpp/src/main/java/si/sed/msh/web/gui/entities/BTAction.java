/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.gui.entities;

/**
 *
 * @author sluzba
 */
public class BTAction {
    String name;
    
    String leftEnd;
    String rightEnd;

    public BTAction(String name, String leftEnd, String rightEnd) {
        this.name = name;
        this.leftEnd = leftEnd;
        this.rightEnd = rightEnd;
    }

    
    
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLeftEnd() {
        return leftEnd;
    }

    public void setLeftEnd(String leftEnd) {
        this.leftEnd = leftEnd;
    }

    public String getRightEnd() {
        return rightEnd;
    }

    public void setRightEnd(String rightEnd) {
        this.rightEnd = rightEnd;
    }
    
    
    
    
}
