package org.jabref.gui.preferences.general;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Encodings;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.TelemetryPreferences;

public class GeneralTabViewModel implements PreferenceTabViewModel {
    private final ListProperty<Language> languagesListProperty = new SimpleListProperty<>();
    private final ObjectProperty<Language> selectedLanguageProperty = new SimpleObjectProperty<>();
    private final ListProperty<Charset> encodingsListProperty = new SimpleListProperty<>();
    private final ObjectProperty<Charset> selectedEncodingProperty = new SimpleObjectProperty<>();
    private final ListProperty<BibDatabaseMode> bibliographyModeListProperty = new SimpleListProperty<>();
    private final ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty = new SimpleObjectProperty<>();

    private final BooleanProperty inspectionWarningDuplicateProperty = new SimpleBooleanProperty();
    private final BooleanProperty confirmDeleteProperty = new SimpleBooleanProperty();
    private final BooleanProperty memoryStickModeProperty = new SimpleBooleanProperty();
    private final BooleanProperty collectTelemetryProperty = new SimpleBooleanProperty();
    private final BooleanProperty showAdvancedHintsProperty = new SimpleBooleanProperty();
    private final BooleanProperty markOwnerProperty = new SimpleBooleanProperty();
    private final StringProperty markOwnerNameProperty = new SimpleStringProperty("");
    private final BooleanProperty markOwnerOverwriteProperty = new SimpleBooleanProperty();
    private final BooleanProperty addCreationDateProperty = new SimpleBooleanProperty();
    private final BooleanProperty addModificationDateProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final GeneralPreferences generalPreferences;
    private final PreferencesService preferencesService;
    private final TelemetryPreferences telemetryPreferences;
    private final OwnerPreferences ownerPreferences;
    private final TimestampPreferences timestampPreferences;

    private List<String> restartWarning = new ArrayList<>();

    @SuppressWarnings("ReturnValueIgnored")
    public GeneralTabViewModel(DialogService dialogService, PreferencesService preferencesService, GeneralPreferences generalPreferences, TelemetryPreferences telemetryPreferences, OwnerPreferences ownerPreferences, TimestampPreferences timestampPreferences) {
        this.dialogService = dialogService;
        this.generalPreferences = generalPreferences;
        this.preferencesService = preferencesService;
        this.telemetryPreferences = telemetryPreferences;
        this.ownerPreferences = ownerPreferences;
        this.timestampPreferences = timestampPreferences;
    }

    public void setValues() {
        languagesListProperty.setValue(new SortedList<>(FXCollections.observableArrayList(Language.values()), Comparator.comparing(Language::getDisplayName)));
        selectedLanguageProperty.setValue(preferencesService.getLanguage());

        encodingsListProperty.setValue(FXCollections.observableArrayList(Encodings.getCharsets()));
        selectedEncodingProperty.setValue(generalPreferences.getDefaultEncoding());

        bibliographyModeListProperty.setValue(FXCollections.observableArrayList(BibDatabaseMode.values()));
        selectedBiblatexModeProperty.setValue(generalPreferences.getDefaultBibDatabaseMode());

        inspectionWarningDuplicateProperty.setValue(generalPreferences.warnAboutDuplicatesInInspection());
        confirmDeleteProperty.setValue(generalPreferences.shouldConfirmDelete());
        memoryStickModeProperty.setValue(generalPreferences.isMemoryStickMode());
        collectTelemetryProperty.setValue(telemetryPreferences.shouldCollectTelemetry());
        showAdvancedHintsProperty.setValue(generalPreferences.shouldShowAdvancedHints());

        markOwnerProperty.setValue(ownerPreferences.isUseOwner());
        markOwnerNameProperty.setValue(ownerPreferences.getDefaultOwner());
        markOwnerOverwriteProperty.setValue(ownerPreferences.isOverwriteOwner());

        addCreationDateProperty.setValue(timestampPreferences.shouldAddCreationDate());
        addModificationDateProperty.setValue(timestampPreferences.shouldAddModificationDate());
    }

    public void storeSettings() {
        Language newLanguage = selectedLanguageProperty.getValue();
        if (newLanguage != preferencesService.getLanguage()) {
            preferencesService.setLanguage(newLanguage);
            Localization.setLanguage(newLanguage);
            restartWarning.add(Localization.lang("Changed language") + ": " + newLanguage.getDisplayName());
        }

        if (generalPreferences.isMemoryStickMode() && !memoryStickModeProperty.getValue()) {
            dialogService.showInformationDialogAndWait(Localization.lang("Memory stick mode"),
                    Localization.lang("To disable the memory stick mode"
                            + " rename or remove the jabref.xml file in the same folder as JabRef."));
        }

        generalPreferences.setDefaultEncoding(selectedEncodingProperty.getValue());
        generalPreferences.setDefaultBibDatabaseMode(selectedBiblatexModeProperty.getValue());
        generalPreferences.setWarnAboutDuplicatesInInspection(inspectionWarningDuplicateProperty.getValue());
        generalPreferences.setConfirmDelete(confirmDeleteProperty.getValue());
        generalPreferences.setMemoryStickMode(memoryStickModeProperty.getValue());
        generalPreferences.setShowAdvancedHints(showAdvancedHintsProperty.getValue());

        telemetryPreferences.setCollectTelemetry(collectTelemetryProperty.getValue());

        ownerPreferences.setUseOwner(markOwnerProperty.getValue());
        ownerPreferences.setDefaultOwner(markOwnerNameProperty.getValue());
        ownerPreferences.setOverwriteOwner(markOwnerOverwriteProperty.getValue());

        timestampPreferences.setAddCreationDate(addCreationDateProperty.getValue());
        timestampPreferences.setAddModificationDate(addModificationDateProperty.getValue());
    }

    @Override
    public List<String> getRestartWarnings() {
        return restartWarning;
    }

    // General

    public ListProperty<Language> languagesListProperty() {
        return this.languagesListProperty;
    }

    public ObjectProperty<Language> selectedLanguageProperty() {
        return this.selectedLanguageProperty;
    }

    public ListProperty<Charset> encodingsListProperty() {
        return this.encodingsListProperty;
    }

    public ObjectProperty<Charset> selectedEncodingProperty() {
        return this.selectedEncodingProperty;
    }

    public ListProperty<BibDatabaseMode> biblatexModeListProperty() {
        return this.bibliographyModeListProperty;
    }

    public ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty() {
        return this.selectedBiblatexModeProperty;
    }

    public BooleanProperty inspectionWarningDuplicateProperty() {
        return this.inspectionWarningDuplicateProperty;
    }

    public BooleanProperty confirmDeleteProperty() {
        return this.confirmDeleteProperty;
    }

    public BooleanProperty memoryStickModeProperty() {
        return this.memoryStickModeProperty;
    }

    public BooleanProperty collectTelemetryProperty() {
        return this.collectTelemetryProperty;
    }

    public BooleanProperty showAdvancedHintsProperty() {
        return this.showAdvancedHintsProperty;
    }

    // Entry owner

    public BooleanProperty markOwnerProperty() {
        return this.markOwnerProperty;
    }

    public StringProperty markOwnerNameProperty() {
        return this.markOwnerNameProperty;
    }

    public BooleanProperty markOwnerOverwriteProperty() {
        return this.markOwnerOverwriteProperty;
    }

    // Time stamp

    public BooleanProperty addCreationDateProperty() {
        return addCreationDateProperty;
    }

    public BooleanProperty addModificationDateProperty() {
        return addModificationDateProperty;
    }
}
