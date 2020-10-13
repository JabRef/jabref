package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
public class JstorFetcherTest implements SearchBasedFetcherCapabilityTest {

    private final JstorFetcher fetcher = new JstorFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

    private final BibEntry bibEntry = new BibEntry(StandardEntryType.Article)
            .withCitationKey("10.2307/90002164")
            .withField(StandardField.AUTHOR, "Yang Yanxia")
            .withField(StandardField.TITLE, "Test Anxiety Analysis of Chinese College Students in Computer-based Spoken English Test")
            .withField(StandardField.ISSN, "11763647, 14364522")
            .withField(StandardField.JOURNAL, "Journal of Educational Technology & Society")
            .withField(StandardField.ABSTRACT, "ABSTRACT Test anxiety was a commonly known or assumed factor that could greatly influence performance of test takers. With the employment of designed questionnaires and computer-based spoken English test, this paper explored test anxiety manifestation of Chinese college students from both macro and micro aspects, and found out that the major anxiety in computer-based spoken English test was spoken English test anxiety, which consisted of test anxiety and communication apprehension. Regard to proximal test anxiety, the causes listed in proper order as low spoken English abilities, lack of speaking techniques, anxiety from the evaluative process and inadaptability with computer-based spoken English test format. As to distal anxiety causes, attitude toward learning spoken English and self-evaluation of speaking abilities were significantly negatively correlated with test anxiety. Besides, as test anxiety significantly associated often with test performance, a look at pedagogical implications has been discussed in this paper.")
            .withField(StandardField.PUBLISHER, "International Forum of Educational Technology & Society")
            .withField(StandardField.NUMBER, "2")
            .withField(StandardField.PAGES, "63--73")
            .withField(StandardField.VOLUME, "20")
            .withField(StandardField.URL, "http://www.jstor.org/stable/90002164")
            .withField(StandardField.YEAR, "2017");

    @Test
    void searchByTitle() throws Exception {
        List<BibEntry> entries = fetcher.performSearch("ti: \"Test Anxiety Analysis of Chinese College Students in Computer-based Spoken English Test\"");
        assertEquals(Collections.singletonList(bibEntry), entries);
    }

    @Test
    void fetchPDF() throws IOException, FetcherException {
        Optional<URL> url = fetcher.findFullText(bibEntry);
        assertEquals(Optional.of(new URL("https://www.jstor.org/stable/pdf/90002164.pdf")), url);
    }

    @Override
    public SearchBasedFetcher getFetcher() {
        return fetcher;
    }

    @Override
    public List<String> getTestAuthors() {
        return List.of("Haman", "Medlin");
    }

    @Override
    public String getTestJournal() {
        return "Test";
    }

    @Disabled("jstor does not support search only based on year")
    @Override
    public void supportsYearRangeSearch() throws Exception {

    }

    @Disabled("jstor does not support search only based on year")
    @Override
    public void supportsYearSearch() throws Exception {

    }
}
