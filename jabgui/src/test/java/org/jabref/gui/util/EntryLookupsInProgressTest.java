package org.jabref.gui.util;

import javafx.beans.binding.BooleanBinding;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryLookupsInProgressTest {

    private final EntryLookupsInProgress lookups = new EntryLookupsInProgress();

    @Test
    void staysInProgressUntilLastConcurrentLookupFinishes() {
        BibEntry entry = new BibEntry();
        BooleanBinding inProgress = lookups.inProgress(entry);

        lookups.started(entry);
        lookups.started(entry);
        lookups.finished(entry);
        assertTrue(inProgress.get());

        lookups.finished(entry);
        assertFalse(inProgress.get());
    }

    @Test
    void bindingCreatedLaterSeesRunningLookup() {
        BibEntry entry = new BibEntry();
        lookups.started(entry);

        assertTrue(lookups.inProgress(entry).get());
    }

    @Test
    void entryChangedDuringLookupIsStillTracked() {
        BibEntry entry = new BibEntry();
        lookups.started(entry);
        entry.setField(StandardField.DOI, "10.1000/xyz");

        assertTrue(lookups.inProgress(entry).get());
        assertFalse(lookups.inProgress(new BibEntry().withField(StandardField.DOI, "10.1000/xyz")).get());
    }

    @Test
    void finishingUnknownEntryIsIgnored() {
        BibEntry entry = new BibEntry();
        lookups.finished(entry);

        assertFalse(lookups.inProgress(entry).get());
    }
}
