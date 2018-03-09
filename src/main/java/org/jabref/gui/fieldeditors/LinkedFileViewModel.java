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
import org.jabref.logic.cleanup.MoveFilesCleanup;
import org.jabref.logic.cleanup.RenamePdfCleanup;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;
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

    public LinkedFileViewModel(LinkedFile linkedFile, BibEntry entry, BibDatabaseContext databaseContext, TaskExecutor taskExecutor) {
        this(linkedFile, entry, databaseContext, taskExecutor, new FXDialogService());
    }

    protected LinkedFileViewModel(LinkedFile linkedFile, BibEntry entry, BibDatabaseContext databaseContext,
                                  TaskExecutor taskExecutor, DialogService dialogService) {
        this.linkedFile = linkedFile;
        this.databaseContext = databaseContext;
        this.entry = entry;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;

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
            Optional<Path> file = linkedFile.findIn(databaseContext, Globals.prefs.getFileDirectoryPreferences());
            if (!file.isPresent()) {
                // TODO: Print error message
                // Localization.lang("PDF does not exist");
            } else {
                try {
                    XmpUtilWriter.writeXmp(file.get(), entry, databaseContext.getDatabase(), Globals.prefs.getXMPPreferences());
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
            List<Path> fileDirectories = databaseContext.getFileDirectoriesAsPaths(Globals.prefs.getFileDirectoryPreferences());

            Path destination = constructSuggestedPath(suggestedType, fileDirectories);

            BackgroundTask<Void> downloadTask = new FileDownloadTask(urlDownload.getSource(), destination)
                    .onSuccess(event -> {
                        LinkedFile newLinkedFile = LinkedFilesEditorViewModel.fromFile(destination, fileDirectories);
                        linkedFile.setLink(newLinkedFile.getLink());
                        linkedFile.setFileType(newLinkedFile.getFileType());
                    })
                    .onFailure(ex -> dialogService.showErrorDialogAndWait("", ex));

            downloadProgress.bind(downloadTask.workDonePercentageProperty());
            taskExecutor.execute(downloadTask);
        } catch (MalformedURLException exception) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Invalid URL"),
                    exception);
        }
    }

    private Path constructSuggestedPath(Optional<ExternalFileType> suggestedType, List<Path> fileDirectories) {
        String suffix = suggestedType.map(ExternalFileType::getExtension).orElse("");
        String suggestedName = getSuggestedFileName(suffix);
        Path directory;
        if (fileDirectories.isEmpty()) {
            directory = null;
        } else {
            directory = fileDirectories.get(0);
        }
        final Path suggestDir = directory == null ? Paths.get(System.getProperty("user.home")) : directory;
        return suggestDir.resolve(suggestedName);
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
        try {
            // TODO: what if this takes long time?
            String mimeType = urlDownload.getMimeType(); // Read MIME type
            if (mimeType != null) {
                LOGGER.debug("MIME Type suggested: " + mimeType);
                return ExternalFileTypes.getInstance().getExternalFileTypeByMimeType(mimeType);
            } else {
                return Optional.empty();
            }
        } catch (IOException ex) {
            LOGGER.debug("Error while inferring MIME type for URL " + urlDownload.getSource(), ex);
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
        String plannedName = FileUtil.createFileNameFromPattern(databaseContext.getDatabase(), entry,
                Globals.prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN));

        if (!suffix.isEmpty()) {
            plannedName += "." + suffix;
        }

        /*
        * [ 1548875 ] download pdf produces unsupported filename
        *
        * http://sourceforge.net/tracker/index.php?func=detail&aid=1548875&group_id=92314&atid=600306
        * FIXME: rework this! just allow alphanumeric stuff or so?
        * https://msdn.microsoft.com/en-us/library/windows/desktop/aa365247(v=vs.85).aspx#naming_conventions
        * http://superuser.com/questions/358855/what-characters-are-safe-in-cross-platform-file-names-for-linux-windows-and-os
        * https://support.apple.com/en-us/HT202808
        */
        if (OS.WINDOWS) {
            plannedName = plannedName.replaceAll("\\?|\\*|\\<|\\>|\\||\\\"|\\:|\\.$|\\[|\\]", "");
        } else if (OS.OS_X) {
            plannedName = plannedName.replace(":", "");
        }

        return plannedName;
    }

    public LinkedFile getFile() {
        return linkedFile;
    }
}
