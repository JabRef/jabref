package org.jabref.gui.filelist;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.util.BaseDialog;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class LinkedFileEditDialogView extends BaseDialog<LinkedFile> {

    @FXML private TextField link;
    @FXML private TextField description;
    @FXML private ComboBox<ExternalFileType> fileType;

    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;

    @Inject private PreferencesService preferences;

    private LinkedFilesEditDialogViewModel viewModel;

    private final LinkedFile linkedFile;

    public LinkedFileEditDialogView(LinkedFilesWrapper wrapper) {
        this.linkedFile = wrapper.getLinkedFile();

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

        viewModel = new LinkedFilesEditDialogViewModel(linkedFile, stateManager.getActiveDatabase().get(), dialogService, preferences);
        fileType.itemsProperty().bindBidirectional(viewModel.externalFileType());
        description.textProperty().bindBidirectional(viewModel.description());
        link.textProperty().bindBidirectional(viewModel.link());
        fileType.valueProperty().bindBidirectional(viewModel.selectedExternalFileType());
    }

    @FXML
    private void openBrowseDialog(ActionEvent event) {
        viewModel.openBrowseDialog();
        link.requestFocus();
    }
}
