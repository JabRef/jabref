package org.jabref.gui.journals;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts journal full names to either iso or medline abbreviations for all selected entries.
 */
public class AbbreviateAction implements BaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviateAction.class);
    private final BasePanel panel;
    private final AbbreviationType abbreviationType;

    public AbbreviateAction(BasePanel panel, AbbreviationType abbreviationType) {
        this.panel = panel;
        this.abbreviationType = abbreviationType;
    }

    @Override
    public void action() {
        panel.output(Localization.lang("Abbreviating..."));
        BackgroundTask.wrap(this::abbreviate)
                      .onSuccess(panel::output)
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    private String abbreviate() {
        List<BibEntry> entries = panel.getSelectedEntries();
        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(
                Globals.journalAbbreviationLoader.getRepository(Globals.prefs.getJournalAbbreviationPreferences()),
                abbreviationType);

        NamedCompound ce = new NamedCompound(Localization.lang("Abbreviate journal names"));

        // Collect all callables to execute in one collection.
        Set<Callable<Boolean>> tasks = entries.stream()
                .<Callable<Boolean>>map(entry -> () ->
                        FieldFactory.getJournalNameFields().stream().anyMatch(journalField ->
                                undoableAbbreviator.abbreviate(panel.getDatabase(), entry, journalField, ce)))
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
            panel.getUndoManager().addEdit(ce);
            panel.markBaseChanged();
            return Localization.lang("Abbreviated %0 journal names.", String.valueOf(count));
        }
        return Localization.lang("No journal names could be abbreviated.");
    }
}
