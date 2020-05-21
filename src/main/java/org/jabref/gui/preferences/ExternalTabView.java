package org.jabref.gui.preferences;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.push.PushToApplication;
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

    @FXML private CheckBox useCustomTerminalCommand;
    @FXML private TextField customTerminalCommand;
    @FXML private Button customTerminalBrowse;

    @FXML private CheckBox useCustomFileBrowserCommand;
    @FXML private TextField customFileBrowserCommand;
    @FXML private Button customFileBrowserBrowse;

    private final JabRefFrame frame;

    private final ControlsFxVisualizer validationVisualizer = new ControlsFxVisualizer();

    public ExternalTabView(JabRefPreferences preferences, JabRefFrame frame) {
        this.preferences = preferences;
        this.frame = frame;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("External programs");
    }

    public void initialize() {
        this.viewModel = new ExternalTabViewModel(dialogService, preferences, frame);

        new ViewModelListCellFactory<PushToApplication>()
                .withText(PushToApplication::getApplicationName)
                .withIcon(PushToApplication::getIcon)
                .install(pushToApplicationCombo);

        eMailReferenceSubject.textProperty().bindBidirectional(viewModel.eMailReferenceSubjectProperty());
        autoOpenAttachedFolders.selectedProperty().bindBidirectional(viewModel.autoOpenAttachedFoldersProperty());

        pushToApplicationCombo.itemsProperty().bind(viewModel.pushToApplicationsListProperty());
        pushToApplicationCombo.valueProperty().bindBidirectional(viewModel.selectedPushToApplication());
        citeCommand.textProperty().bindBidirectional(viewModel.citeCommandProperty());

        useCustomTerminalCommand.selectedProperty().bindBidirectional(viewModel.useCustomTerminalCommandProperty());
        customTerminalCommand.textProperty().bindBidirectional(viewModel.customTerminalCommandProperty());
        customTerminalCommand.disableProperty().bind(useCustomTerminalCommand.selectedProperty().not());
        customTerminalBrowse.disableProperty().bind(useCustomTerminalCommand.selectedProperty().not());

        useCustomFileBrowserCommand.selectedProperty().bindBidirectional(viewModel.useCustomFileBrowserCommandProperty());
        customFileBrowserCommand.textProperty().bindBidirectional(viewModel.customFileBrowserCommandProperty());
        customFileBrowserCommand.disableProperty().bind(useCustomFileBrowserCommand.selectedProperty().not());
        customFileBrowserBrowse.disableProperty().bind(useCustomFileBrowserCommand.selectedProperty().not());

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
