package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class DOAJFetcherTest {

    DOAJFetcher fetcher;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        fetcher = new DOAJFetcher(importFormatPreferences);
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry expected = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        expected.setField("author", "Wei Wang and Yun He and Tong Li and Jiajun Zhu and Jinzhuo Liu");
        expected.setField("doi", "10.1155/2018/5913634");
        expected.setField("issn", "1875-919X");
        expected.setField("journal", "Scientific Programming");
        expected.setField("publisher", "Hindawi Limited");
        expected.setField("title", "An Integrated Model for Information Retrieval Based Change Impact Analysis");
        expected.setField("url", "http://dx.doi.org/10.1155/2018/5913634");
        expected.setField("volume", "2018");
        expected.setField("year", "2018");
        expected.setField("abstract", "The paper presents an approach to combine multiple existing information retrieval (IR) techniques to support change impact analysis, which seeks to identify the possible outcomes of a change or determine the necessary modifications for affecting a desired change. The approach integrates a bag-of-words based IR technique, where each class or method is abstracted as a set of words, and a neural network based IR technique to derive conceptual couplings from the source code of a software system. We report rigorous empirical assessments of the changes of three open source systems: jEdit, muCommander, and JabRef. The impact sets obtained are evaluated at the method level of granularity, and the results show that our integrated approach provides statistically significant improvements in accuracy across several cut points relative to the accuracies provided by the individual methods employed independently. Improvements in F-score values of up to 7.3%, 10.9%, and 17.3% are obtained over a baseline technique for jEdit, muCommander, and JabRef, respectively.");

        List<BibEntry> fetchedEntries = fetcher.performSearch("JabRef impact");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }
}
