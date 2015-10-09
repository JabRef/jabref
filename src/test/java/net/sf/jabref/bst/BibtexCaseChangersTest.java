package net.sf.jabref.bst;

import org.junit.Assert;
import org.junit.Test;

import net.sf.jabref.bst.BibtexCaseChanger.FORMAT_MODE;

public class BibtexCaseChangersTest {

    @Test
    public void testChangeCase() {

        assertCaseChangerTitleLowers("i", "i");
        assertCaseChangerAllLowers("i", "i");
        assertCaseChangerAllUppers("I", "i");
        assertCaseChangerTitleLowers("0i~ ", "0I~ ");
        assertCaseChangerAllLowers("0i~ ", "0I~ ");
        assertCaseChangerAllUppers("0I~ ", "0I~ ");
        assertCaseChangerTitleLowers("Hi hi ", "Hi Hi ");
        assertCaseChangerAllLowers("hi hi ", "Hi Hi ");
        assertCaseChangerAllUppers("HI HI ", "Hi Hi ");
        assertCaseChangerTitleLowers("{\\oe}", "{\\oe}");
        assertCaseChangerAllLowers("{\\oe}", "{\\oe}");
        assertCaseChangerAllUppers("{\\OE}", "{\\oe}");
        assertCaseChangerTitleLowers("Hi {\\oe   }hi ", "Hi {\\oe   }Hi ");
        assertCaseChangerAllLowers("hi {\\oe   }hi ", "Hi {\\oe   }Hi ");
        assertCaseChangerAllUppers("HI {\\OE   }HI ", "Hi {\\oe   }Hi ");
        assertCaseChangerTitleLowers(
                "Jonathan meyer and charles louis xavier joseph de la vall{\\'e}e poussin",
                "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
        assertCaseChangerAllLowers(
                "jonathan meyer and charles louis xavier joseph de la vall{\\'e}e poussin",
                "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
        assertCaseChangerAllUppers(
                "JONATHAN MEYER AND CHARLES LOUIS XAVIER JOSEPH DE LA VALL{\\'E}E POUSSIN",
                "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
        assertCaseChangerTitleLowers("{\\'e}", "{\\'e}");
        assertCaseChangerAllLowers("{\\'e}", "{\\'e}");
        assertCaseChangerAllUppers("{\\'E}", "{\\'e}");
        assertCaseChangerTitleLowers("{\\'{E}}douard masterly", "{\\'{E}}douard Masterly");
        assertCaseChangerAllLowers("{\\'{e}}douard masterly", "{\\'{E}}douard Masterly");
        assertCaseChangerAllUppers("{\\'{E}}DOUARD MASTERLY", "{\\'{E}}douard Masterly");
        assertCaseChangerTitleLowers("Ulrich {\\\"{u}}nderwood and ned {\\~n}et and paul {\\={p}}ot",
                "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
        assertCaseChangerAllLowers("ulrich {\\\"{u}}nderwood and ned {\\~n}et and paul {\\={p}}ot",
                "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
        assertCaseChangerAllUppers("ULRICH {\\\"{U}}NDERWOOD AND NED {\\~N}ET AND PAUL {\\={P}}OT",
                "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
        assertCaseChangerTitleLowers("An {$O(n \\log n / \\! \\log\\log n)$} sorting algorithm",
                "An {$O(n \\log n / \\! \\log\\log n)$} Sorting Algorithm");
        assertCaseChangerAllLowers("an {$O(n \\log n / \\! \\log\\log n)$} sorting algorithm",
                "An {$O(n \\log n / \\! \\log\\log n)$} Sorting Algorithm");
        assertCaseChangerAllUppers("AN {$O(n \\log n / \\! \\log\\log n)$} SORTING ALGORITHM",
                "An {$O(n \\log n / \\! \\log\\log n)$} Sorting Algorithm");

        assertCaseChangerTitleLowers("hallo", "hallo");
        assertCaseChangerTitleLowers("Hallo", "HAllo");
        assertCaseChangerTitleLowers("Hallo world", "HAllo World");
        assertCaseChangerTitleLowers("Hallo world. how", "HAllo WORLD. HOW");
        assertCaseChangerTitleLowers("Hallo {WORLD}. how", "HAllo {WORLD}. HOW");
        assertCaseChangerTitleLowers("Hallo {\\world}. how", "HAllo {\\WORLD}. HOW");

        assertCaseChangerAllLowers("hallo", "hallo");
        assertCaseChangerAllLowers("hallo", "HAllo");
        assertCaseChangerAllLowers("hallo world", "HAllo World");
        assertCaseChangerAllLowers("hallo world. how", "HAllo WORLD. HOW");
        assertCaseChangerAllLowers("hallo {worLD}. how", "HAllo {worLD}. HOW");
        assertCaseChangerAllLowers("hallo {\\world}. how", "HAllo {\\WORLD}. HOW");

        assertCaseChangerAllUppers("HALLO", "hallo");
        assertCaseChangerAllUppers("HALLO", "HAllo");
        assertCaseChangerAllUppers("HALLO WORLD", "HAllo World");
        assertCaseChangerAllUppers("HALLO WORLD. HOW", "HAllo World. How");
        assertCaseChangerAllUppers("HALLO {worLD}. HOW", "HAllo {worLD}. how");
        assertCaseChangerAllUppers("HALLO {\\WORLD}. HOW", "HAllo {\\woRld}. hoW");

        assertCaseChangerTitleLowers("On notions of information transfer in {VLSI} circuits",
                "On Notions of Information Transfer in {VLSI} Circuits");

    }

    @Test
    public void testColon() {
        assertCaseChangerTitleLowers("Hallo world: How", "HAllo WORLD: HOW");
        assertCaseChangerTitleLowers("Hallo world! how", "HAllo WORLD! HOW");
        assertCaseChangerTitleLowers("Hallo world? how", "HAllo WORLD? HOW");
        assertCaseChangerTitleLowers("Hallo world. how", "HAllo WORLD. HOW");
        assertCaseChangerTitleLowers("Hallo world, how", "HAllo WORLD, HOW");
        assertCaseChangerTitleLowers("Hallo world; how", "HAllo WORLD; HOW");
        assertCaseChangerTitleLowers("Hallo world- how", "HAllo WORLD- HOW");
    }

    @Test
    public void testSpecialBracketPlacement() {
        // area between brackets spanning multiple words
        assertCaseChangerAllLowers("this i{S REALLY CraZy ST}uff", "tHIS I{S REALLY CraZy ST}UfF");
        assertCaseChangerAllLowers("this i{S R{\\'E}ALLY CraZy ST}uff", "tHIS I{S R{\\'E}ALLY CraZy ST}UfF");

        // real use case: Formulas
        assertCaseChangerAllUppers("AN {$O(n \\log n)$} SORTING ALGORITHM", "An {$O(n \\log n)$} Sorting Algorithm");

        // only one special character, no strange bracket placement
        assertCaseChangerAllLowers("this is r{\\'e}ally crazy stuff", "tHIS IS R{\\'E}ALLY CraZy STUfF");
    }

    @Test
    public void testTitleCase() {
        // CaseChangers.TITLE is good at keeping some words lower case
        // Here some modified test cases to show that escaping with BibtexCaseChanger also works
        // Examples taken from https://github.com/JabRef/jabref/pull/176#issuecomment-142723792
        assertCaseChangerAllLowers("this is a simple example {TITLE}", "This is a simple example {TITLE}");
        assertCaseChangerAllLowers("this {IS} another simple example tit{LE}", "This {IS} another simple example tit{LE}");
        assertCaseChangerAllLowers("{What ABOUT thIS} one?", "{What ABOUT thIS} one?");
        assertCaseChangerAllLowers("{And {thIS} might {a{lso}} be possible}", "{And {thIS} might {a{lso}} be possible}");

        /* the real test would look like as follows. Also from the comment of issue 176, order reversed as the "should be" comes first */
        // assertCaseChangerTitleUppers("This is a Simple Example {TITLE}", "This is a simple example {TITLE}");
        // assertCaseChangerTitleUppers("This {IS} Another Simple Example Tit{LE}", "This {IS} another simple example tit{LE}");
        // assertCaseChangerTitleUppers("{What ABOUT thIS} one?", "{What ABOUT thIS} one?");
        // assertCaseChangerTitleUppers("{And {thIS} might {a{lso}} be possible}", "{And {thIS} might {a{lso}} be possible}");
    }

    private void assertCaseChangerTitleLowers(final String string, final String string2) {
        Assert.assertEquals(string, BibtexCaseChanger.changeCase(string2, FORMAT_MODE.TITLE_LOWERS));

    }

    private void assertCaseChangerAllLowers(final String string, final String string2) {
        Assert.assertEquals(string, BibtexCaseChanger.changeCase(string2, FORMAT_MODE.ALL_LOWERS));
    }

    private void assertCaseChangerAllUppers(final String string, final String string2) {
        Assert.assertEquals(string, BibtexCaseChanger.changeCase(string2, FORMAT_MODE.ALL_UPPERS));
    }
}
