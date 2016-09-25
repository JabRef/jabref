package net.sf.jabref.logic.bibtexkeypattern;

import java.util.Optional;

import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BibtexKeyPatternUtilTest {

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

    private static ImportFormatPreferences importFormatPreferences;


    @Before
    public void setUp() {
        importFormatPreferences = JabRefPreferences.getInstance().getImportFormatPreferences();
        BibtexKeyPatternUtil.setDataBase(new BibDatabase());
    }

    @Test
    public void testAndInAuthorName() {
        Optional<BibEntry> entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Simon Holland}}",
                importFormatPreferences);
        assertEquals("Holland",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth",
                        ','), true));
    }

    @Test
    public void testAndAuthorNames() {
        String bibtexString = "@ARTICLE{whatevery, author={Mari D. Herland and Mona-Iren Hauge and Ingeborg M. Helgeland}}";
        Optional<BibEntry> entry = BibtexParser.singleFromString(bibtexString, importFormatPreferences);
        assertEquals("HerlandHaugeHelgeland",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry.get(), "authors3",
                        ','), true));
    }

    @Test
    public void testSpecialLatexCharacterInAuthorName() {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={Simon Popovi\\v{c}ov\\'{a}}}", importFormatPreferences);
        assertEquals("Popovicova",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry.get(), "auth",
                        ','), true));
    }

    /**
     * Test for https://sourceforge.net/forum/message.php?msg_id=4498555 Test the Labelmaker and all kind of accents Á á
     * Ć ć É é Í í Ĺ ĺ Ń ń Ó ó Ŕ ŕ Ś ś Ú ú Ý ý Ź ź
     */
    @Test
    public void testMakeLabelAndCheckLegalKeys() {

        Optional<BibEntry> entry0 = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={Andreas Köning}, year={2000}}", importFormatPreferences);
        assertEquals("Koen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Áöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Aoen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Éöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Eoen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Íöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Ioen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ĺöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Loen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ńöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Noen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Óöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Ooen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ŕöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Roen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Śöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Soen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Úöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Uoen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ýöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Yoen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Źöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Zoen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));
    }

    /**
     * Test the Labelmaker and with accent grave Chars to test: "ÀÈÌÒÙ";
     */
    @Test
    public void testMakeLabelAndCheckLegalKeysAccentGrave() {
        Optional<BibEntry> entry0 = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={Andreas Àöning}, year={2000}}", importFormatPreferences);
        assertEquals("Aoen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Èöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Eoen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ìöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Ioen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Òöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Ooen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ùöning}, year={2000}}",
                importFormatPreferences);
        assertEquals("Uoen",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry0.get(), "auth3",
                        ','), true));
    }

    /**
     * Tests if checkLegalKey replaces Non-ASCII chars.
     * There are quite a few chars that should be replaced. Perhaps there is a better method than the current.
     *
     * @see BibtexKeyPatternUtil#checkLegalKey(String)
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
        assertEquals(expectedResult, BibtexKeyPatternUtil.checkLegalKey(accents, true));

        accents = "ÄäËëÏïÖöÜüŸÿ";
        expectedResult = "AeaeEeIiOeoeUeueYy";
        assertEquals(expectedResult, BibtexKeyPatternUtil.checkLegalKey(accents, true));

        accents = "Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ";
        expectedResult = "CcGgKkLlNnRrSsTt";
        assertEquals(expectedResult, BibtexKeyPatternUtil.checkLegalKey(accents, true));

        accents = "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ";
        expectedResult = "AaEeGgIiOoUu";
        assertEquals(expectedResult, BibtexKeyPatternUtil.checkLegalKey(accents, true));

        accents = "Ċ ċ Ė ė Ġ ġ İ ı Ż ż";
        expectedResult = "CcEeGgIiZz";
        assertEquals(expectedResult, BibtexKeyPatternUtil.checkLegalKey(accents, true));

        accents = "Ą ą Ę ę Į į Ǫ ǫ Ų ų";
        expectedResult = "AaEeIiOoUu"; // O or Q? o or q?
        assertEquals(expectedResult, BibtexKeyPatternUtil.checkLegalKey(accents, true));

        accents = "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ";
        expectedResult = "AaEeIiOoUuYy";
        assertEquals(expectedResult, BibtexKeyPatternUtil.checkLegalKey(accents, true));

        accents = "Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž";
        expectedResult = "AaCcDdEeIiLlNnOoRrSsTtUuZz";
        assertEquals(expectedResult, BibtexKeyPatternUtil.checkLegalKey(accents, true));

        expectedResult = "AaEeIiNnOoUuYy";
        accents = "ÃãẼẽĨĩÑñÕõŨũỸỹ";
        assertEquals(expectedResult, BibtexKeyPatternUtil.checkLegalKey(accents, true));

        accents = "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ";
        expectedResult = "DdHhLlLlMmNnRrRrSsTt";
        assertEquals(expectedResult, BibtexKeyPatternUtil.checkLegalKey(accents, true));

        String totest = "À à È è Ì ì Ò ò Ù ù   Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ  Ä ä Ë ë Ï ï Ö ö Ü ü Ÿ ÿ    "
                + "Ã ã Ẽ ẽ Ĩ ĩ Ñ ñ Õ õ Ũ ũ Ỹ ỹ   Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ"
                + " Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž   " + "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ"
                + "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ   " + "Ċ ċ Ė ė Ġ ġ İ ı Ż ż   Ą ą Ę ę Į į Ǫ ǫ Ų ų   "
                + "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ   ";
        String expectedResults = "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYyAeaeEeIiOeoeUeueYy"
                + "AaEeIiNnOoUuYyCcGgKkLlNnRrSsTt" + "AaCcDdEeIiLlNnOoRrSsTtUuZz" + "AaEeIiOoUuYy" + "AaEeGgIiOoUu"
                + "CcEeGgIiZzAaEeIiOoUu" + "DdHhLlLlMmNnRrRrSsTt";
        assertEquals(expectedResults, BibtexKeyPatternUtil.checkLegalKey(totest, true));
    }

    @Test
    public void testFirstAuthor() {
        assertEquals("Newton", BibtexKeyPatternUtil.firstAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));
        assertEquals("Newton", BibtexKeyPatternUtil.firstAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));

        // https://sourceforge.net/forum/message.php?msg_id=4498555
        assertEquals("K{\\\"o}ning", BibtexKeyPatternUtil.firstAuthor("K{\\\"o}ning"));

        assertEquals("", BibtexKeyPatternUtil.firstAuthor(""));
    }

    @Test(expected = NullPointerException.class)
    public void testFirstAuthorNull() {
        BibtexKeyPatternUtil.firstAuthor(null);
    }

    @Test
    public void testUniversity() {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Link{\\\"{o}}ping University}}}", importFormatPreferences);
        assertEquals("UniLinkoeping",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry.get(), "auth",
                        ','), true));
    }

    @Test
    public void testDepartment() {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Link{\\\"{o}}ping University, Department of Electrical Engineering}}}",
                importFormatPreferences);
        assertEquals("UniLinkoepingEE",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry.get(), "auth",
                        ','), true));
    }

    @Test
    public void testSchool() {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Link{\\\"{o}}ping University, School of Computer Engineering}}}",
                importFormatPreferences);
        assertEquals("UniLinkoepingCE",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry.get(), "auth",
                        ','), true));
    }

    @Test
    public void testInstituteOfTechnology() {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Massachusetts Institute of Technology}}}", importFormatPreferences);
        assertEquals("MIT",
                BibtexKeyPatternUtil.checkLegalKey(BibtexKeyPatternUtil.makeLabel(entry.get(), "auth",
                        ','), true));
    }

    @Test
    public void testAuthIniN() {
        assertEquals("NMEB", BibtexKeyPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, 4));
        assertEquals("NMEB", BibtexKeyPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 4));
        assertEquals("NeME", BibtexKeyPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 4));
        assertEquals("NeMa", BibtexKeyPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 4));
        assertEquals("Newt", BibtexKeyPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 4));
        assertEquals("", "");

        assertEquals("N", BibtexKeyPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 1));
        assertEquals("", BibtexKeyPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 0));
        assertEquals("", BibtexKeyPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, -1));

        assertEquals("Newton", BibtexKeyPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 6));
        assertEquals("Newton", BibtexKeyPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 7));
    }

    @Test(expected = NullPointerException.class)
    public void testAuthIniNNull() {
        BibtexKeyPatternUtil.authIniN(null, 3);
    }

    @Test
    public void testAuthIniNEmptyReturnsEmpty() {
        assertEquals("", BibtexKeyPatternUtil.authIniN("", 1));
    }

    /**
     * Tests  [auth.auth.ea]
     */
    @Test
    public void authAuthEa() {
        assertEquals("Newton", BibtexKeyPatternUtil.authAuthEa(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1));
        assertEquals("Newton.Maxwell",
                BibtexKeyPatternUtil.authAuthEa(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2));
        assertEquals("Newton.Maxwell.ea",
                BibtexKeyPatternUtil.authAuthEa(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3));
    }

    @Test
    public void testAuthEaEmptyReturnsEmpty() {
        assertEquals("", BibtexKeyPatternUtil.authAuthEa(""));
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
                BibtexKeyPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, delim, append));
        assertEquals("Newton.Maxwell",
                BibtexKeyPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, delim, append));

        // [authEtAl]
        delim = "";
        append = "EtAl";
        assertEquals("NewtonEtAl",
                BibtexKeyPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, delim, append));
        assertEquals("NewtonMaxwell",
                BibtexKeyPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, delim, append));
    }

    /**
     * Test the [authshort] pattern
     */
    @Test
    public void testAuthShort() {
        // tests taken from the comments
        assertEquals("NME+", BibtexKeyPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals("NME", BibtexKeyPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals("NM", BibtexKeyPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("Newton", BibtexKeyPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
    }

    @Test
    public void testAuthShortEmptyReturnsEmpty() {
        assertEquals("", BibtexKeyPatternUtil.authshort(""));
    }

    /**
     * Test the [authN_M] pattern
     */
    @Test
    public void authNM() {
        assertEquals("N", BibtexKeyPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 1, 1));
        assertEquals("Max",
                BibtexKeyPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 3, 2));
        assertEquals("New",
                BibtexKeyPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 3, 1));
        assertEquals("Bo",
                BibtexKeyPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 2, 4));
        assertEquals("Bohr",
                BibtexKeyPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, 6, 4));

        assertEquals("Aal",
                BibtexKeyPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1, 3, 1));
        assertEquals("Less",
                BibtexKeyPatternUtil.authNofMth(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2, 4, 2));

        assertEquals("", BibtexKeyPatternUtil.authNofMth("", 2, 4));
    }

    @Test(expected = NullPointerException.class)
    public void authNMThrowsNPE() {
        BibtexKeyPatternUtil.authNofMth(null, 2, 4);
    }

    /**
     * Tests [authForeIni]
     */
    @Test
    public void firstAuthorForenameInitials() {
        assertEquals("I", BibtexKeyPatternUtil
                .firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("I", BibtexKeyPatternUtil
                .firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("I",
                BibtexKeyPatternUtil.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1));
        assertEquals("I",
                BibtexKeyPatternUtil.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2));
    }

    /**
     * Tests [authFirstFull]
     */
    @Test
    public void firstAuthorVonAndLast() {
        assertEquals("vanderAalst", BibtexKeyPatternUtil
                .firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals("vanderAalst", BibtexKeyPatternUtil
                .firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    @Test
    public void firstAuthorVonAndLastNoVonInName() {
        assertEquals("Newton",
                BibtexKeyPatternUtil.firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1));
        assertEquals("Newton",
                BibtexKeyPatternUtil.firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2));
    }

    /**
     * Tests [authors]
     */
    @Test
    public void testAllAuthors() {
        assertEquals("Newton", BibtexKeyPatternUtil.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("NewtonMaxwell",
                BibtexKeyPatternUtil.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("NewtonMaxwellEinstein",
                BibtexKeyPatternUtil.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
    }

    /**
     * Tests [authorsAlpha]
     */
    @Test
    public void authorsAlpha() {
        assertEquals("New", BibtexKeyPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("NM", BibtexKeyPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("NME", BibtexKeyPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals("NMEB", BibtexKeyPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals("NME+", BibtexKeyPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals("vdAal",
                BibtexKeyPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals("vdAvL",
                BibtexKeyPatternUtil.authorsAlpha(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests [authorLast]
     */
    @Test
    public void lastAuthor() {
        assertEquals("Newton", BibtexKeyPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("Maxwell", BibtexKeyPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("Einstein",
                BibtexKeyPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals("Bohr", BibtexKeyPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals("Unknown", BibtexKeyPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals("Aalst",
                BibtexKeyPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals("Lessen",
                BibtexKeyPatternUtil.lastAuthor(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests [authorLastForeIni]
     */
    @Test
    public void lastAuthorForenameInitials() {
        assertEquals("I",
                BibtexKeyPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("J",
                BibtexKeyPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("A",
                BibtexKeyPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals("N",
                BibtexKeyPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals("H",
                BibtexKeyPatternUtil.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals("W", BibtexKeyPatternUtil
                .lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals("T", BibtexKeyPatternUtil
                .lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests [authorIni]
     */
    @Test
    public void oneAuthorPlusIni() {
        assertEquals("Newto",
                BibtexKeyPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("NewtoM",
                BibtexKeyPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("NewtoME",
                BibtexKeyPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals("NewtoMEB",
                BibtexKeyPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals("NewtoMEBU",
                BibtexKeyPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals("Aalst",
                BibtexKeyPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals("AalstL",
                BibtexKeyPatternUtil.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests the [authorsN] pattern. -> [authors1]
     */
    @Test
    public void testNAuthors1() {
        assertEquals("Newton", BibtexKeyPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 1));
        assertEquals("NewtonEtAl",
                BibtexKeyPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 1));
        assertEquals("NewtonEtAl",
                BibtexKeyPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 1));
        assertEquals("NewtonEtAl",
                BibtexKeyPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 1));
    }

    @Test
    public void testNAuthors1EmptyReturnEmpty() {
        assertEquals("", BibtexKeyPatternUtil.nAuthors("", 1));
    }

    /**
     * Tests the [authorsN] pattern. -> [authors3]
     */
    @Test
    public void testNAuthors3() {
        assertEquals("Newton", BibtexKeyPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 3));
        assertEquals("NewtonMaxwell",
                BibtexKeyPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 3));
        assertEquals("NewtonMaxwellEinstein",
                BibtexKeyPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 3));
        assertEquals("NewtonMaxwellEinsteinEtAl",
                BibtexKeyPatternUtil.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 3));
    }

    @Test
    public void testFirstPage() {
        assertEquals("7", BibtexKeyPatternUtil.firstPage("7--27"));
        assertEquals("27", BibtexKeyPatternUtil.firstPage("--27"));
        assertEquals("", BibtexKeyPatternUtil.firstPage(""));
        assertEquals("42", BibtexKeyPatternUtil.firstPage("42--111"));
        assertEquals("7", BibtexKeyPatternUtil.firstPage("7,41,73--97"));
        assertEquals("7", BibtexKeyPatternUtil.firstPage("41,7,73--97"));
        assertEquals("43", BibtexKeyPatternUtil.firstPage("43+"));
    }

    @Test(expected = NullPointerException.class)
    public void testFirstPageNull() {
        BibtexKeyPatternUtil.firstPage(null);
    }

    @Test
    public void testLastPage() {

        assertEquals("27", BibtexKeyPatternUtil.lastPage("7--27"));
        assertEquals("27", BibtexKeyPatternUtil.lastPage("--27"));
        assertEquals("", BibtexKeyPatternUtil.lastPage(""));
        assertEquals("111", BibtexKeyPatternUtil.lastPage("42--111"));
        assertEquals("97", BibtexKeyPatternUtil.lastPage("7,41,73--97"));
        assertEquals("97", BibtexKeyPatternUtil.lastPage("7,41,97--73"));
        assertEquals("43", BibtexKeyPatternUtil.lastPage("43+"));
    }

    @Test(expected = NullPointerException.class)
    public void testLastPageNull() {
        BibtexKeyPatternUtil.lastPage(null);
    }

    /**
     * Tests [veryShortTitle]
     */
    @Test
    public void veryShortTitle() {
        // veryShortTitle is getTitleWords with "1" as count
        int count = 1;
        assertEquals("application",
                BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH));
        assertEquals("BPEL", BibtexKeyPatternUtil.getTitleWords(count,
                TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals("Process", BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_CASED));
        assertEquals("BPMN",
                BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD));
        assertEquals("Difference", BibtexKeyPatternUtil.getTitleWords(count,
                TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING));
        assertEquals("Cloud",
                BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals("Towards",
                BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD));
        assertEquals("Measurement",
                BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS));
    }

    /**
     * Tests [shortTitle]
     */
    @Test
    public void shortTitle() {
        // veryShortTitle is getTitleWords with "3" as count
        int count = 3;
        assertEquals("applicationmigrationeffort",
                BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH));
        assertEquals("BPELconformanceopen", BibtexKeyPatternUtil.getTitleWords(count,
                TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals("ProcessViewingPatterns", BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_CASED));
        assertEquals("BPMNConformanceOpen",
                BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD));
        assertEquals("DifferenceGraphBased", BibtexKeyPatternUtil.getTitleWords(count,
                TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING));
        assertEquals("CloudComputingNext",
                BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals("TowardsChoreographybased",
                BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD));
        assertEquals("MeasurementDesignTime",
                BibtexKeyPatternUtil.getTitleWords(count, TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS));
    }

    @Test
    public void keywordNKeywordsSeparatedBySpace() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "w1, w2a w2b, w3");

        String result = BibtexKeyPatternUtil.makeLabel(entry, "keyword1", ',');
        assertEquals("w1", result);

        // check keywords with space
        result = BibtexKeyPatternUtil.makeLabel(entry, "keyword2", ',');
        assertEquals("w2a w2b", result);

        // check out of range
        result = BibtexKeyPatternUtil.makeLabel(entry, "keyword4", ',');
        assertEquals("", result);
    }

    @Test
    public void keywordsNKeywordsSeparatedBySpace() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "w1, w2a w2b, w3");

        // all keywords
        String result = BibtexKeyPatternUtil.makeLabel(entry, "keywords", ',');
        assertEquals("w1w2aw2bw3", result);

        // check keywords with space
        result = BibtexKeyPatternUtil.makeLabel(entry, "keywords2", ',');
        assertEquals("w1w2aw2b", result);

        // check out of range
        result = BibtexKeyPatternUtil.makeLabel(entry, "keywords55", ',');
        assertEquals("w1w2aw2bw3", result);
    }

    @Test
    public void testCheckLegalKeyEnforceLegal() {
        assertEquals("AAAA", BibtexKeyPatternUtil.checkLegalKey("AA AA", true));
        assertEquals("SPECIALCHARS", BibtexKeyPatternUtil.checkLegalKey("SPECIAL CHARS#{\\\"}~,^", true));
        assertEquals("", BibtexKeyPatternUtil.checkLegalKey("\n\t\r", true));
    }

    @Test
    public void testCheckLegalKeyDoNotEnforceLegal() {
        assertEquals("AAAA", BibtexKeyPatternUtil.checkLegalKey("AA AA", false));
        assertEquals("SPECIALCHARS#~^", BibtexKeyPatternUtil.checkLegalKey("SPECIAL CHARS#{\\\"}~,^", false));
        assertEquals("", BibtexKeyPatternUtil.checkLegalKey("\n\t\r", false));
    }

    @Test
    public void testCheckLegalNullInNullOut() {
        assertNull(BibtexKeyPatternUtil.checkLegalKey(null, true));
        assertNull(BibtexKeyPatternUtil.checkLegalKey(null, false));
    }

    @Test
    public void testApplyModifiers() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "Green Scheduling of Whatever");
        assertEquals("GSW", BibtexKeyPatternUtil.makeLabel(entry, "shorttitleINI", ','));
        assertEquals("GreenSchedulingWhatever", BibtexKeyPatternUtil.makeLabel(entry, "shorttitle",
                ','));
    }

}
