package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.CompoundEdit;

import org.jabref.gui.journals.AbbreviationType;
import org.jabref.gui.journals.UndoableAbbreviator;
import org.jabref.gui.preferences.JabRefGuiPreferences;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbbreviateJournalCleanup implements CleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviateJournalCleanup.class);
    private final BibDatabase bibDatabase = new BibDatabase();
    private final JournalAbbreviationRepository repository = JournalAbbreviationLoader.loadBuiltInRepository();

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        // Skip if preferences is disabled
        boolean shouldAutoAbbreviateJournals = JabRefGuiPreferences.getInstance().getImporterPreferences().shouldAutoAbbreviateJournals();
        if (!shouldAutoAbbreviateJournals) {
            return changes;
        }

        UndoableAbbreviator undoableAbbreviator = new UndoableAbbreviator(repository, AbbreviationType.DEFAULT, false);

        // Abbreviate journal and journaltitle fields
        if (entry.hasField(StandardField.JOURNAL)) {
            String oldJournal = entry.getField(StandardField.JOURNAL).orElse(null);
            if (undoableAbbreviator.abbreviate(bibDatabase, entry, StandardField.JOURNAL, new CompoundEdit())) {
                String abbreviatedJournal = entry.getField(StandardField.JOURNAL).orElse(null);
                entry.setField(StandardField.JOURNAL, abbreviatedJournal);
                changes.add(new FieldChange(entry, StandardField.JOURNAL, oldJournal, abbreviatedJournal));
            }
        }

        if (entry.hasField(StandardField.JOURNALTITLE)) {
            String oldJournalTitle = entry.getField(StandardField.JOURNALTITLE).orElse(null);
            if (undoableAbbreviator.abbreviate(bibDatabase, entry, StandardField.JOURNALTITLE, new CompoundEdit())) {
                String abbreviatedJournalTitle = entry.getField(StandardField.JOURNAL).orElse(null);
                entry.setField(StandardField.JOURNALTITLE, abbreviatedJournalTitle);
                changes.add(new FieldChange(entry, StandardField.JOURNALTITLE, oldJournalTitle, abbreviatedJournalTitle));
            }
        }

        return changes;
    }
}
