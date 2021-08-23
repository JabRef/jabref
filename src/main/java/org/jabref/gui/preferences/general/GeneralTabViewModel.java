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
    private final PreferencesService preferencesService;
    private final GeneralPreferences initialGeneralPreferences;
    private final TelemetryPreferences initialTelemetryPreferences;
    private final OwnerPreferences initialOwnerPreferences;
    private final TimestampPreferences initialTimestampPreferences;

    private List<String> restartWarning = new ArrayList<>();

    @SuppressWarnings("ReturnValueIgnored")
    public GeneralTabViewModel(DialogService dialogService, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.initialGeneralPreferences = preferencesService.getGeneralPreferences();
        this.initialTelemetryPreferences = preferencesService.getTelemetryPreferences();
        this.initialOwnerPreferences = preferencesService.getOwnerPreferences();
        this.initialTimestampPreferences = preferencesService.getTimestampPreferences();
    }

    public void setValues() {
        languagesListProperty.setValue(new SortedList<>(FXCollections.observableArrayList(Language.values()), Comparator.comparing(Language::getDisplayName)));
        selectedLanguageProperty.setValue(preferencesService.getLanguage());

        encodingsListProperty.setValue(FXCollections.observableArrayList(Encodings.getCharsets()));
        selectedEncodingProperty.setValue(initialGeneralPreferences.getDefaultEncoding());

        bibliographyModeListProperty.setValue(FXCollections.observableArrayList(BibDatabaseMode.values()));
        selectedBiblatexModeProperty.setValue(initialGeneralPreferences.getDefaultBibDatabaseMode());

        inspectionWarningDuplicateProperty.setValue(initialGeneralPreferences.isWarnAboutDuplicatesInInspection());
        confirmDeleteProperty.setValue(initialGeneralPreferences.shouldConfirmDelete());
        memoryStickModeProperty.setValue(initialGeneralPreferences.isMemoryStickMode());
        collectTelemetryProperty.setValue(initialTelemetryPreferences.shouldCollectTelemetry());
        showAdvancedHintsProperty.setValue(initialGeneralPreferences.shouldShowAdvancedHints());

        markOwnerProperty.setValue(initialOwnerPreferences.isUseOwner());
        markOwnerNameProperty.setValue(initialOwnerPreferences.getDefaultOwner());
        markOwnerOverwriteProperty.setValue(initialOwnerPreferences.isOverwriteOwner());

        addCreationDateProperty.setValue(initialTimestampPreferences.shouldAddCreationDate());
        addModificationDateProperty.setValue(initialTimestampPreferences.shouldAddModificationDate());
    }

    public void storeSettings() {
        Language newLanguage = selectedLanguageProperty.getValue();
        if (newLanguage != preferencesService.getLanguage()) {
            preferencesService.setLanguage(newLanguage);
            Localization.setLanguage(newLanguage);
            restartWarning.add(Localization.lang("Changed language") + ": " + newLanguage.getDisplayName());
        }

        if (initialGeneralPreferences.isMemoryStickMode() && !memoryStickModeProperty.getValue()) {
            dialogService.showInformationDialogAndWait(Localization.lang("Memory stick mode"),
                    Localization.lang("To disable the memory stick mode"
                            + " rename or remove the jabref.xml file in the same folder as JabRef."));
        }

        preferencesService.storeGeneralPreferences(new GeneralPreferences(
                selectedEncodingProperty.getValue(),
                selectedBiblatexModeProperty.getValue(),
                inspectionWarningDuplicateProperty.getValue(),
                confirmDeleteProperty.getValue(),
                memoryStickModeProperty.getValue(),
                showAdvancedHintsProperty.getValue()));

        preferencesService.storeTelemetryPreferences(
                initialTelemetryPreferences.withCollectTelemetry(collectTelemetryProperty.getValue()));

        preferencesService.storeOwnerPreferences(new OwnerPreferences(
                markOwnerProperty.getValue(),
                markOwnerNameProperty.getValue().trim(),
                markOwnerOverwriteProperty.getValue()));

        preferencesService.storeTimestampPreferences(new TimestampPreferences(
                addCreationDateProperty.getValue(),
                addModificationDateProperty.getValue(),
                initialTimestampPreferences.shouldUpdateTimestamp(),
                initialTimestampPreferences.getTimestampField(),
                initialTimestampPreferences.getTimestampFormat()));
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
