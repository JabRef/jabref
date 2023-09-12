package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.externalfiles.AutoSetFileLinksUtil;
import org.jabref.gui.externalfiletype.CustomExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.linkedfile.AttachFileFromURLAction;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.importer.FulltextFetchers;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileNameCleaner;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkedFilesEditorViewModel extends AbstractEditorViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFilesEditorViewModel.class);

    private final ListProperty<LinkedFileViewModel> files = new SimpleListProperty<>(FXCollections.observableArrayList(LinkedFileViewModel::getObservables));
    private final BooleanProperty fulltextLookupInProgress = new SimpleBooleanProperty(false);
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferences;

    public LinkedFilesEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider,
                                      DialogService dialogService,
                                      BibDatabaseContext databaseContext,
                                      TaskExecutor taskExecutor,
                                      FieldCheckers fieldCheckers,
                                      PreferencesService preferences,
                                      UndoManager undoManager) {

        super(field, suggestionProvider, fieldCheckers, undoManager);

        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;

        BindingsHelper.bindContentBidirectional(
                files,
                text,
                LinkedFilesEditorViewModel::getStringRepresentation,
                this::parseToFileViewModel);
    }

    private static String getStringRepresentation(List<LinkedFileViewModel> files) {
        // Only serialize linked files, not the ones that are automatically found
        List<LinkedFile> filesToSerialize = files.stream()
                                                 .filter(file -> !file.isAutomaticallyFound())
                                                 .map(LinkedFileViewModel::getFile)
                                                 .collect(Collectors.toList());

        return FileFieldWriter.getStringRepresentation(filesToSerialize);
    }

    /**
     * Creates an instance of {@link LinkedFile} based on the given file.
     * We try to guess the file type and relativize the path against the given file directories.
     *
     * TODO: Move this method to {@link LinkedFile} as soon as {@link CustomExternalFileType} lives in model.
     */
    public static LinkedFile fromFile(Path file, List<Path> fileDirectories, FilePreferences filePreferences) {
        String fileExtension = FileUtil.getFileExtension(file).orElse("");
        ExternalFileType suggestedFileType = ExternalFileTypes.getExternalFileTypeByExt(fileExtension, filePreferences)
                                                              .orElse(new UnknownExternalFileType(fileExtension));
        Path relativePath = FileUtil.relativize(file, fileDirectories);
        return new LinkedFile("", relativePath, suggestedFileType.getName());
    }

    public LinkedFileViewModel fromFile(Path file, FilePreferences filePreferences) {
        List<Path> fileDirectories = databaseContext.getFileDirectories(preferences.getFilePreferences());

        LinkedFile linkedFile = fromFile(file, fileDirectories, filePreferences);
        return new LinkedFileViewModel(
                linkedFile,
                entry,
                databaseContext,
                taskExecutor,
                dialogService,
                preferences);
    }

    private List<LinkedFileViewModel> parseToFileViewModel(String stringValue) {
        return FileFieldParser.parse(stringValue).stream()
                              .map(linkedFile -> new LinkedFileViewModel(
                                      linkedFile,
                                      entry,
                                      databaseContext,
                                      taskExecutor,
                                      dialogService,
                                      preferences))
                              .collect(Collectors.toList());
    }

    public ObservableList<LinkedFileViewModel> getFiles() {
        return files.get();
    }

    public ListProperty<LinkedFileViewModel> filesProperty() {
        return files;
    }

    public void addNewFile() {
        Path workingDirectory = databaseContext.getFirstExistingFileDir(preferences.getFilePreferences())
                                               .orElse(preferences.getFilePreferences().getWorkingDirectory());

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(workingDirectory)
                .build();

        List<Path> fileDirectories = databaseContext.getFileDirectories(preferences.getFilePreferences());
        List<Path> selectedFiles = dialogService.showFileOpenDialogAndGetMultipleFiles(fileDialogConfiguration);

        for (Path fileToAdd : selectedFiles) {
            if (FileUtil.detectBadFileName(fileToAdd.toString())) {
                String newFilename = FileNameCleaner.cleanFileName(fileToAdd.getFileName().toString());

                boolean correctButtonPressed = dialogService.showConfirmationDialogAndWait(Localization.lang("File \"%0\" cannot be added!", fileToAdd.getFileName()),
                        Localization.lang("Illegal characters in the file name detected.\nFile will be renamed to \"%0\" and added.", newFilename),
                        Localization.lang("Rename and add"));

                if (correctButtonPressed) {
                    Path correctPath = fileToAdd.resolveSibling(newFilename);
                    try {
                        Files.move(fileToAdd, correctPath);
                        addNewLinkedFile(correctPath, fileDirectories);
                    } catch (IOException ex) {
                        LOGGER.error("Error moving file", ex);
                        dialogService.showErrorDialogAndWait(ex);
                    }
                }
            } else {
                addNewLinkedFile(fileToAdd, fileDirectories);
            }
        }
    }

    private void addNewLinkedFile(Path correctPath, List<Path> fileDirectories) {
        LinkedFile newLinkedFile = fromFile(correctPath, fileDirectories, preferences.getFilePreferences());
        files.add(new LinkedFileViewModel(
                newLinkedFile,
                entry,
                databaseContext,
                taskExecutor,
                dialogService,
                preferences));
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        super.bindToEntry(entry);

        if ((entry != null) && preferences.getEntryEditorPreferences().autoLinkFilesEnabled()) {
            BackgroundTask<List<LinkedFileViewModel>> findAssociatedNotLinkedFiles = BackgroundTask
                    .wrap(() -> findAssociatedNotLinkedFiles(entry))
                    .onSuccess(files::addAll);
            taskExecutor.execute(findAssociatedNotLinkedFiles);
        }
    }

    /**
     * Find files that are probably associated  to the given entry but not yet linked.
     */
    private List<LinkedFileViewModel> findAssociatedNotLinkedFiles(BibEntry entry) {
        List<LinkedFileViewModel> result = new ArrayList<>();

        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(
                databaseContext,
                preferences.getFilePreferences(),
                preferences.getAutoLinkPreferences());
        try {
            List<LinkedFile> linkedFiles = util.findAssociatedNotLinkedFiles(entry);
            for (LinkedFile linkedFile : linkedFiles) {
                LinkedFileViewModel newLinkedFile = new LinkedFileViewModel(
                        linkedFile,
                        entry,
                        databaseContext,
                        taskExecutor,
                        dialogService,
                        preferences);
                newLinkedFile.markAsAutomaticallyFound();
                result.add(newLinkedFile);
            }
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait("Error accessing the file system", e);
        }

        return result;
    }

    public boolean downloadFile(String urlText) {
        try {
            URL url = new URL(urlText);
            addFromURLAndDownload(url);
            return true;
        } catch (MalformedURLException exception) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Invalid URL"),
                    exception);
            return false;
        }
    }

    public void fetchFulltext() {
        FulltextFetchers fetcher = new FulltextFetchers(
                preferences.getImportFormatPreferences(),
                preferences.getImporterPreferences());
        Optional<String> urlField = entry.getField(StandardField.URL);
        boolean download_success = false;
        if (urlField.isPresent()) {
            download_success = downloadFile(urlField.get());
        }
        if (urlField.isEmpty() || !download_success) {
            BackgroundTask
                .wrap(() -> fetcher.findFullTextPDF(entry))
                .onRunning(() -> fulltextLookupInProgress.setValue(true))
                .onFinished(() -> fulltextLookupInProgress.setValue(false))
                .onSuccess(url -> {
                    if (url.isPresent()) {
                        addFromURLAndDownload(url.get());
                    } else {
                        dialogService.notify(Localization.lang("No full text document found"));
                    }
                })
                .executeWith(taskExecutor);
        }
    }

    public void addFromURL() {
        AttachFileFromURLAction.getUrlForDownloadFromClipBoardOrEntry(dialogService, entry)
                               .ifPresent(this::downloadFile);
    }

    private void addFromURLAndDownload(URL url) {
        LinkedFileViewModel onlineFile = new LinkedFileViewModel(
                new LinkedFile(url, ""),
                entry,
                databaseContext,
                taskExecutor,
                dialogService,
                preferences);
        files.add(onlineFile);
        onlineFile.download();
    }

    public void deleteFile(LinkedFileViewModel file) {
        if (file.getFile().isOnlineLink()) {
            removeFileLink(file);
        } else {
            boolean deleteSuccessful = file.delete();
            if (deleteSuccessful) {
                files.remove(file);
            }
        }
    }

    public void removeFileLink(LinkedFileViewModel file) {
        files.remove(file);
    }

    public ReadOnlyBooleanProperty fulltextLookupInProgressProperty() {
        return fulltextLookupInProgress;
    }
}
