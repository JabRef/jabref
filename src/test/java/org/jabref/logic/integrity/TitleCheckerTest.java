package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TitleCheckerTest {

    private TitleChecker checker;

    @BeforeEach
    public void setUp() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.setMode(BibDatabaseMode.BIBTEX);
        checker = new TitleChecker(databaseContext);
    }

    @Test
    public void firstLetterAsOnlyCapitalLetterInSubTitle2() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1: This is a sub title 2"));
    }

    @Test
    public void noCapitalLetterInSubTitle2() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1: this is a sub title 2"));
    }

    @Test
    public void twoCapitalLettersInSubTitle2() {
        assertNotEquals(Optional.empty(), checker.checkValue("This is a sub title 1: This is A sub title 2"));
    }

    @Test
    public void middleLetterAsOnlyCapitalLetterInSubTitle2() {
        assertNotEquals(Optional.empty(), checker.checkValue("This is a sub title 1: this is A sub title 2"));
    }

    @Test
    public void twoCapitalLettersInSubTitle2WithCurlyBrackets() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1: This is {A} sub title 2"));
    }

    @Test
    public void middleLetterAsOnlyCapitalLetterInSubTitle2WithCurlyBrackets() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1: this is {A} sub title 2"));
    }

    @Test
    public void firstLetterAsOnlyCapitalLetterInSubTitle2AfterContinuousDelimiters() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1...This is a sub title 2"));
    }

    @Test
    public void middleLetterAsOnlyCapitalLetterInSubTitle2AfterContinuousDelimiters() {
        assertNotEquals(Optional.empty(), checker.checkValue("This is a sub title 1... this is a sub Title 2"));
    }

    @Test
    public void firstLetterAsOnlyCapitalLetterInEverySubTitleWithContinuousDelimiters() {
        assertEquals(Optional.empty(), checker.checkValue("This is; A sub title 1.... This is a sub title 2"));
    }

    @Test
    public void firstLetterAsOnlyCapitalLetterInEverySubTitleWithRandomDelimiters() {
        assertEquals(Optional.empty(), checker.checkValue("This!is!!A!Title??"));
    }

    @Test
    public void moreThanOneCapitalLetterInSubTitleWithoutCurlyBrackets() {
        assertNotEquals(Optional.empty(), checker.checkValue("This!is!!A!TitlE??"));
    }

    @Test
    void bibTexAcceptsTitleWithOnlyFirstCapitalLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a title"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptCapitalLettersInsideTitle() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a Title"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexRemovesCapitalLetterInsideTitle() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a {T}itle"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexRemovesEverythingInBracketsAndAcceptsNoTitleInput() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "{This is a Title}"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexRemovesEverythingInBrackets() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a {Title}"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexAcceptsTitleWithLowercaseFirstLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "{C}urrent {C}hronicle"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexAcceptsSubTitlesWithOnlyFirstCapitalLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a sub title 1: This is a sub title 2"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexAcceptsSubTitleWithLowercaseFirstLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a sub title 1: this is a sub title 2"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptCapitalLettersInsideSubTitle() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a sub title 1: This is A sub title 2"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexRemovesCapitalLetterInsideSubTitle() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a sub title 1: this is {A} sub title 2"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexSplitsSubTitlesBasedOnDots() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a sub title 1...This is a sub title 2"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexSplitsSubTitleBasedOnSpecialCharacters() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is; A sub title 1.... This is a sub title 2"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexAcceptsCapitalLetterAfterSpecialCharacter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This!is!!A!Title??"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexAcceptsCapitalLetterOnlyAfterSpecialCharacter() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This!is!!A!TitlE??"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibLaTexAcceptsTitleWithOnlyFirstCapitalLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a title"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsCapitalLettersInsideTitle() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a Title"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexRemovesCapitalLetterInsideTitle() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a {T}itle"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexRemovesEverythingInBracketsAndAcceptsNoTitleInput() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "{This is a Title}"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexRemovesEverythingInBrackets() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a {Title}"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsTitleWithLowercaseFirstLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "{C}urrent {C}hronicle"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsSubTitlesWithOnlyFirstCapitalLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a sub title 1: This is a sub title 2"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsSubTitleWithLowercaseFirstLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a sub title 1: this is a sub title 2"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsCapitalLettersInsideSubTitle() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a sub title 1: This is A sub title 2"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexRemovesCapitalLetterInsideSubTitle() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a sub title 1: this is {A} sub title 2"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexSplitsSubTitlesBasedOnDots() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is a sub title 1...This is a sub title 2"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexSplitsSubTitleBasedOnSpecialCharacters() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This is; A sub title 1.... This is a sub title 2"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsCapitalLetterAfterSpecialCharacter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This!is!!A!Title??"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsCapitalLetterNotOnlyAfterSpecialCharacter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.TITLE, "This!is!!A!TitlE??"), BibDatabaseMode.BIBLATEX));
    }

}
