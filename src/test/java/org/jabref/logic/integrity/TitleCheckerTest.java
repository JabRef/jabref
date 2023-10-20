package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

        @ParameterizedTest(name = "{index}. Title: \"{1}\" {0}")
        @MethodSource("validTitle")
        void titleShouldNotRaiseWarning(String message, String title) {
            assertEquals(Optional.empty(), checker.checkValue(title));
            assertEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        @ParameterizedTest(name = "{index}. Title: \"{1}\" {0}")
        @MethodSource("invalidTitle")
        void titleShouldRaiseWarning(String message, String title) {
            assertNotEquals(Optional.empty(), checker.checkValue(title));
            assertNotEquals(Optional.empty(), checker.checkValue(translatedTitle(title)));
        }

        static Stream<Arguments> validTitle() {
            return Stream.of(
                    Arguments.of("firstLetterAsOnlyCapitalLetterInSubTitle2 ", "This is a sub title 1: This is a sub title 2"),
                    Arguments.of("noCapitalLetterInSubTitle2 ", "This is a sub title 1: this is a sub title 2"),
                    Arguments.of("twoCapitalLettersInSubTitle2WithCurlyBrackets ", "This is a sub title 1: This is {A} sub title 2"),
                    Arguments.of("middleLetterAsOnlyCapitalLetterInSubTitle2WithCurlyBrackets ", "This is a sub title 1: this is {A} sub title 2"),
                    Arguments.of("firstLetterAsOnlyCapitalLetterInSubTitle2AfterContinuousDelimiters ", "This is a sub title 1...This is a sub title 2"),
                    Arguments.of("firstLetterAsOnlyCapitalLetterInEverySubTitleWithContinuousDelimiters ", "This is; A sub title 1.... This is a sub title 2"),
                    Arguments.of("firstLetterAsOnlyCapitalLetterInEverySubTitleWithRandomDelimiters ", "This!is!!A!Title??"),
                    Arguments.of("bibTexAcceptsTitleWithOnlyFirstCapitalLetter ", "This is a title"),
                    Arguments.of("bibTexRemovesCapitalLetterInsideTitle ", "This is a {T}itle"),
                    Arguments.of("bibTexRemovesEverythingInBracketsAndAcceptsNoTitleInput ", "{This is a Title}"),
                    Arguments.of("bibTexRemovesEverythingInBrackets ", "This is a {Title}"),
                    Arguments.of("bibTexAcceptsTitleWithLowercaseFirstLetter ", "{C}urrent {C}hronicle"),
                    Arguments.of("bibTexAcceptsOriginalAndTranslatedTitle ", "This is a title [This is translated title]")
            );
        }

        static Stream<Arguments> invalidTitle() {
            return Stream.of(
                    Arguments.of("twoCapitalLettersInSubTitle2 ", "This is a sub title 1: This is A sub title 2"),
                    Arguments.of("middleLetterAsOnlyCapitalLetterInSubTitle2 ", "This is a sub title 1: this is A sub title 2"),
                    Arguments.of("middleLetterAsOnlyCapitalLetterInSubTitle2AfterContinuousDelimiters ", "This is a sub title 1... this is a sub Title 2"),
                    Arguments.of("moreThanOneCapitalLetterInSubTitleWithoutCurlyBrackets ", "This!is!!A!TitlE??"),
                    Arguments.of("bibTexDoesNotAcceptCapitalLettersInsideTitle ", "This is a Title"),
                    Arguments.of("bibTexDoesNotAcceptsLeadingTranslatedTitleWithOriginal ", "[This is translated title] This is a title")
            );
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

        @ParameterizedTest(name = "{index}. Title: \"{1}\" {0}")
        @MethodSource("validTitle")
        void titleShouldNotRaiseWarning(String message, String title) {
            assertEquals(Optional.empty(), checkerBiblatex.checkValue(title));
            assertEquals(Optional.empty(), checkerBiblatex.checkValue(translatedTitle(title)));
        }

        static Stream<Arguments> validTitle() {
            return Stream.of(
                    Arguments.of("bibLaTexAcceptsTitleWithOnlyFirstCapitalLetter", "This is a title"),
                    Arguments.of("bibLaTexAcceptsCapitalLettersInsideTitle", "This is a Title"),
                    Arguments.of("bibLaTexRemovesCapitalLetterInsideTitle", "This is a {T}itle"),
                    Arguments.of("bibLaTexRemovesEverythingInBracketsAndAcceptsNoTitleInput", "{This is a Title}"),
                    Arguments.of("bibLaTexRemovesEverythingInBrackets", "This is a {Title}"),
                    Arguments.of("bibLaTexAcceptsTitleWithLowercaseFirstLetter", "{C}urrent {C}hronicle"),
                    Arguments.of("bibLaTexAcceptsSubTitlesWithOnlyFirstCapitalLetter", "This is a sub title 1: This is a sub title 2"),
                    Arguments.of("bibLaTexAcceptsSubTitleWithLowercaseFirstLetter", "This is a sub title 1: this is a sub title 2"),
                    Arguments.of("bibLaTexAcceptsCapitalLettersInsideSubTitle", "This is a sub title 1: This is A sub title 2"),
                    Arguments.of("bibLaTexRemovesCapitalLetterInsideSubTitle", "This is a sub title 1: this is {A} sub title 2"),
                    Arguments.of("bibLaTexSplitsSubTitlesBasedOnDots", "This is a sub title 1...This is a sub title 2"),
                    Arguments.of("bibLaTexSplitsSubTitleBasedOnSpecialCharacters", "This is; A sub title 1.... This is a sub title 2"),
                    Arguments.of("bibLaTexAcceptsCapitalLetterAfterSpecialCharacter", "This!is!!A!Title??"),
                    Arguments.of("bibLaTexAcceptsCapitalLetterNotOnlyAfterSpecialCharacter", "This!is!!A!TitlE??")
            );
        }
    }

    private String translatedTitle(String title) {
        return "[%s]".formatted(title);
    }
}
