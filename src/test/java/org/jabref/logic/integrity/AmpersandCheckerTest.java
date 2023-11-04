package org.jabref.logic.integrity;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AmpersandCheckerTest {

    private final AmpersandChecker checker = new AmpersandChecker();
    private final BibEntry entry = new BibEntry();

    @ParameterizedTest
    @MethodSource("provideAcceptedInputs")
    void acceptsAllowedInputs(Field field, String value) {
        entry.setField(field, value);
        assertEquals(List.of(), checker.check(entry));
    }

    private static Stream<Arguments> provideAcceptedInputs() {
        return Stream.of(
                Arguments.of(StandardField.TITLE, "No ampersand at all"),
                Arguments.of(StandardField.FOREWORD, "Properly escaped \\&"),
                Arguments.of(StandardField.AUTHOR, "\\& Multiple properly escaped \\&"),
                Arguments.of(StandardField.BOOKTITLE, "\\\\\\& With multiple backslashes"),
                Arguments.of(StandardField.COMMENT, "\\\\\\& With multiple backslashes multiple times \\\\\\\\\\&"),
                Arguments.of(StandardField.NOTE, "In the \\& middle of \\\\\\& something"),

                // Verbatim fields
                Arguments.of(StandardField.FILE, "one & another.pdf"),
                Arguments.of(StandardField.URL, "https://example.org?key=value&key2=value2")
        );
    }

    @ParameterizedTest
    @MethodSource("provideUnacceptedInputs")
    void rejectsDisallowedInputs(String expectedMessage, Field field, String value) {
        entry.setField(field, value);
        assertEquals(List.of(new IntegrityMessage(expectedMessage, entry, field)), checker.check(entry));
    }

    private static Stream<Arguments> provideUnacceptedInputs() {
        return Stream.of(
                Arguments.of("Found 1 unescaped '&'", StandardField.SUBTITLE, "A single &"),
                Arguments.of("Found 2 unescaped '&'", StandardField.ABSTRACT, "Multiple \\\\& not properly & escaped"),
                Arguments.of("Found 1 unescaped '&'", StandardField.AUTHOR, "To many backslashes \\\\&"),
                Arguments.of("Found 2 unescaped '&'", StandardField.LABEL, "\\\\\\\\& Multiple times \\\\& multiple backslashes"),

                // entryWithEscapedAndUnescapedAmpersand
                Arguments.of("Found 1 unescaped '&'", StandardField.TITLE, "Jack \\& Jill & more"),

                // entryWithMultipleEscapedAndUnescapedAmpersands
                Arguments.of("Found 4 unescaped '&'", StandardField.AFTERWORD, "May the force be with you & live long \\\\& prosper \\& to infinity \\\\\\& beyond & assemble \\\\\\\\& excelsior!")
        );
    }
}
