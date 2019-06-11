package org.jabref.gui.preferences;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.push.PushToApplication;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

public class ExternalTabView extends VBox implements PrefsTab {

    @FXML private TextField eMailReferenceSubject;
    @FXML private CheckBox autoOpenAttachedFolders;

    @FXML private ComboBox<PushToApplication> pushToApplicationCombo;
    @FXML private TextField citeCommand;

    @FXML private RadioButton useTerminalDefault;
    @FXML private RadioButton useTerminalSpecial;
    @FXML private TextField useTerminalCommand;

    @FXML private RadioButton usePDFAcrobat;
    @FXML private TextField usePDFAcrobatCommand;
    @FXML private RadioButton usePDFSumatra;
    @FXML private TextField usePDFSumatraCommand;

    @FXML private RadioButton useFileBrowserDefault;
    @FXML private RadioButton useFileBrowserSpecial;
    @FXML private TextField useFileBrowserSpecialCommand;

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final JabRefFrame frame;

    private ExternalTabViewModel viewModel;

    public ExternalTabView(DialogService dialogService, JabRefPreferences preferences, JabRefFrame frame) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.frame = frame;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        viewModel = new ExternalTabViewModel(dialogService, preferences, frame);

        new ViewModelListCellFactory<PushToApplication>()
                .withText(application -> application.getApplicationName())
                .withIcon(application -> application.getIcon())
                .install(pushToApplicationCombo);

        eMailReferenceSubject.textProperty().bindBidirectional(viewModel.eMailReferenceSubjectProperty());
        autoOpenAttachedFolders.selectedProperty().bindBidirectional(viewModel.autoOpenAttachedFoldersProperty());

        pushToApplicationCombo.itemsProperty().bind(viewModel.pushToApplicationsListProperty());
        pushToApplicationCombo.valueProperty().bindBidirectional(viewModel.selectedPushToApplication());
        citeCommand.textProperty().bindBidirectional(viewModel.citeCommandProperty());

        useTerminalDefault.selectedProperty().bindBidirectional(viewModel.useTerminalDefaultProperty());
        useTerminalSpecial.selectedProperty().bindBidirectional(viewModel.useTerminalSpecialProperty());
        useTerminalCommand.textProperty().bindBidirectional(viewModel.useTerminalCommandProperty());

        usePDFAcrobat.selectedProperty().bindBidirectional(viewModel.usePDFAcrobatProperty());
        usePDFAcrobatCommand.textProperty().bindBidirectional(viewModel.usePDFAcrobatCommandProperty());
        usePDFSumatra.selectedProperty().bindBidirectional(viewModel.usePDFSumatraProperty());
        usePDFSumatraCommand.textProperty().bindBidirectional(viewModel.usePDFSumatraCommandProperty());

        useFileBrowserDefault.selectedProperty().bindBidirectional(viewModel.useFileBrowserDefaultProperty());
        useFileBrowserSpecial.selectedProperty().bindBidirectional(viewModel.useFileBrowserSpecialProperty());
        useFileBrowserSpecialCommand.textProperty().bindBidirectional(viewModel.useFileBrowserSpecialCommandProperty());
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
        viewModel.useTerminalCommandBrowse();
    }

    @FXML
    void usePDFAcrobatCommandBrowse() {
        viewModel.usePDFAcrobatCommandBrowse();
    }

    @FXML
    void usePDFSumatraCommandBrowse() {
        viewModel.usePDFSumatraCommandBrowse();
    }

    @FXML
    void useFileBrowserSpecialCommandBrowse() {
        viewModel.useFileBrowserSpecialCommandBrowse();
    }

    @Override
    public Node getBuilder() {
        return this;
    }

    @Override
    public void setValues() {

    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }

    @Override
    public boolean validateSettings() {
        return viewModel.validateSettings();
    }

    @Override
    public String getTabName() {
        return Localization.lang("External programs");
    }
}
