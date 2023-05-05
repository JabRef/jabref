package org.jabref.gui.preferences.general;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;

import com.airhacks.afterburner.views.ViewLoader;

public class GeneralTab extends AbstractPreferenceTabView<GeneralTabViewModel> implements PreferencesTab {

    @FXML private ComboBox<Language> language;
    @FXML private ComboBox<BibDatabaseMode> biblatexMode;
    @FXML private CheckBox inspectionWarningDuplicate;
    @FXML private CheckBox confirmDelete;
    @FXML private CheckBox memoryStickMode;
    @FXML private CheckBox openLastStartup;
    @FXML private CheckBox collectTelemetry;
    @FXML private CheckBox showAdvancedHints;

    @FXML private CheckBox createBackup;
    @FXML private TextField backupDirectory;

    public GeneralTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("General");
    }

    public void initialize() {
        this.viewModel = new GeneralTabViewModel(preferencesService, dialogService);

        new ViewModelListCellFactory<Language>()
                .withText(Language::getDisplayName)
                .install(language);
        language.itemsProperty().bind(viewModel.languagesListProperty());
        language.valueProperty().bindBidirectional(viewModel.selectedLanguageProperty());

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

        createBackup.selectedProperty().bindBidirectional(viewModel.createBackupProperty());
        backupDirectory.textProperty().bindBidirectional(viewModel.backupDirectoryProperty());
        backupDirectory.disableProperty().bind(viewModel.createBackupProperty().not());
    }

    public void backupFileDirBrowse() {
        viewModel.backupFileDirBrowse();
    }
}
