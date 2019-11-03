package org.jabref.gui.maintable;

import javafx.scene.control.TableColumn;

public class MainTableColumn<T> extends TableColumn<BibEntryTableViewModel, T> {

    private MainTableColumnModel model;

    public MainTableColumn(MainTableColumnModel model) {
        this.model = model;
    }

    public MainTableColumnModel getModel() { return model; }

    public String getDisplayName() { return model.getDisplayName(); }
}
