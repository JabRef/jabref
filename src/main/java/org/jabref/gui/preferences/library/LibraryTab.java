package org.jabref.gui.preferences.library;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;

import com.airhacks.afterburner.views.ViewLoader;

public class LibraryTab extends AbstractPreferenceTabView<LibraryTabViewModel> implements PreferencesTab {

    @FXML private ComboBox<BibDatabaseMode> biblatexMode;
    @FXML private CheckBox alwaysReformatBib;
    @FXML private CheckBox autosaveLocalLibraries;
    @FXML private Button autosaveLocalLibrariesHelp;

    @FXML private CheckBox createBackup;
    @FXML private TextField backupDirectory;

    public LibraryTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Libraries");
    }

    public void initialize() {
        this.viewModel = new LibraryTabViewModel(dialogService, preferencesService.getLibraryPreferences(), preferencesService.getFilePreferences());

        new ViewModelListCellFactory<BibDatabaseMode>()
                .withText(BibDatabaseMode::getFormattedName)
                .install(biblatexMode);
        biblatexMode.itemsProperty().bind(viewModel.biblatexModeListProperty());
        biblatexMode.valueProperty().bindBidirectional(viewModel.selectedBiblatexModeProperty());

        alwaysReformatBib.selectedProperty().bindBidirectional(viewModel.alwaysReformatBibProperty());
        autosaveLocalLibraries.selectedProperty().bindBidirectional(viewModel.autosaveLocalLibrariesProperty());
        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AUTOSAVE, dialogService), autosaveLocalLibrariesHelp);

        createBackup.selectedProperty().bindBidirectional(viewModel.createBackupProperty());
        backupDirectory.textProperty().bindBidirectional(viewModel.backupDirectoryProperty());
        backupDirectory.disableProperty().bind(viewModel.createBackupProperty().not());
    }

    public void backupFileDirBrowse() {
        viewModel.backupFileDirBrowse();
    }
}
