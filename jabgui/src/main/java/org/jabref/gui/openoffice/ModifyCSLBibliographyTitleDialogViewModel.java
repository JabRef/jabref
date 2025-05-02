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
    private final StringProperty cslBibliographySelectedBodyFormat = new SimpleStringProperty();
    private final ReadOnlyListProperty<String> formatListProperty =
             new ReadOnlyListWrapper<>(FXCollections.observableArrayList(
                    Arrays.stream(CSLFormatUtils.Format.values()).map(CSLFormatUtils.Format::getFormat).toList()
            ));

    public ModifyCSLBibliographyTitleDialogViewModel(OpenOfficePreferences openOfficePreferences) {
        this.cslBibliographyTitle.set(openOfficePreferences.getCslBibliographyTitle());
        this.cslBibliographySelectedHeaderFormat.set(openOfficePreferences.getCslBibliographyHeaderFormat());
        this.cslBibliographySelectedBodyFormat.set(openOfficePreferences.getCslBibliographyBodyFormat());

        cslBibliographyTitle.bindBidirectional(openOfficePreferences.cslBibliographyTitleProperty());
        cslBibliographySelectedHeaderFormat.bindBidirectional(openOfficePreferences.cslBibliographyHeaderFormatProperty());
        cslBibliographySelectedBodyFormat.bindBidirectional(openOfficePreferences.cslBibliographyBodyFormatProperty());
    }

    public StringProperty cslBibliographyTitleProperty() {
        return cslBibliographyTitle;
    }

    public StringProperty cslBibliographySelectedHeaderFormatProperty() {
        return cslBibliographySelectedHeaderFormat;
    }

    public StringProperty cslBibliographySelectedBodyFormatProperty() {
        return cslBibliographySelectedBodyFormat;
    }

    public ReadOnlyListProperty<String> formatListProperty() {
        return formatListProperty;
    }
}
