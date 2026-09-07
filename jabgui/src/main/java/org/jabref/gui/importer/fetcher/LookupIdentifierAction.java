package org.jabref.gui.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.Identifier;
import org.jabref.model.undo.CompoundEdit;
import org.jabref.model.undo.UndoableFieldChange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

public class LookupIdentifierAction<T extends Identifier> extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LookupIdentifierAction.class);

    private final IdFetcher<T> fetcher;
    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public LookupIdentifierAction(IdFetcher<T> fetcher,
                                  StateManager stateManager,
                                  DialogService dialogService,
                                  TaskExecutor taskExecutor) {
        this.fetcher = fetcher;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(this.stateManager).and(needsEntriesSelected(this.stateManager)));
        this.statusMessage.bind(BindingsHelper.ifThenElse(executable, "", Localization.lang("This operation requires one or more entries to be selected.")));
    }

    @Override
    public void execute() {
        stateManager.getActiveDatabase().ifPresent(this::lookUpIn);
    }

    /// The journal and the entries are taken here rather than when the lookup runs: the work
    /// belongs to the library the user started on, the state manager replaces its selection when
    /// they switch away from it, and asking for a journal after that library closed would create
    /// one nothing can reach.
    private void lookUpIn(BibDatabaseContext databaseContext) {
        UndoManager undoManager = stateManager.getUndoManager(databaseContext);
        List<BibEntry> selectedEntries = List.copyOf(stateManager.getSelectedEntries());

        try {
            BackgroundTask.wrap(() -> lookupIdentifiers(undoManager, selectedEntries))
                          .onSuccess(dialogService::notify)
                          .executeWith(taskExecutor);
        } catch (Exception e) {
            LOGGER.error("Problem running ID Worker", e);
        }
    }

    public Action getAction() {
        return fetcher::getIdentifierName;
    }

    private String lookupIdentifiers(UndoManager undoManager, List<BibEntry> bibEntries) {
        String totalCount = Integer.toString(bibEntries.size());
        CompoundEdit compoundEdit = new CompoundEdit(Localization.lang("Look up %0", fetcher.getIdentifierName()));
        int count = 0;
        int foundCount = 0;
        for (BibEntry bibEntry : bibEntries) {
            count++;
            final String statusMessage = Localization.lang("Looking up %0... - entry %1 out of %2 - found %3",
                    fetcher.getIdentifierName(), Integer.toString(count), totalCount, Integer.toString(foundCount));
            UiTaskExecutor.runInJavaFXThread(() -> dialogService.notify(statusMessage));
            Optional<T> identifier = Optional.empty();
            try {
                identifier = fetcher.findIdentifier(bibEntry);
            } catch (FetcherException e) {
                LOGGER.error("Could not fetch {}", fetcher.getIdentifierName(), e);
            }
            if (identifier.isPresent()) {
                T foundIdentifier = identifier.get();
                // BibEntry uses an ObservableMap which notifies JavaFX listeners.
                Optional<FieldChange> fieldChange = UiTaskExecutor.runInJavaFXThread(() -> {
                    if (bibEntry.hasField(foundIdentifier.getDefaultField())) {
                        return Optional.empty();
                    }
                    return bibEntry.setField(foundIdentifier.getDefaultField(), foundIdentifier.asString());
                });
                if (fieldChange != null && fieldChange.isPresent()) {
                    compoundEdit.addEdit(new UndoableFieldChange(fieldChange.get()));
                    foundCount++;
                    final String nextStatusMessage = Localization.lang("Looking up %0... - entry %1 out of %2 - found %3",
                            fetcher.getIdentifierName(), Integer.toString(count), totalCount, Integer.toString(foundCount));
                    UiTaskExecutor.runInJavaFXThread(() -> dialogService.notify(nextStatusMessage));
                }
            }
        }
        undoManager.addEdit(compoundEdit.toChangeSet());
        return Localization.lang("Determined %0 for %1 entries", fetcher.getIdentifierName(), Integer.toString(foundCount));
    }
}
