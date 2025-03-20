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

public class ModifyCSLBibliographyTitleDialogViewModel {

    private final StringProperty clsBibliographyTitle = new SimpleStringProperty("");
    private final ObjectProperty<Format> cslSelectedFormat = new SimpleObjectProperty<>();
    private final ReadOnlyListProperty<Format> formatListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(Format.values()));

    public ModifyCSLBibliographyTitleDialogViewModel(OpenOfficePreferences preferences) {
        this.clsBibliographyTitle.set(preferences.cslBibliographyTitle().get());
        this.cslSelectedFormat.set(preferences.cslHeaderFormat().get());

        clsBibliographyTitle.bindBidirectional(preferences.cslBibliographyTitle());
        cslSelectedFormat.bindBidirectional(preferences.cslHeaderFormat());
    }

    public StringProperty cslBibliographyTitle() {
        return clsBibliographyTitle;
    }

    public ObjectProperty<Format> cslSelectedFormat() {
        return this.cslSelectedFormat;
    }

    public ReadOnlyListProperty<Format> formatListProperty() {
        return this.formatListProperty;
    }
}
