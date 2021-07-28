package org.jabref.gui.duplicationFinder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefExecutorService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog.DuplicateResolverResult;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog.DuplicateResolverType;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class DuplicateSearch extends SimpleCommand {

    private final JabRefFrame frame;
    private final BlockingQueue<List<BibEntry>> duplicates = new LinkedBlockingQueue<>();

    private final AtomicBoolean libraryAnalyzed = new AtomicBoolean();
    private final AtomicBoolean autoRemoveExactDuplicates = new AtomicBoolean();
    private final AtomicInteger duplicateCount = new AtomicInteger();
    private final SimpleStringProperty duplicateCountObservable = new SimpleStringProperty();
    private final SimpleStringProperty duplicateTotal = new SimpleStringProperty();
    private final SimpleIntegerProperty duplicateProgress = new SimpleIntegerProperty(0);
    private final DialogService dialogService;
    private final StateManager stateManager;

    public DuplicateSearch(JabRefFrame frame, DialogService dialogService, StateManager stateManager) {
        this.frame = frame;
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        dialogService.notify(Localization.lang("Searching for duplicates..."));

        List<BibEntry> entries = database.getEntries();
        duplicates.clear();
        libraryAnalyzed.set(false);
        autoRemoveExactDuplicates.set(false);
        duplicateCount.set(0);

        if (entries.size() < 2) {
            return;
        }

        duplicateCountObservable.addListener((obj, oldValue, newValue) -> DefaultTaskExecutor.runAndWaitInJavaFXThread(() -> duplicateTotal.set(newValue)));

        JabRefExecutorService.INSTANCE.executeInterruptableTask(() -> searchPossibleDuplicates(entries, database.getMode()), "DuplicateSearcher");
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

                if (new DuplicateCheck(Globals.entryTypesManager).isDuplicate(first, second, databaseMode)) {
                    duplicates.add(Arrays.asList(first, second));
                    duplicateCountObservable.set(String.valueOf(duplicateCount.incrementAndGet()));
                }
            }
        }
        libraryAnalyzed.set(true);
    }

    private DuplicateSearchResult verifyDuplicates() {
        DuplicateSearchResult result = new DuplicateSearchResult();

        while (!libraryAnalyzed.get() || !duplicates.isEmpty()) {
            duplicateProgress.set(duplicateProgress.getValue() + 1);

            List<BibEntry> dups;
            try {
                // poll with timeout in case the library is not analyzed completely, but contains no more duplicates
                dups = this.duplicates.poll(100, TimeUnit.MILLISECONDS);
                if (dups == null) {
                    continue;
                }
            } catch (InterruptedException e) {
                return null;
            }

            BibEntry first = dups.get(0);
            BibEntry second = dups.get(1);

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

                DefaultTaskExecutor.runAndWaitInJavaFXThread(() -> askResolveStrategy(result, first, second, resolverType));
            }
        }

        return result;
    }

    private void askResolveStrategy(DuplicateSearchResult result, BibEntry first, BibEntry second, DuplicateResolverType resolverType) {
        DuplicateResolverDialog dialog = new DuplicateResolverDialog(first, second, resolverType, frame.getCurrentLibraryTab().getBibDatabaseContext(), stateManager);

        dialog.titleProperty().bind(Bindings.concat(dialog.getTitle()).concat(" (").concat(duplicateProgress.getValue()).concat("/").concat(duplicateTotal).concat(")"));

        DuplicateResolverResult resolverResult = dialogService.showCustomDialogAndWait(dialog)
                                                              .orElse(DuplicateResolverResult.BREAK);

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
            duplicates.clear();
        } else if (resolverResult == DuplicateResolverResult.KEEP_MERGE) {
            result.replace(first, second, dialog.getMergedEntry());
        }
    }

    private void handleDuplicates(DuplicateSearchResult result) {
        if (result == null) {
            return;
        }

        LibraryTab libraryTab = frame.getCurrentLibraryTab();
        final NamedCompound compoundEdit = new NamedCompound(Localization.lang("duplicate removal"));
        // Now, do the actual removal:
        if (!result.getToRemove().isEmpty()) {
            compoundEdit.addEdit(new UndoableRemoveEntries(libraryTab.getDatabase(), result.getToRemove()));
            libraryTab.getDatabase().removeEntries(result.getToRemove());
            libraryTab.markBaseChanged();
        }
        // and adding merged entries:
        if (!result.getToAdd().isEmpty()) {
            compoundEdit.addEdit(new UndoableInsertEntries(libraryTab.getDatabase(), result.getToAdd()));
            libraryTab.getDatabase().insertEntries(result.getToAdd());
            libraryTab.markBaseChanged();
        }

        duplicateProgress.set(0);

        dialogService.notify(Localization.lang("Duplicates found") + ": " + duplicateCount.get() + ' '
                + Localization.lang("pairs processed") + ": " + result.getDuplicateCount());
        compoundEdit.end();
        libraryTab.getUndoManager().addEdit(compoundEdit);
    }

    /**
     * Result of a duplicate search.
     * Uses {@link System#identityHashCode(Object)} for identifying objects for removal, as completely identical
     * {@link BibEntry BibEntries} are equal to each other.
     */
    static class DuplicateSearchResult {

        private final Map<Integer, BibEntry> toRemove = new HashMap<>();
        private final List<BibEntry> toAdd = new ArrayList<>();

        private int duplicates = 0;

        public synchronized List<BibEntry> getToRemove() {
            return new ArrayList<>(toRemove.values());
        }

        public synchronized List<BibEntry> getToAdd() {
            return toAdd;
        }

        public synchronized void remove(BibEntry entry) {
            toRemove.put(System.identityHashCode(entry), entry);
            duplicates++;
        }

        public synchronized void replace(BibEntry first, BibEntry second, BibEntry replacement) {
            remove(first);
            remove(second);
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
