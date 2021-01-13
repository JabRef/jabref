package org.jabref.migrations;

import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeStampToDateAddAndModifyTest {
    TimestampPreferences timestampPreferences = Mockito.mock(TimestampPreferences.class);
    Field customTimeStampField = new UnknownField("dateOfCreation");

    public void makeMockReturnCustomField() {
        Mockito.when(timestampPreferences.getTimestampField()).then(invocation -> customTimeStampField);
    }

    public void makeMockReturnStandardField() {
        Mockito.when(timestampPreferences.getTimestampField()).then(invocation -> StandardField.TIMESTAMP);
    }

    public void makeMockToMigrateToCreationDate() {
        Mockito.when(timestampPreferences.shouldUpdateTimestamp()).then(invocation -> Boolean.FALSE);
    }

    public void makeMockToMigrateToModificationDate() {
        Mockito.when(timestampPreferences.shouldUpdateTimestamp()).then(invocation -> Boolean.TRUE);
    }

    /**
     * Tests migration to CreationDate if the users uses the default ISO yyyy-mm-dd format and the standard timestamp field
     */
    @Test
    public void withStandardFieldToCreationDate() {
        makeMockToMigrateToCreationDate();
        makeMockReturnStandardField();
        TimeStampToDateAddAndModify migrator = new TimeStampToDateAddAndModify(timestampPreferences);
        ParserResult entries = getEntriesWithSameFormatAndStandardField();

        migrator.performMigration(entries);

        assertEquals(getEntriesMigratedToCreationDate(), entries.getDatabase().getEntries());
    }

    /**
     * Tests migration to CreationDate if the users uses the default ISO yyyy-mm-dd format and a custom timestamp field
     */
    @Test
    public void withCustomFieldToCreationDate() {
        makeMockToMigrateToCreationDate();
        makeMockReturnCustomField();
        TimeStampToDateAddAndModify migrator = new TimeStampToDateAddAndModify(timestampPreferences);
        ParserResult entries = getEntriesWithSameFormatAndCustomField();

        migrator.performMigration(entries);

        assertEquals(getEntriesMigratedToCreationDate(), entries.getDatabase().getEntries());
    }

    /**
     * Tests migration to ModificationDate if the users uses the default ISO yyyy-mm-dd format and the standard timestamp field
     */
    @Test
    public void withStandardFieldToModificationDate() {
        makeMockToMigrateToModificationDate();
        makeMockReturnStandardField();
        TimeStampToDateAddAndModify migrator = new TimeStampToDateAddAndModify(timestampPreferences);
        ParserResult entries = getEntriesWithSameFormatAndStandardField();

        migrator.performMigration(entries);

        assertEquals(getEntriesMigratedToModificationDate(), entries.getDatabase().getEntries());
    }

    /**
     * Tests migration to ModificationDate if the users uses the default ISO yyyy-mm-dd format and a custom timestamp field
     */
    @Test
    public void withCustomFieldToModificationDate() {
        makeMockToMigrateToModificationDate();
        makeMockReturnCustomField();
        TimeStampToDateAddAndModify migrator = new TimeStampToDateAddAndModify(timestampPreferences);
        ParserResult entries = getEntriesWithSameFormatAndCustomField();

        migrator.performMigration(entries);

        assertEquals(getEntriesMigratedToModificationDate(), entries.getDatabase().getEntries());
    }

    /**
     * Tests migration of different timestamp formats with the standard timestamp field
     */
    @Test
    public void withDifferentFormats() {
        makeMockToMigrateToCreationDate();
        makeMockReturnStandardField();
        TimeStampToDateAddAndModify migrator = new TimeStampToDateAddAndModify(timestampPreferences);
        ParserResult entries = getEntriesWithDifferentFormatsAndStandardField();

        migrator.performMigration(entries);

        assertEquals(getEntriesMigratedToCreationDateFromDifferentFormats(), entries.getDatabase().getEntries());
    }

    public ParserResult getEntriesWithSameFormatAndStandardField() {
        // All entries with yyyy-mm-dd format
        BibEntry bibEntry1 = new BibEntry().withField(StandardField.TIMESTAMP, "2018-09-10");
        BibEntry bibEntry2 = new BibEntry().withField(StandardField.TIMESTAMP, "2020-12-24");
        BibEntry bibEntry3 = new BibEntry().withField(StandardField.TIMESTAMP, "2020-12-31");

        return new ParserResult(List.of(bibEntry1, bibEntry2, bibEntry3));
    }

    private ParserResult getEntriesWithSameFormatAndCustomField() {
        // All entries with yyyy-mm-dd format
        BibEntry bibEntry1 = new BibEntry().withField(customTimeStampField, "2018-09-10");
        BibEntry bibEntry2 = new BibEntry().withField(customTimeStampField, "2020-12-24");
        BibEntry bibEntry3 = new BibEntry().withField(customTimeStampField, "2020-12-31");

        return new ParserResult(List.of(bibEntry1, bibEntry2, bibEntry3));
    }

    private List<BibEntry> getEntriesMigratedToCreationDate() {
        BibEntry bibEntry1 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-09-10T00:00:00");
        BibEntry bibEntry2 = new BibEntry().withField(StandardField.CREATIONDATE, "2020-12-24T00:00:00");
        BibEntry bibEntry3 = new BibEntry().withField(StandardField.CREATIONDATE, "2020-12-31T00:00:00");

        return List.of(bibEntry1, bibEntry2, bibEntry3);
    }

    private List<BibEntry> getEntriesMigratedToModificationDate() {
        BibEntry bibEntry1 = new BibEntry().withField(StandardField.MODIFICATIONDATE, "2018-09-10T00:00:00");
        BibEntry bibEntry2 = new BibEntry().withField(StandardField.MODIFICATIONDATE, "2020-12-24T00:00:00");
        BibEntry bibEntry3 = new BibEntry().withField(StandardField.MODIFICATIONDATE, "2020-12-31T00:00:00");

        return List.of(bibEntry1, bibEntry2, bibEntry3);
    }

    private ParserResult getEntriesWithDifferentFormatsAndStandardField() {
        /* Test cases extracted from {@link org.jabref.model.entry.Date}:
         *  - "M/y" (covers 9/15, 9/2015, and 09/2015)
         *  - "MMMM (dd), yyyy" (covers September 1, 2015 and September, 2015)
         *  - "yyyy-MM-dd" (covers 2009-1-15) (already tested)
         *  - "dd-MM-yyyy" (covers 15-1-2009)
         *  - "d.M.uuuu" (covers 15.1.2015)
         *  - "uuuu.M.d" (covers 2015.1.15)
         *  - "MMM, uuuu" (covers Jan, 2020)
         */

        // M/y
        BibEntry bibEntry1 = new BibEntry().withField(StandardField.TIMESTAMP, "1/18");
        BibEntry bibEntry2 = new BibEntry().withField(StandardField.TIMESTAMP, "2/2018");
        BibEntry bibEntry3 = new BibEntry().withField(StandardField.TIMESTAMP, "03/2018");

        // MMMM, yyyy
        BibEntry bibEntry4 = new BibEntry().withField(StandardField.TIMESTAMP, "January, 2018");

        // MMMM dd, yyyy
        BibEntry bibEntry5 = new BibEntry().withField(StandardField.TIMESTAMP, "January 2, 2018");
        BibEntry bibEntry6 = new BibEntry().withField(StandardField.TIMESTAMP, "January 12, 2018");

        // dd-MM-yyyy
        BibEntry bibEntry7 = new BibEntry().withField(StandardField.TIMESTAMP, "2-1-2018");
        // Double digit day/month
        BibEntry bibEntry8 = new BibEntry().withField(StandardField.TIMESTAMP, "12-01-2018");

        // d.M.uuuu
        BibEntry bibEntry9 = new BibEntry().withField(StandardField.TIMESTAMP, "2.1.2018");
        // Double digit day/month
        BibEntry bibEntry10 = new BibEntry().withField(StandardField.TIMESTAMP, "12.01.2018");

        // uuuu.M.d
        BibEntry bibEntry11 = new BibEntry().withField(StandardField.TIMESTAMP, "2018.1.2");
        // Double digit day/month
        BibEntry bibEntry12 = new BibEntry().withField(StandardField.TIMESTAMP, "2018.01.12");

        // MMM, uuuu
        BibEntry bibEntry13 = new BibEntry().withField(StandardField.TIMESTAMP, "Jan, 2018");

        return new ParserResult(List.of(bibEntry1, bibEntry2, bibEntry3, bibEntry4, bibEntry5, bibEntry6,
                bibEntry7, bibEntry8, bibEntry9, bibEntry10, bibEntry11, bibEntry12, bibEntry13));
    }

    private List<BibEntry> getEntriesMigratedToCreationDateFromDifferentFormats() {
        // From M/y
        BibEntry bibEntry1 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-01T00:00:00");
        BibEntry bibEntry2 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-02-01T00:00:00");
        BibEntry bibEntry3 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-03-01T00:00:00");

        // MMMM, yyyy
        BibEntry bibEntry4 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-01T00:00:00");

        // MMMM dd, yyyy
        BibEntry bibEntry5 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-02T00:00:00");
        BibEntry bibEntry6 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-12T00:00:00");

        // dd-MM-yyyy
        BibEntry bibEntry7 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-02T00:00:00");
        BibEntry bibEntry8 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-12T00:00:00");

        // d.M.uuuu
        BibEntry bibEntry9 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-02T00:00:00");
        BibEntry bibEntry10 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-12T00:00:00");

        // uuuu.M.d
        BibEntry bibEntry11 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-02T00:00:00");
        BibEntry bibEntry12 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-12T00:00:00");

        // MMM, uuuu
        BibEntry bibEntry13 = new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-01T00:00:00");

        return List.of(bibEntry1, bibEntry2, bibEntry3, bibEntry4, bibEntry5, bibEntry6, bibEntry7,
                bibEntry8, bibEntry9, bibEntry10, bibEntry11, bibEntry12, bibEntry13);
    }
}
