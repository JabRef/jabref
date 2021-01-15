package org.jabref.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.crawler.Crawler;
import org.jabref.logic.crawler.git.GitHandler;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartLiteratureReviewAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartLiteratureReviewAction.class);
    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final Path workingDirectory;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferencesService;
    private final ImportFormatPreferences importFormatPreferneces;
    private final SavePreferences savePreferences;

    public StartLiteratureReviewAction(JabRefFrame frame, FileUpdateMonitor fileUpdateMonitor, Path standardWorkingDirectory, TaskExecutor taskExecutor, PreferencesService preferencesService, ImportFormatPreferences importFormatPreferences, SavePreferences savePreferences) {
        this.frame = frame;
        this.dialogService = frame.getDialogService();
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.workingDirectory = getInitialDirectory(standardWorkingDirectory);
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;
        this.importFormatPreferneces = importFormatPreferences;
        this.savePreferences = savePreferences;
    }

    @Override
    public void execute() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(workingDirectory)
                .build();

        Optional<Path> studyDefinitionFile = dialogService.showFileOpenDialog(fileDialogConfiguration);
        if (studyDefinitionFile.isEmpty()) {
            // Do nothing if selection was canceled
            return;
        }
        final Crawler crawler;
        try {
            crawler = new Crawler(studyDefinitionFile.get(), new GitHandler(studyDefinitionFile.get().getParent()), fileUpdateMonitor, importFormatPreferneces, savePreferences, preferencesService.getTimestampPreferences(), new BibEntryTypesManager());
        } catch (IOException | ParseException | GitAPIException e) {
            LOGGER.error("Error during reading of study definition file.", e);
            dialogService.showErrorDialogAndWait(Localization.lang("Error during reading of study definition file."), e);
            return;
        }
        BackgroundTask.wrap(() -> {
            crawler.performCrawl();
            return 0; // Return any value to make this a callable instead of a runnable. This allows throwing exceptions.
        })
                      .onFailure(e -> {
                          LOGGER.error("Error during persistence of crawling results.");
                          dialogService.showErrorDialogAndWait(Localization.lang("Error during persistence of crawling results."), e);
                      })
                      .onSuccess(unused -> new OpenDatabaseAction(frame, preferencesService, dialogService).openFile(Path.of(studyDefinitionFile.get().getParent().toString(), "studyResult.bib"), true))
                      .executeWith(taskExecutor);
    }

    /**
     * @return Path of current panel database directory or the standard working directory
     */
    private Path getInitialDirectory(Path standardWorkingDirectory) {
        if (frame.getBasePanelCount() == 0) {
            return standardWorkingDirectory;
        } else {
            Optional<Path> databasePath = frame.getCurrentLibraryTab().getBibDatabaseContext().getDatabasePath();
            return databasePath.map(Path::getParent).orElse(standardWorkingDirectory);
        }
    }
}
