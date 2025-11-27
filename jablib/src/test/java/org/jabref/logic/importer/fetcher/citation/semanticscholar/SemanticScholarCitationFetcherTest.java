package org.jabref.logic.importer.fetcher.citation.semanticscholar;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@FetcherTest
class SemanticScholarCitationFetcherTest {

    SemanticScholarCitationFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new SemanticScholarCitationFetcher(mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    void smoke() throws FetcherException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Macht_2007")
                .withField(StandardField.AUTHOR, "Macht, Michael and Mueller, Jochen")
                .withField(StandardField.TITLE, "Immediate effects of chocolate on experimentally induced mood states")
                .withField(StandardField.JOURNALTITLE, "Appetite")
                .withField(StandardField.DATE, "2007-11")
                .withField(StandardField.VOLUME, "49")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.PAGES, "667--674")
                .withField(StandardField.DOI, "10.1016/j.appet.2007.05.004")
                .withField(StandardField.ISSN, "0195-6663")
                .withField(StandardField.PUBLISHER, "Elsevier BV");

        List<BibEntry> result = fetcher.getReferences(entry);
        // Paper has more than 400 cites, but server returns "null" as data
        assertNotEquals(null, result);
    }

    @Test
    void smokeCitationCount() throws FetcherException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Macht_2007")
                .withField(StandardField.AUTHOR, "Macht, Michael and Mueller, Jochen")
                .withField(StandardField.TITLE, "Immediate effects of chocolate on experimentally induced mood states")
                .withField(StandardField.JOURNALTITLE, "Appetite")
                .withField(StandardField.DATE, "2007-11")
                .withField(StandardField.VOLUME, "49")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.PAGES, "667--674")
                .withField(StandardField.DOI, "10.1016/j.appet.2007.05.004")
                .withField(StandardField.ISSN, "0195-6663")
                .withField(StandardField.PUBLISHER, "Elsevier BV");

        Optional<Integer> result = fetcher.getCitationCount(entry);
        assertNotNull(result.get());
        assertThat(result.get(), greaterThan(0));
    }
}
