package org.jabref.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import javafx.application.Platform;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.DuplicateResolverDialog.DuplicateResolverResult;
import org.jabref.gui.DuplicateResolverDialog.DuplicateResolverType;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.gui.undo.UndoableRemoveEntry;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

public class DuplicateSearch extends SimpleCommand {

    private final JabRefFrame frame;
    private BlockingQueue<List<BibEntry>> dups;

    private AtomicBoolean libraryAnalyzed;
    private AtomicBoolean autoRemoveExactDuplicates;
    private AtomicInteger duplicateCount;

    public DuplicateSearch(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        BasePanel panel = frame.getCurrentBasePanel();
        panel.output(Localization.lang("Searching for duplicates..."));

        List<BibEntry> entries = panel.getDatabase().getEntries();
        dups = new LinkedBlockingQueue<>();
        libraryAnalyzed = new AtomicBoolean(false);
        autoRemoveExactDuplicates = new AtomicBoolean(false);
        duplicateCount = new AtomicInteger(0);

        if (entries.size() < 2) {
            return;
        }

        JabRefExecutorService.INSTANCE
                .executeInterruptableTask(() -> searchPossibleDuplicates(entries, panel.getBibDatabaseContext().getMode()), "DuplicateSearcher");
        BackgroundTask.wrap(this::verifyDuplicates)
                      .onSuccess(this::handleDuplicates)
                      .executeWith(Globals.TASK_EXECUTOR);

    }

    private void searchPossibleDuplicates(List<BibEntry> entries, BibDatabaseMode databaseMode) {
        for (int i = 0; (i < (entries.size() - 1)); i++) {
            for (int j = i + 1; (j < entries.size()); j++) {
                if (Thread.interrupted()) {
                    return;
                }

                BibEntry first = entries.get(i);
                BibEntry second = entries.get(j);

                if (DuplicateCheck.isDuplicate(first, second, databaseMode)) {
                    dups.add(Arrays.asList(first, second));
                    duplicateCount.getAndIncrement();
                }
            }
        }
        libraryAnalyzed.set(true);
    }

    private DuplicateSearchResult verifyDuplicates() {
        DuplicateSearchResult result = new DuplicateSearchResult();

        while (!libraryAnalyzed.get() || !dups.isEmpty()) {
            List<BibEntry> duplicates;
            try {
                // poll with timeout in case the library is not analyzed completely, but contains no more duplicates
                duplicates = dups.poll(100, TimeUnit.MILLISECONDS);
                if (duplicates == null) {
                    continue;
                }
            } catch (InterruptedException e) {
                return null;
            }

            BibEntry first = duplicates.get(0);
            BibEntry second = duplicates.get(1);

            if (!result.isToRemove(first) && !result.isToRemove(second)) {
                // Check if they are exact duplicates:
                boolean askAboutExact = false;
                if (DuplicateCheck.compareEntriesStrictly(first, second) > 1) {
                    if (autoRemoveExactDuplicates.get()) {
                        result.remove(second);
                        continue;
                    }
                    askAboutExact = true;
                }

                DuplicateResolverType resolverType = askAboutExact ? DuplicateResolverType.DUPLICATE_SEARCH_WITH_EXACT : DuplicateResolverType.DUPLICATE_SEARCH;

                CountDownLatch latch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    askResolveStrategy(result, first, second, resolverType);
                    latch.countDown();
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }

        return result;
    }

    private void askResolveStrategy(DuplicateSearchResult result, BibEntry first, BibEntry second, DuplicateResolverType resolverType) {
        DuplicateResolverDialog dialog = new DuplicateResolverDialog(frame, first, second, resolverType);
        dialog.setVisible(true);
        dialog.dispose();

        DuplicateResolverResult resolverResult = dialog.getSelected();

        if ((resolverResult == DuplicateResolverResult.KEEP_LEFT)
                || (resolverResult == DuplicateResolverResult.AUTOREMOVE_EXACT)) {
            result.remove(second);
            if (resolverResult == DuplicateResolverResult.AUTOREMOVE_EXACT) {
                autoRemoveExactDuplicates.set(true); // Remember choice
            }
        } else if (resolverResult == DuplicateResolverResult.KEEP_RIGHT) {
            result.remove(first);
        } else if (resolverResult == DuplicateResolverResult.BREAK) {
            libraryAnalyzed.set(true);
            dups.clear();
        } else if (resolverResult == DuplicateResolverResult.KEEP_MERGE) {
            result.replace(first, second, dialog.getMergedEntry());
        }
    }

    private void handleDuplicates(DuplicateSearchResult result) {
        if (result == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            BasePanel panel = frame.getCurrentBasePanel();
            final NamedCompound ce = new NamedCompound(Localization.lang("duplicate removal"));
            // Now, do the actual removal:
            if (!result.getToRemove().isEmpty()) {
                for (BibEntry entry : result.getToRemove()) {
                    panel.getDatabase().removeEntry(entry);
                    ce.addEdit(new UndoableRemoveEntry(panel.getDatabase(), entry, panel));
                }
                panel.markBaseChanged();
            }
            // and adding merged entries:
            if (!result.getToAdd().isEmpty()) {
                for (BibEntry entry : result.getToAdd()) {
                    panel.getDatabase().insertEntry(entry);
                    ce.addEdit(new UndoableInsertEntry(panel.getDatabase(), entry));
                }
                panel.markBaseChanged();
            }

            panel.output(Localization.lang("Duplicates found") + ": " + duplicateCount.get() + ' '
                    + Localization.lang("pairs processed") + ": " + result.getDuplicateCount());
            ce.end();
            panel.getUndoManager().addEdit(ce);
        });
    }

    /**
     * Result of a duplicate search.
     * Uses {@link System#identityHashCode(Object)} for identifying objects for removal, as completely identical
     * {@link BibEntry BibEntries} are equal to each other.
     */
    class DuplicateSearchResult {

        private final Map<Integer, BibEntry> toRemove = new HashMap<>();
        private final List<BibEntry> toAdd = new ArrayList<>();

        private int duplicates = 0;

        public synchronized Collection<BibEntry> getToRemove() {
            return toRemove.values();
        }

        public synchronized List<BibEntry> getToAdd() {
            return toAdd;
        }

        public synchronized void remove(BibEntry entry) {
            toRemove.put(System.identityHashCode(entry), entry);
            duplicates++;
        }

        public synchronized void replace(BibEntry replace1, BibEntry replace2, BibEntry replacement) {
            remove(replace1);
            remove(replace2);
            toAdd.add(replacement);
            duplicates++;
        }

        public synchronized boolean isToRemove(BibEntry entry) {
            return toRemove.containsKey(System.identityHashCode(entry));
        }

        public synchronized int getDuplicateCount() {
            return duplicates;
        }
    }
}
