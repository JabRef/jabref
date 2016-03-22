package net.sf.jabref.logic.layout;

import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.regex.Pattern;
import static org.mockito.Mockito.*;

/**
 * The test class LayoutEntryTest test the net.sf.jabref.export.layout.LayoutEntry.
 * Indirectly the net.sf.jabref.export.layout.Layout is tested too.
 * <p/>
 * The LayoutEntry creates a human readable String assigned with HTML formatters.
 * To test the Highlighting Feature, an instance of LayoutEntry will be instantiated via Layout and LayoutHelper.
 * With these instance the doLayout() Method is called several times for each test case.
 * To simulate a search, a BibEntry will be created, which will be used by LayoutEntry.
 * The definition of the search is set by
 * <p/>
 * LayoutEntry.setWordsToHighlight(words); and
 * Globals.prefs.putBoolean("caseSensitiveSearch", false);
 * <p/>
 * There are five test cases:
 * - The shown result text has no words which should be highlighted.
 * - There is one word which will be highlighted ignoring case sensitivity.
 * - There are two words which will be highlighted ignoring case sensitivity.
 * - There is one word which will be highlighted case sensitivity.
 * - There are more words which will be highlighted case sensitivity.
 *
 * @author Arne
 */

public class LayoutEntryTest {

    private BibEntry mBTE;


    /**
     * Initialize Preferences.
     */
    @Before
    public void setUp() {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
            Globals.prefs.putBoolean("highLightWords", Boolean.TRUE);
        }

        // create Bibtext Entry

        mBTE = new BibEntry("testid");
        mBTE.setField("abstract", "In this paper, we initiate a formal study of security on Android: Google's new open-source platform for mobile devices. Tags: Paper android google Open-Source Devices");
        //  Specifically, we present a core typed language to describe Android applications, and to reason about their data-flow security properties. Our operational semantics and type system provide some necessary foundations to help both users and developers of Android applications deal with their security concerns.
        mBTE.setField("keywords", "android, mobile devices, security");
        mBTE.setField("posted-at", "2010-08-11 15:00:49");
        mBTE.setField("location", "Dublin, Ireland");
        mBTE.setField("bibtexkey", "chaudhuri-plas09");
        mBTE.setField("pages", "1--7");
        mBTE.setField("booktitle", "PLAS '09: Proceedings of the ACM SIGPLAN Fourth Workshop on Programming Languages and Analysis for Security");
        mBTE.setField("citeulike-article-id", "7615801");
        mBTE.setField("citeulike-linkout-1", "http://dx.doi.org/10.1145/1554339.1554341");
        mBTE.setField("url", "http://dx.doi.org/10.1145/1554339.1554341");
        mBTE.setField("publisher", "ACM");
        mBTE.setField("timestamp", "2010.11.11");
        mBTE.setField("author", "Chaudhuri, Avik");
        mBTE.setField("title", "Language-based security on Android");
        mBTE.setField("address", "New York, NY, USA");
        mBTE.setField("priority", "2");
        mBTE.setField("isbn", "978-1-60558-645-8");
        mBTE.setField("owner", "Arne");
        mBTE.setField("year", "2009");
        mBTE.setField("citeulike-linkout-0", "http://portal.acm.org/citation.cfm?id=1554339.1554341");
        mBTE.setField("doi", "10.1145/1554339.1554341");
    }

    // helper Methods

    public String layout(String layoutFile, BibEntry entry, Optional<Pattern> highlightPattern) throws IOException {
        StringReader sr = new StringReader(layoutFile.replace("__NEWLINE__", "\n"));
        Layout layout = new LayoutHelper(sr, mock(JournalAbbreviationRepository.class)).getLayoutFromText();

        return layout.doLayout(entry, null, highlightPattern);
    }

    /*************************/
    /****** tests Cases ******/
    /*************************/

    /**
     * @throws Exception
     */
    @Test
    @Ignore
    public void testNoHighlighting() throws IOException {
        // say that this bibtex object was found
        mBTE.setSearchHit(true);

        String result = this.layout("<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>", mBTE, Optional.empty());
        String expecting = "<font face=\"arial\"><BR><BR><b>Abstract: </b> In this paper, we initiate a formal study of security on Android: Google's new open-source platform for mobile devices. Tags: Paper android google Open-Source Devices</font>";

        Assert.assertEquals(expecting, result);
    }

    /**
     * @throws Exception
     */
    @Test
    public void testHighlightingOneWordCaseInsesitive() throws IOException {
        // say that this bibtex object was found
        mBTE.setSearchHit(true);

        Optional<Pattern> highlightPattern = Optional.of(Pattern.compile("(google)", Pattern.CASE_INSENSITIVE));

        String result = this.layout("<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>", mBTE, highlightPattern);
        String containing = "<span style=\"background-color:#3399FF;\">Google</span>";

        // check
        Assert.assertTrue("Actual message: " + result, result.contains(containing));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testHighlightingTwoWordsCaseInsesitive() throws IOException {
        // say that this bibtex object was found
        mBTE.setSearchHit(true);

        Optional<Pattern> highlightPattern = Optional.of(Pattern.compile("(Android|study)", Pattern.CASE_INSENSITIVE));

        String result = this.layout("<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>", mBTE, highlightPattern);

        String containing = "<span style=\"background-color:#3399FF;\">Android</span>";
        String containing2 = "<span style=\"background-color:#3399FF;\">study</span>";

        // check
        Assert.assertTrue(result.contains(containing));
        Assert.assertTrue(result.contains(containing2));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testHighlightingOneWordCaseSesitive() throws IOException {
        // say that this bibtex object was found
        mBTE.setSearchHit(true);

        Optional<Pattern> highlightPattern = Optional.of(Pattern.compile("(google)"));

        String result = this.layout("<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>", mBTE, highlightPattern);
        String expected = "<font face=\"arial\"><BR><BR><b>Abstract: </b> In this paper, we initiate a formal study of security on Android: Google's new open-source platform for mobile devices. Tags: Paper android <span style=\"background-color:#3399FF;\">google</span> Open-Source Devices</font>";

        // check
        Assert.assertEquals(expected, result);
    }

    /**
     * @throws Exception
     */
    @Test
    public void testHighlightingMoreWordsCaseSesitive() throws IOException {
        // say that this bibtex object was found
        mBTE.setSearchHit(true);

        Optional<Pattern> highlightPattern = Optional.of(Pattern.compile("(Android|study|Open)", Pattern.CASE_INSENSITIVE));

        String highlightColor = "#3399FF;";
        String result = this.layout("<font face=\"arial\">\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract}\\end{abstract}</font>", mBTE, highlightPattern);
        String expected = "<font face=\"arial\"><BR><BR><b>Abstract: </b> In this paper, we initiate a formal <span style=\"background-color:" + highlightColor + "\">study</span> of security on <span style=\"background-color:" + highlightColor + "\">Android</span>: Google's new <span style=\"background-color:" + highlightColor + "\">open</span>-source platform for mobile devices. Tags: Paper <span style=\"background-color:" + highlightColor + "\">android</span> google <span style=\"background-color:" + highlightColor + "\">Open</span>-Source Devices</font>";

        // check
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testParseMethodCalls() {

        Assert.assertEquals(1, LayoutEntry.parseMethodsCalls("bla").size());
        Assert.assertEquals("bla", (LayoutEntry.parseMethodsCalls("bla").get(0)).get(0));

        Assert.assertEquals(1, LayoutEntry.parseMethodsCalls("bla,").size());
        Assert.assertEquals("bla", (LayoutEntry.parseMethodsCalls("bla,").get(0)).get(0));

        Assert.assertEquals(1, LayoutEntry.parseMethodsCalls("_bla.bla.blub,").size());
        Assert.assertEquals("_bla.bla.blub", (LayoutEntry.parseMethodsCalls("_bla.bla.blub,").get(0)).get(0));

        Assert.assertEquals(2, LayoutEntry.parseMethodsCalls("bla,foo").size());
        Assert.assertEquals("bla", (LayoutEntry.parseMethodsCalls("bla,foo").get(0)).get(0));
        Assert.assertEquals("foo", (LayoutEntry.parseMethodsCalls("bla,foo").get(1)).get(0));

        Assert.assertEquals(2, LayoutEntry.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").size());
        Assert.assertEquals("bla", (LayoutEntry.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(0)).get(0));
        Assert.assertEquals("foo", (LayoutEntry.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(1)).get(0));
        Assert.assertEquals("test", (LayoutEntry.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(0)).get(1));
        Assert.assertEquals("fark", (LayoutEntry.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(1)).get(1));

        Assert.assertEquals(2, LayoutEntry.parseMethodsCalls("bla(test),foo(fark)").size());
        Assert.assertEquals("bla", (LayoutEntry.parseMethodsCalls("bla(test),foo(fark)").get(0)).get(0));
        Assert.assertEquals("foo", (LayoutEntry.parseMethodsCalls("bla(test),foo(fark)").get(1)).get(0));
        Assert.assertEquals("test", (LayoutEntry.parseMethodsCalls("bla(test),foo(fark)").get(0)).get(1));
        Assert.assertEquals("fark", (LayoutEntry.parseMethodsCalls("bla(test),foo(fark)").get(1)).get(1));
    }

}
