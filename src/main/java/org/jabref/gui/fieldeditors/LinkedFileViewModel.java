package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.swing.SwingUtilities;
import javax.xml.transform.TransformerException;

import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import org.jabref.Globals;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.externalfiles.DownloadExternalFile;
import org.jabref.gui.externalfiles.FileDownloadTask;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.filelist.FileListEntryEditor;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.MoveFilesCleanup;
import org.jabref.logic.cleanup.RenamePdfCleanup;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.preferences.JabRefPreferences;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javafx.scene.control.ButtonBar.ButtonData;

public class LinkedFileViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFileViewModel.class);

    private final LinkedFile linkedFile;
    private final BibDatabaseContext databaseContext;
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(-1);
    private final BooleanProperty downloadOngoing = new SimpleBooleanProperty(false);
    private final BooleanProperty isAutomaticallyFound = new SimpleBooleanProperty(false);
    private final BooleanProperty canWriteXMPMetadata = new SimpleBooleanProperty(false);
    private final DialogService dialogService;
    private final BibEntry entry;
    private final TaskExecutor taskExecutor;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private final CleanupPreferences cleanupPreferences;
    private final LayoutFormatterPreferences layoutFormatterPreferences;
    private final XmpPreferences xmpPreferences;
    private final String fileNamePattern;

    /**
     * @deprecated use {@link #LinkedFileViewModel(LinkedFile, BibEntry, BibDatabaseContext, TaskExecutor, DialogService, JabRefPreferences, JournalAbbreviationLoader)} instead
     */
    @Deprecated
    public LinkedFileViewModel(LinkedFile linkedFile, BibEntry entry, BibDatabaseContext databaseContext, TaskExecutor taskExecutor) {
        this(linkedFile, entry, databaseContext, taskExecutor, new FXDialogService(), Globals.prefs, Globals.journalAbbreviationLoader);
    }

    public LinkedFileViewModel(LinkedFile linkedFile, BibEntry entry, BibDatabaseContext databaseContext,
                               TaskExecutor taskExecutor, DialogService dialogService, JabRefPreferences preferences, JournalAbbreviationLoader abbreviationLoader) {
        this.linkedFile = linkedFile;
        this.databaseContext = databaseContext;
        this.entry = entry;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;

        cleanupPreferences = preferences.getCleanupPreferences(abbreviationLoader);
        layoutFormatterPreferences = preferences.getLayoutFormatterPreferences(abbreviationLoader);
        xmpPreferences = preferences.getXMPPreferences();
        fileNamePattern = preferences.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        fileDirectoryPreferences = preferences.getFileDirectoryPreferences();

        downloadOngoing.bind(downloadProgress.greaterThanOrEqualTo(0).and(downloadProgress.lessThan(1)));
        canWriteXMPMetadata.setValue(!linkedFile.isOnlineLink() && linkedFile.getFileType().equalsIgnoreCase("pdf"));
    }

    public BooleanProperty canWriteXMPMetadataProperty() {
        return canWriteXMPMetadata;
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

    public StringProperty linkProperty() {
        return linkedFile.linkProperty();
    }

    public StringProperty descriptionProperty() {
        return linkedFile.descriptionProperty();
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
                for (Path folder : databaseContext.getFileDirectoriesAsPaths(fileDirectoryPreferences)) {
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
        Optional<Path> fileDir = databaseContext.getFirstExistingFileDir(fileDirectoryPreferences);
        if (!fileDir.isPresent()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Rename file"),
                    Localization.lang("File directory is not set or does not exist!"));
            return;
        }

        Optional<Path> file = linkedFile.findIn(databaseContext, fileDirectoryPreferences);
        if ((file.isPresent()) && Files.exists(file.get())) {
            RenamePdfCleanup pdfCleanup = new RenamePdfCleanup(false,
                    databaseContext,
                    cleanupPreferences.getFileNamePattern(),
                    layoutFormatterPreferences,
                    fileDirectoryPreferences, linkedFile);

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
            try {
                pdfCleanup.cleanupWithException(entry);
            } catch (IOException e) {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Rename failed"),
                        Localization.lang("JabRef cannot access the file because it is being used by another process."));
            }
        } else {
            confirm = dialogService.showConfirmationDialogAndWait(
                    Localization.lang("File exists"),
                    Localization.lang("'%0' exists. Overwrite file?", targetFileName),
                    Localization.lang("Overwrite"),
                    Localization.lang("Cancel"));
            if (confirm) {
                try {
                    FileUtil.renameFileWithException(fileConflictCheck.get(), file.get(), true);
                    pdfCleanup.cleanupWithException(entry);
                } catch (IOException e) {
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("Rename failed"),
                            Localization.lang("JabRef cannot access the file because it is being used by another process."));
                }
            }
        }
    }

    public void moveToDefaultDirectory() {
        if (linkedFile.isOnlineLink()) {
            // Cannot move remote links
            return;
        }

        // Get target folder
        Optional<Path> fileDir = databaseContext.getFirstExistingFileDir(fileDirectoryPreferences);
        if (!fileDir.isPresent()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Move file"),
                    Localization.lang("File directory is not set or does not exist!"));
            return;
        }

        Optional<Path> file = linkedFile.findIn(databaseContext, fileDirectoryPreferences);
        if ((file.isPresent()) && Files.exists(file.get())) {
            // Linked file exists, so move it
            MoveFilesCleanup moveFiles = new MoveFilesCleanup(databaseContext,
                    cleanupPreferences.getFileDirPattern(),
                    fileDirectoryPreferences,
                    layoutFormatterPreferences, linkedFile);

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

    public boolean delete(FileDirectoryPreferences prefs) {
        Optional<Path> file = linkedFile.findIn(databaseContext, prefs);

        if (!file.isPresent()) {
            LOGGER.warn("Could not find file " + linkedFile.getLink());
            return true;
        }

        ButtonType removeFromEntry = new ButtonType(Localization.lang("Remove from entry"), ButtonData.YES);
        ButtonType deleteFromEntry = new ButtonType(Localization.lang("Delete from disk"));
        Optional<ButtonType> buttonType = dialogService.showCustomButtonDialogAndWait(AlertType.INFORMATION,
                Localization.lang("Delete '%0'", file.get().toString()),
                Localization.lang("Delete the selected file permanently from disk, or just remove the file from the entry? Pressing Delete will delete the file permanently from disk."),
                removeFromEntry, deleteFromEntry, ButtonType.CANCEL);

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
        }
        return false;
    }

    public void edit() {
        FileListEntryEditor editor = new FileListEntryEditor(linkedFile, false, true, databaseContext);
        SwingUtilities.invokeLater(() -> editor.setVisible(true, false));
    }

    public void writeXMPMetadata() {
        // Localization.lang("Writing XMP-metadata...")
        BackgroundTask<Void> writeTask = BackgroundTask.wrap(() -> {
            Optional<Path> file = linkedFile.findIn(databaseContext, fileDirectoryPreferences);
            if (!file.isPresent()) {
                // TODO: Print error message
                // Localization.lang("PDF does not exist");
            } else {
                try {
                    XmpUtilWriter.writeXmp(file.get(), entry, databaseContext.getDatabase(), xmpPreferences);
                } catch (IOException | TransformerException ex) {
                    // TODO: Print error message
                    // Localization.lang("Error while writing") + " '" + file.toString() + "': " + ex;
                }
            }
            return null;
        });

        // Localization.lang("Finished writing XMP-metadata.")

        // TODO: Show progress
        taskExecutor.execute(writeTask);
    }

    public void download() {
        if (!linkedFile.isOnlineLink()) {
            throw new UnsupportedOperationException("In order to download the file it has to be an online link");
        }

        try {
            URLDownload urlDownload = new URLDownload(linkedFile.getLink());
            Optional<ExternalFileType> suggestedType = inferFileType(urlDownload);
            String suggestedTypeName = suggestedType.map(ExternalFileType::getName).orElse("");
            linkedFile.setFileType(suggestedTypeName);

            Optional<Path> targetDirectory = databaseContext.getFirstExistingFileDir(fileDirectoryPreferences);
            if (!targetDirectory.isPresent()) {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Download file"),
                        Localization.lang("File directory is not set or does not exist!"));
                return;
            }
            String suffix = suggestedType.map(ExternalFileType::getExtension).orElse("");
            String suggestedName = getSuggestedFileName(suffix);
            Path destination = targetDirectory.get().resolve(suggestedName);

            BackgroundTask<Void> downloadTask = new FileDownloadTask(urlDownload.getSource(), destination)
                    .onSuccess(event -> {
                        LinkedFile newLinkedFile = LinkedFilesEditorViewModel.fromFile(destination, databaseContext.getFileDirectoriesAsPaths(fileDirectoryPreferences));
                        linkedFile.setLink(newLinkedFile.getLink());
                        linkedFile.setFileType(newLinkedFile.getFileType());
                    })
                    .onFailure(ex -> dialogService.showErrorDialogAndWait("Download failed", ex));

            downloadProgress.bind(downloadTask.workDonePercentageProperty());
            taskExecutor.execute(downloadTask);
        } catch (MalformedURLException exception) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Invalid URL"),
                    exception);
        }
    }

    private Optional<ExternalFileType> inferFileType(URLDownload urlDownload) {
        Optional<ExternalFileType> suggestedType = inferFileTypeFromMimeType(urlDownload);

        // If we did not find a file type from the MIME type, try based on extension:
        if (!suggestedType.isPresent()) {
            suggestedType = inferFileTypeFromURL(urlDownload.getSource().toExternalForm());
        }
        return suggestedType;
    }

    private Optional<ExternalFileType> inferFileTypeFromMimeType(URLDownload urlDownload) {
        // TODO: what if this takes long time?
        String mimeType = urlDownload.getMimeType();

        if (mimeType != null) {
            LOGGER.debug("MIME Type suggested: " + mimeType);
            return ExternalFileTypes.getInstance().getExternalFileTypeByMimeType(mimeType);
        } else {
            return Optional.empty();
        }
    }

    private Optional<ExternalFileType> inferFileTypeFromURL(String url) {
        String extension = DownloadExternalFile.getSuffix(url);
        if (extension != null) {
            return ExternalFileTypes.getInstance().getExternalFileTypeByExt(extension);
        } else {
            return Optional.empty();
        }
    }

    private String getSuggestedFileName(String suffix) {
        String plannedName = FileUtil.createFileNameFromPattern(databaseContext.getDatabase(), entry, fileNamePattern);

        if (!suffix.isEmpty()) {
            plannedName += "." + suffix;
        }
        return plannedName;
    }

    public LinkedFile getFile() {
        return linkedFile;
    }
}
