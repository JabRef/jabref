package org.jabref.gui.preferences;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class LinkedFilesTabView extends AbstractPreferenceTabView<LinkedFilesTabViewModel> implements PreferencesTab {

    @FXML private TextField mainFileDirectory;
    @FXML private CheckBox useBibLocationAsPrimary;
    @FXML private Button autolinkRegexHelp;
    @FXML private RadioButton autolinkFileStartsBibtex;
    @FXML private RadioButton autolinkFileExactBibtex;
    @FXML private RadioButton autolinkUseRegex;
    @FXML private TextField autolinkRegexKey;
    @FXML private CheckBox searchFilesOnOpen;
    @FXML private CheckBox openBrowseOnCreate;

    @FXML private ComboBox<String> fileNamePattern;
    @FXML private TextField fileDirectoryPattern;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public LinkedFilesTabView(PreferencesService preferences) {
        this.preferences = preferences;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Linked files");
    }

    public void initialize() {
        this.viewModel = new LinkedFilesTabViewModel(dialogService, preferences);

        mainFileDirectory.textProperty().bindBidirectional(viewModel.mainFileDirectoryProperty());
        useBibLocationAsPrimary.selectedProperty().bindBidirectional(viewModel.useBibLocationAsPrimaryProperty());
        autolinkFileStartsBibtex.selectedProperty().bindBidirectional(viewModel.autolinkFileStartsBibtexProperty());
        autolinkFileExactBibtex.selectedProperty().bindBidirectional(viewModel.autolinkFileExactBibtexProperty());
        autolinkUseRegex.selectedProperty().bindBidirectional(viewModel.autolinkUseRegexProperty());
        autolinkRegexKey.textProperty().bindBidirectional(viewModel.autolinkRegexKeyProperty());
        autolinkRegexKey.disableProperty().bind(autolinkUseRegex.selectedProperty().not());
        searchFilesOnOpen.selectedProperty().bindBidirectional(viewModel.searchFilesOnOpenProperty());
        openBrowseOnCreate.selectedProperty().bindBidirectional(viewModel.openBrowseOnCreateProperty());
        fileNamePattern.valueProperty().bindBidirectional(viewModel.fileNamePatternProperty());
        fileNamePattern.itemsProperty().bind(viewModel.defaultFileNamePatternsProperty());
        fileDirectoryPattern.textProperty().bindBidirectional(viewModel.fileDirectoryPatternProperty());

        ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());
        actionFactory.configureIconButton(StandardActions.HELP_REGEX_SEARCH, new HelpAction(HelpFile.REGEX_SEARCH), autolinkRegexHelp);

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> validationVisualizer.initVisualization(viewModel.mainFileDirValidationStatus(), mainFileDirectory));
    }

    public void mainFileDirBrowse() {
        viewModel.mainFileDirBrowse();
    }
}
