package org.jabref.gui.openoffice;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.openoffice.OpenOfficePreferences;

public class ModifyBibliographyTitleDialogViewModel {

    public final StringProperty bibliographyTitle = new SimpleStringProperty("");
    public final ObjectProperty<Formats> selectedFormat = new SimpleObjectProperty<>();
    private final ReadOnlyListProperty<Formats> formatListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(Formats.values()));

    public ModifyBibliographyTitleDialogViewModel(OpenOfficePreferences preferences) {
        this.bibliographyTitle.set(preferences.bibliographyTitle().get());
        this.selectedFormat.set(preferences.headerFormat().get());

        bibliographyTitle.bindBidirectional(preferences.bibliographyTitle());
        selectedFormat.bindBidirectional(preferences.headerFormat());
    }

    public ObjectProperty<Formats> selectedFormat() {
        return this.selectedFormat;
    }

    public ReadOnlyListProperty<Formats> formatListProperty() {
        return this.formatListProperty;
    }
}
