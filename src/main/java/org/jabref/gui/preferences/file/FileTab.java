package org.jabref.gui.preferences.file;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class FileTab extends AbstractPreferenceTabView<FileTabViewModel> implements PreferencesTab {

    @FXML private CheckBox openLastStartup;
    @FXML private TextField noWrapFiles;
    @FXML private RadioButton doNotResolveStrings;
    @FXML private RadioButton resolveStrings;
    @FXML private TextField resolveStringsForFields;
    @FXML private CheckBox alwaysReformatBib;

    @FXML private CheckBox autosaveLocalLibraries;
    @FXML private Button autosaveLocalLibrariesHelp;

    public FileTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new FileTabViewModel(preferencesService);
        openLastStartup.selectedProperty().bindBidirectional(viewModel.openLastStartupProperty());
        noWrapFiles.textProperty().bindBidirectional(viewModel.noWrapFilesProperty());
        resolveStringsForFields.textProperty().bindBidirectional(viewModel.resolveStringsForFieldsProperty());
        resolveStringsForFields.disableProperty().bind(doNotResolveStrings.selectedProperty());
        alwaysReformatBib.selectedProperty().bindBidirectional(viewModel.alwaysReformatBibProperty());

        autosaveLocalLibraries.selectedProperty().bindBidirectional(viewModel.autosaveLocalLibrariesProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AUTOSAVE), autosaveLocalLibrariesHelp);
    }

    @Override
    public String getTabName() {
        return Localization.lang("File");
    }
}
