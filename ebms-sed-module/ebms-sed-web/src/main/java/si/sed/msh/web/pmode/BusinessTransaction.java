/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.pmode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.primefaces.model.diagram.Connection;
import org.primefaces.model.diagram.DefaultDiagramModel;
import org.primefaces.model.diagram.DiagramModel;
import org.primefaces.model.diagram.Element;
import org.primefaces.model.diagram.connector.StraightConnector;
import org.primefaces.model.diagram.endpoint.BlankEndPoint;
import org.primefaces.model.diagram.endpoint.EndPoint;
import org.primefaces.model.diagram.endpoint.EndPointAnchor;
import org.primefaces.model.diagram.overlay.ArrowOverlay;
import org.primefaces.model.diagram.overlay.LabelOverlay;
import si.sed.msh.web.gui.entities.BTAction;

@ManagedBean(name = "businessTransaction")
@SessionScoped
public class BusinessTransaction implements Serializable {

    List<BTAction> mlst = new ArrayList<>();

    private boolean suspendEvent;

    @PostConstruct
    public void init() {
       mlst.add(new BTAction("DeliveryNotify", "sq-right", "sq-left-arrow"));
       mlst.add(new BTAction("AdviceOfDelivery", "sq-right-arrow", "sq-left"));
       mlst.add(new BTAction("DeliveryFiction", "sq-right", "sq-left-arrow"));
    }

 
    
    public void addAction(){
        
    }
    
    public List<BTAction> getActions(){
        return mlst;
    }


}
