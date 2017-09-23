package org.jabref.gui.filelist;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.model.database.BibDatabaseContext;

public class FileListDialogViewModel extends AbstractViewModel {

    private final StringProperty linkProperty = new SimpleStringProperty("");
    private final StringProperty descriptionProperty = new SimpleStringProperty("");
    private final ListProperty<ExternalFileType> externalfilesTypes = new SimpleListProperty<>(FXCollections.emptyObservableList());
    private final BibDatabaseContext bibDatabaseContext;

    public FileListDialogViewModel(BibDatabaseContext bibDatabaseContext) {

        this.bibDatabaseContext = bibDatabaseContext;
        externalfilesTypes.set(FXCollections.observableArrayList(ExternalFileTypes.getInstance().getExternalFileTypeSelection()));


    }
    //


    public StringProperty linkProperty() {
        return linkProperty;
    }

    public StringProperty descriptionProperty() {
        return descriptionProperty;
    }

    public ListProperty<ExternalFileType> externalFileTypeProperty() {
        return externalfilesTypes;
    }

}
