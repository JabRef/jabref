package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.swing.SwingUtilities;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import org.jabref.Globals;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.filelist.FileListEntryEditor;
import org.jabref.logic.cleanup.MoveFilesCleanup;
import org.jabref.logic.cleanup.RenamePdfCleanup;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LinkedFileViewModel extends AbstractViewModel {

    private static final Log LOGGER = LogFactory.getLog(LinkedFileViewModel.class);

    private final LinkedFile linkedFile;
    private final BibDatabaseContext databaseContext;
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(-1);
    private final BooleanProperty downloadOngoing = new SimpleBooleanProperty(false);
    private final BooleanProperty isAutomaticallyFound = new SimpleBooleanProperty(false);

    private final DialogService dialogService = new FXDialogService();
    private final BibEntry entry;

    public LinkedFileViewModel(LinkedFile linkedFile, BibEntry entry, BibDatabaseContext databaseContext) {
        this.linkedFile = linkedFile;
        this.databaseContext = databaseContext;
        this.entry = entry;

        downloadOngoing.bind(downloadProgress.greaterThanOrEqualTo(0).and(downloadProgress.lessThan(100)));
    }

    public boolean isAutomaticallyFound() {
        return isAutomaticallyFound.get();
    }

    public BooleanProperty isAutomaticallyFoundProperty() {
        return isAutomaticallyFound;
    }

    public BooleanProperty downloadOngoingProperty() {
        return downloadOngoing;
    }

    public DoubleProperty downloadProgressProperty() {
        return downloadProgress;
    }

    public LinkedFile getFile() {
        return linkedFile;
    }

    public String getLink() {
        return linkedFile.getLink();
    }

    public String getDescription() {
        return linkedFile.getDescription();
    }

    public Optional<Path> findIn(List<Path> directories) {
        return linkedFile.findIn(directories);
    }

    /**
     * TODO: Be a bit smarter and try to infer correct icon, for example using {@link
     * org.jabref.gui.externalfiletype.ExternalFileTypes#getExternalFileTypeByName(String)}
     */
    public GlyphIcons getTypeIcon() {
        return MaterialDesignIcon.FILE_PDF;
    }

    public void markAsAutomaticallyFound() {
        isAutomaticallyFound.setValue(true);
    }

    public void acceptAsLinked() {
        isAutomaticallyFound.setValue(false);
    }

    public Observable[] getObservables() {
        List<Observable> observables = new ArrayList<>(Arrays.asList(linkedFile.getObservables()));
        observables.add(downloadOngoing);
        observables.add(downloadProgress);
        observables.add(isAutomaticallyFound);
        return observables.toArray(new Observable[observables.size()]);

    }

    public void open() {
        try {
            Optional<ExternalFileType> type = ExternalFileTypes.getInstance().fromLinkedFile(linkedFile, true);
            JabRefDesktop.openExternalFileAnyFormat(databaseContext, linkedFile.getLink(), type);
        } catch (IOException e) {
            LOGGER.warn("Cannot open selected file.", e);
        }
    }

    public void openFolder() {
        try {
            Path path = null;
            // absolute path
            if (Paths.get(linkedFile.getLink()).isAbsolute()) {
                path = Paths.get(linkedFile.getLink());
            } else {
                // relative to file folder
                for (Path folder : databaseContext.getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences())) {
                    Path file = folder.resolve(linkedFile.getLink());
                    if (Files.exists(file)) {
                        path = file;
                        break;
                    }
                }
            }
            if (path != null) {
                JabRefDesktop.openFolderAndSelectFile(path);
            } else {
                dialogService.showErrorDialogAndWait(Localization.lang("File not found"));
            }
        } catch (IOException ex) {
            LOGGER.debug("Cannot open folder", ex);
        }
    }

    public void rename() {
        if (linkedFile.isOnlineLink()) {
            // Cannot rename remote links
            return;
        }
        Optional<Path> fileDir = databaseContext.getFirstExistingFileDir(Globals.prefs.getFileDirectoryPreferences());
        if (!fileDir.isPresent()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Rename file"),
                    Localization.lang("File directory is not set or does not exist!"));
            return;
        }

        Optional<Path> file = linkedFile.findIn(databaseContext, Globals.prefs.getFileDirectoryPreferences());
        if ((file.isPresent()) && Files.exists(file.get())) {
            RenamePdfCleanup pdfCleanup = new RenamePdfCleanup(false,
                    databaseContext,
                    Globals.prefs.getCleanupPreferences(Globals.journalAbbreviationLoader).getFileNamePattern(),
                    Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader),
                    Globals.prefs.getFileDirectoryPreferences(), linkedFile);

            String targetFileName = pdfCleanup.getTargetFileName(linkedFile, entry);

            boolean confirm = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Rename file"),
                    Localization.lang("Rename file to") + " " + targetFileName,
                    Localization.lang("Rename file"),
                    Localization.lang("Cancel"));

            if (confirm) {
                Optional<Path> fileConflictCheck = pdfCleanup.findExistingFile(linkedFile, entry);
                performRenameWithConflictCheck(file, pdfCleanup, targetFileName, fileConflictCheck);
            }
        } else {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("File not found"),
                    Localization.lang("Could not find file '%0'.", linkedFile.getLink()));
        }
    }

    private void performRenameWithConflictCheck(Optional<Path> file, RenamePdfCleanup pdfCleanup, String targetFileName, Optional<Path> fileConflictCheck) {
        boolean confirm;
        if (!fileConflictCheck.isPresent()) {
            pdfCleanup.cleanup(entry);
        } else {
            confirm = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("File exists"),
                    Localization.lang("'%0' exists. Overwrite file?", targetFileName),
                    Localization.lang("Overwrite"),
                    Localization.lang("Cancel"));
            if (confirm) {
                FileUtil.renameFile(fileConflictCheck.get(), file.get(), true);
                pdfCleanup.cleanup(entry);
            }
        }
    }

    public void moveToDefaultDirectory() {
        if (linkedFile.isOnlineLink()) {
            // Cannot move remote links
            return;
        }

        // Get target folder
        Optional<Path> fileDir = databaseContext.getFirstExistingFileDir(Globals.prefs.getFileDirectoryPreferences());
        if (!fileDir.isPresent()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Move file"),
                    Localization.lang("File directory is not set or does not exist!"));
            return;
        }

        Optional<Path> file = linkedFile.findIn(databaseContext, Globals.prefs.getFileDirectoryPreferences());
        if ((file.isPresent()) && Files.exists(file.get())) {
            // Linked file exists, so move it
            MoveFilesCleanup moveFiles = new MoveFilesCleanup(databaseContext,
                    Globals.prefs.getCleanupPreferences(Globals.journalAbbreviationLoader).getFileDirPattern(),
                    Globals.prefs.getFileDirectoryPreferences(),
                    Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader), linkedFile);

            boolean confirm = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Move file"),
                    Localization.lang("Move file to file directory?") + " " + fileDir.get(),
                    Localization.lang("Move file"),
                    Localization.lang("Cancel"));
            if (confirm) {
                moveFiles.cleanup(entry);
            }
        } else {
            // File doesn't exist, so we can't move it.
            dialogService.showErrorDialogAndWait(
                    Localization.lang("File not found"),
                    Localization.lang("Could not find file '%0'.", linkedFile.getLink()));
        }
    }

    public boolean delete() {
        Optional<Path> file = linkedFile.findIn(databaseContext, Globals.prefs.getFileDirectoryPreferences());
        if (file.isPresent()) {

            ButtonType removeFromEntry = new ButtonType(Localization.lang("Remove from entry"));

            ButtonType deleteFromEntry = new ButtonType(Localization.lang("Delete from disk"));
            Optional<ButtonType> buttonType = dialogService.showCustomButtonDialogAndWait(AlertType.INFORMATION,
                    Localization.lang("Delete '%0'", file.get().toString()),
                    Localization.lang("Delete the selected file permanently from disk, or just remove the file from the entry? Pressing Delete will delete the file permanently from disk."),
                    deleteFromEntry, removeFromEntry, ButtonType.CANCEL);

            if (buttonType.isPresent()) {
                if (buttonType.get().equals(removeFromEntry)) {
                    return true;
                }
                if (buttonType.get().equals(deleteFromEntry)) {

                    try {
                        Files.delete(file.get());
                        return true;
                    } catch (IOException ex) {
                        dialogService.showErrorDialogAndWait(
                                Localization.lang("Cannot delete file"),
                                Localization.lang("File permission error"));
                        LOGGER.warn("File permission error while deleting: " + linkedFile, ex);
                    }
                }
            } else {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("File not found"),
                        Localization.lang("Could not find file '%0'.", linkedFile.getLink()));
                return true;
            }
        }

        return false;
    }

    public void edit() {
        FileListEntryEditor editor = new FileListEntryEditor(linkedFile, false, true, databaseContext);
        SwingUtilities.invokeLater(() -> editor.setVisible(true, false));

    }
}
