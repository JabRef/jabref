package org.jabref.logic.citationstyle;

import java.util.List;

import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CitationStyleTest {

    @Test
    void getDefault() throws Exception {
        assertNotNull(CitationStyle.getDefault());
    }

    @Test
    void testDefaultCitation() {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(TestEntry.getTestEntry())));
        context.setMode(BibDatabaseMode.BIBLATEX);
        String citation = CitationStyleGenerator.generateCitation(TestEntry.getTestEntry(), CitationStyle.getDefault().getSource(), CitationStyleOutputFormat.HTML, context, new BibEntryTypesManager());

        // if the default citation style changes this has to be modified
        String expected = "  <div class=\"csl-entry\">\n"
                          + "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">B. Smith, B. Jones, and J. Williams, &ldquo;Title of the test entry,&rdquo; <span style=\"font-style: italic\">BibTeX Journal</span>, vol. 34, no. 7, Art. no. 3, 2016-07, doi: 10.1001/bla.blubb.</div>\n"
                          + "  </div>\n"
                          + "";

        assertEquals(expected, citation);
    }

    @Test
    void testDiscoverCitationStylesNotNull() throws Exception {
        List<CitationStyle> styleList = CitationStyle.discoverCitationStyles();
        assertNotNull(styleList);
    }
}
