package org.jabref.gui.preferences.library;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;

import com.airhacks.afterburner.views.ViewLoader;

public class LibraryTab extends AbstractPreferenceTabView<LibraryTabViewModel> implements PreferencesTab {

    @FXML private ComboBox<BibDatabaseMode> biblatexMode;
    @FXML private CheckBox inspectionWarningDuplicate;
    @FXML private CheckBox confirmDelete;
    @FXML private CheckBox memoryStickMode;
    @FXML private CheckBox openLastStartup;
    @FXML private CheckBox collectTelemetry;
    @FXML private CheckBox showAdvancedHints;

    public LibraryTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Library");
    }

    public void initialize() {
        this.viewModel = new LibraryTabViewModel(dialogService, preferencesService.getGeneralPreferences(), preferencesService.getTelemetryPreferences());

        new ViewModelListCellFactory<BibDatabaseMode>()
                .withText(BibDatabaseMode::getFormattedName)
                .install(biblatexMode);
        biblatexMode.itemsProperty().bind(viewModel.biblatexModeListProperty());
        biblatexMode.valueProperty().bindBidirectional(viewModel.selectedBiblatexModeProperty());

        inspectionWarningDuplicate.selectedProperty().bindBidirectional(viewModel.inspectionWarningDuplicateProperty());
        confirmDelete.selectedProperty().bindBidirectional(viewModel.confirmDeleteProperty());
        memoryStickMode.selectedProperty().bindBidirectional(viewModel.memoryStickModeProperty());
        openLastStartup.selectedProperty().bindBidirectional(viewModel.openLastStartupProperty());
        collectTelemetry.selectedProperty().bindBidirectional(viewModel.collectTelemetryProperty());
        showAdvancedHints.selectedProperty().bindBidirectional(viewModel.showAdvancedHintsProperty());
    }
}
