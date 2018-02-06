package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RTFCharsTest {
    private LayoutFormatter formatter;

    @Before
    public void setUp() {
        formatter = new RTFChars();
    }

    @After
    public void tearDown() {
        formatter = null;
    }

    @Test
    public void testBasicFormat() {
        Assert.assertEquals("", formatter.format(""));

        Assert.assertEquals("hallo", formatter.format("hallo"));

        Assert.assertEquals("R\\u233eflexions sur le timing de la quantit\\u233e",
                formatter.format("Réflexions sur le timing de la quantité"));

        Assert.assertEquals("h\\'e1llo", formatter.format("h\\'allo"));
        Assert.assertEquals("h\\'e1llo", formatter.format("h\\'allo"));
    }

    @Test
    public void testLaTeXHighlighting() {
        Assert.assertEquals("{\\i hallo}", formatter.format("\\emph{hallo}"));
        Assert.assertEquals("{\\i hallo}", formatter.format("{\\emph hallo}"));
        Assert.assertEquals("An article title with {\\i a book title} emphasized", formatter.format("An article title with \\emph{a book title} emphasized"));

        Assert.assertEquals("{\\i hallo}", formatter.format("\\textit{hallo}"));
        Assert.assertEquals("{\\i hallo}", formatter.format("{\\textit hallo}"));

        Assert.assertEquals("{\\b hallo}", formatter.format("\\textbf{hallo}"));
        Assert.assertEquals("{\\b hallo}", formatter.format("{\\textbf hallo}"));
    }

    @Test
    public void testComplicated() {
        Assert.assertEquals("R\\u233eflexions sur le timing de la quantit\\u233e {\\u230ae} should be \\u230ae",
                formatter.format("Réflexions sur le timing de la quantité {\\ae} should be æ"));
    }

    @Test
    public void  testComplicated2() {
        Assert.assertEquals("h\\'e1ll{\\u339oe}", formatter.format("h\\'all{\\oe}"));
    }

    @Test
    public void testComplicated3() {
        Assert.assertEquals("Le c\\u339oeur d\\u233e\\u231cu mais l'\\u226ame plut\\u244ot na\\u239ive, Lou\\u255ys r" +
                "\\u234eva de crapa\\u252?ter en cano\\u235e au del\\u224a des \\u238iles, pr\\u232es du m\\u228alstr" +
                "\\u246om o\\u249u br\\u251ulent les nov\\u230ae.", formatter.format("Le cœur déçu mais l'âme plutôt " +
                "naïve, Louÿs rêva de crapaüter en canoë au delà des îles, près du mälström où brûlent les novæ."));
    }

    @Test
    public void testComplicated4() {
        Assert.assertEquals("l'\\u238ile exigu\\u235e\n" +
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
        Assert.assertEquals("\\u193Arv\\u237izt\\u369?r\\u337? t\\u252?k\\u246orf\\u250ur\\u243og\\u233ep",
                formatter.format("Árvíztűrő tükörfúrógép"));
    }

    @Test
    public void testComplicated6() {
        Assert.assertEquals("Pchn\\u261a\\u263c w t\\u281e \\u322l\\u243od\\u378z je\\u380za lub o\\u347sm skrzy\\u324n fig"
                ,formatter.format("Pchnąć w tę łódź jeża lub ośm skrzyń fig"));
    }

    @Test
    public void testSpecialCharacters() {
        Assert.assertEquals("\\'f3", formatter.format("\\'{o}")); // ó
        Assert.assertEquals("\\'f2", formatter.format("\\`{o}")); // ò
        Assert.assertEquals("\\'f4", formatter.format("\\^{o}")); // ô
        Assert.assertEquals("\\'f6", formatter.format("\\\"{o}")); // ö
        Assert.assertEquals("\\u245o", formatter.format("\\~{o}")); // õ
        Assert.assertEquals("\\u333o", formatter.format("\\={o}"));
        Assert.assertEquals("\\u335o", formatter.format("{\\uo}"));
        Assert.assertEquals("\\u231c", formatter.format("{\\cc}")); // ç
        Assert.assertEquals("{\\u339oe}", formatter.format("{\\oe}"));
        Assert.assertEquals("{\\u338OE}", formatter.format("{\\OE}"));
        Assert.assertEquals("{\\u230ae}", formatter.format("{\\ae}")); // æ
        Assert.assertEquals("{\\u198AE}", formatter.format("{\\AE}")); // Æ

        Assert.assertEquals("", formatter.format("\\.{o}")); // ???
        Assert.assertEquals("", formatter.format("\\vo")); // ???
        Assert.assertEquals("", formatter.format("\\Ha")); // ã // ???
        Assert.assertEquals("", formatter.format("\\too"));
        Assert.assertEquals("", formatter.format("\\do")); // ???
        Assert.assertEquals("", formatter.format("\\bo")); // ???
        Assert.assertEquals("\\u229a", formatter.format("{\\aa}")); // å
        Assert.assertEquals("\\u197A", formatter.format("{\\AA}")); // Å
        Assert.assertEquals("\\u248o", formatter.format("{\\o}")); // ø
        Assert.assertEquals("\\u216O", formatter.format("{\\O}")); // Ø
        Assert.assertEquals("\\u322l", formatter.format("{\\l}"));
        Assert.assertEquals("\\u321L", formatter.format("{\\L}"));
        Assert.assertEquals("\\u223ss", formatter.format("{\\ss}")); // ß
        Assert.assertEquals("\\u191?", formatter.format("\\`?")); // ¿
        Assert.assertEquals("\\u161!", formatter.format("\\`!")); // ¡

        Assert.assertEquals("", formatter.format("\\dag"));
        Assert.assertEquals("", formatter.format("\\ddag"));
        Assert.assertEquals("\\u167S", formatter.format("{\\S}")); // §
        Assert.assertEquals("\\u182P", formatter.format("{\\P}")); // ¶
        Assert.assertEquals("\\u169?", formatter.format("{\\copyright}")); // ©
        Assert.assertEquals("\\u163?", formatter.format("{\\pounds}")); // £
    }

    @Test
    public void testRTFCharacters(){
        Assert.assertEquals("\\'e0",formatter.format("\\`{a}"));
        Assert.assertEquals("\\'e8",formatter.format("\\`{e}"));
        Assert.assertEquals("\\'ec",formatter.format("\\`{i}"));
        Assert.assertEquals("\\'f2",formatter.format("\\`{o}"));
        Assert.assertEquals("\\'f9",formatter.format("\\`{u}"));

        Assert.assertEquals("\\'e1",formatter.format("\\'a"));
        Assert.assertEquals("\\'e9",formatter.format("\\'e"));
        Assert.assertEquals("\\'ed",formatter.format("\\'i"));
        Assert.assertEquals("\\'f3",formatter.format("\\'o"));
        Assert.assertEquals("\\'fa",formatter.format("\\'u"));

        Assert.assertEquals("\\'e2",formatter.format("\\^a"));
        Assert.assertEquals("\\'ea",formatter.format("\\^e"));
        Assert.assertEquals("\\'ee",formatter.format("\\^i"));
        Assert.assertEquals("\\'f4",formatter.format("\\^o"));
        Assert.assertEquals("\\'fa",formatter.format("\\^u"));

        Assert.assertEquals("\\'e4",formatter.format("\\\"a"));
        Assert.assertEquals("\\'eb",formatter.format("\\\"e"));
        Assert.assertEquals("\\'ef",formatter.format("\\\"i"));
        Assert.assertEquals("\\'f6",formatter.format("\\\"o"));
        Assert.assertEquals("\\u252u",formatter.format("\\\"u"));

        Assert.assertEquals("\\'f1",formatter.format("\\~n"));
    }

    @Test
    public void testRTFCharactersCapital() {
        Assert.assertEquals("\\'c0",formatter.format("\\`A"));
        Assert.assertEquals("\\'c8",formatter.format("\\`E"));
        Assert.assertEquals("\\'cc",formatter.format("\\`I"));
        Assert.assertEquals("\\'d2",formatter.format("\\`O"));
        Assert.assertEquals("\\'d9",formatter.format("\\`U"));

        Assert.assertEquals("\\'c1",formatter.format("\\'A"));
        Assert.assertEquals("\\'c9",formatter.format("\\'E"));
        Assert.assertEquals("\\'cd",formatter.format("\\'I"));
        Assert.assertEquals("\\'d3",formatter.format("\\'O"));
        Assert.assertEquals("\\'da",formatter.format("\\'U"));

        Assert.assertEquals("\\'c2",formatter.format("\\^A"));
        Assert.assertEquals("\\'ca",formatter.format("\\^E"));
        Assert.assertEquals("\\'ce",formatter.format("\\^I"));
        Assert.assertEquals("\\'d4",formatter.format("\\^O"));
        Assert.assertEquals("\\'db",formatter.format("\\^U"));

        Assert.assertEquals("\\'c4",formatter.format("\\\"A"));
        Assert.assertEquals("\\'cb",formatter.format("\\\"E"));
        Assert.assertEquals("\\'cf",formatter.format("\\\"I"));
        Assert.assertEquals("\\'d6",formatter.format("\\\"O"));
        Assert.assertEquals("\\'dc",formatter.format("\\\"U"));
    }

}
