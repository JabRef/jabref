package org.jabref.gui.filelist;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.jabref.Globals;
import org.jabref.gui.AbstractController;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileListDialogController extends AbstractController<FileListDialogViewModel> {

    private static final Log LOGGER = LogFactory.getLog(FileListDialogController.class);
    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");

    @FXML private TextField tfLink;
    @FXML private Button btnBrowse;
    @FXML private Button btnOpen;
    @FXML private TextField tfDescription;
    @FXML private ComboBox<ExternalFileType> cmbFileType;
    @FXML private Button btnOk;
    @FXML private Button btnCancel;

    @Inject private PreferencesService preferences;
    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;

    private boolean showSaveDialog;

    @FXML
    private void initialize() {
        viewModel = new FileListDialogViewModel(stateManager.getActiveDatabase().get());
        setBindings();

    }

    private void setBindings() {

        cmbFileType.itemsProperty().bindBidirectional(viewModel.externalFileTypeProperty());
        tfDescription.textProperty().bindBidirectional(viewModel.descriptionProperty());
        tfLink.textProperty().bindBidirectional(viewModel.linkProperty());
    }

    @FXML
    void browseFileDialog(ActionEvent event) {

        String fileText = viewModel.linkProperty().get();

        Optional<Path> file = FileHelper.expandFilename(stateManager.getActiveDatabase().get(), fileText,
                Globals.prefs.getFileDirectoryPreferences());

        Path workingDir = file.orElse(Paths.get(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)));
        String fileName = Paths.get(fileText).getFileName().toString();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(workingDir)
                .withInitialFileName(fileName).build();
        Optional<Path> path;
        if (showSaveDialog) {
            path = dialogService.showFileSaveDialog(fileDialogConfiguration);
        } else {
            path = dialogService.showFileOpenDialog(fileDialogConfiguration);
        }
        path.ifPresent(newFile -> {
            // Store the directory for next time:
            Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, newFile.toString());

            // If the file is below the file directory, make the path relative:
            List<Path> fileDirectories = this.stateManager.getActiveDatabase().get()
                    .getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences());
            newFile = FileUtil.shortenFileName(newFile, fileDirectories);

            viewModel.linkProperty().set(newFile.toString());
            tfLink.requestFocus();
        });
    }

    @FXML
    void cancel(ActionEvent event) {
        getStage().close();
    }

    @FXML
    void ok_clicked(ActionEvent event) {

    }

    private void setValues(LinkedFile entry) {
        tfDescription.setText(entry.getDescription());
        tfLink.setText(entry.getLink());

        cmbFileType.getSelectionModel().clearSelection();
        // See what is a reasonable selection for the type combobox:
        Optional<ExternalFileType> fileType = ExternalFileTypes.getInstance().fromLinkedFile(entry, false);
        if (fileType.isPresent() && !(fileType.get() instanceof UnknownExternalFileType)) {
            cmbFileType.getSelectionModel().select(fileType.get());
        } else if ((entry.getLink() != null) && (!entry.getLink().isEmpty())) {
            checkExtension();
        }
    }

    private void checkExtension() {
        if (cmbFileType.getSelectionModel().isEmpty() && (!tfLink.getText().trim().isEmpty())) {

            // Check if this looks like a remote link:
            if (REMOTE_LINK_PATTERN.matcher(tfLink.getText()).matches()) {
                Optional<ExternalFileType> type = ExternalFileTypes.getInstance().getExternalFileTypeByExt("html");
                if (type.isPresent()) {
                    cmbFileType.getSelectionModel().select(type.get());
                    return;
                }
            }

            // Try to guess the file type:
            String theLink = tfLink.getText().trim();
            ExternalFileTypes.getInstance().getExternalFileTypeForName(theLink).ifPresent(type -> cmbFileType.getSelectionModel().select(type));
        }

    }

    @FXML
    void openFile(ActionEvent event) {

        ExternalFileType type = cmbFileType.getSelectionModel().getSelectedItem();
        if (type != null) {
            try {
                JabRefDesktop.openExternalFileAnyFormat(stateManager.getActiveDatabase().get(), viewModel.linkProperty().get(), Optional.of(type));
            } catch (IOException e) {
                LOGGER.error("File could not be opened", e);
            }
        }
    }
}
