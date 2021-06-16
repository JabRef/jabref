package org.jabref.logic.citationstyle;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("For some reason, instead of vol and pp we get null. No idea about the origin of this problem.")
class CitationStyleGeneratorTest {

    @Test
    void testIgnoreNewLine() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Last, First and\nDoe, Jane");

        // if the default citation style changes this has to be modified
        String expected = "  <div class=\"csl-entry\">\n" +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">F. Last and J. Doe, .</div>\n" +
                "  </div>\n";
        String citation = CitationStyleGenerator.generateCitation(entry, CitationStyle.getDefault());
        assertEquals(expected, citation);
    }

    @Test
    void testIgnoreCarriageReturnNewLine() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Last, First and\r\nDoe, Jane");

        // if the default citation style changes this has to be modified
        String expected = "  <div class=\"csl-entry\">\n" +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">F. Last and J. Doe, .</div>\n" +
                "  </div>\n";
        String citation = CitationStyleGenerator.generateCitation(entry, CitationStyle.getDefault());
        assertEquals(expected, citation);
    }

    @Test
    void testMissingCitationStyle() {
        String expected = Localization.lang("Cannot generate preview based on selected citation style.");
        String citation = CitationStyleGenerator.generateCitation(new BibEntry(), "faulty citation style");
        assertEquals(expected, citation);
    }

    @Test
    void testAsciiDocFormat() {
        String expectedCitation = "[1] B. Smith, B. Jones, and J. Williams, ``Title of the test entry,'' __BibTeX Journal__, vol. 34, no. 3, pp. 45–67, Jul. 2016.\n";
        BibEntry entry = TestEntry.getTestEntry();
        String style = CitationStyle.getDefault().getSource();
        CitationStyleOutputFormat format = CitationStyleOutputFormat.ASCII_DOC;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format);
        assertEquals(expectedCitation, actualCitation);
    }

    @Test
    void testHtmlFormat() {
        String expectedCitation = "  <div class=\"csl-entry\">\n" +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</div>\n" +
                "  </div>\n";
        BibEntry entry = TestEntry.getTestEntry();
        String style = CitationStyle.getDefault().getSource();
        CitationStyleOutputFormat format = CitationStyleOutputFormat.HTML;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format);
        assertEquals(expectedCitation, actualCitation);
    }

    @Test
    void testRtfFormat() {
        String expectedCitation = "[1]\\tab B. Smith, B. Jones, and J. Williams, \\uc0\\u8220{}Title of the test entry,\\uc0\\u8221{} {\\i{}BibTeX Journal}, vol. 34, no. 3, pp. 45\\uc0\\u8211{}67, Jul. 2016.\r\n";
        BibEntry entry = TestEntry.getTestEntry();
        String style = CitationStyle.getDefault().getSource();
        CitationStyleOutputFormat format = CitationStyleOutputFormat.RTF;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format);
        assertEquals(expectedCitation, actualCitation);
    }

    @Test
    void testTextFormat() {
        String expectedCitation = "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016.\n";
        BibEntry entry = TestEntry.getTestEntry();
        String style = CitationStyle.getDefault().getSource();
        CitationStyleOutputFormat format = CitationStyleOutputFormat.TEXT;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format);
        assertEquals(expectedCitation, actualCitation);
    }

    @Test
    void testXslFoFormat() {
        String expectedCitation = "<fo:block id=\"Smith2016\">\n" +
                "  <fo:table table-layout=\"fixed\" width=\"100%\">\n" +
                "    <fo:table-column column-number=\"1\" column-width=\"2.5em\"/>\n" +
                "    <fo:table-column column-number=\"2\" column-width=\"proportional-column-width(1)\"/>\n" +
                "    <fo:table-body>\n" +
                "      <fo:table-row>\n" +
                "        <fo:table-cell>\n" +
                "          <fo:block>[1]</fo:block>\n" +
                "        </fo:table-cell>\n" +
                "        <fo:table-cell>\n" +
                "          <fo:block>B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <fo:inline font-style=\"italic\">BibTeX Journal</fo:inline>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</fo:block>\n" +
                "        </fo:table-cell>\n" +
                "      </fo:table-row>\n" +
                "    </fo:table-body>\n" +
                "  </fo:table>\n" +
                "</fo:block>\n";
        BibEntry entry = TestEntry.getTestEntry();
        String style = CitationStyle.getDefault().getSource();
        CitationStyleOutputFormat format = CitationStyleOutputFormat.XSL_FO;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format);
        assertEquals(expectedCitation, actualCitation);
    }

    @Test
    void testHandleDiacritics() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "L{\"a}st, First and Doe, Jane");
        // if the default citation style changes this has to be modified.
        // in this case ä was added to check if it is formatted appropriately
        String expected = "  <div class=\"csl-entry\">\n" +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">F. Läst and J. Doe, .</div>\n" +
                "  </div>\n";
        String citation = CitationStyleGenerator.generateCitation(entry, CitationStyle.getDefault());
        assertEquals(expected, citation);
    }

    @Test
    void testHandleAmpersand() {
        String expectedCitation = "[1]B. Smith, B. Jones, and J. Williams, “&TitleTest&” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016.\n";
        BibEntry entry = TestEntry.getTestEntry();
        entry.setField(StandardField.TITLE, "“&TitleTest&”");
        String style = CitationStyle.getDefault().getSource();
        CitationStyleOutputFormat format = CitationStyleOutputFormat.TEXT;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format);
        assertEquals(expectedCitation, actualCitation);
    }
}
