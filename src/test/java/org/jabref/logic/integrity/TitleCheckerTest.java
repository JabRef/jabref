package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TitleCheckerTest {
    @Nested
    class BibTexChecker {
        private TitleChecker checker;

        @BeforeEach
        public void setUp() {
            BibDatabaseContext databaseContext = new BibDatabaseContext();
            databaseContext.setMode(BibDatabaseMode.BIBTEX);
            checker = new TitleChecker(databaseContext);
        }

        @Test
        public void firstLetterAsOnlyCapitalLetterInSubTitle2() {
            String title = "This is a sub title 1: This is a sub title 2";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        public void noCapitalLetterInSubTitle2() {
            String title = "This is a sub title 1: this is a sub title 2";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        public void twoCapitalLettersInSubTitle2() {
            String title = "This is a sub title 1: This is A sub title 2";
            assertNotEquals(Optional.empty(), checker.checkValue(title));
            assertNotEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        public void middleLetterAsOnlyCapitalLetterInSubTitle2() {
            String title = "This is a sub title 1: this is A sub title 2";
            assertNotEquals(Optional.empty(), checker.checkValue(title));
            assertNotEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        public void twoCapitalLettersInSubTitle2WithCurlyBrackets() {
            String title = "This is a sub title 1: This is {A} sub title 2";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        public void middleLetterAsOnlyCapitalLetterInSubTitle2WithCurlyBrackets() {
            String title = "This is a sub title 1: this is {A} sub title 2";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        public void firstLetterAsOnlyCapitalLetterInSubTitle2AfterContinuousDelimiters() {
            String title = "This is a sub title 1...This is a sub title 2";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        public void middleLetterAsOnlyCapitalLetterInSubTitle2AfterContinuousDelimiters() {
            String title = "This is a sub title 1... this is a sub Title 2";
            assertNotEquals(Optional.empty(), checker.checkValue(title));
            assertNotEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        public void firstLetterAsOnlyCapitalLetterInEverySubTitleWithContinuousDelimiters() {
            String title = "This is; A sub title 1.... This is a sub title 2";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        public void firstLetterAsOnlyCapitalLetterInEverySubTitleWithRandomDelimiters() {
            String title = "This!is!!A!Title??";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        public void moreThanOneCapitalLetterInSubTitleWithoutCurlyBrackets() {
            String title = "This!is!!A!TitlE??";
            assertNotEquals(Optional.empty(), checker.checkValue(title));
            assertNotEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexAcceptsTitleWithOnlyFirstCapitalLetter() {
            String title = "This is a title";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexDoesNotAcceptCapitalLettersInsideTitle() {
            String title = "This is a Title";
            assertNotEquals(Optional.empty(), checker.checkValue(title));
            assertNotEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexRemovesCapitalLetterInsideTitle() {
            String title = "This is a {T}itle";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexRemovesEverythingInBracketsAndAcceptsNoTitleInput() {
            String title = "{This is a Title}";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexRemovesEverythingInBrackets() {
            String title = "This is a {Title}";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexAcceptsTitleWithLowercaseFirstLetter() {
            String title = "{C}urrent {C}hronicle";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexAcceptsSubTitlesWithOnlyFirstCapitalLetter() {
            String title = "This is a sub title 1: This is a sub title 2";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexAcceptsSubTitleWithLowercaseFirstLetter() {
            String title = "This is a sub title 1: this is a sub title 2";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexDoesNotAcceptCapitalLettersInsideSubTitle() {
            String title = "This is a sub title 1: This is A sub title 2";
            assertNotEquals(Optional.empty(), checker.checkValue(title));
            assertNotEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexRemovesCapitalLetterInsideSubTitle() {
            String title = "This is a sub title 1: this is {A} sub title 2";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexSplitsSubTitlesBasedOnDots() {
            String title = "This is a sub title 1...This is a sub title 2";
            assertEquals(Optional.empty(), checker.checkValue(title));
        }

        @Test
        void bibTexSplitsSubTitleBasedOnSpecialCharacters() {
            String title = "This is; A sub title 1.... This is a sub title 2";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexAcceptsCapitalLetterAfterSpecialCharacter() {
            String title = "This!is!!A!Title??";
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexAcceptsCapitalLetterOnlyAfterSpecialCharacter() {
            String title = "This!is!!A!TitlE??";
            assertNotEquals(Optional.empty(), checker.checkValue(title));
            assertNotEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @Test
        void bibTexAcceptsOriginalAndTranslatedTitle() {
            String title = "This is a title [This is translated title]";
            assertEquals(Optional.empty(), checker.checkValue(title));
        }

        @Test
        void bibTexNotAcceptsLeadingTranslatedTitleWithOriginal() {
            String title = "[This is translated title] This is a title";
            assertNotEquals(Optional.empty(), checker.checkValue(title));
        }

        private String translatedTitle(String title) {
            return "[%s]".formatted(title);
        }
    }

    @Nested
    class BibLaTexChecker {
        private TitleChecker checkerBiblatex;

        @BeforeEach
        public void setUp() {
            BibDatabaseContext databaseBiblatex = new BibDatabaseContext();
            databaseBiblatex.setMode(BibDatabaseMode.BIBLATEX);
            checkerBiblatex = new TitleChecker(databaseBiblatex);
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
}
