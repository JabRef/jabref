package net.sf.jabref.logic.labelpattern;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LabelPatternUtilTest {

    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1 = "Isaac Newton";
    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2 = "Isaac Newton and James Maxwell";
    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3 = "Isaac Newton and James Maxwell and Albert Einstein";

    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1 = "Wil van der Aalst";
    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2 = "Wil van der Aalst and Tammo van Lessen";

    private static final String AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1 = "I. Newton";
    private static final String AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2 = "I. Newton and J. Maxwell";
    private static final String AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3 = "I. Newton and J. Maxwell and A. Einstein";
    private static final String AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4 = "I. Newton and J. Maxwell and A. Einstein and N. Bohr";
    private static final String AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5 = "I. Newton and J. Maxwell and A. Einstein and N. Bohr and Harry Unknown";

    private static final String TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH = "application migration effort in the cloud - the case of cloud platforms";
    private static final String TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON = "{BPEL} conformance in open source engines: the case of static analysis";
    private static final String TITLE_STRING_CASED = "Process Viewing Patterns";
    private static final String TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD = "BPMN Conformance in Open Source Engines";
    private static final String TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING = "The Difference Between Graph-Based and Block-Structured Business Process Modelling Languages";
    private static final String TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON = "Cloud Computing: The Next Revolution in IT";
    private static final String TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD = "Towards Choreography-based Process Distribution in the Cloud";
    private static final String TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS = "On the Measurement of Design-Time Adaptability for Process-Based Systems ";

    @BeforeClass
    public static void setUpGlobalsPrefs() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Before
    public void setUp() {
        LabelPatternUtil.setDataBase(new BibDatabase());
    }

    @Test
    public void testAndInAuthorName() {
        BibEntry entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Simon Holland}}");
        assertEquals("Holland", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth")));
    }

    @Test
    public void testAndAuthorNames() {
        String bibtexString = "@ARTICLE{whatevery, author={Mari D. Herland and Mona-Iren Hauge and Ingeborg M. Helgeland}}";
        BibEntry entry = BibtexParser.singleFromString(bibtexString);
        assertEquals("HerlandHaugeHelgeland", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry, "authors3")));
    }

    @Test
    public void testSpecialLatexCharacterInAuthorName() {
        BibEntry entry = BibtexParser.singleFromString("@ARTICLE{kohn, author={Simon Popovi\\v{c}ov\\'{a}}}");
        assertEquals("Popovicova", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry, "auth")));
    }

    /**
     * Test for https://sourceforge.net/forum/message.php?msg_id=4498555 Test the Labelmaker and all kind of accents Á á
     * Ć ć É é Í í Ĺ ĺ Ń ń Ó ó Ŕ ŕ Ś ś Ú ú Ý ý Ź ź
     */
    @Test
    public void testMakeLabelAndCheckLegalKeys() {
        BibEntry entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Köning}, year={2000}}");
        assertEquals("Koen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Áöning}, year={2000}}");
        assertEquals("Aoen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Éöning}, year={2000}}");
        assertEquals("Eoen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Íöning}, year={2000}}");
        assertEquals("Ioen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ĺöning}, year={2000}}");
        assertEquals("Loen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ńöning}, year={2000}}");
        assertEquals("Noen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Óöning}, year={2000}}");
        assertEquals("Ooen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ŕöning}, year={2000}}");
        assertEquals("Roen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Śöning}, year={2000}}");
        assertEquals("Soen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Úöning}, year={2000}}");
        assertEquals("Uoen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ýöning}, year={2000}}");
        assertEquals("Yoen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Źöning}, year={2000}}");
        assertEquals("Zoen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    }

    /**
     * Test the Labelmaker and with accent grave Chars to test: "ÀÈÌÒÙ";
     */
    @Test
    public void testMakeLabelAndCheckLegalKeysAccentGrave() {
        BibEntry entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Àöning}, year={2000}}");
        assertEquals("Aoen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Èöning}, year={2000}}");
        assertEquals("Eoen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ìöning}, year={2000}}");
        assertEquals("Ioen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Òöning}, year={2000}}");
        assertEquals("Ooen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ùöning}, year={2000}}");
        assertEquals("Uoen", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    }

    /**
     * Tests if checkLegalKey replaces Non-ASCII chars.
     * There are quite a few chars that should be replaced. Perhaps there is a better method than the current.
     *
     * @see LabelPatternUtil#checkLegalKey(String)
     */
    @Test
    public void testCheckLegalKey() {
        // not tested/ not in hashmap UNICODE_CHARS:
        // Ł ł   Ő ő Ű ű   Ŀ ŀ   Ħ ħ   Ð ð Þ þ   Œ œ   Æ æ Ø ø Å å   Ə ə Đ đ   Ů ů	Ǣ ǣ ǖ ǘ ǚ ǜ
        //" Ǣ ǣ ǖ ǘ ǚ ǜ   " +
        //"Đ đ   Ů ů  " +
        //"Ł ł   Ő ő Ű ű   Ŀ ŀ   Ħ ħ   Ð ð Þ þ   Œ œ   Æ æ Ø ø Å å   Ə ə
        String accents = "ÀàÈèÌìÒòÙù Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ";
        String expectedResult = "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYy";
        assertEquals(expectedResult, LabelPatternUtil.checkLegalKey(accents));

        accents = "ÄäËëÏïÖöÜüŸÿ";
        expectedResult = "AeaeEeIiOeoeUeueYy";
        assertEquals(expectedResult, LabelPatternUtil.checkLegalKey(accents));

        accents = "Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ";
        expectedResult = "CcGgKkLlNnRrSsTt";
        assertEquals(expectedResult, LabelPatternUtil.checkLegalKey(accents));

        accents = "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ";
        expectedResult = "AaEeGgIiOoUu";
        assertEquals(expectedResult, LabelPatternUtil.checkLegalKey(accents));

        accents = "Ċ ċ Ė ė Ġ ġ İ ı Ż ż";
        expectedResult = "CcEeGgIiZz";
        assertEquals(expectedResult, LabelPatternUtil.checkLegalKey(accents));

        accents = "Ą ą Ę ę Į į Ǫ ǫ Ų ų";
        expectedResult = "AaEeIiOoUu"; // O or Q? o or q?
        assertEquals(expectedResult, LabelPatternUtil.checkLegalKey(accents));

        accents = "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ";
        expectedResult = "AaEeIiOoUuYy";
        assertEquals(expectedResult, LabelPatternUtil.checkLegalKey(accents));

        accents = "Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž";
        expectedResult = "AaCcDdEeIiLlNnOoRrSsTtUuZz";
        assertEquals(expectedResult, LabelPatternUtil.checkLegalKey(accents));

        expectedResult = "AaEeIiNnOoUuYy";
        accents = "ÃãẼẽĨĩÑñÕõŨũỸỹ";
        assertEquals(expectedResult, LabelPatternUtil.checkLegalKey(accents));

        accents = "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ";
        expectedResult = "DdHhLlLlMmNnRrRrSsTt";
        assertEquals(expectedResult, LabelPatternUtil.checkLegalKey(accents));

        String totest = "À à È è Ì ì Ò ò Ù ù   Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ  Ä ä Ë ë Ï ï Ö ö Ü ü Ÿ ÿ    "
                + "Ã ã Ẽ ẽ Ĩ ĩ Ñ ñ Õ õ Ũ ũ Ỹ ỹ   Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ"
                + " Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž   " + "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ"
                + "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ   " + "Ċ ċ Ė ė Ġ ġ İ ı Ż ż   Ą ą Ę ę Į į Ǫ ǫ Ų ų   "
                + "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ   ";
        String expectedResults = "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYyAeaeEeIiOeoeUeueYy" +
                "AaEeIiNnOoUuYyCcGgKkLlNnRrSsTt" +
                "AaCcDdEeIiLlNnOoRrSsTtUuZz" +
                "AaEeIiOoUuYy" +
                "AaEeGgIiOoUu" +
                "CcEeGgIiZzAaEeIiOoUu" +
                "DdHhLlLlMmNnRrRrSsTt";
        assertEquals(expectedResults, LabelPatternUtil.checkLegalKey(totest));
    }

    @Test
    public void testFirstAuthor() {
        assertEquals(
                "Newton",
                LabelPatternUtil
                        .firstAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));
        assertEquals("Newton", LabelPatternUtil.firstAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));

        // https://sourceforge.net/forum/message.php?msg_id=4498555
        assertEquals("K{\\\"o}ning", LabelPatternUtil
                .firstAuthor("K{\\\"o}ning"));

        assertEquals("", LabelPatternUtil.firstAuthor(""));
    }

    @Test(expected = NullPointerException.class)
    public void testFirstAuthorNull() {
            LabelPatternUtil.firstAuthor(null);
    }

    @Test
    public void testUniversity() {
        BibEntry entry = BibtexParser.singleFromString("@ARTICLE{kohn, author={{Link{\\\"{o}}ping University}}}");
        assertEquals("UniLinkoeping", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry, "auth")));
    }

    @Test
    public void testDepartment() {
        BibEntry entry = BibtexParser
                .singleFromString(
                        "@ARTICLE{kohn, author={{Link{\\\"{o}}ping University, Department of Electrical Engineering}}}");
        assertEquals("UniLinkoepingEE", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry, "auth")));
    }

    @Test
    public void testSchool() {
        BibEntry entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Link{\\\"{o}}ping University, School of Computer Engineering}}}");
        assertEquals("UniLinkoepingCE", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry, "auth")));
    }

    @Test
    public void testInstituteOfTechnology() {
        BibEntry entry = BibtexParser
                .singleFromString("@ARTICLE{kohn, author={{Massachusetts Institute of Technology}}}");
        assertEquals("MIT", LabelPatternUtil.checkLegalKey(LabelPatternUtil.makeLabel(entry, "auth")));
    }

    @Test
    public void testAuthIniN() {
        assertEquals(
                "NMEB",
                LabelPatternUtil
                        .authIniN(
                                AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5,
                                4));
        assertEquals("NMEB", LabelPatternUtil.authIniN(
                AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 4));
        assertEquals("NeME", LabelPatternUtil.authIniN(
                AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 4));
        assertEquals("NeMa", LabelPatternUtil.authIniN(
                AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 4));
        assertEquals("Newt", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 4));
        assertEquals("", "");

        assertEquals("N", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 1));
        assertEquals("", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 0));
        assertEquals("", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, -1));

        assertEquals("Newton", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 6));
        assertEquals("Newton", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 7));
    }

    @Test(expected = NullPointerException.class)
    public void testAuthIniNNull() {
            LabelPatternUtil.authIniN(null, 3);
    }

    /**
     * Tests  [auth.auth.ea]
     */
    @Test
    public void authAuthEa() {
        assertEquals("Newton", LabelPatternUtil.authAuthEa(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1));
        assertEquals("Newton.Maxwell", LabelPatternUtil.authAuthEa(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2));
        assertEquals("Newton.Maxwell.ea",
                LabelPatternUtil.authAuthEa(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3));
    }

    /**
     * Tests the [auth.etal] and [authEtAl] patterns
     */
    @Test
    public void testAuthEtAl() {
        // tests taken from the comments

        // [auth.etal]
        String delim = ".";
        String append = ".etal";
        assertEquals("Newton.etal",
                LabelPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, delim, append));
        assertEquals("Newton.Maxwell",
                LabelPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, delim, append));

        // [authEtAl]
        delim = "";
        append = "EtAl";
        assertEquals("NewtonEtAl",
                LabelPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, delim, append));
        assertEquals("NewtonMaxwell",
                LabelPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, delim, append));
    }

    /**
     * Test the [authshort] pattern
     */
    @Test
    public void testAuthShort() {
        // tests taken from the comments
        assertEquals(
                "NME+",
                LabelPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals(
                "NME",
                LabelPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals(
                "NM",
                LabelPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals(
                "Newton",
                LabelPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
    }

    /**
     * Test the [authN_M] pattern
     */
    @Test
    public void authNM() {
        assertEquals(
                "N",
                LabelPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 1, 1));
        assertEquals(
                "Max",
                LabelPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 3, 2));
        assertEquals(
                "New",
                LabelPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 3, 1));
        assertEquals(
                "Bo",
                LabelPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 2, 4));
        assertEquals(
                "Bohr",
                LabelPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, 6, 4));

        assertEquals(
                "Aal",
                LabelPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1, 3, 1));
        assertEquals(
                "Less",
                LabelPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2, 4, 2));

        assertEquals(
                "",
                LabelPatternUtil.authNofMth("", 2, 4));
    }

    @Test(expected = NullPointerException.class)
    public void authNMThrowsNPE() {
        LabelPatternUtil.authNofMth(null, 2, 4);
    }

    /**
     * Tests [authForeIni]
     */
    @Test
    public void firstAuthorForenameInitials() {
        assertEquals(
                "I",
                LabelPatternUtil.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals(
                "I",
                LabelPatternUtil.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals(
                "I",
                LabelPatternUtil.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1));
        assertEquals(
                "I",
                LabelPatternUtil.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2));
    }

    /**
     * Tests [authFirstFull]
     */
    @Test
    public void firstAuthorVonAndLast() {
        assertEquals(
                "vanderAalst",
                LabelPatternUtil.firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals(
                "vanderAalst",
                LabelPatternUtil.firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests [authors]
     */
    @Test
    public void testAllAuthors() {
        assertEquals(
                "Newton",
                LabelPatternUtil.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals(
                "NewtonMaxwell",
                LabelPatternUtil.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals(
                "NewtonMaxwellEinstein",
                LabelPatternUtil.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
    }

    /**
     * Tests [authorsAlpha]
     */
    @Test
    public void authorsAlpha() {
        assertEquals(
                "New",
                LabelPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals(
                "NM",
                LabelPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals(
                "NME",
                LabelPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals(
                "NMEB",
                LabelPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals(
                "NME+",
                LabelPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals(
                "vdAal",
                LabelPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals(
                "vdAvL",
                LabelPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests [authorLast]
     */
    @Test
    public void lastAuthor() {
        assertEquals(
                "Newton",
                LabelPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals(
                "Maxwell",
                LabelPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals(
                "Einstein",
                LabelPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals(
                "Bohr",
                LabelPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals(
                "Unknown",
                LabelPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals(
                "Aalst",
                LabelPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals(
                "Lessen",
                LabelPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests [authorLastForeIni]
     */
    @Test
    public void lastAuthorForenameInitials() {
        assertEquals(
                "I",
                LabelPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals(
                "J",
                LabelPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals(
                "A",
                LabelPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals(
                "N",
                LabelPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals(
                "H",
                LabelPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals(
                "W",
                LabelPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals(
                "T",
                LabelPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests [authorIni]
     */
    @Test
    public void oneAuthorPlusIni() {
        assertEquals(
                "Newto",
                LabelPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals(
                "NewtoM",
                LabelPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals(
                "NewtoME",
                LabelPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals(
                "NewtoMEB",
                LabelPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals(
                "NewtoMEBU",
                LabelPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals(
                "Aalst",
                LabelPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals(
                "AalstL",
                LabelPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests the [authorsN] pattern. -> [authors1]
     */
    @Test
    public void testNAuthors1() {
        assertEquals("Newton",
                LabelPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 1));
        assertEquals("NewtonEtAl",
                LabelPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 1));
        assertEquals("NewtonEtAl",
                LabelPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 1));
        assertEquals("NewtonEtAl",
                LabelPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 1));
    }

    /**
     * Tests the [authorsN] pattern. -> [authors3]
     */
    @Test
    public void testNAuthors3() {
        assertEquals(
                "Newton",
                LabelPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 3));
        assertEquals(
                "NewtonMaxwell",
                LabelPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 3));
        assertEquals(
                "NewtonMaxwellEinstein",
                LabelPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 3));
        assertEquals(
                "NewtonMaxwellEinsteinEtAl",
                LabelPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 3));
    }

    @Test
    public void testFirstPage() {
        assertEquals("7", LabelPatternUtil.firstPage("7--27"));
        assertEquals("27", LabelPatternUtil.firstPage("--27"));
        assertEquals("", LabelPatternUtil.firstPage(""));
        assertEquals("42", LabelPatternUtil.firstPage("42--111"));
        assertEquals("7", LabelPatternUtil.firstPage("7,41,73--97"));
        assertEquals("7", LabelPatternUtil.firstPage("41,7,73--97"));
        assertEquals("43", LabelPatternUtil.firstPage("43+"));
    }

    @Test(expected = NullPointerException.class)
    public void testFirstPageNull() {
        LabelPatternUtil.firstPage(null);
    }

    @Test
    public void testLastPage() {

        assertEquals("27", LabelPatternUtil.lastPage("7--27"));
        assertEquals("27", LabelPatternUtil.lastPage("--27"));
        assertEquals("", LabelPatternUtil.lastPage(""));
        assertEquals("111", LabelPatternUtil.lastPage("42--111"));
        assertEquals("97", LabelPatternUtil.lastPage("7,41,73--97"));
        assertEquals("97", LabelPatternUtil.lastPage("7,41,97--73"));
        assertEquals("43", LabelPatternUtil.lastPage("43+"));
    }

    @Test(expected = NullPointerException.class)
    public void testLastPageNull() {
        LabelPatternUtil.lastPage(null);
    }

    /**
     * Tests [veryShortTitle]
     */
    @Test
    public void veryShortTitle() {
        // veryShortTitle is getTitleWords with "1" as count
        int count = 1;
        assertEquals(
                "application",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH));
        assertEquals(
                "BPEL",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals(
                "Process",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED));
        assertEquals(
                "BPMN",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD));
        assertEquals(
                "Difference",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING));
        assertEquals(
                "Cloud",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals(
                "Towards",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD));
        assertEquals(
                "Measurement",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS));
    }

    /**
     * Tests [shortTitle]
     */
    @Test
    public void shortTitle() {
        // veryShortTitle is getTitleWords with "3" as count
        int count = 3;
        assertEquals(
                "applicationmigrationeffort",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH));
        assertEquals(
                "BPELconformanceopen",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals(
                "ProcessViewingPatterns",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED));
        assertEquals(
                "BPMNConformanceOpen",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD));
        assertEquals(
                "DifferenceGraphBased",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING));
        assertEquals(
                "CloudComputingNext",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals(
                "TowardsChoreographybased",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD));
        assertEquals(
                "MeasurementDesignTime",
                LabelPatternUtil.getTitleWords(count, TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS));
    }

    @Test
    public void keywordNKeywordsSeparatedBySpace() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "w1, w2a w2b, w3");

        String result = LabelPatternUtil.makeLabel(entry, "keyword1");
        assertEquals("w1", result);

        // check keywords with space
        result = LabelPatternUtil.makeLabel(entry, "keyword2");
        assertEquals("w2a w2b", result);

        // check out of range
        result = LabelPatternUtil.makeLabel(entry, "keyword4");
        assertEquals("", result);
    }

    @Test
    public void keywordsNKeywordsSeparatedBySpace() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "w1, w2a w2b, w3");

        // all keywords
        String result = LabelPatternUtil.makeLabel(entry, "keywords");
        assertEquals("w1w2aw2bw3", result);

        // check keywords with space
        result = LabelPatternUtil.makeLabel(entry, "keywords2");
        assertEquals("w1w2aw2b", result);

        // check out of range
        result = LabelPatternUtil.makeLabel(entry, "keywords55");
        assertEquals("w1w2aw2bw3", result);
    }

    @Test
    public void testCheckLegalKey2() {
        // Enforce legal keys
        assertEquals("AAAA", LabelPatternUtil.checkLegalKey("AA AA", true));
        assertEquals("SPECIALCHARS", LabelPatternUtil.checkLegalKey("SPECIAL CHARS#{\\\"}~,^", true));
        assertEquals("", LabelPatternUtil.checkLegalKey("\n\t\r", true));

        // Do not enforce legal keys
        assertEquals("AAAA", LabelPatternUtil.checkLegalKey("AA AA", false));
        assertEquals("SPECIALCHARS#~^", LabelPatternUtil.checkLegalKey("SPECIAL CHARS#{\\\"}~,^", false));
        assertEquals("", LabelPatternUtil.checkLegalKey("\n\t\r", false));

        // Check null input
        assertNull(LabelPatternUtil.checkLegalKey(null));
        assertNull(LabelPatternUtil.checkLegalKey(null, true));
        assertNull(LabelPatternUtil.checkLegalKey(null, false));

        // Use preferences setting
        assertEquals("AAAA", LabelPatternUtil.checkLegalKey("AA AA"));
        assertEquals("", LabelPatternUtil.checkLegalKey("\n\t\r"));
    }

}
