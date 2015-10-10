package net.sf.jabref.logic.labelPattern;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.util.Util;

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

    @BeforeClass
    public static void setUpGlobalsPrefs() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Before
    public void setUp() {
        LabelPatternUtil.setDataBase(new BibtexDatabase());
    }

    @Test
    public void testAndInAuthorName() {
        BibtexEntry entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Simon Holland}}");
        Assert.assertEquals("Holland", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth")));
    }

    @Test
    public void testAndAuthorNames() {
        String bibtexString = "@ARTICLE{whatevery, author={Mari D. Herland and Mona-Iren Hauge and Ingeborg M. Helgeland}}";
        BibtexEntry entry = BibtexParser.singleFromString(bibtexString);
        Assert.assertEquals("HerlandHaugeHelgeland", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry, "authors3")));
    }

    /**
     * Test for https://sourceforge.net/forum/message.php?msg_id=4498555
     * Test the Labelmaker and all kind of accents
     * Á á Ć ć É é Í í Ĺ ĺ Ń ń Ó ó Ŕ ŕ Ś ś Ú ú Ý ý Ź ź
     */
    @Test
    public void testMakeLabelAndCheckLegalKeys() {
        BibtexEntry entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Köning}, year={2000}}");
        Assert.assertEquals("Koen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Áöning}, year={2000}}");
        Assert.assertEquals("Aoen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Éöning}, year={2000}}");
        Assert.assertEquals("Eoen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Íöning}, year={2000}}");
        Assert.assertEquals("Ioen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ĺöning}, year={2000}}");
        Assert.assertEquals("Loen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ńöning}, year={2000}}");
        Assert.assertEquals("Noen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Óöning}, year={2000}}");
        Assert.assertEquals("Ooen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ŕöning}, year={2000}}");
        Assert.assertEquals("Roen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Śöning}, year={2000}}");
        Assert.assertEquals("Soen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Úöning}, year={2000}}");
        Assert.assertEquals("Uoen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ýöning}, year={2000}}");
        Assert.assertEquals("Yoen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Źöning}, year={2000}}");
        Assert.assertEquals("Zoen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    }

    /**
     * Test the Labelmaker and with accent grave
     * Chars to test: "ÀÈÌÒÙ";
     */
    @Test
    public void testMakeLabelAndCheckLegalKeysAccentGrave() {
        BibtexEntry entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Àöning}, year={2000}}");
        Assert.assertEquals("Aoen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Èöning}, year={2000}}");
        Assert.assertEquals("Eoen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ìöning}, year={2000}}");
        Assert.assertEquals("Ioen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Òöning}, year={2000}}");
        Assert.assertEquals("Ooen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ùöning}, year={2000}}");
        Assert.assertEquals("Uoen", Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    }

    /**
     * Tests if checkLegalKey replaces Non-ASCII chars.
     * There are quite a few chars that should be replaced. Perhaps there is a better method than the current.
     *
     * @see Util#checkLegalKey(String)
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
        assertEquals(expectedResult, Util.checkLegalKey(accents));

        accents = "ÄäËëÏïÖöÜüŸÿ";
        expectedResult = "AeaeEeIiOeoeUeueYy";
        assertEquals(expectedResult, Util.checkLegalKey(accents));

        accents = "Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ";
        expectedResult = "CcGgKkLlNnRrSsTt";
        assertEquals(expectedResult, Util.checkLegalKey(accents));

        accents = "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ";
        expectedResult = "AaEeGgIiOoUu";
        assertEquals(expectedResult, Util.checkLegalKey(accents));

        accents = "Ċ ċ Ė ė Ġ ġ İ ı Ż ż";
        expectedResult = "CcEeGgIiZz";
        assertEquals(expectedResult, Util.checkLegalKey(accents));

        accents = "Ą ą Ę ę Į į Ǫ ǫ Ų ų";
        expectedResult = "AaEeIiOoUu"; // O or Q? o or q?
        assertEquals(expectedResult, Util.checkLegalKey(accents));

        accents = "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ";
        expectedResult = "AaEeIiOoUuYy";
        assertEquals(expectedResult, Util.checkLegalKey(accents));

        accents = "Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž";
        expectedResult = "AaCcDdEeIiLlNnOoRrSsTtUuZz";
        assertEquals(expectedResult, Util.checkLegalKey(accents));

        expectedResult = "AaEeIiNnOoUuYy";
        accents = "ÃãẼẽĨĩÑñÕõŨũỸỹ";
        assertEquals(expectedResult, Util.checkLegalKey(accents));

        accents = "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ";
        expectedResult = "DdHhLlLlMmNnRrRrSsTt";
        assertEquals(expectedResult, Util.checkLegalKey(accents));

        String totest = "À à È è Ì ì Ò ò Ù ù   Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ  Ä ä Ë ë Ï ï Ö ö Ü ü Ÿ ÿ    " +
                "Ã ã Ẽ ẽ Ĩ ĩ Ñ ñ Õ õ Ũ ũ Ỹ ỹ   Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ" +
                " Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž   " +
                "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ" +
                "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ   " +
                "Ċ ċ Ė ė Ġ ġ İ ı Ż ż   Ą ą Ę ę Į į Ǫ ǫ Ų ų   " +
                "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ   ";
        String expectedResults = "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYyAeaeEeIiOeoeUeueYy" +
                "AaEeIiNnOoUuYyCcGgKkLlNnRrSsTt" +
                "AaCcDdEeIiLlNnOoRrSsTtUuZz" +
                "AaEeIiOoUuYy" +
                "AaEeGgIiOoUu" +
                "CcEeGgIiZzAaEeIiOoUu" +
                "DdHhLlLlMmNnRrRrSsTt";
        assertEquals(expectedResults, Util.checkLegalKey(totest));
    }

    @Test
    public void testFirstAuthor() {
        Assert.assertEquals(
                "Newton",
                LabelPatternUtil
                        .firstAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));
        Assert.assertEquals("Newton", LabelPatternUtil.firstAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));

        // https://sourceforge.net/forum/message.php?msg_id=4498555
        Assert.assertEquals("K{\\\"o}ning", LabelPatternUtil
                .firstAuthor("K{\\\"o}ning"));

        Assert.assertEquals("", LabelPatternUtil.firstAuthor(""));

        try {
            LabelPatternUtil.firstAuthor(null);
            Assert.fail();
        } catch (NullPointerException ignored) {

        }
    }

    @Test
    public void testAuthIniN() {
        Assert.assertEquals(
                "NMEB",
                LabelPatternUtil
                        .authIniN(
                                AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5,
                                4));
        Assert.assertEquals("NMEB", LabelPatternUtil.authIniN(
                AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 4));
        Assert.assertEquals("NeME", LabelPatternUtil.authIniN(
                AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 4));
        Assert.assertEquals("NeMa", LabelPatternUtil.authIniN(
                AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 4));
        Assert.assertEquals("Newt", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 4));
        Assert.assertEquals("", "");

        Assert.assertEquals("N", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 1));
        Assert.assertEquals("", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 0));
        Assert.assertEquals("", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, -1));

        Assert.assertEquals("Newton", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 6));
        Assert.assertEquals("Newton", LabelPatternUtil.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 7));

        try {
            LabelPatternUtil.authIniN(null, 3);
            Assert.fail();
        } catch (NullPointerException ignored) {

        }
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
        Assert.assertEquals("Newton.etal", LabelPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, delim, append));
        Assert.assertEquals("Newton.Maxwell", LabelPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, delim, append));

        // [authEtAl]
        delim = "";
        append = "EtAl";
        Assert.assertEquals("NewtonEtAl", LabelPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, delim, append));
        Assert.assertEquals("NewtonMaxwell", LabelPatternUtil.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, delim, append));
    }

    /**
     * Test the [authshort] pattern
     */
    @Test
    public void testAuthShort() {
        // tests taken from the comments
        Assert.assertEquals(
                "NME+",
                LabelPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        Assert.assertEquals(
                "NME",
                LabelPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        Assert.assertEquals(
                "NM",
                LabelPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        Assert.assertEquals(
                "Newton",
                LabelPatternUtil.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
    }

    /**
     * Tests [authForeIni]
     */
    @Test
    public void firstAuthorForenameInitials() {
        Assert.assertEquals(
                "I",
                LabelPatternUtil.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        Assert.assertEquals(
                "I",
                LabelPatternUtil.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        Assert.assertEquals(
                "I",
                LabelPatternUtil.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1));
        Assert.assertEquals(
                "I",
                LabelPatternUtil.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2));
    }

    /**
     * Tests [authFirstFull]
     */
    @Test
    public void firstAuthorVonAndLast() {
        Assert.assertEquals(
                "vanderAalst",
                LabelPatternUtil.firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        Assert.assertEquals(
                "vanderAalst",
                LabelPatternUtil.firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests [authors]
     */
    @Test
    public void testAllAuthors() {
        Assert.assertEquals(
                "Newton",
                LabelPatternUtil.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        Assert.assertEquals(
                "NewtonMaxwell",
                LabelPatternUtil.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        Assert.assertEquals(
                "NewtonMaxwellEinstein",
                LabelPatternUtil.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
    }

    /**
     * Tests the [authorsN] pattern.
     */
    @Test
    public void testNAuthors() {
        // test [authors3]
        Assert.assertEquals(
                "Newton",
                LabelPatternUtil.NAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 3));
        Assert.assertEquals(
                "NewtonMaxwell",
                LabelPatternUtil.NAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 3));
        Assert.assertEquals(
                "NewtonMaxwellEinstein",
                LabelPatternUtil.NAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 3));
        Assert.assertEquals(
                "NewtonMaxwellEinsteinEtAl",
                LabelPatternUtil.NAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 3));
    }

    @Test
    public void testFirstPage() {
        Assert.assertEquals("7", LabelPatternUtil.firstPage("7--27"));
        Assert.assertEquals("27", LabelPatternUtil.firstPage("--27"));
        Assert.assertEquals("", LabelPatternUtil.firstPage(""));
        Assert.assertEquals("42", LabelPatternUtil.firstPage("42--111"));
        Assert.assertEquals("7", LabelPatternUtil.firstPage("7,41,73--97"));
        Assert.assertEquals("7", LabelPatternUtil.firstPage("41,7,73--97"));
        Assert.assertEquals("43", LabelPatternUtil.firstPage("43+"));

        try {
            LabelPatternUtil.firstPage(null);
            Assert.fail();
        } catch (NullPointerException ignored) {

        }
    }

    @Test
    public void testLastPage() {

        Assert.assertEquals("27", LabelPatternUtil.lastPage("7--27"));
        Assert.assertEquals("27", LabelPatternUtil.lastPage("--27"));
        Assert.assertEquals("", LabelPatternUtil.lastPage(""));
        Assert.assertEquals("111", LabelPatternUtil.lastPage("42--111"));
        Assert.assertEquals("97", LabelPatternUtil.lastPage("7,41,73--97"));
        Assert.assertEquals("97", LabelPatternUtil.lastPage("7,41,97--73"));
        Assert.assertEquals("43", LabelPatternUtil.lastPage("43+"));
        try {
            LabelPatternUtil.lastPage(null);
            Assert.fail();
        } catch (NullPointerException ignored) {

        }
    }

}
