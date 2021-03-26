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

public class BibStringCheckerTest {

    private final BibStringChecker checker = new BibStringChecker();
    private final BibEntry entry = new BibEntry();

    @ParameterizedTest
    @MethodSource("provideAcceptedInputs")
    void acceptsAllowedInputs(List<IntegrityMessage> expected, Field field, String value) {
        entry.setField(field, value);
        assertEquals(expected, checker.check(entry));
    }

    private static Stream<Arguments> provideAcceptedInputs() {
        return Stream.of(
                Arguments.of(Collections.emptyList(), StandardField.TITLE, "Not a single hash mark"),
                Arguments.of(Collections.emptyList(), StandardField.MONTH, "#jan#"),
                Arguments.of(Collections.emptyList(), StandardField.AUTHOR, "#einstein# and #newton#")
        );
    }

    @Test
    void monthDoesNotAcceptOddNumberOfHashMarks() {
        entry.setField(StandardField.MONTH, "#jan");
        assertEquals(List.of(new IntegrityMessage("odd number of unescaped '#'", entry, StandardField.MONTH)), checker.check(entry));
    }

    @Test
    void authorDoesNotAcceptOddNumberOfHashMarks() {
        entry.setField(StandardField.AUTHOR, "#einstein# #amp; #newton#");
        assertEquals(List.of(new IntegrityMessage("odd number of unescaped '#'", entry, StandardField.AUTHOR)), checker.check(entry));
    }
}
