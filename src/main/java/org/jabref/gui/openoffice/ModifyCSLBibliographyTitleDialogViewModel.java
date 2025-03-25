package org.jabref.gui.openoffice;

import java.util.Arrays;

import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.openoffice.OpenOfficePreferences;

import static org.jabref.logic.openoffice.oocsltext.CSLFormatUtils.Format;

public class ModifyCSLBibliographyTitleDialogViewModel {

    private final StringProperty cslBibliographyTitle = new SimpleStringProperty();
    private final StringProperty cslBibliographyHeaderSelectedFormat = new SimpleStringProperty();
    private final ReadOnlyListProperty<String> formatListProperty =
             new ReadOnlyListWrapper<>(FXCollections.observableArrayList(
                    Arrays.stream(Format.values()).map(Format::getFormat).toList()
            ));

    public ModifyCSLBibliographyTitleDialogViewModel(OpenOfficePreferences preferences) {
        this.cslBibliographyTitle.set(preferences.cslBibliographyTitle().get());
        this.cslBibliographyHeaderSelectedFormat.set(preferences.cslBibliographyHeaderFormat().get());

        cslBibliographyTitle.bindBidirectional(preferences.cslBibliographyTitle());
        cslBibliographyHeaderSelectedFormat.bindBidirectional(preferences.cslBibliographyHeaderFormat());
    }

    public StringProperty cslBibliographyTitle() {
        return cslBibliographyTitle;
    }

    public StringProperty cslBibliographyHeaderSelectedFormat() {
        return this.cslBibliographyHeaderSelectedFormat;
    }

    public ReadOnlyListProperty<String> formatListProperty() {
        return this.formatListProperty;
    }
}
