package org.jabref.migrations;

import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimeStampToDateAddAndModifyTest {

    @Test
    public void testMigrationToCreationDate() {
        TimeStampToDateAddAndModify migrator = new TimeStampToDateAddAndModify(setUpForMigrationToCreationDate());
        ParserResult entries = getBibEntriesWithSameFormat();

        migrator.performMigration(entries);

        assertEquals(getBibEntriesWithSameFormatAfterMigrationToCreationDate(), entries.getDatabase().getEntries());
    }

    // TODO: Mock that
    public TimestampPreferences setUpForMigrationToCreationDate() {
        return new TimestampPreferences(false, false, false, StandardField.TIMESTAMP, "yyyy-mm-dd");
    }

    // TODO: Mock that
    public TimestampPreferences setUpForMigrationWithCustomName() {
        return new TimestampPreferences(false, false, false, new UnknownField("dateOfCreation"), "yyyy-mm-dd");
    }

    // TODO: Mock that
    public TimestampPreferences setUpForMigrationToModificationDate() {
        return new TimestampPreferences(false, false, true, StandardField.TIMESTAMP, "yyyy-mm-dd");
    }

    private List<BibEntry> getBibEntriesWithSameFormatAfterMigrationToCreationDate() {
        BibEntry bibEntry1 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-09-10T00:00");
        BibEntry bibEntry2 = new BibEntry().withField(StandardField.CREATIONDATE, "2020-12-24T00:00");
        BibEntry bibEntry3 = new BibEntry().withField(StandardField.CREATIONDATE, "2020-12-31T00:00");

        return List.of(bibEntry1, bibEntry2, bibEntry3);
    }

    public ParserResult getBibEntriesWithSameFormat() {
        // All entries with yyyy-mm-dd format
        BibEntry bibEntry1 = new BibEntry().withField(StandardField.TIMESTAMP, "2018-09-10");
        BibEntry bibEntry2 = new BibEntry().withField(StandardField.TIMESTAMP, "2020-12-24");
        BibEntry bibEntry3 = new BibEntry().withField(StandardField.TIMESTAMP, "2020-12-31");

        return new ParserResult(List.of(bibEntry1, bibEntry2, bibEntry3));
    }
}
