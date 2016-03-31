/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.sed.msh.web.admin.pmode;

import com.sun.javafx.scene.traversal.Direction;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import org.primefaces.model.diagram.Connection;
import org.primefaces.model.diagram.DefaultDiagramModel;
import org.primefaces.model.diagram.DiagramModel;
import org.primefaces.model.diagram.Element;
import org.primefaces.model.diagram.connector.FlowChartConnector;
import org.primefaces.model.diagram.connector.StraightConnector;
import org.primefaces.model.diagram.endpoint.BlankEndPoint;
import org.primefaces.model.diagram.endpoint.EndPoint;
import org.primefaces.model.diagram.endpoint.EndPointAnchor;
import org.primefaces.model.diagram.overlay.ArrowOverlay;
import org.primefaces.model.diagram.overlay.LabelOverlay;

@ManagedBean(name = "businessTransaction")
@SessionScoped
public class BusinessTransaction implements Serializable {

    private DefaultDiagramModel model;

    private boolean suspendEvent;

    @PostConstruct
    public void init() {
        model = new DefaultDiagramModel();
        model.setMaxConnections(-1);

        StraightConnector connector = new StraightConnector();
        connector.setPaintStyle("{strokeStyle:'#98AFC7', lineWidth:3}");
        connector.setHoverPaintStyle("{strokeStyle:'#5C738B'}");
        model.setDefaultConnector(connector);

        Element sender = new Element("sender", "20px", "20px");
        sender.setDraggable(false);
        Element receiver = new Element("receiver", "200px", "20px");
        receiver.setDraggable(false);
        sender.addEndPoint(new BlankEndPoint(EndPointAnchor.RIGHT));
        receiver.addEndPoint(new BlankEndPoint(EndPointAnchor.RIGHT));

        model.addElement(sender);
        model.addElement(receiver);
       
        createAction("DeliveryNotification", 0, 1);
        createAction("AdviceOfDelivery", 1, -1);
        createAction("FictionNotification", 2, 1);

    }

    public DiagramModel getModel() {
        return model;
    }

    private void createAction(String action, int i, int direction) {
        String y = (62 + 40 * i) + "px";
        Element s1 = new Element("", "60px", y);
        s1.setDraggable(false);
        s1.setStyleClass("ui-diagram-seq-line");
        s1.addEndPoint(new BlankEndPoint(EndPointAnchor.RIGHT));

        Element r1 = new Element("", "240px", y);
        r1.setDraggable(false);
        r1.setStyleClass("ui-diagram-seq-line");
        r1.addEndPoint(new BlankEndPoint(EndPointAnchor.LEFT));

        model.connect(createConnection(s1.getEndPoints().get(0), r1.getEndPoints().get(0), action, direction));
        model.addElement(s1);
        model.addElement(r1);
    }
    
    public void addAction(){
        createAction("NewAction", 3, 0);
    }

    private Connection createConnection(EndPoint from, EndPoint to, String label,  int direction ) {
        Connection conn = new Connection(from, to);
        switch (direction) {
            case  1: 
                conn.getOverlays().add(new ArrowOverlay(20, 20,1,1));
            break;
            case  -1: 
                conn.getOverlays().add(new ArrowOverlay(20, 20,0,-1));
            break;
            case  0: 
                conn.getOverlays().add(new ArrowOverlay(20, 20,1,1));
                conn.getOverlays().add(new ArrowOverlay(20, 20,0,-1));
            break;
        }

        if (label != null) {
            conn.getOverlays().add(new LabelOverlay(label, "flow-label",0.5));
        }
        return conn;
    }

}
