package org.jabref.logic.importer.fetcher;


import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.importer.fetcher.CollectionOfComputerScienceBibliographiesParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@FetcherTest
public class CollectionOfComputerScienceBibliographiesParserTest {
    @Test
    public void parseEntriesReturnsEmptyListIfXmlHasNoResults() throws Exception {
        parseXmlAndCheckResults("collection_of_computer_science_bibliographies_empty_result.xml", Collections.emptyList());
    }

    @Test
    public void parseEntriesReturnsOneBibEntryInListIfXmlHasOneResult() throws Exception {
        parseXmlAndCheckResults("collection_of_computer_science_bibliographies_single_result.xml", Collections.singletonList("collection_of_computer_science_bibliographies_single_result.bib"));
    }

    @Test
    public void parseEntriesReturnsMultipleBibEntriesInListIfXmlHasMultipleResults() throws Exception {
        parseXmlAndCheckResults("collection_of_computer_science_bibliographies_multiple_results.xml", Arrays.asList("collection_of_computer_science_bibliographies_multiple_results_first_result.bib", "collection_of_computer_science_bibliographies_multiple_results_second_result.bib"));
    }

    private void parseXmlAndCheckResults(String xmlName, List<String> resourceNames) throws Exception {
        InputStream is = CollectionOfComputerScienceBibliographiesParserTest.class.getResourceAsStream(xmlName);
        CollectionOfComputerScienceBibliographiesParser parser = new CollectionOfComputerScienceBibliographiesParser();
        List<BibEntry> entries = parser.parseEntries(is);
        assertNotNull(entries);
        assertEquals(resourceNames.size(), entries.size());
        for (int i = 0; i < resourceNames.size(); i++) {
            BibEntryAssert.assertEquals(GvkParserTest.class, resourceNames.get(i), entries.get(i));
        }
    }
}
