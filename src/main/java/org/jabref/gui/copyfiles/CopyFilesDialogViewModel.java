package org.jabref.gui.copyfiles;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.model.database.BibDatabaseContext;

public class CopyFilesDialogViewModel extends AbstractViewModel {

    private final SimpleListProperty<CopyFilesResultItemViewModel> copyFilesResultItems = new SimpleListProperty<>(
            FXCollections.observableArrayList());

    //TODO: The results is empty!
    public CopyFilesDialogViewModel(CopyFilesResultListDependency results, BibDatabaseContext bibdatabasecontext) {

        copyFilesResultItems.addAll(results.getResults());
    }

    public SimpleListProperty<CopyFilesResultItemViewModel> abbreviationsProperty() {
        return this.copyFilesResultItems;
    }

}
