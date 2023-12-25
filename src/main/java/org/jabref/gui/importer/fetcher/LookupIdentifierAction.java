package org.jabref.gui.importer.fetcher;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;
import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

public class LookupIdentifierAction<T extends Identifier> extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LookupIdentifierAction.class);

    private final IdFetcher<T> fetcher;
    private final StateManager stateManager;
    private final UndoManager undoManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    public LookupIdentifierAction(IdFetcher<T> fetcher,
                                  StateManager stateManager,
                                  UndoManager undoManager,
                                  DialogService dialogService,
                                  TaskExecutor taskExecutor) {
        this.fetcher = fetcher;
        this.stateManager = stateManager;
        this.undoManager = undoManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.executable.bind(needsDatabase(this.stateManager).and(needsEntriesSelected(this.stateManager)));
        this.statusMessage.bind(BindingsHelper.ifThenElse(executable, "", Localization.lang("This operation requires one or more entries to be selected.")));
    }

    @Override
    public void execute() {
        try {
            BackgroundTask.wrap(() -> lookupIdentifiers(stateManager.getSelectedEntries()))
                          .onSuccess(dialogService::notify)
                          .executeWith(taskExecutor);
        } catch (Exception e) {
            LOGGER.error("Problem running ID Worker", e);
        }
    }

    public Action getAction() {
        return fetcher::getIdentifierName;
    }

    private String lookupIdentifiers(List<BibEntry> bibEntries) {
        String totalCount = Integer.toString(bibEntries.size());
        NamedCompound namedCompound = new NamedCompound(Localization.lang("Look up %0", fetcher.getIdentifierName()));
        int count = 0;
        int foundCount = 0;
        for (BibEntry bibEntry : bibEntries) {
            count++;
            final String statusMessage = Localization.lang("Looking up %0... - entry %1 out of %2 - found %3",
                    fetcher.getIdentifierName(), Integer.toString(count), totalCount, Integer.toString(foundCount));
            DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.notify(statusMessage));
            Optional<T> identifier = Optional.empty();
            try {
                identifier = fetcher.findIdentifier(bibEntry);
            } catch (FetcherException e) {
                LOGGER.error("Could not fetch " + fetcher.getIdentifierName(), e);
            }
            if (identifier.isPresent() && !bibEntry.hasField(identifier.get().getDefaultField())) {
                Optional<FieldChange> fieldChange = bibEntry.setField(identifier.get().getDefaultField(), identifier.get().getNormalized());
                if (fieldChange.isPresent()) {
                    namedCompound.addEdit(new UndoableFieldChange(fieldChange.get()));
                    foundCount++;
                    final String nextStatusMessage = Localization.lang("Looking up %0... - entry %1 out of %2 - found %3",
                            fetcher.getIdentifierName(), Integer.toString(count), totalCount, Integer.toString(foundCount));
                    DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.notify(nextStatusMessage));
                }
            }
        }
        namedCompound.end();
        if (foundCount > 0) {
            undoManager.addEdit(namedCompound);
        }
        return Localization.lang("Determined %0 for %1 entries", fetcher.getIdentifierName(), Integer.toString(foundCount));
    }
}
