package net.sf.jabref.labelPattern;

import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Util;
import net.sf.jabref.imports.BibtexParser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LabelPatternUtilTest {

    @Before
    public void setUp() {
        LabelPatternUtil.setDataBase(new BibtexDatabase());
    }

    @Test
    public void testAndInAuthorName() {
        BibtexEntry entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Simon Holland}}");
        Assert.assertEquals("Holland", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth")));
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
     * �? á Ć ć É é �? í Ĺ ĺ Ń ń Ó ó Ŕ ŕ Ś ś Ú ú �? ý Ź ź
     */
    @Test
    public void testMakeLabelAndCheckLegalKeys() {
        BibtexEntry entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Köning}, year={2000}}");
        Assert.assertEquals("Koen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas �?öning}, year={2000}}");
        Assert.assertEquals("Aoen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Éöning}, year={2000}}");
        Assert.assertEquals("Eoen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas �?öning}, year={2000}}");
        Assert.assertEquals("Ioen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ĺöning}, year={2000}}");
        Assert.assertEquals("Loen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ńöning}, year={2000}}");
        Assert.assertEquals("Noen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Óöning}, year={2000}}");
        Assert.assertEquals("Ooen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ŕöning}, year={2000}}");
        Assert.assertEquals("Roen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Śöning}, year={2000}}");
        Assert.assertEquals("Soen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Úöning}, year={2000}}");
        Assert.assertEquals("Uoen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas �?öning}, year={2000}}");
        Assert.assertEquals("Yoen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Źöning}, year={2000}}");
        Assert.assertEquals("Zoen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    }

    /**
     * Test the Labelmaker and with accent grave
     * Chars to test: "ÀÈÌÒÙ";
     */
    @Test
    public void testMakeLabelAndCheckLegalKeysAccentGrave() {
        BibtexEntry entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Àöning}, year={2000}}");
        Assert.assertEquals("Aoen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Èöning}, year={2000}}");
        Assert.assertEquals("Eoen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ìöning}, year={2000}}");
        Assert.assertEquals("Ioen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Òöning}, year={2000}}");
        Assert.assertEquals("Ooen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ùöning}, year={2000}}");
        Assert.assertEquals("Uoen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    }

    /**
     * Tests if checkLegalKey replaces Non-ASCII chars.
     * There are quite a few chars that should be replaced. Perhaps there is a better method than the current.
     *
     * @see net.sf.jabref.Util#checkLegalKey(String)
     */
    @Test
    public void testCheckLegalKey() {
        // not tested/ not in hashmap UNICODE_CHARS:
        // �? ł   �? ő Ű ű   Ŀ ŀ   Ħ ħ   �? ð Þ þ   Œ œ   Æ æ Ø ø Å å   �? ə �? đ   Ů ů	Ǣ ǣ ǖ ǘ ǚ ǜ
        //" Ǣ ǣ ǖ ǘ ǚ ǜ   " +
        //"�? đ   Ů ů  " +
        //"�? ł   �? ő Ű ű   Ŀ ŀ   Ħ ħ   �? ð Þ þ   Œ œ   Æ æ Ø ø Å å   �? ə
        String accents = "ÀàÈèÌìÒòÙù Â â Ĉ ĉ Ê ê Ĝ �? Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ �? Û û Ŵ ŵ Ŷ ŷ";
        String expectedResult = "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYy";
        Assert.assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

        accents = "ÄäËë�?ïÖöÜüŸÿ";
        expectedResult = "AeaeEeIiOeoeUeueYy";
        Assert.assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

        accents = "Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ";
        expectedResult = "CcGgKkLlNnRrSsTt";
        Assert.assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

        accents = "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ �? Ŭ ŭ";
        expectedResult = "AaEeGgIiOoUu";
        Assert.assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

        accents = "Ċ ċ Ė ė Ġ ġ İ ı Ż ż";
        expectedResult = "CcEeGgIiZz";
        Assert.assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

        accents = "Ą ą Ę ę Į į Ǫ ǫ Ų ų";
        expectedResult = "AaEeIiOoUu"; // O or Q? o or q?
        Assert.assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

        accents = "Ā �? Ē ē Ī ī Ō �? Ū ū Ȳ ȳ";
        expectedResult = "AaEeIiOoUuYy";
        Assert.assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

        accents = "�? ǎ Č �? Ď �? Ě ě �? �? Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž";
        expectedResult = "AaCcDdEeIiLlNnOoRrSsTtUuZz";
        Assert.assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

        expectedResult = "AaEeIiNnOoUuYy";
        accents = "ÃãẼẽĨĩÑñÕõŨũỸỹ";
        Assert.assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

        accents = "Ḍ �? Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ �? Ṣ ṣ Ṭ ṭ";
        expectedResult = "DdHhLlLlMmNnRrRrSsTt";
        Assert.assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

        String totest = "À à È è Ì ì Ò ò Ù ù   Â â Ĉ ĉ Ê ê Ĝ �? Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ �? Û û Ŵ ŵ Ŷ ŷ  Ä ä Ë ë �? ï Ö ö Ü ü Ÿ ÿ    " +
                "Ã ã Ẽ ẽ Ĩ ĩ Ñ ñ Õ õ Ũ ũ Ỹ ỹ   Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ" +
                " �? ǎ Č �? Ď �? Ě ě �? �? Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž   " +
                "Ā �? Ē ē Ī ī Ō �? Ū ū Ȳ ȳ" +
                "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ �? Ŭ ŭ   " +
                "Ċ ċ Ė ė Ġ ġ İ ı Ż ż   Ą ą Ę ę Į į Ǫ ǫ Ų ų   " +
                "Ḍ �? Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ �? Ṣ ṣ Ṭ ṭ   ";
        String expectedResults = "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYyAeaeEeIiOeoeUeueYy" +
                "AaEeIiNnOoUuYyCcGgKkLlNnRrSsTt" +
                "AaCcDdEeIiLlNnOoRrSsTtUuZz" +
                "AaEeIiOoUuYy" +
                "AaEeGgIiOoUu" +
                "CcEeGgIiZzAaEeIiOoUu" +
                "DdHhLlLlMmNnRrRrSsTt";
        Assert.assertEquals(expectedResults, net.sf.jabref.Util.checkLegalKey(totest));
    }

    @Test
    public void testFirstAuthor() {
        Assert.assertEquals(
                "Newton",
                LabelPatternUtil
                        .firstAuthor("I. Newton and J. Maxwell and A. Einstein and N. Bohr and Harry Unknown"));
        Assert.assertEquals("Newton", LabelPatternUtil.firstAuthor("I. Newton"));

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
                                "I. Newton and J. Maxwell and A. Einstein and N. Bohr and Harry Unknown",
                                4));
        Assert.assertEquals("NMEB", LabelPatternUtil.authIniN(
                "I. Newton and J. Maxwell and A. Einstein and N. Bohr", 4));
        Assert.assertEquals("NeME", LabelPatternUtil.authIniN(
                "I. Newton and J. Maxwell and A. Einstein", 4));
        Assert.assertEquals("NeMa", LabelPatternUtil.authIniN(
                "I. Newton and J. Maxwell", 4));
        Assert.assertEquals("Newt", LabelPatternUtil.authIniN("I. Newton", 4));
        Assert.assertEquals("", "");

        Assert.assertEquals("N", LabelPatternUtil.authIniN("I. Newton", 1));
        Assert.assertEquals("", LabelPatternUtil.authIniN("I. Newton", 0));
        Assert.assertEquals("", LabelPatternUtil.authIniN("I. Newton", -1));

        Assert.assertEquals("Newton", LabelPatternUtil.authIniN("I. Newton", 6));
        Assert.assertEquals("Newton", LabelPatternUtil.authIniN("I. Newton", 7));

        try {
            LabelPatternUtil.authIniN(null, 3);
            Assert.fail();
        } catch (NullPointerException ignored) {

        }
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
