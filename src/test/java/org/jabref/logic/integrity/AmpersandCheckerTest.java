package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AmpersandCheckerTest {

    private final AmpersandChecker checker = new AmpersandChecker();
    private final BibEntry entry = new BibEntry();

    @ParameterizedTest
    @MethodSource("provideAcceptedInputs")
    void acceptsAllowedInputs(List<IntegrityMessage> expected, Field field, String value) {
        entry.setField(field, value);
        assertEquals(expected, checker.check(entry));
    }

    private static Stream<Arguments> provideAcceptedInputs() {
        return Stream.of(
                Arguments.of(Collections.emptyList(), StandardField.TITLE, "No ampersand at all"),
                Arguments.of(Collections.emptyList(), StandardField.FOREWORD, "Properly escaped \\&"),
                Arguments.of(Collections.emptyList(), StandardField.AUTHOR, "\\& Multiple properly escaped \\&"),
                Arguments.of(Collections.emptyList(), StandardField.BOOKTITLE, "\\\\\\& With multiple backslashes"),
                Arguments.of(Collections.emptyList(), StandardField.COMMENT, "\\\\\\& With multiple backslashes multiple times \\\\\\\\\\&"),
                Arguments.of(Collections.emptyList(), StandardField.NOTE, "In the \\& middle of \\\\\\& something")
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
                Arguments.of("Found 2 unescaped '&'", StandardField.LABEL, "\\\\\\\\& Multiple times \\\\& multiple backslashes")
        );
    }

    @Test
    void entryWithEscapedAndUnescapedAmpersand() {
        entry.setField(StandardField.TITLE, "Jack \\& Jill & more");
        assertEquals(List.of(new IntegrityMessage("Found 1 unescaped '&'", entry, StandardField.TITLE)), checker.check(entry));
    }

    @Test
    void entryWithMultipleEscapedAndUnescapedAmpersands() {
        entry.setField(StandardField.AFTERWORD, "May the force be with you & live long \\\\& prosper \\& to infinity \\\\\\& beyond & assemble \\\\\\\\& excelsior!");
        assertEquals(List.of(new IntegrityMessage("Found 4 unescaped '&'", entry, StandardField.AFTERWORD)), checker.check(entry));
    }
}
