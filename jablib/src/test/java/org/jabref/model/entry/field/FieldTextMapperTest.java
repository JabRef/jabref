package org.jabref.model.entry.field;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldTextMapperTest {

    @ParameterizedTest
    @MethodSource("provideFieldsAndExpectedNames")
    void getDisplayNameResolvesExpectedLabels(Field field, String expectedDisplayName) {
        assertEquals(expectedDisplayName, FieldTextMapper.getDisplayName(field));
    }

    private static Stream<Arguments> provideFieldsAndExpectedNames() {
        return Stream.of(
                // Standard fields
                Arguments.of(StandardField.AUTHOR, "Author"),
                Arguments.of(StandardField.TITLE, "Title"),
                Arguments.of(StandardField.DOI, "DOI"),
                Arguments.of(StandardField.ISBN, "ISBN"),
                Arguments.of(StandardField.URL, "URL"),

                // Internal fields
                Arguments.of(InternalField.TYPE_HEADER, "Entry Type"),
                Arguments.of(InternalField.OBSOLETE_TYPE_HEADER, "Entry Type"),
                Arguments.of(InternalField.KEY_FIELD, "Citation Key"),
                Arguments.of(InternalField.INTERNAL_ALL_FIELD, "All"),
                Arguments.of(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, "All text fields"),

                // Special fields
                Arguments.of(SpecialField.PRINTED, "Printed"),
                Arguments.of(SpecialField.PRIORITY, "Priority"),
                Arguments.of(SpecialField.QUALITY, "Quality"),
                Arguments.of(SpecialField.RANKING, "Ranking"),
                Arguments.of(SpecialField.READ_STATUS, "Read status"),
                Arguments.of(SpecialField.RELEVANCE, "Relevance")
        );
    }

    static Stream<Arguments> displayNames() {
        return Stream.of(
                Arguments.of("Author", StandardField.AUTHOR),
                Arguments.of("DOI", StandardField.DOI),
                Arguments.of("Citation Key", InternalField.KEY_FIELD),
                Arguments.of("Entry Type", InternalField.TYPE_HEADER),
                Arguments.of("JabRef-internal-id", InternalField.INTERNAL_ID_FIELD),
                Arguments.of("myField", new UnknownField("myField"))
        );
    }

    @Test
    void getDisplayNameForOrFieldsJoinsComponents() {
        OrFields orFields = new OrFields(StandardField.AUTHOR, StandardField.EDITOR);
        assertEquals("Author/Editor", FieldTextMapper.getDisplayName(orFields));
    }

    @ParameterizedTest
    @MethodSource
    void displayNames(String expected, Field field) {
        assertEquals(expected, FieldTextMapper.getDisplayName(field));
    }
}
