package org.jabref.gui.filewizard;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.*;
import java.io.File;
import java.io.IOException;


/**
 * Represents the first window encountered when clicking on "File Wizard" in the Tools section of the toolbar.
 */
public class FileWizardView extends BaseDialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileWizardView.class);

    private final DialogService dialogService;
    private final StateManager stateManager;
    private FileWizardViewModel viewModel;
    private File directory;
    private final FileWizardAction action;

    @FXML TextField directoryTextField;
    @FXML CheckBox openDirectoryCheckbox;
    @FXML ButtonType startButton;

    @Inject private PreferencesService preferencesService;

    public FileWizardView(FileWizardAction action, DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.action = action;

        this.setTitle(Localization.lang("File Wizard"));

        ViewLoader.view(this)
                .load()
                .setAsDialogPane(this);

        ControlHelper.setAction(startButton, this.getDialogPane(), event -> startWizard());
    }

    @FXML
    private void initialize() {
        viewModel = new FileWizardViewModel(dialogService, preferencesService);

        directoryTextField.textProperty().bindBidirectional(viewModel.getDirectoryProperty());
    }

    /**
     * Here the wizard starts working. The method is activated when the "Start" button is pressed in the wizard interface.
     */
    private void startWizard() {
        directory = new File(viewModel.getDirectoryProperty().get());

        if(!directory.exists()) {
            if(directory.toString().equals("")) {
                dialogService.showErrorDialogAndWait("Please choose a path");
                return;
            }
            dialogService.showErrorDialogAndWait(directory.toPath() + " is not a valid pathname");
            LOGGER.error(directory.getPath() + " is not a valid pathname");
            return;
        }


        action.closeFileWizardControlPanel();
        // Manages the file wizard's doings.
        FileWizardManager manager = new FileWizardManager(dialogService, stateManager,
                openDirectoryCheckbox.isSelected(), directory, preferencesService);
    }

    @FXML
    public void browseDirectory(ActionEvent event) {
        viewModel.browseFileDirectory();
    }
}
