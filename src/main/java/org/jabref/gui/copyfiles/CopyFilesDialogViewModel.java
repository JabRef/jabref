package org.jabref.gui.copyfiles;

import java.util.List;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;

public class CopyFilesDialogViewModel extends AbstractViewModel {

    private final SimpleListProperty<CopyFilesResultItemViewModel> copyFilesResultItems = new SimpleListProperty<>(
            FXCollections.observableArrayList());

    //TODO: How do I add the data here?
    public CopyFilesDialogViewModel(List<CopyFilesResultItemViewModel> items) {

        this.copyFilesResultItems.addAll(items);
    }

    public CopyFilesDialogViewModel() {
        // TODO Auto-generated constructor stub
    }

    public SimpleListProperty<CopyFilesResultItemViewModel> abbreviationsProperty() {
        return this.copyFilesResultItems;
    }

}
