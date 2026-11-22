package org.jabref.logic.integrity;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UserSpecificCommentField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
@ResourceLock("Localization.lang")
class PercentCheckerTest {

    private final PercentChecker checker = new PercentChecker();

    @ParameterizedTest
    @MethodSource("provideAcceptedInputs")
    void acceptsAllowedInputs(Field field, String value) {
        BibEntry entry = new BibEntry().withField(field, value);
        assertEquals(List.of(), checker.check(entry));
    }

    private static Stream<Arguments> provideAcceptedInputs() {
        return Stream.of(
                Arguments.of(StandardField.TITLE, "No percent sign at all"),
                Arguments.of(StandardField.TITLE, "Properly escaped 50\\%"),
                Arguments.of(StandardField.ABSTRACT, "\\% Multiple properly escaped \\%"),
                Arguments.of(StandardField.BOOKTITLE, "\\\\\\% With three backslashes"),
                Arguments.of(StandardField.URL, "https://example.com/my%20paper.pdf"),
                Arguments.of(StandardField.COMMENT, "50% unescaped in comment is fine"),
                Arguments.of(new UserSpecificCommentField("test"), "100% fine in user comment")
        );
    }

    @ParameterizedTest
    @MethodSource("provideUnacceptedInputs")
    void rejectsDisallowedInputs(String expectedMessage, Field field, String value) {
        BibEntry entry = new BibEntry().withField(field, value);
        assertEquals(List.of(new IntegrityMessage(expectedMessage, entry, field)), checker.check(entry));
    }

    private static Stream<Arguments> provideUnacceptedInputs() {
        return Stream.of(
                Arguments.of("Found 1 unescaped '%'", StandardField.TITLE, "50% accuracy"),
                Arguments.of("Found 2 unescaped '%'", StandardField.ABSTRACT, "From 10% to 90%"),
                Arguments.of("Found 1 unescaped '%'", StandardField.NOTE, "10\\% escaped and 20% unescaped"),
                Arguments.of("Found 1 unescaped '%'", StandardField.BOOKTITLE, "\\\\% With two backslashes"),
                Arguments.of("Found 2 unescaped '%'", StandardField.TITLE, "\\\\% First and \\\\\\\\% second")
        );
    }
}
