package org.jabref.gui.filelist;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.BaseDialog;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class LinkedFileRenameDialogView extends BaseDialog<LinkedFile> {

    @FXML private TextField link;

    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;

    @Inject private PreferencesService preferences;

    private LinkedFilesRenameDialogViewModel viewModel;

    private final LinkedFile linkedFile;
    private ExternalFileTypes externalFileTypes;

    public LinkedFileRenameDialogView(LinkedFile linkedFile) {
        this.linkedFile = linkedFile;

        this.externalFileTypes = ExternalFileTypes.getInstance();
        ViewLoader.view(this)
                .load()
                .setAsContent(this.getDialogPane());

        this.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);

        this.setResultConverter(button -> {
            if (button == ButtonType.APPLY) {
                return viewModel.getNewLinkedFile();
            } else {
                return null;
            }
        });
    }

    @FXML
    private void initialize() {

        viewModel = new LinkedFilesRenameDialogViewModel(linkedFile, stateManager.getActiveDatabase().get(), dialogService, preferences, externalFileTypes);
        link.textProperty().bindBidirectional(viewModel.linkProperty());
    }

    @FXML
    private void openBrowseDialog(ActionEvent event) {
        viewModel.openBrowseDialog();
        link.requestFocus();
    }
}
