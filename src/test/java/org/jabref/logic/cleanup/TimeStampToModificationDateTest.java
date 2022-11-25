package org.jabref.logic.cleanup;

import java.util.stream.Stream;

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

class TimeStampToModificationDateTest {

    private static Field customTimeStampField = new UnknownField("dateOfCreation");

    private TimestampPreferences timestampPreferences = Mockito.mock(TimestampPreferences.class);

    public void makeMockReturnCustomField() {
        Mockito.when(timestampPreferences.getTimestampField()).then(invocation -> customTimeStampField);
    }

    public void makeMockReturnStandardField() {
        Mockito.when(timestampPreferences.getTimestampField()).then(invocation -> StandardField.TIMESTAMP);
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
     * Tests migration to field "modificationdate" if the users uses the default ISO yyyy-mm-dd format and the standard timestamp field
     */
    @ParameterizedTest
    @MethodSource("standardFieldToModificationDate")
    public void withStandardFieldToModificationDate(BibEntry expected, BibEntry input) {
        makeMockReturnStandardField();
        TimeStampToModificationDate migrator = new TimeStampToModificationDate(timestampPreferences);
        migrator.cleanup(input);
        assertEquals(expected, input);
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
     * Tests migration to field "modificationdate" if the users uses the default ISO yyyy-mm-dd format and a custom timestamp field
     */
    @ParameterizedTest
    @MethodSource("customFieldToModificationDate")
    public void withCustomFieldToModificationDate(BibEntry expected, BibEntry input) {
        makeMockReturnCustomField();
        TimeStampToModificationDate migrator = new TimeStampToModificationDate(timestampPreferences);
        migrator.cleanup(input);
        assertEquals(expected, input);
    }
}
