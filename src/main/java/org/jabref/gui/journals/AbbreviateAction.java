package org.jabref.gui.journals;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefExecutorService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts journal full names to either iso or medline abbreviations for all selected entries.
 */
public class AbbreviateAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviateAction.class);

    private final StandardActions action;
    private final JabRefFrame frame;
    private final DialogService dialogService;
    private final StateManager stateManager;

    private AbbreviationType abbreviationType;

    public AbbreviateAction(StandardActions action,
                            JabRefFrame frame,
                            DialogService dialogService,
                            StateManager stateManager) {

        this.action = action;
        this.frame = frame;
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        switch (action) {
            case ABBREVIATE_DEFAULT -> abbreviationType = AbbreviationType.DEFAULT;
            case ABBREVIATE_MEDLINE -> abbreviationType = AbbreviationType.MEDLINE;
            case ABBREVIATE_SHORTEST_UNIQUE -> abbreviationType = AbbreviationType.SHORTEST_UNIQUE;
            default -> LOGGER.debug("Unknown action: " + action.name());
        }

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {

        if (action == StandardActions.ABBREVIATE_DEFAULT
                || action == StandardActions.ABBREVIATE_MEDLINE
                || action == StandardActions.ABBREVIATE_SHORTEST_UNIQUE) {

            dialogService.notify(Localization.lang("Abbreviating..."));
            stateManager.getActiveDatabase().ifPresent(databaseContext ->
                    BackgroundTask.wrap(() -> abbreviate(stateManager.getActiveDatabase().get(), stateManager.getSelectedEntries()))
                                  .onSuccess(dialogService::notify)
                                  .executeWith(Globals.TASK_EXECUTOR));
        } else if (action == StandardActions.UNABBREVIATE) {

            dialogService.notify(Localization.lang("Unabbreviating..."));
            stateManager.getActiveDatabase().ifPresent(databaseContext ->
                    BackgroundTask.wrap(() -> unabbreviate(stateManager.getActiveDatabase().get(), stateManager.getSelectedEntries()))
                                  .onSuccess(dialogService::notify)
                                  .executeWith(Globals.TASK_EXECUTOR));
        } else {
            LOGGER.debug("Unknown action: " + action.name());
        }
    }

    private String abbreviate(BibDatabaseContext databaseContext, List<BibEntry> entries) {
        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(
                Globals.journalAbbreviationRepository,
                abbreviationType);

        NamedCompound ce = new NamedCompound(Localization.lang("Abbreviate journal names"));

        // Collect all callables to execute in one collection.
        Set<Callable<Boolean>> tasks = entries.stream().<Callable<Boolean>>map(entry -> () ->
                FieldFactory.getJournalNameFields().stream().anyMatch(journalField ->
                        undoableAbbreviator.abbreviate(databaseContext.getDatabase(), entry, journalField, ce)))
                .collect(Collectors.toSet());

        // Execute the callables and wait for the results.
        List<Future<Boolean>> futures = JabRefExecutorService.INSTANCE.executeAll(tasks);

        // Evaluate the results of the callables.
        long count = futures.stream().filter(future -> {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException exception) {
                LOGGER.error("Unable to retrieve value.", exception);
                return false;
            }
        }).count();

        if (count > 0) {
            ce.end();
            frame.getUndoManager().addEdit(ce);
            frame.getCurrentLibraryTab().markBaseChanged();
            return Localization.lang("Abbreviated %0 journal names.", String.valueOf(count));
        }
        return Localization.lang("No journal names could be abbreviated.");
    }

    private String unabbreviate(BibDatabaseContext databaseContext, List<BibEntry> entries) {
        UndoableUnabbreviator undoableAbbreviator = new UndoableUnabbreviator(Globals.journalAbbreviationRepository);

        NamedCompound ce = new NamedCompound(Localization.lang("Unabbreviate journal names"));
        int count = entries.stream().mapToInt(entry ->
                (int) FieldFactory.getJournalNameFields().stream().filter(journalField ->
                        undoableAbbreviator.unabbreviate(databaseContext.getDatabase(), entry, journalField, ce)).count()).sum();
        if (count > 0) {
            ce.end();
            frame.getUndoManager().addEdit(ce);
            frame.getCurrentLibraryTab().markBaseChanged();
            return Localization.lang("Unabbreviated %0 journal names.", String.valueOf(count));
        }
        return Localization.lang("No journal names could be unabbreviated.");
    }
}
