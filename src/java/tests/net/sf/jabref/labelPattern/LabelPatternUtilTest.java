package tests.net.sf.jabref.labelPattern;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.labelPattern.LabelPatternUtil;

public class LabelPatternUtilTest extends TestCase {


    /**
     * Test for https://sourceforge.net/forum/message.php?msg_id=4498555
     * Test the Labelmaker and all kind of accents
     * Á á Ć ć É é Í í Ĺ ĺ Ń ń Ó ó Ŕ ŕ Ś ś Ú ú Ý ý Ź ź  
     */
    public void testMakeLabelAndCheckLegalKeys() {
    	BibtexEntry entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Köning}, year={2000}}");
    	assertEquals("Koen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Áöning}, year={2000}}");
    	assertEquals("Aoen",net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    			
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Éöning}, year={2000}}");
    	assertEquals("Eoen",net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Íöning}, year={2000}}");
    	assertEquals("Ioen",net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ĺöning}, year={2000}}");
    	assertEquals("Loen",net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ńöning}, year={2000}}");
    	assertEquals("Noen",net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Óöning}, year={2000}}");
    	assertEquals("Ooen",net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ŕöning}, year={2000}}");
    	assertEquals("Roen",net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Śöning}, year={2000}}");
    	assertEquals("Soen",net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Úöning}, year={2000}}");
    	assertEquals("Uoen",net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ýöning}, year={2000}}");
    	assertEquals("Yoen",net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Źöning}, year={2000}}");
    	assertEquals("Zoen",net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    }
    
    /**
     * Test the Labelmaker and with accent grave
     * Chars to test: "ÀÈÌÒÙ";
     */
    public void testMakeLabelAndCheckLegalKeysAccentGrave() {
    	BibtexEntry entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Àöning}, year={2000}}");
    	assertEquals("Aoen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Èöning}, year={2000}}");
    	assertEquals("Eoen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ìöning}, year={2000}}");
    	assertEquals("Ioen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Òöning}, year={2000}}");
    	assertEquals("Ooen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    	
    	entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ùöning}, year={2000}}");
    	assertEquals("Uoen", net.sf.jabref.Util.checkLegalKey(LabelPatternUtil.makeLabel(entry0, "auth3")));
    }
    
    /**
     * Tests if checkLegalKey replaces Non-ASCII chars.
     * There are quite a few chars that should be replaced. Perhaps there is a better method than the current.
     * @see net.sf.jabref.Util.checkLegalKey
     */
    public void testCheckLegalKey(){
    	// not tested/ not in hashmap UNICODE_CHARS:
    	// Ł ł   Ő ő Ű ű   Ŀ ŀ   Ħ ħ   Ð ð Þ þ   Œ œ   Æ æ Ø ø Å å   Ə ə Đ đ   Ů ů	Ǣ ǣ ǖ ǘ ǚ ǜ   
    	//" Ǣ ǣ ǖ ǘ ǚ ǜ   " +
    	//"Đ đ   Ů ů  " +
    	//"Ł ł   Ő ő Ű ű   Ŀ ŀ   Ħ ħ   Ð ð Þ þ   Œ œ   Æ æ Ø ø Å å   Ə ə
    	String accents =			"ÀàÈèÌìÒòÙù Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ";
    	String expectedResult = 	"AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYy";
    	assertEquals( expectedResult, net.sf.jabref.Util.checkLegalKey(accents));
    	
    	accents = 			"ÄäËëÏïÖöÜüŸÿ";
    	expectedResult = 	"AeaeEeIiOeoeUeueYy";
    	assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));
    		
    	accents = 			"Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ";
    	expectedResult = 	"CcGgKkLlNnRrSsTt";
    	assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));
    	
    	accents = 			"Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ";  
    	expectedResult = 	"AaEeGgIiOoUu";
    	assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));
    	
    	accents = 			"Ċ ċ Ė ė Ġ ġ İ ı Ż ż";
    	expectedResult = 	"CcEeGgIiZz";
    	assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));
    		
    	accents = 			"Ą ą Ę ę Į į Ǫ ǫ Ų ų";
    	expectedResult = 	"AaEeIiOoUu"; // O or Q? o or q? 
    	assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));  	
     	
    	accents = 			"Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ";
    	expectedResult = 	"AaEeIiOoUuYy";
    	assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

    	accents = 			"Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž";
    	expectedResult = 	"AaCcDdEeIiLlNnOoRrSsTtUuZz";
    	assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

    	expectedResult = 	"AaEeIiNnOoUuYy";
    	accents = 			"ÃãẼẽĨĩÑñÕõŨũỸỹ";
    	assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

    	accents = 			"Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ";
    	expectedResult = 	"DdHhLlLlMmNnRrRrSsTt";
    	assertEquals(expectedResult, net.sf.jabref.Util.checkLegalKey(accents));

    	String totest =	"À à È è Ì ì Ò ò Ù ù   Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ  Ä ä Ë ë Ï ï Ö ö Ü ü Ÿ ÿ    " +
    	"Ã ã Ẽ ẽ Ĩ ĩ Ñ ñ Õ õ Ũ ũ Ỹ ỹ   Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ" +
    	" Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž   " +
    	"Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ" +
    	"Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ   " +
    	"Ċ ċ Ė ė Ġ ġ İ ı Ż ż   Ą ą Ę ę Į į Ǫ ǫ Ų ų   " +
    	"Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ   " ;
    	String expectedResults =	"AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYyAeaeEeIiOeoeUeueYy" +
    	"AaEeIiNnOoUuYyCcGgKkLlNnRrSsTt" +
    	"AaCcDdEeIiLlNnOoRrSsTtUuZz" +
    	"AaEeIiOoUuYy" +
    	"AaEeGgIiOoUu" +
    	"CcEeGgIiZzAaEeIiOoUu" +
    	"DdHhLlLlMmNnRrRrSsTt";
    	assertEquals(expectedResults, net.sf.jabref.Util.checkLegalKey(totest));
    }
    
    public void testFirstAuthor() {
        assertEquals(
            "Newton",
            LabelPatternUtil
                .firstAuthor("I. Newton and J. Maxwell and A. Einstein and N. Bohr and Harry Unknown"));
        assertEquals("Newton", LabelPatternUtil.firstAuthor("I. Newton"));

        // https://sourceforge.net/forum/message.php?msg_id=4498555
        assertEquals("K{\\\"o}ning", LabelPatternUtil
            .firstAuthor("K{\\\"o}ning"));

        assertEquals("", LabelPatternUtil.firstAuthor(""));

        try {
            LabelPatternUtil.firstAuthor(null);
            fail();
        } catch (NullPointerException e) {

        }
    }

    public void testAuthIniN() {
        assertEquals(
            "NMEB",
            LabelPatternUtil
                .authIniN(
                    "I. Newton and J. Maxwell and A. Einstein and N. Bohr and Harry Unknown",
                    4));
        assertEquals("NMEB", LabelPatternUtil.authIniN(
            "I. Newton and J. Maxwell and A. Einstein and N. Bohr", 4));
        assertEquals("NeME", LabelPatternUtil.authIniN(
            "I. Newton and J. Maxwell and A. Einstein", 4));
        assertEquals("NeMa", LabelPatternUtil.authIniN(
            "I. Newton and J. Maxwell", 4));
        assertEquals("Newt", LabelPatternUtil.authIniN("I. Newton", 4));
        assertEquals("", "");

        assertEquals("N", LabelPatternUtil.authIniN("I. Newton", 1));
        assertEquals("", LabelPatternUtil.authIniN("I. Newton", 0));
        assertEquals("", LabelPatternUtil.authIniN("I. Newton", -1));

        assertEquals("Newton", LabelPatternUtil.authIniN("I. Newton", 6));
        assertEquals("Newton", LabelPatternUtil.authIniN("I. Newton", 7));

        try {
            LabelPatternUtil.authIniN(null, 3);
            fail();
        } catch (NullPointerException e) {

        }
    }

    public void testFirstPage() {
        assertEquals("7", LabelPatternUtil.firstPage("7--27"));
        assertEquals("27", LabelPatternUtil.firstPage("--27"));
        assertEquals("", LabelPatternUtil.firstPage(""));
        assertEquals("42", LabelPatternUtil.firstPage("42--111"));
        assertEquals("7", LabelPatternUtil.firstPage("7,41,73--97"));
        assertEquals("7", LabelPatternUtil.firstPage("41,7,73--97"));
        assertEquals("43", LabelPatternUtil.firstPage("43+"));

        try {
            LabelPatternUtil.firstPage(null);
            fail();
        } catch (NullPointerException e) {

        }
    }

    public void testLastPage() {

        assertEquals("27", LabelPatternUtil.lastPage("7--27"));
        assertEquals("27", LabelPatternUtil.lastPage("--27"));
        assertEquals("", LabelPatternUtil.lastPage(""));
        assertEquals("111", LabelPatternUtil.lastPage("42--111"));
        assertEquals("97", LabelPatternUtil.lastPage("7,41,73--97"));
        assertEquals("97", LabelPatternUtil.lastPage("7,41,97--73"));
        assertEquals("43", LabelPatternUtil.lastPage("43+"));
        try {
            LabelPatternUtil.lastPage(null);
            fail();
        } catch (NullPointerException e) {

        }
    }

}
