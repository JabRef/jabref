package org.jabref.gui.externalfiletype;

import javax.inject.Inject;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class EditExternalFileTypeDialog extends BaseDialog<Void> {

    @FXML private RadioButton defaultApplication;
    @FXML private ToggleGroup applicationToggleGroup;
    @FXML private TextField extension;
    @FXML private TextField name;
    @FXML private TextField mimeType;
    @FXML private RadioButton customApplication;
    @FXML private TextField selectedApplication;
    @FXML private Button btnBrowse;
    private final NativeDesktop nativeDesktop = JabRefDesktop.getNativeDesktop();
    private final FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder().withInitialDirectory(nativeDesktop.getApplicationDirectory()).build();

    @Inject private DialogService dialogService;
    private EditExternalFileTypeViewModel viewModel;
    private CustomExternalFileType entry;

    public EditExternalFileTypeDialog(CustomExternalFileType entry) {
        this.entry = entry;
        this.setTitle(Localization.lang("Edit external file type"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                viewModel.storeSettings();
            }
            return null;
        });
    }

    @FXML
    public void initialize() {
        viewModel = new EditExternalFileTypeViewModel(entry);

        viewModel.defaultApplicationSelectedProperty().bindBidirectional(defaultApplication.selectedProperty());
        selectedApplication.disableProperty().bind(viewModel.defaultApplicationSelectedProperty());
        btnBrowse.disableProperty().bind(viewModel.defaultApplicationSelectedProperty());

        extension.textProperty().bindBidirectional(viewModel.extensionProperty());
        name.textProperty().bindBidirectional(viewModel.nameProperty());
        mimeType.textProperty().bindBidirectional(viewModel.mimeTypeProperty());
        selectedApplication.textProperty().bindBidirectional(viewModel.selectedApplicationProperty());

    }

    @FXML
    private void openFileChooser(ActionEvent event) {
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(path -> viewModel.selectedApplicationProperty().setValue(path.toAbsolutePath().toString()));

    }

}
