package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbbreviateJournalCleanup implements CleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviateJournalCleanup.class);

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        String m = "ABBREVIATE_JOURNAL_CLEANUP";
        LOGGER.debug("Some state {}", m);

        // String abbreviatedJournal;
        entry.setField(StandardField.JOURNAL, "abbrTest")
             .ifPresent(changes::add);

        return changes;
    }
}
