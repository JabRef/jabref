package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.After;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RTFCharsTest {
    private LayoutFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RTFChars();
    }

    @After
    public void tearDown() {
        formatter = null;
    }

    @Test
    public void testBasicFormat() {
        assertEquals("", formatter.format(""));

        assertEquals("hallo", formatter.format("hallo"));

        assertEquals("R\\u233eflexions sur le timing de la quantit\\u233e",
                formatter.format("Réflexions sur le timing de la quantité"));

        assertEquals("h\\'e1llo", formatter.format("h\\'allo"));
        assertEquals("h\\'e1llo", formatter.format("h\\'allo"));
    }

    @Test
    public void testLaTeXHighlighting() {
        assertEquals("{\\i hallo}", formatter.format("\\emph{hallo}"));
        assertEquals("{\\i hallo}", formatter.format("{\\emph hallo}"));
        assertEquals("An article title with {\\i a book title} emphasized", formatter.format("An article title with \\emph{a book title} emphasized"));

        assertEquals("{\\i hallo}", formatter.format("\\textit{hallo}"));
        assertEquals("{\\i hallo}", formatter.format("{\\textit hallo}"));

        assertEquals("{\\b hallo}", formatter.format("\\textbf{hallo}"));
        assertEquals("{\\b hallo}", formatter.format("{\\textbf hallo}"));
    }

    @Test
    public void testComplicated() {
        assertEquals("R\\u233eflexions sur le timing de la quantit\\u233e {\\u230ae} should be \\u230ae",
                formatter.format("Réflexions sur le timing de la quantité {\\ae} should be æ"));
    }

    @Test
    public void  testComplicated2() {
        assertEquals("h\\'e1ll{\\u339oe}", formatter.format("h\\'all{\\oe}"));
    }

    @Test
    public void testComplicated3() {
        assertEquals("Le c\\u339oeur d\\u233e\\u231cu mais l'\\u226ame plut\\u244ot na\\u239ive, Lou\\u255ys r" +
                "\\u234eva de crapa\\u252?ter en cano\\u235e au del\\u224a des \\u238iles, pr\\u232es du m\\u228alstr" +
                "\\u246om o\\u249u br\\u251ulent les nov\\u230ae.", formatter.format("Le cœur déçu mais l'âme plutôt " +
                "naïve, Louÿs rêva de crapaüter en canoë au delà des îles, près du mälström où brûlent les novæ."));
    }

    @Test
    public void testComplicated4() {
        assertEquals("l'\\u238ile exigu\\u235e\n" +
                "  O\\u249u l'ob\\u232ese jury m\\u251ur\n" +
                "  F\\u234ete l'ha\\u239i volap\\u252?k,\n" +
                "  \\u194Ane ex a\\u233equo au whist,\n" +
                "  \\u212Otez ce v\\u339oeu d\\u233e\\u231cu.", formatter.format("l'île exiguë\n" +
                "  Où l'obèse jury mûr\n" +
                "  Fête l'haï volapük,\n" +
                "  Âne ex aéquo au whist,\n" +
                "  Ôtez ce vœu déçu."));
    }

    @Test
    public void testComplicated5() {
        assertEquals("\\u193Arv\\u237izt\\u369?r\\u337? t\\u252?k\\u246orf\\u250ur\\u243og\\u233ep",
                formatter.format("Árvíztűrő tükörfúrógép"));
    }

    @Test
    public void testComplicated6() {
        assertEquals("Pchn\\u261a\\u263c w t\\u281e \\u322l\\u243od\\u378z je\\u380za lub o\\u347sm skrzy\\u324n fig"
                ,formatter.format("Pchnąć w tę łódź jeża lub ośm skrzyń fig"));
    }

    @Test
    public void testSpecialCharacters() {
        assertEquals("\\'f3", formatter.format("\\'{o}")); // ó
        assertEquals("\\'f2", formatter.format("\\`{o}")); // ò
        assertEquals("\\'f4", formatter.format("\\^{o}")); // ô
        assertEquals("\\'f6", formatter.format("\\\"{o}")); // ö
        assertEquals("\\u245o", formatter.format("\\~{o}")); // õ
        assertEquals("\\u333o", formatter.format("\\={o}"));
        assertEquals("\\u335o", formatter.format("{\\uo}"));
        assertEquals("\\u231c", formatter.format("{\\cc}")); // ç
        assertEquals("{\\u339oe}", formatter.format("{\\oe}"));
        assertEquals("{\\u338OE}", formatter.format("{\\OE}"));
        assertEquals("{\\u230ae}", formatter.format("{\\ae}")); // æ
        assertEquals("{\\u198AE}", formatter.format("{\\AE}")); // Æ

        assertEquals("", formatter.format("\\.{o}")); // ???
        assertEquals("", formatter.format("\\vo")); // ???
        assertEquals("", formatter.format("\\Ha")); // ã // ???
        assertEquals("", formatter.format("\\too"));
        assertEquals("", formatter.format("\\do")); // ???
        assertEquals("", formatter.format("\\bo")); // ???
        assertEquals("\\u229a", formatter.format("{\\aa}")); // å
        assertEquals("\\u197A", formatter.format("{\\AA}")); // Å
        assertEquals("\\u248o", formatter.format("{\\o}")); // ø
        assertEquals("\\u216O", formatter.format("{\\O}")); // Ø
        assertEquals("\\u322l", formatter.format("{\\l}"));
        assertEquals("\\u321L", formatter.format("{\\L}"));
        assertEquals("\\u223ss", formatter.format("{\\ss}")); // ß
        assertEquals("\\u191?", formatter.format("\\`?")); // ¿
        assertEquals("\\u161!", formatter.format("\\`!")); // ¡

        assertEquals("", formatter.format("\\dag"));
        assertEquals("", formatter.format("\\ddag"));
        assertEquals("\\u167S", formatter.format("{\\S}")); // §
        assertEquals("\\u182P", formatter.format("{\\P}")); // ¶
        assertEquals("\\u169?", formatter.format("{\\copyright}")); // ©
        assertEquals("\\u163?", formatter.format("{\\pounds}")); // £
    }

    @Test
    public void testRTFCharacters(){
        assertEquals("\\'e0",formatter.format("\\`{a}"));
        assertEquals("\\'e8",formatter.format("\\`{e}"));
        assertEquals("\\'ec",formatter.format("\\`{i}"));
        assertEquals("\\'f2",formatter.format("\\`{o}"));
        assertEquals("\\'f9",formatter.format("\\`{u}"));

        assertEquals("\\'e1",formatter.format("\\'a"));
        assertEquals("\\'e9",formatter.format("\\'e"));
        assertEquals("\\'ed",formatter.format("\\'i"));
        assertEquals("\\'f3",formatter.format("\\'o"));
        assertEquals("\\'fa",formatter.format("\\'u"));

        assertEquals("\\'e2",formatter.format("\\^a"));
        assertEquals("\\'ea",formatter.format("\\^e"));
        assertEquals("\\'ee",formatter.format("\\^i"));
        assertEquals("\\'f4",formatter.format("\\^o"));
        assertEquals("\\'fa",formatter.format("\\^u"));

        assertEquals("\\'e4",formatter.format("\\\"a"));
        assertEquals("\\'eb",formatter.format("\\\"e"));
        assertEquals("\\'ef",formatter.format("\\\"i"));
        assertEquals("\\'f6",formatter.format("\\\"o"));
        assertEquals("\\u252u",formatter.format("\\\"u"));

        assertEquals("\\'f1",formatter.format("\\~n"));
    }

    @Test
    public void testRTFCharactersCapital() {
        assertEquals("\\'c0",formatter.format("\\`A"));
        assertEquals("\\'c8",formatter.format("\\`E"));
        assertEquals("\\'cc",formatter.format("\\`I"));
        assertEquals("\\'d2",formatter.format("\\`O"));
        assertEquals("\\'d9",formatter.format("\\`U"));

        assertEquals("\\'c1",formatter.format("\\'A"));
        assertEquals("\\'c9",formatter.format("\\'E"));
        assertEquals("\\'cd",formatter.format("\\'I"));
        assertEquals("\\'d3",formatter.format("\\'O"));
        assertEquals("\\'da",formatter.format("\\'U"));

        assertEquals("\\'c2",formatter.format("\\^A"));
        assertEquals("\\'ca",formatter.format("\\^E"));
        assertEquals("\\'ce",formatter.format("\\^I"));
        assertEquals("\\'d4",formatter.format("\\^O"));
        assertEquals("\\'db",formatter.format("\\^U"));

        assertEquals("\\'c4",formatter.format("\\\"A"));
        assertEquals("\\'cb",formatter.format("\\\"E"));
        assertEquals("\\'cf",formatter.format("\\\"I"));
        assertEquals("\\'d6",formatter.format("\\\"O"));
        assertEquals("\\'dc",formatter.format("\\\"U"));
    }

}
