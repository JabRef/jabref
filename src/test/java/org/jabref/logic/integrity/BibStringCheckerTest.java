package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibStringCheckerTest {

    private final BibStringChecker checker = new BibStringChecker();
    private final BibEntry entry = new BibEntry();

    @Test
    void fieldAcceptsNoHashMarks() {
        entry.setField(StandardField.TITLE, "Not a single hash mark");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void monthAcceptsEvenNumberOfHashMarks() {
        entry.setField(StandardField.MONTH, "#jan#");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void authorAcceptsEvenNumberOfHashMarks() {
        entry.setField(StandardField.AUTHOR, "#einstein# and #newton#");
        assertEquals(Collections.emptyList(), checker.check(entry));
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
