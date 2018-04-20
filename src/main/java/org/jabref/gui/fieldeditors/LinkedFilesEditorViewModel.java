package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.externalfiles.AutoSetFileLinksUtil;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.FulltextFetchers;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FileFieldParser;
import org.jabref.model.entry.FileFieldWriter;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.JabRefPreferences;

public class LinkedFilesEditorViewModel extends AbstractEditorViewModel {

    private final ListProperty<LinkedFileViewModel> files = new SimpleListProperty<>(FXCollections.observableArrayList(LinkedFileViewModel::getObservables));
    private final BooleanProperty fulltextLookupInProgress = new SimpleBooleanProperty(false);
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final TaskExecutor taskExecutor;
    private final JabRefPreferences preferences;

    public LinkedFilesEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, DialogService dialogService, BibDatabaseContext databaseContext, TaskExecutor taskExecutor, FieldCheckers fieldCheckers, JabRefPreferences preferences) {
        super(fieldName, suggestionProvider, fieldCheckers);

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
     * TODO: Move this method to {@link LinkedFile} as soon as {@link ExternalFileType} lives in model.
     */
    public static LinkedFile fromFile(Path file, List<Path> fileDirectories) {
        String fileExtension = FileHelper.getFileExtension(file).orElse("");
        ExternalFileType suggestedFileType = ExternalFileTypes.getInstance()
                .getExternalFileTypeByExt(fileExtension)
                .orElse(new UnknownExternalFileType(fileExtension));
        Path relativePath = FileUtil.shortenFileName(file, fileDirectories);
        return new LinkedFile("", relativePath.toString(), suggestedFileType.getName());
    }

    public LinkedFileViewModel fromFile(Path file) {
        List<Path> fileDirectories = databaseContext.getFileDirectoriesAsPaths(preferences.getFileDirectoryPreferences());

        LinkedFile linkedFile = fromFile(file, fileDirectories);
        return new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor);

    }

    public boolean isFulltextLookupInProgress() {
        return fulltextLookupInProgress.get();
    }

    public BooleanProperty fulltextLookupInProgressProperty() {
        return fulltextLookupInProgress;
    }

    private List<LinkedFileViewModel> parseToFileViewModel(String stringValue) {
        return FileFieldParser.parse(stringValue)
                .stream()
                .map(linkedFile -> new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor))
                .collect(Collectors.toList());
    }

    public ObservableList<LinkedFileViewModel> getFiles() {
        return files.get();
    }

    public ListProperty<LinkedFileViewModel> filesProperty() {
        return files;
    }

    public void addNewFile() {
        Path workingDirectory = databaseContext.getFirstExistingFileDir(preferences.getFileDirectoryPreferences())
                                               .orElse(Paths.get(preferences.get(JabRefPreferences.WORKING_DIRECTORY)));

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(workingDirectory)
                .build();

        List<Path> fileDirectories = databaseContext.getFileDirectoriesAsPaths(preferences.getFileDirectoryPreferences());
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(
                newFile -> {
                    LinkedFile newLinkedFile = fromFile(newFile, fileDirectories);
                    files.add(new LinkedFileViewModel(newLinkedFile, entry, databaseContext, taskExecutor));
                });
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        super.bindToEntry(entry);

        if (entry != null) {
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

        AutoSetFileLinksUtil util = new AutoSetFileLinksUtil(databaseContext, preferences.getFileDirectoryPreferences(), preferences.getAutoLinkPreferences(), ExternalFileTypes.getInstance());
        try {
            List<LinkedFile> linkedFiles = util.findAssociatedNotLinkedFiles(entry);
            for (LinkedFile linkedFile : linkedFiles) {
                LinkedFileViewModel newLinkedFile = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor);
                newLinkedFile.markAsAutomaticallyFound();
                result.add(newLinkedFile);
            }
        } catch (IOException e) {
            dialogService.showErrorDialogAndWait("Error accessing the file system", e);
        }

        return result;
    }

    public void fetchFulltext() {
        FulltextFetchers fetcher = new FulltextFetchers(preferences.getImportFormatPreferences());
        BackgroundTask
                .wrap(() -> fetcher.findFullTextPDF(entry))
                .onRunning(() -> fulltextLookupInProgress.setValue(true))
                .onFinished(() -> fulltextLookupInProgress.setValue(false))
                .onSuccess(url -> {
                    if (url.isPresent()) {
                        addFromURL(url.get());
                    } else {
                        dialogService.notify(Localization.lang("No full text document found"));
                    }
                })
                .executeWith(taskExecutor);
    }

    public void addFromURL() {
        Optional<String> urlText = dialogService.showInputDialogAndWait(
                Localization.lang("Download file"), Localization.lang("Enter URL to download"));
        if (urlText.isPresent()) {
            try {
                URL url = new URL(urlText.get());
                addFromURL(url);
            } catch (MalformedURLException exception) {
                dialogService.showErrorDialogAndWait(
                        Localization.lang("Invalid URL"),
                        exception);
            }
        }
    }

    private void addFromURL(URL url) {
        LinkedFileViewModel onlineFile = new LinkedFileViewModel(new LinkedFile(url, ""), entry, databaseContext, taskExecutor);
        files.add(onlineFile);
        onlineFile.download();
    }

    public void deleteFile(LinkedFileViewModel file) {
        if (file.getFile().isOnlineLink()) {
            removeFileLink(file);
        } else {
            boolean deleteSuccessful = file.delete(preferences.getFileDirectoryPreferences());
            if (deleteSuccessful) {
                files.remove(file);
            }
        }
    }

    public void removeFileLink(LinkedFileViewModel file) {
        files.remove(file);
    }
}
