package org.jabref.gui.openoffice;

import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.oocsltext.CSLFormatUtils;

public class ModifyCSLBibliographyPropertiesDialogViewModel {

    private final StringProperty cslBibliographyTitle = new SimpleStringProperty();
    private final StringProperty cslBibliographySelectedHeaderFormat = new SimpleStringProperty();
    private final StringProperty cslBibliographySelectedBodyFormat = new SimpleStringProperty();
    private final ReadOnlyListProperty<String> headerFormatListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(CSLFormatUtils.BIBLIOGRAPHY_TITLE_FORMATS));
    private final ReadOnlyListProperty<String> bodyFormatListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(CSLFormatUtils.BIBLIOGRAPHY_BODY_FORMATS));

    private final OpenOfficePreferences openOfficePreferences;

    public ModifyCSLBibliographyPropertiesDialogViewModel(OpenOfficePreferences openOfficePreferences) {
        this.openOfficePreferences = openOfficePreferences;

        this.cslBibliographyTitle.set(openOfficePreferences.getCslBibliographyTitle());
        this.cslBibliographySelectedHeaderFormat.set(openOfficePreferences.getCslBibliographyHeaderFormat());
        this.cslBibliographySelectedBodyFormat.set(openOfficePreferences.getCslBibliographyBodyFormat());
    }

    public void savePreferences() {
        openOfficePreferences.setCslBibliographyTitle(cslBibliographyTitle.get());
        openOfficePreferences.setCslBibliographyHeaderFormat(cslBibliographySelectedHeaderFormat.get());
        openOfficePreferences.setCslBibliographyBodyFormat(cslBibliographySelectedBodyFormat.get());
    }

    public StringProperty cslBibliographyTitleProperty() {
        return cslBibliographyTitle;
    }

    public StringProperty cslBibliographySelectedHeaderFormatProperty() {
        return cslBibliographySelectedHeaderFormat;
    }

    public ReadOnlyListProperty<String> headerFormatListProperty() {
        return headerFormatListProperty;
    }

    public StringProperty cslBibliographySelectedBodyFormatProperty() {
        return cslBibliographySelectedBodyFormat;
    }

    public ReadOnlyListProperty<String> bodyFormatListProperty() {
        return bodyFormatListProperty;
    }
}
