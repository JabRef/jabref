package tests.net.sf.jabref.labelPattern;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.labelPattern.LabelPatternUtil;

public class LabelPatternUtilTest extends TestCase {

    /**
     * Test for https://sourceforge.net/forum/message.php?msg_id=4498555
     */
    public void testMakeLabel() {

        BibtexEntry entry = BibtexParser
            .singleFromString("@ARTICLE{kohn, author={Andreas K{\\\"o}ning}, year={2000}}");

        assertEquals("Kon", LabelPatternUtil.makeLabel(entry, "auth3"));
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
