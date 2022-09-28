package org.jabref.gui.linkedfile;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class LinkedFileEditDialogView extends BaseDialog<LinkedFile> {

    @FXML private TextField link;
    @FXML private TextField description;
    @FXML private ComboBox<ExternalFileType> fileType;

    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;

    @Inject private PreferencesService preferences;

    private LinkedFilesEditDialogViewModel viewModel;

    private final LinkedFile linkedFile;

    public LinkedFileEditDialogView(LinkedFile linkedFile) {
        this.linkedFile = linkedFile;

        ViewLoader.view(this)
                  .load()
                  .setAsContent(this.getDialogPane());

        this.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
        this.setResizable(false);
        this.setTitle(Localization.lang("Edit file link"));

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
        viewModel = new LinkedFilesEditDialogViewModel(linkedFile, stateManager.getActiveDatabase().get(), dialogService, preferences.getFilePreferences());
        fileType.itemsProperty().bindBidirectional(viewModel.externalFileTypeProperty());
        new ViewModelListCellFactory<ExternalFileType>()
                .withIcon(ExternalFileType::getIcon)
                .withText(ExternalFileType::getName)
                .install(fileType);

        description.textProperty().bindBidirectional(viewModel.descriptionProperty());
        link.textProperty().bindBidirectional(viewModel.linkProperty());
        fileType.valueProperty().bindBidirectional(viewModel.selectedExternalFileTypeProperty());
    }

    @FXML
    private void openBrowseDialog(ActionEvent event) {
        viewModel.openBrowseDialog();
        link.requestFocus();
    }
}
