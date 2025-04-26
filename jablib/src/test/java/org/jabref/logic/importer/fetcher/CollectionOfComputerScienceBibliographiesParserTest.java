package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.support.BibEntryAssert;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
@Disabled
class CollectionOfComputerScienceBibliographiesParserTest {

    @Test
    void parseEntriesReturnsEmptyListIfXmlHasNoResults() throws ParseException, IOException {
        parseXmlAndCheckResults("collection_of_computer_science_bibliographies_empty_result.xml", List.of());
    }

    @Disabled("Parse/fetcher remote side does not return anything valid for the link")
    @Test
    void parseEntriesReturnsOneBibEntryInListIfXmlHasSingleResult() throws ParseException, IOException {
        parseXmlAndCheckResults("collection_of_computer_science_bibliographies_single_result.xml", List.of("collection_of_computer_science_bibliographies_single_result.bib"));
    }

    @Test
    void parseEntriesReturnsMultipleBibEntriesInListIfXmlHasMultipleResults() throws ParseException, IOException {
        parseXmlAndCheckResults("collection_of_computer_science_bibliographies_multiple_results.xml", Arrays.asList("collection_of_computer_science_bibliographies_multiple_results_first_result.bib", "collection_of_computer_science_bibliographies_multiple_results_second_result.bib"));
    }

    private void parseXmlAndCheckResults(String xmlName, List<String> resourceNames) throws ParseException, IOException {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        InputStream is = CollectionOfComputerScienceBibliographiesParserTest.class.getResourceAsStream(xmlName);
        CollectionOfComputerScienceBibliographiesParser parser = new CollectionOfComputerScienceBibliographiesParser(importFormatPreferences);
        List<BibEntry> entries = parser.parseEntries(is);
        assertEquals(resourceNames.size(), entries.size());
        assertNotNull(entries);
        for (int i = 0; i < resourceNames.size(); i++) {
            BibEntryAssert.assertEquals(CollectionOfComputerScienceBibliographiesParserTest.class, resourceNames.get(i), entries.get(i));
        }
    }
}
