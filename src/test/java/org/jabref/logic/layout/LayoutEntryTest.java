package org.jabref.logic.layout;

import java.io.IOException;
import java.io.StringReader;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * The test class LayoutEntryTest test the net.sf.jabref.export.layout.LayoutEntry.
 * Indirectly the net.sf.jabref.export.layout.Layout is tested too.
 * <p/>
 * The LayoutEntry creates a human readable String assigned with HTML formatters.
 * To test the Highlighting Feature, an instance of LayoutEntry will be instantiated via Layout and LayoutHelper.
 * With these instance the doLayout() Method is called several times for each test case.
 * To simulate a search, a BibEntry will be created, which will be used by LayoutEntry.
 *
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
    @BeforeEach
    public void setUp() {

        // create Bibtext Entry

        mBTE = new BibEntry();
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

    public String layout(String layoutFile, BibEntry entry) throws IOException {
        StringReader sr = new StringReader(layoutFile.replace("__NEWLINE__", "\n"));
        Layout layout = new LayoutHelper(sr, mock(LayoutFormatterPreferences.class)).getLayoutFromText();

        return layout.doLayout(entry, null);
    }

    /*************************/
    /****** tests Cases ******/
    /*************************/

    @Test
    public void testParseMethodCalls() {

        assertEquals(1, LayoutEntry.parseMethodsCalls("bla").size());
        assertEquals("bla", (LayoutEntry.parseMethodsCalls("bla").get(0)).get(0));

        assertEquals(1, LayoutEntry.parseMethodsCalls("bla,").size());
        assertEquals("bla", (LayoutEntry.parseMethodsCalls("bla,").get(0)).get(0));

        assertEquals(1, LayoutEntry.parseMethodsCalls("_bla.bla.blub,").size());
        assertEquals("_bla.bla.blub", (LayoutEntry.parseMethodsCalls("_bla.bla.blub,").get(0)).get(0));

        assertEquals(2, LayoutEntry.parseMethodsCalls("bla,foo").size());
        assertEquals("bla", (LayoutEntry.parseMethodsCalls("bla,foo").get(0)).get(0));
        assertEquals("foo", (LayoutEntry.parseMethodsCalls("bla,foo").get(1)).get(0));

        assertEquals(2, LayoutEntry.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").size());
        assertEquals("bla", (LayoutEntry.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(0)).get(0));
        assertEquals("foo", (LayoutEntry.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(1)).get(0));
        assertEquals("test", (LayoutEntry.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(0)).get(1));
        assertEquals("fark", (LayoutEntry.parseMethodsCalls("bla(\"test\"),foo(\"fark\")").get(1)).get(1));

        assertEquals(2, LayoutEntry.parseMethodsCalls("bla(test),foo(fark)").size());
        assertEquals("bla", (LayoutEntry.parseMethodsCalls("bla(test),foo(fark)").get(0)).get(0));
        assertEquals("foo", (LayoutEntry.parseMethodsCalls("bla(test),foo(fark)").get(1)).get(0));
        assertEquals("test", (LayoutEntry.parseMethodsCalls("bla(test),foo(fark)").get(0)).get(1));
        assertEquals("fark", (LayoutEntry.parseMethodsCalls("bla(test),foo(fark)").get(1)).get(1));
    }

}
