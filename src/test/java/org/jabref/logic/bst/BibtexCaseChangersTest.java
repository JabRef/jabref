package org.jabref.logic.bst;

import java.util.stream.Stream;

import org.jabref.logic.bst.BibtexCaseChanger.FORMAT_MODE;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibtexCaseChangersTest {

    @ParameterizedTest
    @MethodSource("provideStringsForTitleLowers")
    public void testChangeCaseTitleLowers(String expected, String toBeFormatted) {
        assertEquals(expected, BibtexCaseChanger.changeCase(toBeFormatted, FORMAT_MODE.TITLE_LOWERS));
    }

    private static Stream<Arguments> provideStringsForTitleLowers() {
        return Stream.of(
                Arguments.of("i", "i"),
                Arguments.of("0i~ ", "0I~ "),
                Arguments.of("Hi hi ", "Hi Hi "),
                Arguments.of("{\\oe}", "{\\oe}"),
                Arguments.of("Hi {\\oe   }hi ", "Hi {\\oe   }Hi "),
                Arguments.of("Jonathan meyer and charles louis xavier joseph de la vall{\\'e}e poussin", "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin"),
                Arguments.of("{\\'{E}}douard masterly", "{\\'{E}}douard Masterly"),
                Arguments.of("Ulrich {\\\"{u}}nderwood and ned {\\~n}et and paul {\\={p}}ot", "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot"),
                Arguments.of("An {$O(n \\log n / \\! \\log\\log n)$} sorting algorithm", "An {$O(n \\log n / \\! \\log\\log n)$} Sorting Algorithm"),
                Arguments.of("On notions of information transfer in {VLSI} circuits", "On Notions of Information Transfer in {VLSI} Circuits"),

                Arguments.of("hallo", "hallo"),
                Arguments.of("Hallo", "HAllo"),
                Arguments.of("Hallo world", "HAllo World"),
                Arguments.of("Hallo world. how", "HAllo WORLD. HOW"),
                Arguments.of("Hallo {WORLD}. how", "HAllo {WORLD}. HOW"),
                Arguments.of("Hallo {\\world}. how", "HAllo {\\WORLD}. HOW"),

                // testSpecialCharacters
                Arguments.of("Hallo world: How", "HAllo WORLD: HOW"),
                Arguments.of("Hallo world! how", "HAllo WORLD! HOW"),
                Arguments.of("Hallo world? how", "HAllo WORLD? HOW"),
                Arguments.of("Hallo world. how", "HAllo WORLD. HOW"),
                Arguments.of("Hallo world, how", "HAllo WORLD, HOW"),
                Arguments.of("Hallo world; how", "HAllo WORLD; HOW"),
                Arguments.of("Hallo world- how", "HAllo WORLD- HOW"),

                // testSpecialBracketPlacement
                Arguments.of("this i{S REALLY CraZy ST}uff", "tHIS I{S REALLY CraZy ST}UfF"),
                Arguments.of("this i{S R{\\'E}ALLY CraZy ST}uff", "tHIS I{S R{\\'E}ALLY CraZy ST}UfF"),
                Arguments.of("this is r{\\'e}ally crazy stuff", "tHIS IS R{\\'E}ALLY CraZy STUfF")
        );
    }

    @ParameterizedTest
    @MethodSource("provideStringsForAllLowers")
    public void testChangeCaseAllLowers(String expected, String toBeFormatted) {
        assertEquals(expected, BibtexCaseChanger.changeCase(toBeFormatted, FORMAT_MODE.ALL_LOWERS));
    }

    private static Stream<Arguments> provideStringsForAllLowers() {
        return Stream.of(
                Arguments.of("i", "i"),
                Arguments.of("0i~ ", "0I~ "),
                Arguments.of("hi hi ", "Hi Hi "),
                Arguments.of("{\\oe}", "{\\oe}"),
                Arguments.of("hi {\\oe   }hi ", "Hi {\\oe   }Hi "),
                Arguments.of("jonathan meyer and charles louis xavier joseph de la vall{\\'e}e poussin", "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin"),
                Arguments.of("{\\'e}", "{\\'e}"),
                Arguments.of("{\\'{e}}douard masterly", "{\\'{E}}douard Masterly"),
                Arguments.of("ulrich {\\\"{u}}nderwood and ned {\\~n}et and paul {\\={p}}ot", "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot"),
                Arguments.of("an {$O(n \\log n / \\! \\log\\log n)$} sorting algorithm", "An {$O(n \\log n / \\! \\log\\log n)$} Sorting Algorithm"),

                Arguments.of("hallo", "hallo"),
                Arguments.of("hallo", "HAllo"),
                Arguments.of("hallo world", "HAllo World"),
                Arguments.of("hallo world. how", "HAllo WORLD. HOW"),
                Arguments.of("hallo {worLD}. how", "HAllo {worLD}. HOW"),
                Arguments.of("hallo {\\world}. how", "HAllo {\\WORLD}. HOW"),

                // testSpecialBracketPlacement
                Arguments.of("an {$O(n \\log n)$} sorting algorithm", "An {$O(n \\log n)$} Sorting Algorithm")
        );
    }

    @ParameterizedTest
    @MethodSource("provideStringsForAllUppers")
    public void testChangeCaseAllUppers(String expected, String toBeFormatted) {
        assertEquals(expected, BibtexCaseChanger.changeCase(toBeFormatted, FORMAT_MODE.ALL_UPPERS));
    }

    private static Stream<Arguments> provideStringsForAllUppers() {
        return Stream.of(
                Arguments.of("I", "i"),
                Arguments.of("0I~ ", "0I~ "),
                Arguments.of("HI HI ", "Hi Hi "),
                Arguments.of("{\\OE}", "{\\oe}"),
                Arguments.of("HI {\\OE   }HI ", "Hi {\\oe   }Hi "),
                Arguments.of("JONATHAN MEYER AND CHARLES LOUIS XAVIER JOSEPH DE LA VALL{\\'E}E POUSSIN", "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin"),
                Arguments.of("{\\'E}", "{\\'e}"),
                Arguments.of("{\\'{E}}DOUARD MASTERLY", "{\\'{E}}douard Masterly"),
                Arguments.of("ULRICH {\\\"{U}}NDERWOOD AND NED {\\~N}ET AND PAUL {\\={P}}OT", "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot"),
                Arguments.of("AN {$O(n \\log n / \\! \\log\\log n)$} SORTING ALGORITHM", "An {$O(n \\log n / \\! \\log\\log n)$} Sorting Algorithm"),

                Arguments.of("HALLO", "hallo"),
                Arguments.of("HALLO", "HAllo"),
                Arguments.of("HALLO WORLD", "HAllo World"),
                Arguments.of("HALLO WORLD. HOW", "HAllo World. How"),
                Arguments.of("HALLO {worLD}. HOW", "HAllo {worLD}. how"),
                Arguments.of("HALLO {\\WORLD}. HOW", "HAllo {\\woRld}. hoW"),

                // testSpecialBracketPlacement
                Arguments.of("AN {$O(n \\log n)$} SORTING ALGORITHM", "An {$O(n \\log n)$} Sorting Algorithm")
        );
    }

    @ParameterizedTest
    @MethodSource("provideTitleCaseAllLowers")
    public void testTitleCaseAllLowers(String expected, String toBeFormatted) {
        assertEquals(expected, BibtexCaseChanger.changeCase(toBeFormatted, FORMAT_MODE.ALL_LOWERS));
    }

    private static Stream<Arguments> provideTitleCaseAllLowers() {
        return Stream.of(
                // CaseChangers.TITLE is good at keeping some words lower case
                // Here some modified test cases to show that escaping with BibtexCaseChanger also works
                // Examples taken from https://github.com/JabRef/jabref/pull/176#issuecomment-142723792
                Arguments.of("this is a simple example {TITLE}", "This is a simple example {TITLE}"),
                Arguments.of("this {IS} another simple example tit{LE}", "This {IS} another simple example tit{LE}"),
                Arguments.of("{What ABOUT thIS} one?", "{What ABOUT thIS} one?"),
                Arguments.of("{And {thIS} might {a{lso}} be possible}", "{And {thIS} might {a{lso}} be possible}")
        );
    }

    @Disabled
    @Test
    public void testTitleCaseAllUppers() {
        /* the real test would look like as follows. Also from the comment of issue 176, order reversed as the "should be" comes first */
        // assertCaseChangerTitleUppers("This is a Simple Example {TITLE}", "This is a simple example {TITLE}");
        // assertCaseChangerTitleUppers("This {IS} Another Simple Example Tit{LE}", "This {IS} another simple example tit{LE}");
        // assertCaseChangerTitleUppers("{What ABOUT thIS} one?", "{What ABOUT thIS} one?");
        // assertCaseChangerTitleUppers("{And {thIS} might {a{lso}} be possible}", "{And {thIS} might {a{lso}} be possible}")
    }
}
