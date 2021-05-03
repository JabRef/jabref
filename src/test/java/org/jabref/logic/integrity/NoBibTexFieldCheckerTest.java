package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoBibTexFieldCheckerTest {

    private final NoBibtexFieldChecker checker = new NoBibtexFieldChecker();

    private static Stream<Arguments> nonBiblatexOnlyFields() {
        return Stream.of(
                // arbitrary field
                Arguments.of(new UnknownField("fieldNameNotDefinedInThebiblatexManual"), Collections.emptyList()),

                // these fields are displayed by JabRef as default
                Arguments.of(StandardField.ABSTRACT, Collections.emptyList()),
                Arguments.of(StandardField.COMMENT, Collections.emptyList()),
                Arguments.of(StandardField.DOI, Collections.emptyList()),
                Arguments.of(StandardField.URL, Collections.emptyList()),

                // these fields are not recognized as biblatex only fields
                Arguments.of(StandardField.ADDRESS, Collections.emptyList()),
                Arguments.of(StandardField.INSTITUTION, Collections.emptyList()),
                Arguments.of(StandardField.JOURNAL, Collections.emptyList()),
                Arguments.of(StandardField.KEYWORDS, Collections.emptyList()),
                Arguments.of(StandardField.REVIEW, Collections.emptyList())
        );
    }

    @ParameterizedTest()
    @MethodSource("nonBiblatexOnlyFields")
    void nonBiblatexOnlyField(Field field, List<IntegrityMessage> expectedResult) {
        BibEntry entry = new BibEntry().withField(field, "test");
        assertEquals(expectedResult, checker.check(entry));
    }

    @ParameterizedTest(name = "field={0}")
    @CsvSource({
            "AFTERWORD",
            "JOURNALTITLE",
            "LOCATION"
    })
    void biblatexOnlyField(StandardField field) {
        BibEntry entry = new BibEntry().withField(field, "test");
        IntegrityMessage message = new IntegrityMessage("biblatex field only", entry, field);
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

}
