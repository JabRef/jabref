package net.sf.jabref.gui.prrv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.event.Event;
import javafx.scene.control.Label;
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

    private List<Label> labelList = new ArrayList<>();


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

        while (it.hasNext()) {
            VisualItem item = (VisualItem) it.next();
            if (item instanceof NodeItem) {
                System.out.println(item.get("bibtexkey"));
            }
        }
    }

    public void showNodeKey(VisualItem item, FxDisplay display, BorderPane pane) {
        String msg = item.get("bibtexkey").toString();
        Label label = new Label(msg);
        label.getStyleClass().add("outline");
        label.translateXProperty().bind(item.xProperty());
        label.translateYProperty().bind(item.yProperty());
        label.setStyle("-fx-fill: lightseagreen;" +
                "-fx-stroke: firebrick;" +
                "-fx-stroke-width: 2px;");
        labelList.add(label);
        display.getChildren().addAll(label);
    }

    public void hideNodeKeys() {
        labelList.forEach(Label -> {
            Label.setVisible(false);
        });
    }

    public void viewOnSelectedBibTexKey(String bibtexKey, FxDisplay display, BorderPane root) {
        Iterator<VisualItem> it = display.getVisualization().items();
        while (it.hasNext()) {
            if (it.hasNext()) {
                VisualItem item = it.next();
                if (item instanceof NodeItem) {
                    if (item.get("bibtexkey").toString().matches(bibtexKey)) {
                        display.setTranslateX(-item.getX());
                        display.setTranslateY(-item.getY());
                        display.computeAreaInScreen();
                    }
                }
            }
        }
    }


}
