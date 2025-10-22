package org.jabref.logic.biblog;

import java.util.List;

import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.model.biblog.BibWarning;
import org.jabref.model.biblog.SeverityType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibWarningToIntegrityMessageConverterTest {
    @Test
    void convertsWarningsToIntegrityMessagesTest() {
        BibEntry firstEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Scholey_2013")
                .withField(StandardField.AUTHOR, "Scholey");
        BibEntry secondEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Tan_2021")
                .withField(StandardField.AUTHOR, "Tan");
        BibDatabase database = new BibDatabase();
        database.insertEntry(firstEntry);
        database.insertEntry(secondEntry);

        BibDatabaseContext context = new BibDatabaseContext.Builder().database(database).build();

        List<BibWarning> warnings = List.of(
                new BibWarning(SeverityType.WARNING, "empty journal", "journal", "Scholey_2013"),
                new BibWarning(SeverityType.WARNING, "empty year", "year", "Tan_2021")
        );
        List<IntegrityMessage> actualMessages = BibWarningToIntegrityMessageConverter.convert(warnings, context);

        List<IntegrityMessage> expectedMessages = List.of(
                new IntegrityMessage("empty journal", firstEntry, FieldFactory.parseField("journal")),
                new IntegrityMessage("empty year", secondEntry, FieldFactory.parseField("year"))
        );

        assertEquals(expectedMessages, actualMessages);
    }

    /**
     * Skips warnings for entries not found in the database.
     */
    @Test
    void skipsWarningsForMissingEntries() {
        // Arrange: only one entry
        BibEntry scholey = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Scholey_2013");

        BibDatabase database = new BibDatabase();
        database.insertEntry(scholey);
        BibDatabaseContext context = new BibDatabaseContext.Builder().database(database).build();

        List<BibWarning> warnings = List.of(
                new BibWarning(SeverityType.WARNING, "empty journal", "journal", "Scholey_2013"),
                new BibWarning(SeverityType.WARNING, "empty year", "year", "NonExistent_9999") // should be skipped
        );

        List<IntegrityMessage> messages = BibWarningToIntegrityMessageConverter.convert(warnings, context);

        List<IntegrityMessage> expectedMessages = List.of(
                new IntegrityMessage(
                        "empty journal",
                        scholey,
                        FieldFactory.parseField("journal")
                )
        );
        assertEquals(expectedMessages, messages);
    }
}
