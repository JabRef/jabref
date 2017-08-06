package org.jabref.gui.filelist;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

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
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

public class FileListDialogController extends AbstractController<FileListDialogViewModel> {


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
        viewModel = new FileListDialogViewModel();
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

    @FXML
    void openFile(ActionEvent event) {

    }
}
