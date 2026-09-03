package org.jabref.gui.importer.actions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.directorylibrary.BibMirror;
import org.jabref.logic.directorylibrary.DirectoryLibraryConverter;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.function.Predicate.not;

/// Converts the current `.bib` library into a directory library (see
/// [DirectoryLibraryConverter]) and reopens its root as such. Only offered for saved local
/// libraries; aborts with an explanation when the library does not fit under one root.
public class ConvertToDirectoryLibraryAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertToDirectoryLibraryAction.class);
    private static final int MAX_REPORTED_OBSTACLES = 10;

    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final OpenDirectoryLibraryAction openDirectoryLibraryAction;
    private final TaskExecutor taskExecutor;
    private final DirectoryLibraryConverter converter = new DirectoryLibraryConverter();

    public ConvertToDirectoryLibraryAction(LibraryTabContainer tabContainer,
                                           DialogService dialogService,
                                           GuiPreferences preferences,
                                           StateManager stateManager,
                                           BibEntryTypesManager entryTypesManager,
                                           JournalAbbreviationRepository journalAbbreviationRepository,
                                           OpenDirectoryLibraryAction openDirectoryLibraryAction,
                                           TaskExecutor taskExecutor) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.entryTypesManager = entryTypesManager;
        this.journalAbbreviationRepository = journalAbbreviationRepository;
        this.openDirectoryLibraryAction = openDirectoryLibraryAction;
        this.taskExecutor = taskExecutor;

        this.executable.bind(ActionHelper.needsSavedLocalDatabase(stateManager));
    }

    @Override
    public void execute() {
        LibraryTab libraryTab = tabContainer.getCurrentLibraryTab();
        BibDatabaseContext context = libraryTab.getBibDatabaseContext();
        DirectoryLibraryConverter.determineRoot(context, preferences.getFilePreferences())
                                 .ifPresent(root -> convert(libraryTab, context, root));
    }

    private void convert(LibraryTab libraryTab, BibDatabaseContext context, Path root) {
        String title = Localization.lang("Convert to folder library");
        List<String> obstacles = converter.obstacles(context, root, preferences.getFilePreferences());
        if (!obstacles.isEmpty()) {
            dialogService.showErrorDialogAndWait(title,
                    Localization.lang("The library cannot be converted.") + "\n\n" + reportedObstacles(obstacles));
            return;
        }

        Path mirrorTarget = root.resolve(BibMirror.fileName(root));
        boolean overwritesForeignFile = context.getDatabasePath().filter(not(mirrorTarget::equals)).isPresent() && Files.exists(mirrorTarget);
        if (overwritesForeignFile) {
            dialogService.showErrorDialogAndWait(title,
                    Localization.lang("'%0' already exists and would be overwritten.", mirrorTarget.toString()));
            return;
        }

        boolean confirmed = dialogService.showConfirmationDialogAndWait(title,
                Localization.lang("Every entry gets a Markdown sidecar next to its linked file, and the library file moves to '%0', staying in sync with the folder from now on.", mirrorTarget.toString()));
        if (!confirmed) {
            return;
        }
        if (new SaveDatabaseAction(libraryTab, dialogService, preferences, entryTypesManager, stateManager, journalAbbreviationRepository).save() != SaveDatabaseAction.SaveResult.SUCCESS) {
            return;
        }

        BackgroundTask.wrap(() -> converter.convert(context, root, preferences.getFilePreferences()))
                      .onSuccess(_ -> {
                          if (!tabContainer.closeTab(libraryTab)) {
                              LOGGER.warn("The converted library's tab stays open although its file moved to {}", mirrorTarget);
                          }
                          openDirectoryLibraryAction.openDirectory(root);
                      })
                      .onFailure(exception -> {
                          LOGGER.error("Could not convert the library to a folder library at {}", root, exception);
                          dialogService.showErrorDialogAndWait(title, exception);
                      })
                      .executeWith(taskExecutor);
    }

    private static String reportedObstacles(List<String> obstacles) {
        String reported = obstacles.stream()
                                   .limit(MAX_REPORTED_OBSTACLES)
                                   .collect(Collectors.joining("\n"));
        if (obstacles.size() > MAX_REPORTED_OBSTACLES) {
            return reported + "\n" + Localization.lang("... and %0 more", Integer.toString(obstacles.size() - MAX_REPORTED_OBSTACLES));
        }
        return reported;
    }
}
