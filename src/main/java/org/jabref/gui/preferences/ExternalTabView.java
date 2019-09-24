package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.push.PushToApplication;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class ExternalTabView extends AbstractPreferenceTabView<ExternalTabViewModel> implements PreferencesTab {

    @FXML private TextField eMailReferenceSubject;
    @FXML private CheckBox autoOpenAttachedFolders;

    @FXML private ComboBox<PushToApplication> pushToApplicationCombo;
    @FXML private TextField citeCommand;

    @FXML private RadioButton useTerminalDefault;
    @FXML private RadioButton useTerminalSpecial;
    @FXML private TextField useTerminalCommand;
    @FXML private Button useTerminalBrowse;

    @FXML private RadioButton usePDFAcrobat;
    @FXML private TextField usePDFAcrobatCommand;
    @FXML private Button usePDFAcrobatBrowse;
    @FXML private RadioButton usePDFSumatra;
    @FXML private TextField usePDFSumatraCommand;
    @FXML private Button usePDFSumatraBrowse;

    @FXML private RadioButton useFileBrowserDefault;
    @FXML private RadioButton useFileBrowserSpecial;
    @FXML private TextField useFileBrowserSpecialCommand;
    @FXML private Button useFileBrowserSpecialBrowse;

    private final JabRefFrame frame;

    public ExternalTabView(JabRefPreferences preferences, JabRefFrame frame) {
        this.preferences = preferences;
        this.frame = frame;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() { return Localization.lang("External programs"); }

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

        useTerminalDefault.selectedProperty().bindBidirectional(viewModel.useTerminalDefaultProperty());
        useTerminalSpecial.selectedProperty().bindBidirectional(viewModel.useTerminalSpecialProperty());
        useTerminalCommand.textProperty().bindBidirectional(viewModel.useTerminalCommandProperty());
        useTerminalCommand.disableProperty().bind(useTerminalSpecial.selectedProperty().not());
        useTerminalBrowse.disableProperty().bind(useTerminalSpecial.selectedProperty().not());

        usePDFAcrobat.selectedProperty().bindBidirectional(viewModel.usePDFAcrobatProperty());
        usePDFAcrobatCommand.textProperty().bindBidirectional(viewModel.usePDFAcrobatCommandProperty());
        usePDFAcrobatCommand.disableProperty().bind(usePDFAcrobat.selectedProperty().not());
        usePDFAcrobatBrowse.disableProperty().bind(usePDFAcrobat.selectedProperty().not());

        usePDFSumatra.selectedProperty().bindBidirectional(viewModel.usePDFSumatraProperty());
        usePDFSumatraCommand.textProperty().bindBidirectional(viewModel.usePDFSumatraCommandProperty());
        usePDFSumatraCommand.disableProperty().bind(usePDFSumatra.selectedProperty().not());
        usePDFSumatraBrowse.disableProperty().bind(usePDFSumatra.selectedProperty().not());

        useFileBrowserDefault.selectedProperty().bindBidirectional(viewModel.useFileBrowserDefaultProperty());
        useFileBrowserSpecial.selectedProperty().bindBidirectional(viewModel.useFileBrowserSpecialProperty());
        useFileBrowserSpecialCommand.textProperty().bindBidirectional(viewModel.useFileBrowserSpecialCommandProperty());
        useFileBrowserSpecialCommand.disableProperty().bind(useFileBrowserSpecial.selectedProperty().not());
        useFileBrowserSpecialBrowse.disableProperty().bind(useFileBrowserSpecial.selectedProperty().not());
    }

    @FXML
    void pushToApplicationSettings() { viewModel.pushToApplicationSettings(); }

    @FXML
    void manageExternalFileTypes() { viewModel.manageExternalFileTypes(); }

    @FXML
    void useTerminalCommandBrowse() { viewModel.useTerminalCommandBrowse(); }

    @FXML
    void usePDFAcrobatCommandBrowse() { viewModel.usePDFAcrobatCommandBrowse(); }

    @FXML
    void usePDFSumatraCommandBrowse() { viewModel.usePDFSumatraCommandBrowse(); }

    @FXML
    void useFileBrowserSpecialCommandBrowse() { viewModel.useFileBrowserSpecialCommandBrowse(); }
}
