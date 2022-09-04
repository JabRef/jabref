package org.jabref.gui.slr;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.crawler.Crawler;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.git.SlrGitHandler;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
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

    protected Path studyDirectory;
    protected final PreferencesService preferencesService;
    protected final StateManager stateManager;

    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;
    private final ThemeManager themeManager;
    private final GeneralPreferences generalPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final ImporterPreferences importerPreferences;
    private final SavePreferences savePreferences;
    private final JabRefFrame frame;
    private final OpenDatabaseAction openDatabaseAction;

    /**
     * @param frame Required to close the tab before the study is updated
     * @param openDatabaseAction Required to open the tab after the study is exectued
     */
    public ExistingStudySearchAction(
            JabRefFrame frame,
            OpenDatabaseAction openDatabaseAction,
            DialogService dialogService,
            FileUpdateMonitor fileUpdateMonitor,
            TaskExecutor taskExecutor,
            PreferencesService preferencesService,
            StateManager stateManager,
            ThemeManager themeManager) {
        this(frame, openDatabaseAction, dialogService, fileUpdateMonitor, taskExecutor, preferencesService, stateManager, themeManager, false);
    }

    protected ExistingStudySearchAction(
            JabRefFrame frame,
            OpenDatabaseAction openDatabaseAction,
            DialogService dialogService,
            FileUpdateMonitor fileUpdateMonitor,
            TaskExecutor taskExecutor,
            PreferencesService preferencesService,
            StateManager stateManager,
            ThemeManager themeManager,
            boolean isNew) {
        this.frame = frame;
        this.openDatabaseAction = openDatabaseAction;
        this.dialogService = dialogService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.generalPreferences = preferencesService.getGeneralPreferences();
        this.themeManager = themeManager;
        this.importFormatPreferences = preferencesService.getImportFormatPreferences();
        this.importerPreferences = preferencesService.getImporterPreferences();
        this.savePreferences = preferencesService.getSavePreferences();

        if (!isNew) {
            this.executable.bind(ActionHelper.needsStudyDatabase(stateManager));
        }
    }

    @Override
    public void execute() {
        if (stateManager.getActiveDatabase().isEmpty()) {
            LOGGER.error("Database is not present, even if it should");
            return;
        }
        BibDatabaseContext bibDatabaseContext = stateManager.getActiveDatabase().get();

        if (bibDatabaseContext.getDatabasePath().isEmpty()) {
            LOGGER.error("Database path is not present, even if it should");
            return;
        }
        this.studyDirectory = bibDatabaseContext.getDatabasePath().get().getParent();

        crawl();
    }

    protected void crawl() {
        // We hard close the tab
        // Future work: Properly close the tab (with saving, ...)
        frame.closeCurrentTab();

        try {
            setupRepository(this.studyDirectory);
        } catch (IOException | GitAPIException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Study repository could not be created"), e);
            return;
        }

        final Crawler crawler;
        try {
            crawler = new Crawler(
                    this.studyDirectory,
                    new SlrGitHandler(this.studyDirectory),
                    generalPreferences,
                    importFormatPreferences,
                    importerPreferences,
                    savePreferences,
                    new BibEntryTypesManager(),
                    fileUpdateMonitor);
        } catch (IOException | ParseException e) {
            LOGGER.error("Error during reading of study definition file.", e);
            dialogService.showErrorDialogAndWait(Localization.lang("Error during reading of study definition file."), e);
            return;
        }

        dialogService.notify(Localization.lang("Searching..."));
        BackgroundTask.wrap(() -> {
                          crawler.performCrawl();
                          return 0; // Return any value to make this a callable instead of a runnable. This allows throwing exceptions.
                      })
                      .onFailure(e -> {
                          LOGGER.error("Error during persistence of crawling results.");
                          dialogService.showErrorDialogAndWait(Localization.lang("Error during persistence of crawling results."), e);
                      })
                      .onSuccess(unused -> {
                          dialogService.notify(Localization.lang("Finished Searching"));
                          openDatabaseAction.openFile(Path.of(this.studyDirectory.toString(), Crawler.FILENAME_STUDY_RESULT_BIB));
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
