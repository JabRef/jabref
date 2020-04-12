package org.jabref.logic.integrity;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class BibtexKeyCheckerTest {

    @Test
    void bibTexAcceptsKeyFromAuthorAndYear() {
        final BibDatabaseContext correctContext = IntegrityCheckTest.createContext(InternalField.KEY_FIELD, "Knuth2014");
        correctContext.getDatabase().getEntries().get(0).setField(StandardField.AUTHOR, "Knuth");
        correctContext.getDatabase().getEntries().get(0).setField(StandardField.YEAR, "2014");
        IntegrityCheckTest.assertCorrect(correctContext);
    }

    @Test
    void bibtexDooesNotAcceptRandomKey() {
        final BibDatabaseContext wrongContext = IntegrityCheckTest.createContext(InternalField.KEY_FIELD, "Knuth2014a");
        wrongContext.getDatabase().getEntries().get(0).setField(StandardField.AUTHOR, "Knuth");
        wrongContext.getDatabase().getEntries().get(0).setField(StandardField.YEAR, "2014");
        IntegrityCheckTest.assertWrong(wrongContext);
    }

}
