package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PersonNamesCheckerTest {

    private PersonNamesChecker checker;

    @BeforeEach
    public void setUp() throws Exception {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.setMode(BibDatabaseMode.BIBTEX);
        checker = new PersonNamesChecker(databaseContext);
    }

    @Test
    public void validNameFirstnameAuthor() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("Kolb, Stefan"));
    }

    @Test
    public void validNameFirstnameAuthors() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("Kolb, Stefan and Harrer, Simon"));
    }

    @Test
    public void validFirstnameNameAuthor() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("Stefan Kolb"));
    }

    @Test
    public void validFirstnameNameAuthors() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("Stefan Kolb and Simon Harrer"));
    }

    @Test
    public void complainAboutPersonStringWithTwoManyCommas() throws Exception {
        assertEquals(Optional.of("Names are not in the standard BibTeX format."),
                checker.checkValue("Test1, Test2, Test3, Test4, Test5, Test6"));
    }

    @Test
    public void doNotComplainAboutSecondNameInFront() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("M. J. Gotay"));
    }

    @Test
    public void validCorporateNameInBrackets() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("{JabRef}"));
    }

    @Test
    public void validCorporateNameAndPerson() throws Exception {
        assertEquals(Optional.empty(), checker.checkValue("{JabRef} and Stefan Kolb"));
        assertEquals(Optional.empty(), checker.checkValue("{JabRef} and Kolb, Stefan"));
    }

    @Test
    void authorAcceptsVoidInput() {
        for (Field field : FieldFactory.getPersonNameFields()) {
            // getPersonNameFields returns fields that are available in biblatex only
            // if run without mode, the NoBibtexFieldChecker will complain that "afterword" is a biblatex only field
            IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(field, ""), BibDatabaseMode.BIBLATEX));
        }
    }

    @Test
    void authorAcceptsLastNameOnly() {
        for (Field field : FieldFactory.getPersonNameFields()) {
            IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(field, "Knuth"), BibDatabaseMode.BIBLATEX));
        }
    }

    @Test
    void authorDoesNotAcceptSpacesBeforeFormat() {
        for (Field field : FieldFactory.getPersonNameFields()) {
            IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(field, "   Knuth, Donald E. "), BibDatabaseMode.BIBLATEX));
        }
    }

    @Test
    void authorDoesNotAcceptDifferentFormats() {
        for (Field field : FieldFactory.getPersonNameFields()) {
            IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(field, "Knuth, Donald E. and Kurt Cobain and A. Einstein"), BibDatabaseMode.BIBLATEX));
        }
    }

    @Test
    void authorAcceptsMultipleAuthors() {
        for (Field field : FieldFactory.getPersonNameFields()) {
            IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(field, "Donald E. Knuth and Kurt Cobain and A. Einstein"), BibDatabaseMode.BIBLATEX));
        }
    }

    @Test
    void authorCanNotStartWithComma() {
        for (Field field : FieldFactory.getPersonNameFields()) {
            IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(field, ", and Kurt Cobain and A. Einstein"), BibDatabaseMode.BIBLATEX));
        }
    }

    @Test
    void authorDoesNotAcceptCommaAsAuthor() {
        for (Field field : FieldFactory.getPersonNameFields()) {
            IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(field, "Donald E. Knuth and Kurt Cobain and ,"), BibDatabaseMode.BIBLATEX));
        }
    }

    @Test
    void authorCanNotStartWithAnd() {
        for (Field field : FieldFactory.getPersonNameFields()) {
            IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(field, "and Kurt Cobain and A. Einstein"), BibDatabaseMode.BIBLATEX));
        }
    }

    @Test
    void authorDoesNotAcceptUnfinishedSentence() {
        for (Field field : FieldFactory.getPersonNameFields()) {
            IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(field, "Donald E. Knuth and Kurt Cobain and"), BibDatabaseMode.BIBLATEX));
        }
    }

}
