package org.jabref.gui.openoffice;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.WriterParagraphStyles;

public class ModifyBibliographyPropertiesDialogViewModel {

    private final StringProperty cslBibliographyTitle = new SimpleStringProperty();
    private final StringProperty cslBibliographySelectedHeaderFormat = new SimpleStringProperty();
    private final StringProperty cslBibliographySelectedBodyFormat = new SimpleStringProperty();
    private final ReadOnlyListProperty<String> headerFormatListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(WriterParagraphStyles.BIBLIOGRAPHY_TITLE_FORMATS));
    private final ReadOnlyListProperty<String> bodyFormatListProperty =
            new ReadOnlyListWrapper<>(FXCollections.observableArrayList(WriterParagraphStyles.BIBLIOGRAPHY_BODY_FORMATS));

    private final OpenOfficePreferences openOfficePreferences;
    private final BooleanProperty shouldDisableHeadingControls;

    public ModifyBibliographyPropertiesDialogViewModel(OpenOfficePreferences openOfficePreferences) {
        this.openOfficePreferences = openOfficePreferences;
        this.shouldDisableHeadingControls = new SimpleBooleanProperty();
        this.shouldDisableHeadingControls.bind(Bindings.createBooleanBinding(
                () -> openOfficePreferences.getCurrentStyle() instanceof CitationStyle && openOfficePreferences.getZoteroCompatibilityMode(),
                openOfficePreferences.currentStyleProperty(), openOfficePreferences.zoteroCompatibilityModeProperty()));

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

    public BooleanProperty shouldDisableHeadingControlsProperty() {
        return shouldDisableHeadingControls;
    }
}
