package org.jabref.gui.slr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.logic.JabRefException;
import org.jabref.logic.crawler.Crawler;
import org.jabref.logic.git.SlrGitHandler;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExistingStudySearchAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStudySearchAction.class);

    protected final DialogService dialogService;

    protected Path studyDirectory;
    protected final CliPreferences preferences;
    protected final StateManager stateManager;

    private final FileUpdateMonitor fileUpdateMonitor;
    private final TaskExecutor taskExecutor;
    private final LibraryTabContainer tabContainer;
    private final Supplier<OpenDatabaseAction> openDatabaseActionSupplier;

    /**
     * @param tabContainer               Required to close the tab before the study is updated
     * @param openDatabaseActionSupplier Required to open the tab after the study is executed
     */
    public ExistingStudySearchAction(
            LibraryTabContainer tabContainer,
            Supplier<OpenDatabaseAction> openDatabaseActionSupplier,
            DialogService dialogService,
            FileUpdateMonitor fileUpdateMonitor,
            TaskExecutor taskExecutor,
            CliPreferences preferences,
            StateManager stateManager) {
        this(tabContainer,
                openDatabaseActionSupplier,
                dialogService,
                fileUpdateMonitor,
                taskExecutor,
                preferences,
                stateManager,
                false);
    }

    protected ExistingStudySearchAction(
            LibraryTabContainer tabContainer,
            Supplier<OpenDatabaseAction> openDatabaseActionSupplier,
            DialogService dialogService,
            FileUpdateMonitor fileUpdateMonitor,
            TaskExecutor taskExecutor,
            CliPreferences preferences,
            StateManager stateManager,
            boolean isNew) {
        this.tabContainer = tabContainer;
        this.openDatabaseActionSupplier = openDatabaseActionSupplier;
        this.dialogService = dialogService;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
        this.stateManager = stateManager;

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
        try {
            crawlPreparation(this.studyDirectory);
        } catch (IOException | GitAPIException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Study repository could not be created"), e);
            return;
        }

        final Crawler crawler;
        try {
            crawler = new Crawler(
                    this.studyDirectory,
                    new SlrGitHandler(this.studyDirectory, preferences.getGitPreferences()),
                    preferences,
                    new BibEntryTypesManager(),
                    fileUpdateMonitor);
        } catch (IOException | ParseException | JabRefException e) {
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
                          openDatabaseActionSupplier.get().openFile(Path.of(this.studyDirectory.toString(), Crawler.FILENAME_STUDY_RESULT_BIB));
                      })
                      .executeWith(taskExecutor);
    }

    /**
     * Hook for setting up the crawl phase (e.g., initialization the repository)
     */
    protected void crawlPreparation(Path studyRepositoryRoot) throws IOException, GitAPIException {
        // Do nothing with the repository as repository is already setup

        // The user focused an SLR
        // We hard close the tab
        // Future work: Properly close the tab (with saving, ...)
        tabContainer.closeTab(tabContainer.getCurrentLibraryTab());
    }
}
