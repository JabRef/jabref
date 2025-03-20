package org.jabref.gui.openoffice;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.oocsltext.Format;

public class ModifyBibliographyTitleDialogViewModel {

    private final StringProperty bibliographyTitle = new SimpleStringProperty("");
    private final ObjectProperty<Format> selectedFormat = new SimpleObjectProperty<>();
    private final ReadOnlyListProperty<Format> formatListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(Format.values()));

    public ModifyBibliographyTitleDialogViewModel(OpenOfficePreferences preferences) {
        this.bibliographyTitle.set(preferences.cslBibliographyTitle().get());
        this.selectedFormat.set(preferences.cslHeaderFormat().get());

        bibliographyTitle.bindBidirectional(preferences.cslBibliographyTitle());
        selectedFormat.bindBidirectional(preferences.cslHeaderFormat());
    }

    public StringProperty cslBibliographyTitle() {
        return bibliographyTitle;
    }

    public ObjectProperty<Format> cslSelectedFormat() {
        return this.selectedFormat;
    }

    public ReadOnlyListProperty<Format> formatListProperty() {
        return this.formatListProperty;
    }
}
