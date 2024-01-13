package org.jabref.gui.preferences.externalfiletypes;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.util.OS;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;

public class EditExternalFileTypeEntryDialog extends BaseDialog<Void> {

    @FXML private RadioButton defaultApplication;
    @FXML private ToggleGroup applicationToggleGroup;
    @FXML private TextField extension;
    @FXML private TextField name;
    @FXML private TextField mimeType;
    @FXML private RadioButton customApplication;
    @FXML private TextField selectedApplication;
    @FXML private Button btnBrowse;
    @FXML private Label icon;
    @Inject private DialogService dialogService;

    private final NativeDesktop nativeDesktop = OS.getNativeDesktop();
    private final FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder().withInitialDirectory(nativeDesktop.getApplicationDirectory()).build();
    private final ExternalFileTypeItemViewModel item;

    private final ObservableList<ExternalFileTypeItemViewModel> fileTypes;
    private EditExternalFileTypeViewModel viewModel;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public EditExternalFileTypeEntryDialog(ExternalFileTypeItemViewModel item, String dialogTitle, ObservableList<ExternalFileTypeItemViewModel> fileTypes) {
        this.item = item;
        this.fileTypes = fileTypes;
        this.setTitle(dialogTitle);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        final Button confirmDialogButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        confirmDialogButton.disableProperty().bind(viewModel.validationStatus().validProperty().not());
        this.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                viewModel.storeSettings();
            }
            return null;
        });
    }

    @FXML
    public void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());

        viewModel = new EditExternalFileTypeViewModel(item, fileTypes);

        icon.setGraphic(viewModel.getIcon());

        defaultApplication.selectedProperty().bindBidirectional(viewModel.defaultApplicationSelectedProperty());
        customApplication.selectedProperty().bindBidirectional(viewModel.customApplicationSelectedProperty());
        selectedApplication.disableProperty().bind(viewModel.defaultApplicationSelectedProperty());
        btnBrowse.disableProperty().bind(viewModel.defaultApplicationSelectedProperty());
        extension.textProperty().bindBidirectional(viewModel.extensionProperty());
        name.textProperty().bindBidirectional(viewModel.nameProperty());
        mimeType.textProperty().bindBidirectional(viewModel.mimeTypeProperty());
        selectedApplication.textProperty().bindBidirectional(viewModel.selectedApplicationProperty());

        Platform.runLater(() -> {
            visualizer.initVisualization(viewModel.extensionValidation(), extension, true);
            visualizer.initVisualization(viewModel.nameValidation(), name, true);
            visualizer.initVisualization(viewModel.mimeTypeValidation(), mimeType, true);
        });
    }

    @FXML
    private void openFileChooser(ActionEvent event) {
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(path -> viewModel.selectedApplicationProperty().setValue(path.toAbsolutePath().toString()));
    }
}
