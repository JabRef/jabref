package org.jabref.gui.preferences.library;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.TelemetryPreferences;

public class LibraryTabViewModel implements PreferenceTabViewModel {
    private final ListProperty<BibDatabaseMode> bibliographyModeListProperty = new SimpleListProperty<>();
    private final ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty = new SimpleObjectProperty<>();

    private final BooleanProperty memoryStickModeProperty = new SimpleBooleanProperty();
    private final BooleanProperty collectTelemetryProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final GeneralPreferences generalPreferences;
    private final TelemetryPreferences telemetryPreferences;

    @SuppressWarnings("ReturnValueIgnored")
    public LibraryTabViewModel(DialogService dialogService, GeneralPreferences generalPreferences, TelemetryPreferences telemetryPreferences) {
        this.dialogService = dialogService;
        this.generalPreferences = generalPreferences;
        this.telemetryPreferences = telemetryPreferences;
    }

    public void setValues() {
        bibliographyModeListProperty.setValue(FXCollections.observableArrayList(BibDatabaseMode.values()));
        selectedBiblatexModeProperty.setValue(generalPreferences.getDefaultBibDatabaseMode());

        memoryStickModeProperty.setValue(generalPreferences.isMemoryStickMode());
        collectTelemetryProperty.setValue(telemetryPreferences.shouldCollectTelemetry());
    }

    public void storeSettings() {
        if (generalPreferences.isMemoryStickMode() && !memoryStickModeProperty.getValue()) {
            dialogService.showInformationDialogAndWait(Localization.lang("Memory stick mode"),
                    Localization.lang("To disable the memory stick mode"
                            + " rename or remove the jabref.xml file in the same folder as JabRef."));
        }

        generalPreferences.setDefaultBibDatabaseMode(selectedBiblatexModeProperty.getValue());
        generalPreferences.setMemoryStickMode(memoryStickModeProperty.getValue());
        telemetryPreferences.setCollectTelemetry(collectTelemetryProperty.getValue());
    }

    public ListProperty<BibDatabaseMode> biblatexModeListProperty() {
        return this.bibliographyModeListProperty;
    }

    public ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty() {
        return this.selectedBiblatexModeProperty;
    }

    public BooleanProperty memoryStickModeProperty() {
        return this.memoryStickModeProperty;
    }

    public BooleanProperty collectTelemetryProperty() {
        return this.collectTelemetryProperty;
    }
}
