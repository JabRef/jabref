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

public class LinkedFileAddDialogController extends BaseDialog<LinkedFile> {

    @FXML private TextField link;
    @FXML private TextField description;
    @FXML private ComboBox<ExternalFileType> fileType;
    @FXML private TextField sourceUrl;

    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;
    @Inject private PreferencesService preferences;

    private LinkedFilesEditDialogViewModel viewModel;
    private final LinkedFile linkedFile;

    public LinkedFileAddDialogController() {
        this.linkedFile = new LinkedFile("", "", "");

        ViewLoader.view(this)
                  .load()
                  .setAsContent(this.getDialogPane());

        ButtonType addButtonType = new ButtonType(Localization.lang("Add"), ButtonType.OK.getButtonData());
        this.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        this.setResizable(false);
        this.setTitle(Localization.lang("Add file link"));

        this.setResultConverter(button -> {
            if (button == addButtonType) {
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
        sourceUrl.textProperty().bindBidirectional(viewModel.sourceUrlProperty());
    }

    @FXML
    private void openBrowseDialog(ActionEvent event) {
        viewModel.openBrowseDialog();
        link.requestFocus();
    }
}
