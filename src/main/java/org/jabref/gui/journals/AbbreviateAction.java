package org.jabref.gui.journals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts journal full names to either iso or medline abbreviations for all
 * selected entries.
 */
public class AbbreviateAction implements BaseAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviateAction.class);
    private final BasePanel panel;
    private final boolean iso;

    public AbbreviateAction(BasePanel panel, boolean iso) {
        this.panel = panel;
        this.iso = iso;
    }

    @Override
    public void action() {
        BackgroundTask.wrap(this::abbreviate)
                      .onSuccess(panel::output)
                      .executeWith(Globals.TASK_EXECUTOR);

    }

    private String abbreviate() {
        panel.output(Localization.lang("Abbreviating..."));

        List<BibEntry> entries = panel.getSelectedEntries();
        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(
                Globals.journalAbbreviationLoader.getRepository(Globals.prefs.getJournalAbbreviationPreferences()),
                iso);

        NamedCompound ce = new NamedCompound(Localization.lang("Abbreviate journal names"));
        Set<Callable<Boolean>> tasks = new HashSet<>();

        // Collect all callables to execute in one collection.
        for (BibEntry entry : entries) {
            Callable<Boolean> callable = () -> {
                for (String journalField : InternalBibtexFields.getJournalNameFields()) {
                    if (undoableAbbreviator.abbreviate(panel.getDatabase(), entry, journalField, ce)) {
                        return true;
                    }
                }
                return false;
            };
            tasks.add(callable);
        }

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
        } else {
            return Localization.lang("No journal names could be abbreviated.");
        }
    }
}
