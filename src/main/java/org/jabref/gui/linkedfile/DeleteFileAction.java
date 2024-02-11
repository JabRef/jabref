package org.jabref.gui.linkedfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteFileAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteFileAction.class);

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final BibDatabaseContext databaseContext;
    private final LinkedFilesEditorViewModel viewModel;
    private final ListView<LinkedFileViewModel> listView;

    public DeleteFileAction(DialogService dialogService,
                            PreferencesService preferences,
                            BibDatabaseContext databaseContext,
                            LinkedFilesEditorViewModel viewModel,
                            ListView<LinkedFileViewModel> listView) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.databaseContext = databaseContext;
        this.viewModel = viewModel;
        this.listView = listView;
    }

    @Override
    public void execute() {
        List<LinkedFileViewModel> toBeDeleted = List.copyOf(listView.getSelectionModel().getSelectedItems());

        if (toBeDeleted.isEmpty()) {
            dialogService.notify(Localization.lang("This operation requires selected linked files."));
            return;
        }

        String dialogTitle;
        String dialogContent;

        if (toBeDeleted.size() != 1) {
            dialogTitle = Localization.lang("Delete %0 files", toBeDeleted.size());
            dialogContent = Localization.lang("Delete %0 files permanently from disk, or just remove the files from the entry? " +
                    "Pressing Delete will delete the files permanently from disk.", toBeDeleted.size());
        } else {
            Optional<Path> file = toBeDeleted.getFirst().getFile().findIn(databaseContext, preferences.getFilePreferences());

            if (file.isPresent()) {
                dialogTitle = Localization.lang("Delete '%0'", file.get().getFileName().toString());
                dialogContent = Localization.lang("Delete '%0' permanently from disk, or just remove the file from the entry? " +
                        "Pressing Delete will delete the file permanently from disk.", file.get().toString());
            } else {
                dialogService.notify(Localization.lang("Error accessing file '%0'.", toBeDeleted.getFirst().getFile().getLink()));
                return;
            }
        }

        ButtonType removeFromEntry = new ButtonType(Localization.lang("Remove from entry"), ButtonBar.ButtonData.YES);
        ButtonType deleteFromEntry = new ButtonType(Localization.lang("Delete from disk"));
        Optional<ButtonType> buttonType = dialogService.showCustomButtonDialogAndWait(Alert.AlertType.INFORMATION,
                dialogTitle, dialogContent, removeFromEntry, deleteFromEntry, ButtonType.CANCEL);

        if (buttonType.isPresent()) {
            if (buttonType.get().equals(removeFromEntry)) {
                deleteFiles(toBeDeleted, false);
            }

            if (buttonType.get().equals(deleteFromEntry)) {
                deleteFiles(toBeDeleted, true);
            }
        }
    }

    /**
     * Deletes the files from the entry and optionally from disk.
     *
     * @param toBeDeleted the files to be deleted
     * @param deleteFromDisk if true, the files are deleted from disk, otherwise they are only removed from the entry
     */
    private void deleteFiles(List<LinkedFileViewModel> toBeDeleted, boolean deleteFromDisk) {
        for (LinkedFileViewModel fileViewModel : toBeDeleted) {
            if (fileViewModel.getFile().isOnlineLink()) {
                viewModel.removeFileLink(fileViewModel);
            } else {
                if (deleteFromDisk) {
                    deleteFileFromDisk(fileViewModel);
                }
                viewModel.getFiles().remove(fileViewModel);
            }
        }
    }

    /**
     * Deletes the file from disk without asking the user for confirmation.
     *
     * @param fileViewModel the file to be deleted
     */
    public void deleteFileFromDisk(LinkedFileViewModel fileViewModel) {
        LinkedFile linkedFile = fileViewModel.getFile();

        Optional<Path> file = linkedFile.findIn(databaseContext, preferences.getFilePreferences());

        if (file.isEmpty()) {
            LOGGER.warn("Could not find file {}", linkedFile.getLink());
        }

        if (file.isPresent()) {
            try {
                Files.delete(file.get());
            } catch (
                    IOException ex) {
                dialogService.showErrorDialogAndWait(Localization.lang("Cannot delete file"), Localization.lang("File permission error"));
                LOGGER.warn("File permission error while deleting: {}", linkedFile, ex);
            }
        } else {
            dialogService.notify(Localization.lang("Error accessing file '%0'.", linkedFile.getLink()));
        }
    }
}
