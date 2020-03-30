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

    @BeforeEach
    public void setUp() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.setMode(BibDatabaseMode.BIBTEX);
        checker = new TitleChecker(databaseContext);
    }

    @Test
    public void FirstLetterAsOnlyCapitalLetterInSubTitle2() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1: This is a sub title 2"));
    }

    @Test
    public void NoCapitalLetterInSubTitle2() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1: this is a sub title 2"));
    }

    @Test
    public void TwoCapitalLettersInSubTitle2() {
        assertNotEquals(Optional.empty(), checker.checkValue("This is a sub title 1: This is A sub title 2"));
    }

    @Test
    public void MiddleLetterAsOnlyCapitalLetterInSubTitle2() {
        assertNotEquals(Optional.empty(), checker.checkValue("This is a sub title 1: this is A sub title 2"));
    }

    @Test
    public void TwoCapitalLettersInSubTitle2WithCurlyBrackets() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1: This is {A} sub title 2"));
    }

    @Test
    public void MiddleLetterAsOnlyCapitalLetterInSubTitle2WithCurlyBrackets() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1: this is {A} sub title 2"));
    }

    @Test
    public void FirstLetterAsOnlyCapitalLetterInSubTitle2AfterContinuousDelimiters() {
        assertEquals(Optional.empty(), checker.checkValue("This is a sub title 1...This is a sub title 2"));
    }

    @Test
    public void MiddleLetterAsOnlyCapitalLetterInSubTitle2AfterContinuousDelimiters() {
        assertNotEquals(Optional.empty(), checker.checkValue("This is a sub title 1... this is a sub Title 2"));
    }

    @Test
    public void FirstLetterAsOnlyCapitalLetterInEverySubTitleWithContinuousDelimiters() {
        assertEquals(Optional.empty(), checker.checkValue("This is; A sub title 1.... This is a sub title 2"));
    }

    @Test
    public void FirstLetterAsOnlyCapitalLetterInEverySubTitleWithRandomDelimiters() {
        assertEquals(Optional.empty(), checker.checkValue("This!is!!A!Title??"));
    }

    @Test
    public void MoreThanOneCapitalLetterInSubTitleWithoutCurlyBrackets() {
        assertNotEquals(Optional.empty(), checker.checkValue("This!is!!A!TitlE??"));
    }
}
