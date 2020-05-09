package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TitleCheckerTest {

    private TitleChecker checker;
    private TitleChecker checkerBiblatex;

    @BeforeEach
    public void setUp() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        BibDatabaseContext databaseBiblatex = new BibDatabaseContext();
        databaseContext.setMode(BibDatabaseMode.BIBTEX);
        checker = new TitleChecker(databaseContext);
        databaseBiblatex.setMode(BibDatabaseMode.BIBLATEX);
        checkerBiblatex = new TitleChecker(databaseBiblatex);
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
        assertEquals(Optional.empty(), checker.checkValue("This is a title"));
    }

    @Test
    void bibTexDoesNotAcceptCapitalLettersInsideTitle() {
        assertNotEquals(Optional.empty(), checker.checkValue("This is a Title"));
    }

    @Test
    void bibTexRemovesCapitalLetterInsideTitle() {
        assertEquals(Optional.empty(), checker.checkValue("This is a {T}itle"));
    }

    @Test
    void bibTexRemovesEverythingInBracketsAndAcceptsNoTitleInput() {
        assertEquals(Optional.empty(), checker.checkValue("{This is a Title}"));
    }

    @Test
    void bibTexRemovesEverythingInBrackets() {
        assertEquals(Optional.empty(), checker.checkValue("This is a {Title}"));
    }

    @Test
    void bibTexAcceptsTitleWithLowercaseFirstLetter() {
        assertEquals(Optional.empty(), checker.checkValue("{C}urrent {C}hronicle"));
    }

    @Test
    void bibTexAcceptsSubTitlesWithOnlyFirstCapitalLetter() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1: This is a sub title 2"));
    }

    @Test
    void bibTexAcceptsSubTitleWithLowercaseFirstLetter() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1: this is a sub title 2"));
    }

    @Test
    void bibTexDoesNotAcceptCapitalLettersInsideSubTitle() {
        assertNotEquals(Optional.empty(), checker.checkValue("This is a sub title 1: This is A sub title 2"));
    }

    @Test
    void bibTexRemovesCapitalLetterInsideSubTitle() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1: this is {A} sub title 2"));
    }

    @Test
    void bibTexSplitsSubTitlesBasedOnDots() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1...This is a sub title 2"));
    }

    @Test
    void bibTexSplitsSubTitleBasedOnSpecialCharacters() {
        assertEquals(Optional.empty(), checker.checkValue("This is; A sub title 1.... This is a sub title 2"));
    }

    @Test
    void bibTexAcceptsCapitalLetterAfterSpecialCharacter() {
        assertEquals(Optional.empty(), checker.checkValue("This!is!!A!Title??"));
    }

    @Test
    void bibTexAcceptsCapitalLetterOnlyAfterSpecialCharacter() {
        assertNotEquals(Optional.empty(), checker.checkValue("This!is!!A!TitlE??"));
    }

    @Test
    void bibLaTexAcceptsTitleWithOnlyFirstCapitalLetter() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This is a title"));
    }

    @Test
    void bibLaTexAcceptsCapitalLettersInsideTitle() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This is a Title"));
    }

    @Test
    void bibLaTexRemovesCapitalLetterInsideTitle() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This is a {T}itle"));
    }

    @Test
    void bibLaTexRemovesEverythingInBracketsAndAcceptsNoTitleInput() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("{This is a Title}"));
    }

    @Test
    void bibLaTexRemovesEverythingInBrackets() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This is a {Title}"));
    }

    @Test
    void bibLaTexAcceptsTitleWithLowercaseFirstLetter() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("{C}urrent {C}hronicle"));
    }

    @Test
    void bibLaTexAcceptsSubTitlesWithOnlyFirstCapitalLetter() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This is a sub title 1: This is a sub title 2"));
    }

    @Test
    void bibLaTexAcceptsSubTitleWithLowercaseFirstLetter() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This is a sub title 1: this is a sub title 2"));
    }

    @Test
    void bibLaTexAcceptsCapitalLettersInsideSubTitle() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This is a sub title 1: This is A sub title 2"));
    }

    @Test
    void bibLaTexRemovesCapitalLetterInsideSubTitle() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This is a sub title 1: this is {A} sub title 2"));
    }

    @Test
    void bibLaTexSplitsSubTitlesBasedOnDots() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This is a sub title 1...This is a sub title 2"));
    }

    @Test
    void bibLaTexSplitsSubTitleBasedOnSpecialCharacters() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This is; A sub title 1.... This is a sub title 2"));
    }

    @Test
    void bibLaTexAcceptsCapitalLetterAfterSpecialCharacter() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This!is!!A!Title??"));
    }

    @Test
    void bibLaTexAcceptsCapitalLetterNotOnlyAfterSpecialCharacter() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("This!is!!A!TitlE??"));
    }

}
