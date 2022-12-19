package org.jabref.gui.preferences.general;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.TelemetryPreferences;

public class GeneralTabViewModel implements PreferenceTabViewModel {
    private final ListProperty<Language> languagesListProperty = new SimpleListProperty<>();
    private final ObjectProperty<Language> selectedLanguageProperty = new SimpleObjectProperty<>();
    private final ListProperty<BibDatabaseMode> bibliographyModeListProperty = new SimpleListProperty<>();
    private final ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty = new SimpleObjectProperty<>();

    private final BooleanProperty inspectionWarningDuplicateProperty = new SimpleBooleanProperty();
    private final BooleanProperty confirmDeleteProperty = new SimpleBooleanProperty();
    private final BooleanProperty memoryStickModeProperty = new SimpleBooleanProperty();
    private final BooleanProperty collectTelemetryProperty = new SimpleBooleanProperty();
    private final BooleanProperty showAdvancedHintsProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final GeneralPreferences generalPreferences;
    private final TelemetryPreferences telemetryPreferences;

    private final List<String> restartWarning = new ArrayList<>();

    @SuppressWarnings("ReturnValueIgnored")
    public GeneralTabViewModel(DialogService dialogService, GeneralPreferences generalPreferences, TelemetryPreferences telemetryPreferences) {
        this.dialogService = dialogService;
        this.generalPreferences = generalPreferences;
        this.telemetryPreferences = telemetryPreferences;
    }

    public void setValues() {
        languagesListProperty.setValue(new SortedList<>(FXCollections.observableArrayList(Language.values()), Comparator.comparing(Language::getDisplayName)));
        selectedLanguageProperty.setValue(generalPreferences.getLanguage());

        bibliographyModeListProperty.setValue(FXCollections.observableArrayList(BibDatabaseMode.values()));
        selectedBiblatexModeProperty.setValue(generalPreferences.getDefaultBibDatabaseMode());

        inspectionWarningDuplicateProperty.setValue(generalPreferences.warnAboutDuplicatesInInspection());
        confirmDeleteProperty.setValue(generalPreferences.shouldConfirmDelete());
        memoryStickModeProperty.setValue(generalPreferences.isMemoryStickMode());
        collectTelemetryProperty.setValue(telemetryPreferences.shouldCollectTelemetry());
        showAdvancedHintsProperty.setValue(generalPreferences.shouldShowAdvancedHints());
    }

    public void storeSettings() {
        Language newLanguage = selectedLanguageProperty.getValue();
        if (newLanguage != generalPreferences.getLanguage()) {
            generalPreferences.setLanguage(newLanguage);
            Localization.setLanguage(newLanguage);
            restartWarning.add(Localization.lang("Changed language") + ": " + newLanguage.getDisplayName());
        }

        if (generalPreferences.isMemoryStickMode() && !memoryStickModeProperty.getValue()) {
            dialogService.showInformationDialogAndWait(Localization.lang("Memory stick mode"),
                    Localization.lang("To disable the memory stick mode"
                            + " rename or remove the jabref.xml file in the same folder as JabRef."));
        }

        generalPreferences.setDefaultBibDatabaseMode(selectedBiblatexModeProperty.getValue());
        generalPreferences.setWarnAboutDuplicatesInInspection(inspectionWarningDuplicateProperty.getValue());
        generalPreferences.setConfirmDelete(confirmDeleteProperty.getValue());
        generalPreferences.setMemoryStickMode(memoryStickModeProperty.getValue());
        generalPreferences.setShowAdvancedHints(showAdvancedHintsProperty.getValue());

        telemetryPreferences.setCollectTelemetry(collectTelemetryProperty.getValue());
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
}
