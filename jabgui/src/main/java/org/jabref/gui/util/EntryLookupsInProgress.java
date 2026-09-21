package org.jabref.gui.util;

import java.util.IdentityHashMap;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Counts the running lookups of one kind per entry, so a progress indicator in the entry editor
/// survives the editor rebuilding its field editors on an entry switch, reflects lookups started
/// elsewhere (e.g. from the main menu for several entries), and stays on until the last of several
/// concurrent lookups of the same entry finishes.
///
/// Entries are compared by identity because a lookup usually changes the entry it runs for.
/// Must only be used on the JavaFX thread.
@NullMarked
public final class EntryLookupsInProgress {

    public static final EntryLookupsInProgress DOI = new EntryLookupsInProgress();
    public static final EntryLookupsInProgress FULLTEXT = new EntryLookupsInProgress();

    private final ObservableMap<BibEntry, Integer> running = FXCollections.observableMap(new IdentityHashMap<>());

    EntryLookupsInProgress() {
    }

    public void started(BibEntry entry) {
        running.merge(entry, 1, Integer::sum);
    }

    public void finished(BibEntry entry) {
        running.computeIfPresent(entry, (_, count) -> count == 1 ? null : count - 1);
    }

    public BooleanBinding inProgress(BibEntry entry) {
        return Bindings.createBooleanBinding(() -> running.containsKey(entry), running);
    }
}
