package org.jabref.gui.copyfiles;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;

public class CopyFilesDialogViewModel extends AbstractViewModel {

    private final SimpleListProperty<CopyFilesResultItemViewModel> copyFilesResultItems = new SimpleListProperty<>(
            FXCollections.observableArrayList());

    public CopyFilesDialogViewModel(CopyFilesResultListDependency results) {
        copyFilesResultItems.addAll(results.getResults());
    }

    public SimpleListProperty<CopyFilesResultItemViewModel> copyFilesResultListProperty() {
        return this.copyFilesResultItems;
    }

}
