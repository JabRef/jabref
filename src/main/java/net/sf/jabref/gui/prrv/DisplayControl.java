package net.sf.jabref.gui.prrv;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import prefux.FxDisplay;
import prefux.visual.NodeItem;
import prefux.visual.VisualItem;


/**
 * Handle actions on prrv runtime environment
 *
 */
public class DisplayControl {

    // Stores bibtexkey of each node
    private List<Label> labelList = new ArrayList<>();

    /**
     * Displays labels, containing bibtexkey
     * @param item      Show bibtexkey of item
     * @param fxDisplay   On fxdisplay
     */
    public void showNodeKey(VisualItem item, FxDisplay fxDisplay) {
        String msg = item.get("bibtexkey").toString();
        Label label = new Label(msg);
        label.translateXProperty().bind(item.xProperty());
        label.translateYProperty().bind(item.yProperty());
        label.setStyle("-fx-stroke-width: 2px;" +
                "-fx-stroke-type: outside;");
        labelList.add(label);
        fxDisplay.getChildren().addAll(label);
    }

    /**
     * Hide all labels
     */
    public void hideNodeKeys() {
        labelList.forEach(Label -> {
            Label.setVisible(false);
        });
    }

    /**
     * Set position of the window on node of selected bibtexentry
     * @param bibtexKey
     * @param display
     * @param root
     */
    public void viewOnSelectedBibTexKey(String bibtexKey, FxDisplay display, BorderPane root) {
        Iterator<VisualItem> it = display.getVisualization().items();
        while (it.hasNext()) {
            if (it.hasNext()) {
                VisualItem item = it.next();
                if (item instanceof NodeItem) {
                    if (item.get("bibtexkey").toString().matches(bibtexKey)) {
                        display.setTranslateX(-item.getX());
                        display.setTranslateY(-item.getY()-50);
                        display.computeAreaInScreen();
                    }
                }
            }
        }
    }


}
