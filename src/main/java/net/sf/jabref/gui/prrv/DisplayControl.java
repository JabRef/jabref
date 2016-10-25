package net.sf.jabref.gui.prrv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.event.Event;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;

import prefux.FxDisplay;
import prefux.Visualization;
import prefux.controls.ControlAdapter;
import prefux.data.Table;
import prefux.data.event.TableListener;
import prefux.visual.NodeItem;
import prefux.visual.VisualItem;


/**
 * Created by Daniel on 10/13/2016.
 */
public class DisplayControl extends ControlAdapter implements TableListener {

    private List<Tooltip> toolTipList = new ArrayList<>();


    @Override
    public void tableChanged(Table table, int i, int i1, int i2, int i3) {


    }

    @Override
    public void itemEvent(VisualItem item, Event e) {

        System.out.println(item.get("bibtexkey").toString());

    }

    public void changes(FxDisplay fdx) {
        Visualization vis = fdx.getVisualization();
        Iterator it = vis.items();

        while(it.hasNext()) {
            VisualItem item = (VisualItem) it.next();
            if (item instanceof NodeItem) {
                System.out.println(item.get("bibtexkey"));
            }
        }
    }

    public void showNodeKey(VisualItem item, FxDisplay display, BorderPane pane) {
        // TODO Instead tooltip -> label with border
/*
            Tooltip mousePositionToolTip = new Tooltip("");
            toolTipList.add(mousePositionToolTip);
            System.out.println("parent"+ display.localToScreen(item.getX(),item.getY()));
            String msg = item.get("bibtexkey").toString();
            mousePositionToolTip.setGraphicTextGap(0);
            mousePositionToolTip.setText(msg);
            Node node = (Node) item.getNode();
            mousePositionToolTip.show(pane.getScene().getWindow(),
                    display.localToScreen(item.getX(),item.getY()).getX() ,
                    display.localToScreen(item.getX(),item.getY()).getY());
//            mousePositionToolTip.xProperty().doubleExpression(node.translateXProperty());
  //          mousePositionToolTip.yProperty().doubleExpression(node.translateYProperty());
*/
// ******************* NEW TRY
        String msg = item.get("bibtexkey").toString();
        Label label = new Label(msg);
        //label.getStyleClass().add("outline");
        label.translateXProperty().bind(item.xProperty());
        label.translateYProperty().bind(item.yProperty());
        label.setStyle("-fx-fill: lightseagreen;" +
                "-fx-stroke: firebrick;" +
                "-fx-stroke-width: 2px;");
//pane.getChildren().addAll(label);
display.getChildren().addAll(label);
       // display.repaint();

            //mousePositionToolTip.xProperty().doubleExpression(((ObservableDoubleValue) display.localToScreen(item.getX(), item.getY())));
            //mousePositionToolTip.yProperty().doubleExpression(((ObservableDoubleValue) display.localToScreen(item.getX(), item.getY())));



        }

    public void hideNodeKeys() {
        toolTipList.forEach(Tooltip -> {
            Tooltip.hide();
        });
    }

    public void viewOnSelectedBibTexKey(String bibtexKey) {

    }



}
