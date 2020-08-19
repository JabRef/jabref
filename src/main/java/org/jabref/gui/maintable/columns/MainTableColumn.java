package org.jabref.gui.maintable.columns;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;

import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableColumnModel;
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

        BindingsHelper.bindBidirectional(
                this.sortTypeProperty(),
                (ObservableValue<SortType>) model.sortTypeProperty(),
                value -> this.setSortType(model.sortTypeProperty().getValue()),
                value -> model.sortTypeProperty().setValue(this.getSortType()));
    }

    public MainTableColumnModel getModel() {
        return model;
    }

    public String getDisplayName() {
        return model.getDisplayName();
    }
}
