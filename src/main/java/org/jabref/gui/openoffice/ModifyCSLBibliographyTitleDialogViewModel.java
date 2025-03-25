package org.jabref.gui.openoffice;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.openoffice.OpenOfficePreferences;

import static org.jabref.logic.openoffice.oocsltext.CSLFormatUtils.Format;

public class ModifyCSLBibliographyTitleDialogViewModel {

    private final StringProperty cslBibliographyTitle = new SimpleStringProperty("");
    private final ObjectProperty<Format> cslBibliographyHeaderSelectedFormat = new SimpleObjectProperty<>();
    private final ReadOnlyListProperty<Format> formatListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(Format.values()));

    public ModifyCSLBibliographyTitleDialogViewModel(OpenOfficePreferences preferences) {
        this.cslBibliographyTitle.set(preferences.cslBibliographyTitle().get());
        this.cslBibliographyHeaderSelectedFormat.set(preferences.cslBibliographyHeaderFormat().get());

        cslBibliographyTitle.bindBidirectional(preferences.cslBibliographyTitle());
        cslBibliographyHeaderSelectedFormat.bindBidirectional(preferences.cslBibliographyHeaderFormat());
    }

    public StringProperty cslBibliographyTitle() {
        return cslBibliographyTitle;
    }

    public ObjectProperty<Format> cslBibliographyHeaderSelectedFormat() {
        return this.cslBibliographyHeaderSelectedFormat;
    }

    public ReadOnlyListProperty<Format> formatListProperty() {
        return this.formatListProperty;
    }
}
