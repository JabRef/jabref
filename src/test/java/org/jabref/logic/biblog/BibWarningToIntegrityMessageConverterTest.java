package org.jabref.logic.biblog;

import java.util.List;

import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.model.biblog.BibWarning;
import org.jabref.model.biblog.SeverityType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibWarningToIntegrityMessageConverterTest {
    @Test
    public void convertsWarningsToIntegrityMessagesTest() {
        BibEntry firstEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Scholey_2013")
                .withField(StandardField.AUTHOR, "Scholey");
        BibEntry secondEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Tan_2021")
                .withField(StandardField.AUTHOR, "Tan");
        BibDatabase database = new BibDatabase();
        database.insertEntry(firstEntry);
        database.insertEntry(secondEntry);

        BibDatabaseContext context = new BibDatabaseContext(database);

        List<BibWarning> warnings = List.of(
                new BibWarning(SeverityType.WARNING, "empty journal", "journal", "Scholey_2013"),
                new BibWarning(SeverityType.WARNING, "empty year", "year", "Tan_2021")
        );
        List<IntegrityMessage> messages = BibWarningToIntegrityMessageConverter.convert(warnings, context);

        assertEquals(2, messages.size());

        IntegrityMessage msg1 = messages.getFirst();
        assertEquals("empty journal", msg1.message());
        assertEquals(firstEntry, msg1.entry());
        assertEquals("journal", msg1.field().getName());

        IntegrityMessage msg2 = messages.get(1);
        assertEquals("empty year", msg2.message());
        assertEquals(secondEntry, msg2.entry());
        assertEquals("year", msg2.field().getName());
    }

    /**
     * Gracefully skips warnings for entries not found in the database.
     */
    @Test
    public void skipsWarningsForMissingEntries() {
        // Arrange: only one entry
        BibEntry scholey = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Scholey_2013");

        BibDatabase database = new BibDatabase();
        database.insertEntry(scholey);
        BibDatabaseContext context = new BibDatabaseContext(database);

        List<BibWarning> warnings = List.of(
                new BibWarning(SeverityType.WARNING, "empty journal", "journal", "Scholey_2013"),
                new BibWarning(SeverityType.WARNING, "empty year", "year", "NonExistent_9999") // should be skipped
        );

        List<IntegrityMessage> messages = BibWarningToIntegrityMessageConverter.convert(warnings, context);

        assertEquals(1, messages.size());
        assertEquals("Scholey_2013", messages.getFirst().entry().getCitationKey().orElseThrow());
    }
}
