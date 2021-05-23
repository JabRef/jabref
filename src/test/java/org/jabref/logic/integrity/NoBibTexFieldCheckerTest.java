package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoBibTexFieldCheckerTest {

    private final NoBibtexFieldChecker checker = new NoBibtexFieldChecker();

    private static Stream<Field> nonBiblatexOnlyFields() {
        return Stream.of(
                // arbitrary field
                new UnknownField("fieldNameNotDefinedInThebiblatexManual"),
                StandardField.ABSTRACT,
                StandardField.COMMENT,
                StandardField.DOI,
                StandardField.URL,

                // these fields are not recognized as biblatex only fields
                StandardField.ADDRESS,
                StandardField.INSTITUTION,
                StandardField.JOURNAL,
                StandardField.KEYWORDS,
                StandardField.REVIEW
        );
    }

    @ParameterizedTest()
    @MethodSource("nonBiblatexOnlyFields")
    void nonBiblatexOnlyField(Field field) {
        BibEntry entry = new BibEntry().withField(field, "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
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
