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
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class LinkedFileEditDialog extends BaseDialog<LinkedFile> {

    private static final ButtonType ADD_BUTTON = new ButtonType(Localization.lang("Add"), ButtonType.OK.getButtonData());
    private static final ButtonType EDIT_BUTTON = ButtonType.APPLY;

    @FXML private TextField link;
    @FXML private TextField description;
    @FXML private ComboBox<ExternalFileType> fileType;
    @FXML private TextField sourceUrl;

    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;
    @Inject private PreferencesService preferences;

    private LinkedFileEditDialogViewModel viewModel;
    private final LinkedFile linkedFile;

    /**
     * Constructor for adding a new LinkedFile.
     */
    public LinkedFileEditDialog() {
        this.linkedFile = new LinkedFile("", "", StandardFileType.PDF);
        initializeDialog(Localization.lang("Add file link"), ADD_BUTTON);
    }

    /**
     * Constructor for editing an existing LinkedFile.
     *
     * @param linkedFile The linked file to be edited.
     */
    public LinkedFileEditDialog(LinkedFile linkedFile) {
        this.linkedFile = linkedFile;
        initializeDialog(Localization.lang("Edit file link"), EDIT_BUTTON);
    }

    private void initializeDialog(String title, ButtonType primaryButtonType) {
        ViewLoader.view(this)
                  .load()
                  .setAsContent(this.getDialogPane());

        this.setTitle(title);
        this.setResizable(false);
        this.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL, primaryButtonType);

        this.setResultConverter(button -> {
            if (button == primaryButtonType) {
                return viewModel.getNewLinkedFile();
            } else {
                return null;
            }
        });
    }

    @FXML
    private void initialize() {
        viewModel = new LinkedFileEditDialogViewModel(linkedFile, stateManager.getActiveDatabase().get(), dialogService, preferences.getFilePreferences());

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
