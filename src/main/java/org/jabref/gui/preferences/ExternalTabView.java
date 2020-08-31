package org.jabref.gui.preferences;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.push.PushToApplication;
import org.jabref.gui.push.PushToApplicationsManager;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

public class ExternalTabView extends AbstractPreferenceTabView<ExternalTabViewModel> implements PreferencesTab {

    @FXML private TextField eMailReferenceSubject;
    @FXML private CheckBox autoOpenAttachedFolders;

    @FXML private ComboBox<PushToApplication> pushToApplicationCombo;
    @FXML private TextField citeCommand;

    @FXML private CheckBox useCustomTerminal;
    @FXML private TextField customTerminalCommand;
    @FXML private Button customTerminalBrowse;

    @FXML private CheckBox useCustomFileBrowser;
    @FXML private TextField customFileBrowserCommand;
    @FXML private Button customFileBrowserBrowse;

    private final PushToApplicationsManager pushToApplicationsManager;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public ExternalTabView(JabRefPreferences preferences, PushToApplicationsManager pushToApplicationsManager) {
        this.preferences = preferences;
        this.pushToApplicationsManager = pushToApplicationsManager;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("External programs");
    }

    public void initialize() {
        this.viewModel = new ExternalTabViewModel(dialogService, preferences, pushToApplicationsManager);

        new ViewModelListCellFactory<PushToApplication>()
                .withText(PushToApplication::getApplicationName)
                .withIcon(PushToApplication::getIcon)
                .install(pushToApplicationCombo);

        eMailReferenceSubject.textProperty().bindBidirectional(viewModel.eMailReferenceSubjectProperty());
        autoOpenAttachedFolders.selectedProperty().bindBidirectional(viewModel.autoOpenAttachedFoldersProperty());

        pushToApplicationCombo.itemsProperty().bind(viewModel.pushToApplicationsListProperty());
        pushToApplicationCombo.valueProperty().bindBidirectional(viewModel.selectedPushToApplication());
        citeCommand.textProperty().bindBidirectional(viewModel.citeCommandProperty());

        useCustomTerminal.selectedProperty().bindBidirectional(viewModel.useCustomTerminalProperty());
        customTerminalCommand.textProperty().bindBidirectional(viewModel.customTerminalCommandProperty());
        customTerminalCommand.disableProperty().bind(useCustomTerminal.selectedProperty().not());
        customTerminalBrowse.disableProperty().bind(useCustomTerminal.selectedProperty().not());

        useCustomFileBrowser.selectedProperty().bindBidirectional(viewModel.useCustomFileBrowserProperty());
        customFileBrowserCommand.textProperty().bindBidirectional(viewModel.customFileBrowserCommandProperty());
        customFileBrowserCommand.disableProperty().bind(useCustomFileBrowser.selectedProperty().not());
        customFileBrowserBrowse.disableProperty().bind(useCustomFileBrowser.selectedProperty().not());

        validationVisualizer.setDecoration(new IconValidationDecorator());
        Platform.runLater(() -> {
            validationVisualizer.initVisualization(viewModel.terminalCommandValidationStatus(), customTerminalCommand);
            validationVisualizer.initVisualization(viewModel.fileBrowserCommandValidationStatus(), customFileBrowserCommand);
        });
    }

    @FXML
    void pushToApplicationSettings() {
        viewModel.pushToApplicationSettings();
    }

    @FXML
    void manageExternalFileTypes() {
        viewModel.manageExternalFileTypes();
    }

    @FXML
    void useTerminalCommandBrowse() {
        viewModel.customTerminalBrowse();
    }

    @FXML
    void useFileBrowserSpecialCommandBrowse() {
        viewModel.customFileBrowserBrowse();
    }
}
