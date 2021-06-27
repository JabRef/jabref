package org.jabref.logic.importer.fetcher;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
public class CollectionOfComputerScienceBibliographiesParserTest {
    @Test
    public void parseEntriesReturnsEmptyListIfXmlHasNoResults() throws Exception {
        parseXmlAndCheckResults("collection_of_computer_science_bibliographies_empty_result.xml", Collections.emptyList());
    }

    @Test
    public void parseEntriesReturnsOneBibEntryInListIfXmlHasSingleResult() throws Exception {
        parseXmlAndCheckResults("collection_of_computer_science_bibliographies_single_result.xml", Collections.singletonList("collection_of_computer_science_bibliographies_single_result.bib"));
    }

    @Test
    public void parseEntriesReturnsMultipleBibEntriesInListIfXmlHasMultipleResults() throws Exception {
        parseXmlAndCheckResults("collection_of_computer_science_bibliographies_multiple_results.xml", Arrays.asList("collection_of_computer_science_bibliographies_multiple_results_first_result.bib", "collection_of_computer_science_bibliographies_multiple_results_second_result.bib"));
    }

    private void parseXmlAndCheckResults(String xmlName, List<String> resourceNames) throws Exception {
        PreferencesService preferencesService = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);
        when(preferencesService.getKeywordDelimiter()).thenReturn(',');

        InputStream is = CollectionOfComputerScienceBibliographiesParserTest.class.getResourceAsStream(xmlName);
        CollectionOfComputerScienceBibliographiesParser parser = new CollectionOfComputerScienceBibliographiesParser(preferencesService);
        List<BibEntry> entries = parser.parseEntries(is);
        assertNotNull(entries);
        assertEquals(resourceNames.size(), entries.size());
        for (int i = 0; i < resourceNames.size(); i++) {
            BibEntryAssert.assertEquals(CollectionOfComputerScienceBibliographiesParserTest.class, resourceNames.get(i), entries.get(i));
        }
    }
}
