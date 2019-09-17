package org.jabref.gui.preferences;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.NewLineSeparator;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class FileTabView extends AbstractPreferenceTabView<FileTabViewModel> implements PreferencesTab {

    @FXML private CheckBox openLastStartup;
    @FXML private CheckBox backupOldFile;
    @FXML private TextField noWrapFiles;
    @FXML private RadioButton resolveStringsBibTex;
    @FXML private RadioButton resolveStringsAll;
    @FXML private TextField resolveStringsExcept;
    @FXML private ComboBox<NewLineSeparator> newLineSeparator;
    @FXML private CheckBox alwaysReformatBib;

    @FXML private TextField mainFileDir;
    @FXML private CheckBox useBibLocationAsPrimary;
    @FXML private Button autolinkRegexHelp;
    @FXML private RadioButton autolinkFileStartsBibtex;
    @FXML private RadioButton autolinkFileExactBibtex;
    @FXML private RadioButton autolinkUseRegex;
    @FXML private TextField autolinkRegexKey;
    @FXML private CheckBox searchFilesOnOpen;
    @FXML private CheckBox openBrowseOnCreate;

    @FXML private CheckBox autosaveLocalLibraries;
    @FXML private Button autosaveLocalLibrariesHelp;

    private ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public FileTabView(JabRefPreferences preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() { return Localization.lang("File"); }

    public void initialize() {
        this.viewModel = new FileTabViewModel(dialogService, preferences);

        openLastStartup.selectedProperty().bindBidirectional(viewModel.openLastStartupProperty());
        backupOldFile.selectedProperty().bindBidirectional(viewModel.backupOldFileProperty());
        noWrapFiles.textProperty().bindBidirectional(viewModel.noWrapFilesProperty());
        resolveStringsBibTex.selectedProperty().bindBidirectional(viewModel.resolveStringsBibTexProperty());
        resolveStringsAll.selectedProperty().bindBidirectional(viewModel.resolveStringsAllProperty());
        resolveStringsExcept.textProperty().bindBidirectional(viewModel.resolvStringsExceptProperty());
        resolveStringsExcept.disableProperty().bind(resolveStringsAll.selectedProperty().not());
        newLineSeparator.itemsProperty().bind(viewModel.newLineSeparatorListProperty());
        newLineSeparator.valueProperty().bindBidirectional(viewModel.selectedNewLineSeparatorProperty());
        alwaysReformatBib.selectedProperty().bindBidirectional(viewModel.alwaysReformatBibProperty());

        mainFileDir.textProperty().bindBidirectional(viewModel.mainFileDirProperty());
        useBibLocationAsPrimary.selectedProperty().bindBidirectional(viewModel.useBibLocationAsPrimaryProperty());
        autolinkFileStartsBibtex.selectedProperty().bindBidirectional(viewModel.autolinkFileStartsBibtexProperty());
        autolinkFileExactBibtex.selectedProperty().bindBidirectional(viewModel.autolinkFileExactBibtexProperty());
        autolinkUseRegex.selectedProperty().bindBidirectional(viewModel.autolinkUseRegexProperty());
        autolinkRegexKey.textProperty().bindBidirectional(viewModel.autolinkRegexKeyProperty());
        autolinkRegexKey.disableProperty().bind(autolinkUseRegex.selectedProperty().not());
        searchFilesOnOpen.selectedProperty().bindBidirectional(viewModel.searchFilesOnOpenProperty());
        openBrowseOnCreate.selectedProperty().bindBidirectional(viewModel.openBrowseOnCreateProperty());

        autosaveLocalLibraries.selectedProperty().bindBidirectional(viewModel.autosaveLocalLibrariesProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP_REGEX_SEARCH, new HelpAction(HelpFile.REGEX_SEARCH), autolinkRegexHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AUTOSAVE), autosaveLocalLibrariesHelp);

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.mainFileDirValidationStatus(), mainFileDir));
    }

    public void mainFileDirBrowse() { viewModel.mainFileDirBrowse(); }
}
