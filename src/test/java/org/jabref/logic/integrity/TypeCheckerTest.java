package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeCheckerTest {

    private final TypeChecker checker = new TypeChecker();
    private BibEntry entry;

    @Test
    void inProceedingsHasPagesNumbers() {
        entry = new BibEntry(StandardEntryType.InProceedings);
        entry.setField(StandardField.PAGES, "11--15");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void proceedingsDoesNotHavePageNumbers() {
        entry = new BibEntry(StandardEntryType.Proceedings);
        entry.setField(StandardField.PAGES, "11--15");
        assertEquals(List.of(new IntegrityMessage("wrong entry type as proceedings has page numbers", entry, StandardField.PAGES)), checker.check(entry));
    }

}
