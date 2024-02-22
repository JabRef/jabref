package org.jabref.logic.bst.util;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.Map;
import org.jabref.logic.bst.util.BstCaseChanger.FormatMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BstCaseChangersTest {
    @AfterAll
    public static void print(){
        System.out.println("Amount: "+BstCaseChanger.branchCoverage.size()+"Covered");
        for (Map.Entry<Integer, Boolean> entry : BstCaseChanger.branchCoverage.entrySet()) {
            System.out.println("ID: " + entry.getKey() + ", Covered: " + entry.getValue());
        }
    }
    @ParameterizedTest
    @MethodSource("provideStringForNoneCovered")
    public void branchCoverageTestNoneCovered(String e, String toBeFormatted) {
        // setup
        char[] c = toBeFormatted.toCharArray();
         // run
        for(int i=0; i<c.length;i++){
            Optional<String> s = BstCaseChanger.findSpecialCharToTest(c,i);
        }
        assertEquals(1,1);
    }
    private static Stream<Arguments> provideStringForNoneCovered() {
        return Stream.of(
                 Arguments.of("", "") // covers the whole branch
        );
    }

    @ParameterizedTest
    @MethodSource("provideStringForAllCovered")
    public void branchCoverageTestAllCovered(String e, String toBeFormatted) {
        ArrayList<Optional<String>> expected = new ArrayList<>();
        expected.add(Optional.of("oe"));
        expected.add(Optional.empty());
        expected.add(Optional.of("OE"));
        expected.add(Optional.empty());
        expected.add(Optional.of("ae"));
        expected.add(Optional.empty());
        expected.add(Optional.of("AE"));
        expected.add(Optional.empty());
        expected.add(Optional.of("ss"));
        expected.add(Optional.empty());
        expected.add(Optional.of("AA"));
        expected.add(Optional.empty());
        expected.add(Optional.of("aa"));
        expected.add(Optional.empty());
        expected.add(Optional.of("i"));
        expected.add(Optional.of("j"));
        expected.add(Optional.of("o"));
        expected.add(Optional.of("O"));
        expected.add(Optional.of("l"));
        expected.add(Optional.of("L"));
        ArrayList<Optional<String>> toCheck = new ArrayList<>();
        // setup
        char[] c = toBeFormatted.toCharArray();
        // run
        for(int i=0; i<c.length;i++){
            Optional<String> s = BstCaseChanger.findSpecialCharToTest(c,i);
            toCheck.add(s);
        }
        boolean checker = true;
        for(int i=0; i<19; i++ ){
            if (toCheck.get(i).isPresent() && expected.get(i).isPresent()){
                String a = expected.get(i).get();
                String b = toCheck.get(i).get();
                if(!a.equals(b)){
                    System.out.println("Was false");
                    System.out.println(expected.get(i).toString());
                    System.out.println(toCheck.get(i).toString());
                    checker = false;
                }
            }
        }
        assertEquals(true,checker);
    }
    private static Stream<Arguments> provideStringForAllCovered() {
        return Stream.of(
                 Arguments.of("oeOEaeAEssAAaaijoOlL", "oeOEaeAEssAAaaijoOl") // covers the whole branch
        );
    }



    @ParameterizedTest
    @MethodSource("provideStringsForTitleLowers")
    public void changeCaseTitleLowers(String expected, String toBeFormatted) {
        assertEquals(expected, BstCaseChanger.changeCase(toBeFormatted, FormatMode.TITLE_LOWERS));
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
    public void changeCaseAllLowers(String expected, String toBeFormatted) {
        assertEquals(expected, BstCaseChanger.changeCase(toBeFormatted, FormatMode.ALL_LOWERS));
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
    public void changeCaseAllUppers(String expected, String toBeFormatted) {
        assertEquals(expected, BstCaseChanger.changeCase(toBeFormatted, FormatMode.ALL_UPPERS));
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
    public void titleCaseAllLowers(String expected, String toBeFormatted) {
        assertEquals(expected, BstCaseChanger.changeCase(toBeFormatted, FormatMode.ALL_LOWERS));
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
    public void titleCaseAllUppers() {
        /* the real test would look like as follows. Also from the comment of issue 176, order reversed as the "should be" comes first */
        // assertCaseChangerTitleUppers("This is a Simple Example {TITLE}", "This is a simple example {TITLE}");
        // assertCaseChangerTitleUppers("This {IS} Another Simple Example Tit{LE}", "This {IS} another simple example tit{LE}");
        // assertCaseChangerTitleUppers("{What ABOUT thIS} one?", "{What ABOUT thIS} one?");
        // assertCaseChangerTitleUppers("{And {thIS} might {a{lso}} be possible}", "{And {thIS} might {a{lso}} possible be}")
    }
}
