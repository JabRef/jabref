package org.jabref.gui.maintable;

import javafx.scene.control.TableColumn;

import org.jabref.gui.util.BindingsHelper;

public class MainTableColumn<T> extends TableColumn<BibEntryTableViewModel, T> {

    private MainTableColumnModel model;

    public MainTableColumn(MainTableColumnModel model) {
        this.model = model;

        BindingsHelper.bindBidirectional(
                this.widthProperty(),
                model.widthProperty(),
                value -> this.setPrefWidth(model.widthProperty().getValue()),
                value -> model.widthProperty().setValue(this.getWidth()));
    }

    public MainTableColumnModel getModel() { return model; }

    public String getDisplayName() { return model.getDisplayName(); }
}
