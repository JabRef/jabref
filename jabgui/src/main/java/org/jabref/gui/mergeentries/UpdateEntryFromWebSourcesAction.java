package org.jabref.gui.mergeentries;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportCleanup;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class UpdateEntryFromWebSourcesAction extends SimpleCommand {
    private final StateManager stateManager;
    private final UndoManager undoManager;
    private final GuiPreferences preferences;
    private final NotificationService notificationService;
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;

    public UpdateEntryFromWebSourcesAction(StateManager stateManager, UndoManager undoManager, GuiPreferences preferences, NotificationService notificationService, TaskExecutor taskExecutor, DialogService dialogService) {
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.preferences = preferences;
        this.notificationService = notificationService;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        Optional<BibDatabaseContext> activeDatabase = stateManager.getActiveDatabase();
        if (activeDatabase.isEmpty()) {
            return;
        }
        BibDatabaseContext databaseContext = activeDatabase.get();

        if (stateManager.getSelectedEntries().size() != 1) {
            notificationService.notify(Localization.lang("Select exactly one entry to update from web sources"));
            return;
        }
        BibEntry entry = stateManager.getSelectedEntries().getFirst();

        FetchAndMergeEntry fetchAndMergeEntry = new FetchAndMergeEntry(
                databaseContext,
                taskExecutor,
                preferences,
                dialogService,
                undoManager,
                stateManager
        );

        SortedSet<EntryBasedFetcher> fetchers = WebFetchers.getEntryBasedFetchers(
                preferences.getImporterPreferences(),
                preferences.getImportFormatPreferences(),
                preferences.getFilePreferences(),
                databaseContext
        );

        for (EntryBasedFetcher fetcher : fetchers) {
            BackgroundTask.wrap(() -> fetcher.performSearch(entry).stream().findFirst())
                          .onSuccess(firstResult -> {
                              if (firstResult.isPresent()) {
                                  ImportCleanup cleanup = ImportCleanup.targeting(
                                          databaseContext.getMode(),
                                          preferences.getFieldPreferences()
                                  );
                                  cleanup.doPostCleanup(firstResult.get());
                                  fetchAndMergeEntry.mergeFetchedEntry(entry, firstResult.get(), fetcher);
                              }
                          })
                          .onFailure(exception -> {
                              dialogService.showErrorDialogAndWait(
                                      Localization.lang("Error while fetching from %0", fetcher.getName()),
                                      exception
                              );
                          })
                          .executeWith(taskExecutor);
        }
    }

}
