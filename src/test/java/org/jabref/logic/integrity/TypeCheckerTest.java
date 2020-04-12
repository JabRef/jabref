package org.jabref.logic.integrity;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

public class TypeCheckerTest {

    @Test
    void inProceedingshasPagesNumbers() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.PAGES, "11--15", StandardEntryType.InProceedings));
    }

    @Test
    void proceedingsDoesNotHavePageNumbers() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.PAGES, "11--15", StandardEntryType.Proceedings));
    }

}
