package org.jabref.gui.linkedfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class DeleteFileAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteFileAction.class);

    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final BibDatabaseContext databaseContext;
    private final @Nullable LinkedFilesEditorViewModel viewModel;
    private final List<LinkedFileViewModel> filesToDelete;
    private boolean success = false;

    public DeleteFileAction(DialogService dialogService,
                            FilePreferences filePreferences,
                            BibDatabaseContext databaseContext,
                            @Nullable LinkedFilesEditorViewModel viewModel,
                            List<LinkedFileViewModel> filesToDelete) {
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;
        this.databaseContext = databaseContext;
        this.viewModel = viewModel;
        this.filesToDelete = List.copyOf(filesToDelete);
    }

    public DeleteFileAction(DialogService dialogService,
                            FilePreferences filePreferences,
                            BibDatabaseContext databaseContext,
                            List<LinkedFileViewModel> filesToDelete) {
        this(dialogService, filePreferences, databaseContext, null, filesToDelete);
    }

    @Override
    public void execute() {
        if (filesToDelete.isEmpty()) {
            dialogService.notify(Localization.lang("This operation requires selected linked files."));
            return;
        }

        if (!filePreferences.confirmDeleteLinkedFile()) {
            LOGGER.info("Deleting {} files without confirmation.", filesToDelete.size());
            deleteFiles(true);
            return;
        }

        String dialogTitle;
        String dialogDescription;

        int numberOfLinkedFiles = filesToDelete.size();
        dialogDescription = Localization.lang("Delete %0 file(s) permanently from disk - or remove the file(s) from the entry?", numberOfLinkedFiles);
        if (numberOfLinkedFiles != 1) {
            dialogTitle = Localization.lang("Delete %0 files", numberOfLinkedFiles);
        } else {
            LinkedFile linkedFile = filesToDelete.getFirst().getFile();
            Optional<Path> file = linkedFile.findIn(databaseContext, filePreferences);
            if (file.isPresent()) {
                Path path = file.get();
                dialogTitle = Localization.lang("Delete '%0'", path.getFileName().toString());
            } else {
                dialogService.notify(Localization.lang("Error accessing file '%0'.", linkedFile.getLink()));
                return;
            }
        }

        DialogPane dialogPane = createDeleteFilesDialog(dialogDescription);

        ButtonType removeFromEntry = new ButtonType(Localization.lang("Remove from entry"), ButtonBar.ButtonData.YES);
        ButtonType deleteFromEntry = new ButtonType(Localization.lang("Delete from disk"));

        Optional<ButtonType> buttonType = dialogService.showCustomDialogAndWait(
                dialogTitle, dialogPane, removeFromEntry, deleteFromEntry, ButtonType.CANCEL);

        if (buttonType.isPresent()) {
            ButtonType theButtonType = buttonType.get();
            if (theButtonType.equals(removeFromEntry)) {
                deleteFiles(false);
            } else if (theButtonType.equals(deleteFromEntry)) {
                deleteFiles(true);
            }
        }
    }

    private DialogPane createDeleteFilesDialog(String description) {
        JabRefIconView warning = new JabRefIconView(IconTheme.JabRefIcons.WARNING);
        warning.setGlyphSize(24.0);
        Label header = new Label(description, warning);
        header.setWrapText(true);
        header.setStyle("""
                -fx-padding: 10px;
                -fx-background-color: -fx-background;""");

        ListView<LinkedFileViewModel> filesToDeleteList = new ListView<>(FXCollections.observableArrayList(filesToDelete));
        new ViewModelListCellFactory<LinkedFileViewModel>()
                .withText((item) -> item.getFile().getLink())
                .install(filesToDeleteList);

        VBox content = new VBox(header, filesToDeleteList);
        DialogPane dialogPane = new DialogPane();
        dialogPane.setHeader(header);
        dialogPane.setContent(content);
        return dialogPane;
    }

    /**
     * Deletes the files from the entry and optionally from disk.
     *
     * @param deleteFromDisk if true, the files are deleted from disk, otherwise they are only removed from the entry
     */
    private void deleteFiles(boolean deleteFromDisk) {
        for (LinkedFileViewModel fileViewModel : filesToDelete) {
            if (!fileViewModel.getFile().isOnlineLink() && deleteFromDisk) {
                deleteFileHelper(databaseContext, fileViewModel.getFile());
            }
            if (viewModel != null) {
                viewModel.removeFileLink(fileViewModel);
            }
        }
    }

    /**
     * Helper method to delete the specified file from disk
     *
     * @param linkedFile The LinkedFile (file which linked to an entry) to be deleted from disk
     */
    private void deleteFileHelper(BibDatabaseContext databaseContext, LinkedFile linkedFile) {
        Optional<Path> file = linkedFile.findIn(databaseContext, filePreferences);

        if (file.isEmpty()) {
            LOGGER.warn("Could not find file {}", linkedFile.getLink());
            dialogService.notify(Localization.lang("Error accessing file '%0'.", linkedFile.getLink()));
            return;
        }

        Path theFile = file.get();
        try {
            if (filePreferences.moveToTrash()) {
                JabRefDesktop.moveToTrash(theFile);
            } else {
                Files.delete(theFile);
            }
            success = true;
        } catch (IOException ex) {
            success = false;
            dialogService.showErrorDialogAndWait(Localization.lang("Cannot delete file '%0'", theFile), Localization.lang("File permission error"));
            LOGGER.warn("Error while deleting: {}", linkedFile, ex);
        }
    }

    public boolean isSuccess() {
        return success;
    }
}
