package org.jabref.gui.slr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.crawler.Crawler;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.git.SlrGitHandler;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.PreferencesService;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExistingStudySearchAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStudySearchAction.class);

    protected final DialogService dialogService;
    protected final Path workingDirectory;

    Path studyDirectory;

    private final JabRefFrame frame;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final GeneralPreferences generalPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final SavePreferences savePreferences;
    // This can be either populated before crawl is called or is populated in the call using the directory dialog. This is helpful if the directory is selected in a previous dialog/UI element

    public ExistingStudySearchAction(JabRefFrame frame,
                                     FileUpdateMonitor fileUpdateMonitor,
                                     TaskExecutor taskExecutor,
                                     PreferencesService preferencesService,
                                     StateManager stateManager) {
        this.frame = frame;
        this.dialogService = frame.getDialogService();
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.generalPreferences = preferencesService.getGeneralPreferences();
        this.importFormatPreferences = preferencesService.getImportFormatPreferences();
        this.savePreferences = preferencesService.getSavePreferences();

        this.workingDirectory = stateManager.getActiveDatabase()
                                            .map(database -> FileUtil.getInitialDirectory(database, preferencesService.getFilePreferences().getWorkingDirectory()))
                                            .orElse(preferencesService.getFilePreferences().getWorkingDirectory());
    }

    @Override
    public void execute() {
        // Reset before each execution
        studyDirectory = null;
        crawl();
    }

    public void crawl() {
        if (Objects.isNull(studyDirectory)) {
            DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                    .withInitialDirectory(workingDirectory)
                    .build();

            Optional<Path> studyRepositoryRoot = dialogService.showDirectorySelectionDialog(directoryDialogConfiguration);

            if (studyRepositoryRoot.isEmpty()) {
                // Do nothing if selection was canceled
                return;
            }
            studyDirectory = studyRepositoryRoot.get();
        }

        try {
            setupRepository(studyDirectory);
        } catch (IOException | GitAPIException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Study repository could not be created"), e);
            return;
        }
        final Crawler crawler;
        try {
            crawler = new Crawler(studyDirectory, new SlrGitHandler(studyDirectory), generalPreferences, importFormatPreferences, savePreferences, new BibEntryTypesManager(), fileUpdateMonitor);
        } catch (IOException | ParseException e) {
            LOGGER.error("Error during reading of study definition file.", e);
            dialogService.showErrorDialogAndWait(Localization.lang("Error during reading of study definition file."), e);
            return;
        }
        dialogService.notify(Localization.lang("Searching"));
        BackgroundTask.wrap(() -> {
                          crawler.performCrawl();
                          return 0; // Return any value to make this a callable instead of a runnable. This allows throwing exceptions.
                      })
                      .onFailure(e -> {
                          LOGGER.error("Error during persistence of crawling results.");
                          dialogService.showErrorDialogAndWait(Localization.lang("Error during persistence of crawling results."), e);
                      })
                      .onSuccess(unused -> {
                          new OpenDatabaseAction(frame, preferencesService, dialogService, stateManager).openFile(Path.of(studyDirectory.toString(), "studyResult.bib"), true);
                          // If  finished reset command object for next use
                          studyDirectory = null;
                      })
                      .executeWith(taskExecutor);
    }

    /**
     * Hook for setting up the repository
     */
    protected void setupRepository(Path studyRepositoryRoot) throws IOException, GitAPIException {
        // Do nothing as repository is already setup
    }
}
