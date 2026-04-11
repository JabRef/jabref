package org.jabref.logic.cleanup;

import java.util.List;

import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
class AbbreviateJournalOnSaveTest {

    private JournalAbbreviationRepository repository;
    private BibDatabase database;

    @BeforeEach
    void setUp() {
        repository = JournalAbbreviationLoader.loadBuiltInRepository();
        database = new BibDatabase();
    }

    @Test
    void ltwaAbbreviatesJournalOnSave() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.JOURNAL, "Annals of Mathematics and Pure and Applied Sciences");
        AbbreviateJournalCleanup cleanup = new AbbreviateJournalCleanup(database, repository, AbbreviationType.LTWA, false);

        List<FieldChange> changes = cleanup.cleanup(entry);

        List<FieldChange> expected = List.of(
                new FieldChange(entry, StandardField.JOURNAL, "Annals of Mathematics and Pure and Applied Sciences", "Ann. Math. Pure Appl. Sci.")
        );
        assertEquals(expected, changes);

        BibEntry expectedEntry = new BibEntry()
                .withField(StandardField.JOURNAL, "Ann. Math. Pure Appl. Sci.");
        assertEquals(expectedEntry, entry);
    }
}

