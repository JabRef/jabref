package org.jabref.gui.maintable;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;

import org.jabref.gui.icon.JabRefIcon;

public class MainTableColumn<T> extends TableColumn<BibEntryTableViewModel, T> {

    private MainTableColumnModel model;

    private final Optional<JabRefIcon> iconLabel;

    public MainTableColumn(MainTableColumnModel model) {
        this.model = model;
        this.iconLabel = Optional.empty();
    }

    public MainTableColumnModel getModel() { return model; }

    public Node getHeaderLabel() {
        if (model.getType() == MainTableColumnModel.Type.GROUPS || model.getType() == MainTableColumnModel.Type.FILES) {
            return iconLabel.map(JabRefIcon::getGraphicNode).get();
        } else {
            return new Label(model.getDisplayName());
        }
    }

    public String getDisplayName() {
        return model.getDisplayName();
    }
}
