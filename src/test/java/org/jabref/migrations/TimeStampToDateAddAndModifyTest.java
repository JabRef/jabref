package org.jabref.migrations;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeStampToDateAddAndModifyTest {

    private static Field customTimeStampField = new UnknownField("dateOfCreation");

    private TimestampPreferences timestampPreferences = Mockito.mock(TimestampPreferences.class);

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

    public static Stream<Arguments> standardFieldToCreationDate() {
        return Stream.of(
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-09-10T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "2018-09-10")
                ),
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2020-12-24T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "2020-12-24")
                ),
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2020-12-31T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "2020-12-31")
                )
        );
    }

    /**
     * Tests migration to creationdate if the users uses the default ISO yyyy-mm-dd format and the standard timestamp field
     */
    @ParameterizedTest
    @MethodSource("standardFieldToCreationDate")
    public void withStandardFieldToCreationDate(BibEntry expected, BibEntry input) {
        makeMockToMigrateToCreationDate();
        makeMockReturnStandardField();
        TimeStampToDateAddAndModify migrator = new TimeStampToDateAddAndModify(timestampPreferences);
        ParserResult entries = new ParserResult(List.of(input));
        migrator.performMigration(entries);
        assertEquals(List.of(expected), entries.getDatabase().getEntries());
    }

    public static Stream<Arguments> customFieldToCreationDate() {
        return Stream.of(
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-09-10T00:00:00"),
                        new BibEntry().withField(customTimeStampField, "2018-09-10")
                ),
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2020-12-24T00:00:00"),
                        new BibEntry().withField(customTimeStampField, "2020-12-24")
                ),
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2020-12-31T00:00:00"),
                        new BibEntry().withField(customTimeStampField, "2020-12-31")
                )
        );
    }

    /**
     * Tests migration to creationdate if the users uses the default ISO yyyy-mm-dd format and a custom timestamp field
     */
    @ParameterizedTest
    @MethodSource("customFieldToCreationDate")
    public void withCustomFieldToCreationDate(BibEntry expected, BibEntry input) {
        makeMockToMigrateToCreationDate();
        makeMockReturnCustomField();
        TimeStampToDateAddAndModify migrator = new TimeStampToDateAddAndModify(timestampPreferences);
        ParserResult entries = new ParserResult(List.of(input));
        migrator.performMigration(entries);
        assertEquals(List.of(expected), entries.getDatabase().getEntries());
    }

    public static Stream<Arguments> standardFieldToModificationDate() {
        return Stream.of(
                Arguments.of(
                        new BibEntry().withField(StandardField.MODIFICATIONDATE, "2018-09-10T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "2018-09-10")
                ),
                Arguments.of(
                        new BibEntry().withField(StandardField.MODIFICATIONDATE, "2020-12-24T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "2020-12-24")
                ),
                Arguments.of(
                        new BibEntry().withField(StandardField.MODIFICATIONDATE, "2020-12-31T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "2020-12-31")
                )
        );
    }

    /**
     * Tests migration to modificationdate if the users uses the default ISO yyyy-mm-dd format and the standard timestamp field
     */
    @ParameterizedTest
    @MethodSource("standardFieldToModificationDate")
    public void withStandardFieldToModificationDate(BibEntry expected, BibEntry input) {
        makeMockToMigrateToModificationDate();
        makeMockReturnStandardField();
        TimeStampToDateAddAndModify migrator = new TimeStampToDateAddAndModify(timestampPreferences);
        ParserResult entries = new ParserResult(List.of(input));
        migrator.performMigration(entries);
        assertEquals(List.of(expected), entries.getDatabase().getEntries());
    }

    public static Stream<Arguments> customFieldToModificationDate() {
        return Stream.of(
                Arguments.of(
                        new BibEntry().withField(StandardField.MODIFICATIONDATE, "2018-09-10T00:00:00"),
                        new BibEntry().withField(customTimeStampField, "2018-09-10")
                ),
                Arguments.of(
                        new BibEntry().withField(StandardField.MODIFICATIONDATE, "2020-12-24T00:00:00"),
                        new BibEntry().withField(customTimeStampField, "2020-12-24")
                ),
                Arguments.of(
                        new BibEntry().withField(StandardField.MODIFICATIONDATE, "2020-12-31T00:00:00"),
                        new BibEntry().withField(customTimeStampField, "2020-12-31")
                )
        );
    }

    /**
     * Tests migration to modificationdate if the users uses the default ISO yyyy-mm-dd format and a custom timestamp field
     */
    @ParameterizedTest
    @MethodSource("customFieldToModificationDate")
    public void withCustomFieldToModificationDate(BibEntry expected, BibEntry input) {
        makeMockToMigrateToModificationDate();
        makeMockReturnCustomField();
        TimeStampToDateAddAndModify migrator = new TimeStampToDateAddAndModify(timestampPreferences);
        ParserResult entries = new ParserResult(List.of(input));
        migrator.performMigration(entries);
        assertEquals(List.of(expected), entries.getDatabase().getEntries());
    }

    public static Stream<Arguments> entriesMigratedToCreationDateFromDifferentFormats() {
        return Stream.of(
                // M/y
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-01T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "1/18")
                ),
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-02-01T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "2/2018")
                ),
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-03-01T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "03/2018")
                ),

                // MMMM, yyyy
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-01T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "January, 2018")
                ),

                // MMMM dd, yyyy
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-02T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "January 2, 2018")
                ),
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-12T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "January 12, 2018")
                ),

                // dd-MM-yyyy
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-02T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "2-1-2018")
                ),

                // Double digit day/month
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-12T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "12-01-2018")
                ),

                // d.M.uuuu
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-02T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "2.1.2018")
                ),

                // Double digit day/month
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-12T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "12.01.2018")
                ),

                // uuuu.M.d
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-02T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "2018.1.2")
                ),

                // Double digit day/month
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-12T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "2018.01.12")
                ),

                // MMM, uuuu
                Arguments.of(
                        new BibEntry().withField(StandardField.CREATIONDATE, "2018-01-01T00:00:00"),
                        new BibEntry().withField(StandardField.TIMESTAMP, "Jan, 2018")
                ));
    }

    /**
     * Tests migration of different timestamp formats with the standard timestamp field
     */
    @ParameterizedTest
    @MethodSource("entriesMigratedToCreationDateFromDifferentFormats")
    public void withDifferentFormats(BibEntry expected, BibEntry input) {
        makeMockToMigrateToCreationDate();
        makeMockReturnStandardField();
        TimeStampToDateAddAndModify migrator = new TimeStampToDateAddAndModify(timestampPreferences);
        ParserResult parserResult = new ParserResult(List.of(input));
        migrator.performMigration(parserResult);
        assertEquals(List.of(expected), parserResult.getDatabase().getEntries());
    }

}
