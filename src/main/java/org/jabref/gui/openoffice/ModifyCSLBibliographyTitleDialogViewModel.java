package org.jabref.gui.openoffice;

import java.util.Arrays;

import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.oocsltext.CSLFormatUtils;

public class ModifyCSLBibliographyTitleDialogViewModel {

    private final StringProperty cslBibliographyTitle = new SimpleStringProperty();
    private final StringProperty cslBibliographySelectedHeaderFormat = new SimpleStringProperty();
    private final ReadOnlyListProperty<String> formatListProperty =
             new ReadOnlyListWrapper<>(FXCollections.observableArrayList(
                    Arrays.stream(CSLFormatUtils.Format.values()).map(CSLFormatUtils.Format::getFormat).toList()
            ));

    public ModifyCSLBibliographyTitleDialogViewModel(OpenOfficePreferences preferences) {
        this.cslBibliographyTitle.set(preferences.getCslBibliographyTitle());
        this.cslBibliographySelectedHeaderFormat.set(preferences.getCslBibliographyHeaderFormat());

        cslBibliographyTitle.bindBidirectional(preferences.cslBibliographyTitleProperty());
        cslBibliographySelectedHeaderFormat.bindBidirectional(preferences.cslBibliographyHeaderFormatProperty());
    }

    public StringProperty cslBibliographyTitleProperty() {
        return cslBibliographyTitle;
    }

    public StringProperty cslBibliographySelectedHeaderFormatProperty() {
        return cslBibliographySelectedHeaderFormat;
    }

    public ReadOnlyListProperty<String> formatListProperty() {
        return this.formatListProperty;
    }
}
