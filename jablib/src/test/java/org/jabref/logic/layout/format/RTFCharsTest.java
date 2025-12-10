package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RTFCharsTest {
    private LayoutFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RTFChars();
    }

    @AfterEach
    void tearDown() {
        formatter = null;
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            '' , ''
            hallo , hallo
            R\\u233eflexions sur le timing de la quantit\\u233e , Réflexions sur le timing de la quantité
            h\\'e1llo , h\\'allo
            """)
    void basicFormat(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            {\\i hallo} , \\emph{hallo}
            {\\i hallo} , {\\emph hallo}
            An article title with {\\i a book title} emphasized , An article title with \\emph{a book title} emphasized
            {\\i hallo} , \\textit{hallo}
            {\\i hallo} , {\\textit hallo}
            {\\b hallo} , \\textbf{hallo}
            {\\b hallo} , {\\textbf hallo}
            """)
    void laTeXHighlighting(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    @Test
    void complicated() {
        assertEquals("R\\u233eflexions sur le timing de la quantit\\u233e {\\u230ae} should be \\u230ae",
                formatter.format("Réflexions sur le timing de la quantité {\\ae} should be æ"));
    }

    @Test
    void complicated2() {
        assertEquals("h\\'e1ll{\\u339oe}", formatter.format("h\\'all{\\oe}"));
    }

    @Test
    void complicated3() {
        assertEquals("Le c\\u339oeur d\\u233e\\u231cu mais l'\\u226ame plut\\u244ot na\\u239ive, Lou\\u255ys r" +
                "\\u234eva de crapa\\u252?ter en cano\\u235e au del\\u224a des \\u238iles, pr\\u232es du m\\u228alstr" +
                "\\u246om o\\u249u br\\u251ulent les nov\\u230ae.", formatter.format("Le cœur déçu mais l'âme plutôt " +
                "naïve, Louÿs rêva de crapaüter en canoë au delà des îles, près du mälström où brûlent les novæ."));
    }

    @Test
    void complicated4() {
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
    void complicated5() {
        assertEquals("\\u193Arv\\u237izt\\u369?r\\u337? t\\u252?k\\u246orf\\u250ur\\u243og\\u233ep",
                formatter.format("Árvíztűrő tükörfúrógép"));
    }

    @Test
    void complicated6() {
        assertEquals("Pchn\\u261a\\u263c w t\\u281e \\u322l\\u243od\\u378z je\\u380za lub o\\u347sm skrzy\\u324n fig",
                formatter.format("Pchnąć w tę łódź jeża lub ośm skrzyń fig"));
    }

    static Stream<Arguments> specialCharacterCases() {
        return Stream.of(
                Arguments.of("\\'f3", "\\'{o}"), // ó
                Arguments.of("\\'f2", "\\`{o}"), // ò
                Arguments.of("\\'f4", "\\^{o}"), // ô
                Arguments.of("\\'f6", "\\\"{o}"), // ö
                Arguments.of("\\u245o", "\\~{o}"), // õ
                Arguments.of("\\u333o", "\\={o}"),
                Arguments.of("\\u335o", "{\\uo}"),
                Arguments.of("\\u231c", "{\\cc}"), // ç
                Arguments.of("{\\u339oe}", "{\\oe}"),
                Arguments.of("{\\u338OE}", "{\\OE}"),
                Arguments.of("{\\u230ae}", "{\\ae}"), // æ
                Arguments.of("{\\u198AE}", "{\\AE}"), // Æ

                Arguments.of("", "\\.{o}"), // ???
                Arguments.of("", "\\vo"), // ???
                Arguments.of("", "\\Ha"), // ã // ???
                Arguments.of("", "\\too"),
                Arguments.of("", "\\do"), // ???
                Arguments.of("", "\\bo"), // ???

                Arguments.of("\\u229a", "{\\aa}"), // å
                Arguments.of("\\u197A", "{\\AA}"), // Å
                Arguments.of("\\u248o", "{\\o}"), // ø
                Arguments.of("\\u216O", "{\\O}"), // Ø
                Arguments.of("\\u322l", "{\\l}"),
                Arguments.of("\\u321L", "{\\L}"),
                Arguments.of("\\u223ss", "{\\ss}"), // ß
                Arguments.of("\\u191?", "\\`?"), // ¿
                Arguments.of("\\u161!", "\\`!"), // ¡

                Arguments.of("", "\\dag"),
                Arguments.of("", "\\ddag"),
                Arguments.of("\\u167S", "{\\S}"), // §
                Arguments.of("\\u182P", "{\\P}"), // ¶
                Arguments.of("\\u169?", "{\\copyright}"), // ©
                Arguments.of("\\u163?", "{\\pounds}") // £
        );
    }

    @ParameterizedTest
    @MethodSource("specialCharacterCases")
    void specialCharacters(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    @ParameterizedTest(name = "specialChar={0}, formattedStr={1}")
    @CsvSource(textBlock = """
            # A
            ÀÁÂÃÄĀĂĄ , \\u192A\\u193A\\u194A\\u195A\\u196A\\u256A\\u258A\\u260A
            # a
            àáâãäåāăą , \\u224a\\u225a\\u226a\\u227a\\u228a\\u229a\\u257a\\u259a\\u261a
            # C
            ÇĆĈĊČ , \\u199C\\u262C\\u264C\\u266C\\u268C
            # c
            çćĉċč , \\u231c\\u263c\\u265c\\u267c\\u269c
            # D
            ÐĐ , \\u208D\\u272D
            # d
            ðđ , \\u240d\\u273d
            # E
            ÈÉÊËĒĔĖĘĚ , \\u200E\\u201E\\u202E\\u203E\\u274E\\u276E\\u278E\\u280E\\u282E
            # e
            èéêëēĕėęě , \\u232e\\u233e\\u234e\\u235e\\u275e\\u277e\\u279e\\u281e\\u283e
            # G
            ĜĞĠĢŊ , \\u284G\\u286G\\u288G\\u290G\\u330G
            # g
            ĝğġģŋ , \\u285g\\u287g\\u289g\\u291g\\u331g
            # H
            ĤĦ , \\u292H\\u294H
            # h
            ĥħ , \\u293h\\u295h
            # I
            ÌÍÎÏĨĪĬĮİ , \\u204I\\u205I\\u206I\\u207I\\u296I\\u298I\\u300I\\u302I\\u304I
            # i
            ìíîïĩīĭį , \\u236i\\u237i\\u238i\\u239i\\u297i\\u299i\\u301i\\u303i
            # J
            Ĵ , \\u308J
            # j
            ĵ , \\u309j
            # K
            Ķ , \\u310K
            # k
            ķ , \\u311k
            # L
            ĹĻĿ , \\u313L\\u315L\\u319L
            # l
            ĺļŀł , \\u314l\\u316l\\u320l\\u322l
            # N
            ÑŃŅŇ , \\u209N\\u323N\\u325N\\u327N
            # n
            ñńņň , \\u241n\\u324n\\u326n\\u328n
            # O
            ÒÓÔÕÖØŌŎ , \\u210O\\u211O\\u212O\\u213O\\u214O\\u216O\\u332O\\u334O
            # o
            òóôõöøōŏ , \\u242o\\u243o\\u244o\\u245o\\u246o\\u248o\\u333o\\u335o
            # R
            ŔŖŘ , \\u340R\\u342R\\u344R
            # r
            ŕŗř , \\u341r\\u343r\\u345r
            # S
            ŚŜŞŠ , \\u346S\\u348S\\u350S\\u352S
            # s
            śŝşš , \\u347s\\u349s\\u351s\\u353s
            # T
            ŢŤŦ , \\u354T\\u356T\\u358T
            # t
            ţŧ , \\u355t\\u359t
            # U
            ÙÚÛÜŨŪŬŮŲ , \\u217U\\u218U\\u219U\\u220U\\u360U\\u362U\\u364U\\u366U\\u370U
            # u
            ùúûũūŭůų , \\u249u\\u250u\\u251u\\u361u\\u363u\\u365u\\u367u\\u371u
            # W
            Ŵ , \\u372W
            # w
            ŵ , \\u373w
            # Y
            ŶŸÝ , \\u374Y\\u376Y\\u221Y
            # y
            ŷÿ , \\u375y\\u255y
            # Z
            ŹŻŽ , \\u377Z\\u379Z\\u381Z
            # z
            źżž , \\u378z\\u380z\\u382z
            # AE
            Æ , \\u198AE
            # ae
            æ , \\u230ae
            # OE
            Œ , \\u338OE
            # oe
            œ , \\u339oe
            # TH
            Þ , \\u222TH
            # ss
            ß , \\u223ss
            # !
            ¡ , \\u161!
            """)
    void moreSpecialCharacters(String specialChar, String expectedResult) {
        String formattedStr = formatter.format(specialChar);
        assertEquals(expectedResult, formattedStr);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
             \\'e0, \\`{a}
             \\'e8, \\`{e}
             \\'ec, \\`{i}
             \\'f2, \\`{o}
             \\'f9, \\`{u}
             \\'e1, \\'a
             \\'e9, \\'e
             \\'ed, \\'i
             \\'f3, \\'o
             \\'fa, \\'u
             \\'e2, \\^a
             \\'ea, \\^e
             \\'ee, \\^i
             \\'f4, \\^o
             \\'fa, \\^u
             \\'e4, \\\"a
             \\'eb, \\\"e
             \\'ef, \\\"i
             \\'f6, \\\"o
             \\u252u, \\\"u
             \\'f1, \\~n
            """)
    void rtfCharacters(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            \\'c0 , \\`A
            \\'c8 , \\`E
            \\'cc , \\`I
            \\'d2 , \\`O
            \\'d9 , \\`U
            \\'c1 , \\'A
            \\'c9 , \\'E
            \\'cd , \\'I
            \\'d3 , \\'O
            \\'da , \\'U
            \\'c2 , \\^A
            \\'ca , \\^E
            \\'ce , \\^I
            \\'d4 , \\^O
            \\'db , \\^U
            \\'c4 , \\\"A
            \\'cb , \\\"E
            \\'cf , \\\"I
            \\'d6 , \\\"O
            \\'dc , \\\"U
            """)
    void rTFCharactersCapital(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }
}
