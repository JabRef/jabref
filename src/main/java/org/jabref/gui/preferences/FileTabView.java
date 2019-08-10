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

public class FileTabView extends AbstractPreferenceTabView implements PreferencesTab {

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
        FileTabViewModel fileTabViewModel = new FileTabViewModel(dialogService, preferences);
        this.viewModel = fileTabViewModel;

        openLastStartup.selectedProperty().bindBidirectional(fileTabViewModel.openLastStartupProperty());
        backupOldFile.selectedProperty().bindBidirectional(fileTabViewModel.backupOldFileProperty());
        noWrapFiles.textProperty().bindBidirectional(fileTabViewModel.noWrapFilesProperty());
        resolveStringsBibTex.selectedProperty().bindBidirectional(fileTabViewModel.resolveStringsBibTexProperty());
        resolveStringsAll.selectedProperty().bindBidirectional(fileTabViewModel.resolveStringsAllProperty());
        resolveStringsExcept.textProperty().bindBidirectional(fileTabViewModel.resolvStringsExceptProperty());
        resolveStringsExcept.disableProperty().bind(resolveStringsAll.selectedProperty().not());
        newLineSeparator.itemsProperty().bind(fileTabViewModel.newLineSeparatorListProperty());
        newLineSeparator.valueProperty().bindBidirectional(fileTabViewModel.selectedNewLineSeparatorProperty());
        alwaysReformatBib.selectedProperty().bindBidirectional(fileTabViewModel.alwaysReformatBibProperty());

        mainFileDir.textProperty().bindBidirectional(fileTabViewModel.mainFileDirProperty());
        useBibLocationAsPrimary.selectedProperty().bindBidirectional(fileTabViewModel.useBibLocationAsPrimaryProperty());
        autolinkFileStartsBibtex.selectedProperty().bindBidirectional(fileTabViewModel.autolinkFileStartsBibtexProperty());
        autolinkFileExactBibtex.selectedProperty().bindBidirectional(fileTabViewModel.autolinkFileExactBibtexProperty());
        autolinkUseRegex.selectedProperty().bindBidirectional(fileTabViewModel.autolinkUseRegexProperty());
        autolinkRegexKey.textProperty().bindBidirectional(fileTabViewModel.autolinkRegexKeyProperty());
        autolinkRegexKey.disableProperty().bind(autolinkUseRegex.selectedProperty().not());
        searchFilesOnOpen.selectedProperty().bindBidirectional(fileTabViewModel.searchFilesOnOpenProperty());
        openBrowseOnCreate.selectedProperty().bindBidirectional(fileTabViewModel.openBrowseOnCreateProperty());

        autosaveLocalLibraries.selectedProperty().bindBidirectional(fileTabViewModel.autosaveLocalLibrariesProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP_REGEX_SEARCH, new HelpAction(HelpFile.REGEX_SEARCH), autolinkRegexHelp);
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.AUTOSAVE), autosaveLocalLibrariesHelp);

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(fileTabViewModel.mainFileDirValidationStatus(), mainFileDir));
    }

    public void mainFileDirBrowse() {
        ((FileTabViewModel) viewModel).mainFileDirBrowse();
    }
}
