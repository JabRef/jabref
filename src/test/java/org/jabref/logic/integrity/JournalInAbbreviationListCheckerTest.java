package org.jabref.logic.integrity;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class JournalInAbbreviationListCheckerTest {

    @Test
    void journalAcceptsNameInTheList() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.JOURNAL, "IEEE Software"));
    }

    @Test
    void journalDoesNotAcceptNameNotInList() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.JOURNAL, "IEEE Whocares"));
    }

    @Test
    void bibLaTexDoesNotAcceptRandomInputInTitle() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.JOURNALTITLE, "A journal"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibTexDoesNotAcceptRandomInputInTitle() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.JOURNAL, "A journal"), BibDatabaseMode.BIBTEX));
    }
}
