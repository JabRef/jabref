package org.jabref.logic.integrity;

import java.util.Collections;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibtexKeyCheckerTest {

    private final BibtexKeyChecker checker = new BibtexKeyChecker();
    private final BibEntry entry = new BibEntry();

    @Test
    void bibTexAcceptsKeyFromAuthorAndYear() {
        entry.setField(InternalField.KEY_FIELD, "Knuth2014");
        entry.setField(StandardField.AUTHOR, "Knuth");
        entry.setField(StandardField.YEAR, "2014");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

}
