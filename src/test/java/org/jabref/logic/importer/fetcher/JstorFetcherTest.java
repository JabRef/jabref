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
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
@DisabledOnCIServer("CI server is blocked by JSTOR")
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

    private final BibEntry doiEntry = new BibEntry(StandardEntryType.Article)
            .withCitationKey("10.1086/501484")
            .withField(StandardField.AUTHOR, "Johnmarshall Reeve")
            .withField(StandardField.TITLE, "Teachers as Facilitators: What Autonomy‐Supportive Teachers Do and Why Their Students Benefit")
            .withField(StandardField.ISSN, "00135984, 15548279")
            .withField(StandardField.JOURNAL, "The Elementary School Journal")
            .withField(StandardField.ABSTRACT, "Abstract Students are sometimes proactive and engaged in classroom learning activities, but they are also sometimes only reactive and passive. Recognizing this, in this article I argue that students’ classroom engagement depends, in part, on the supportive quality of the classroom climate in which they learn. According to the dialectical framework within self‐determination theory, students possess inner motivational resources that classroom conditions can support or frustrate. When teachers find ways to nurture these inner resources, they adopt an autonomy‐supportive motivating style. After articulating what autonomy‐supportive teachers say and do during instruction, I discuss 3 points: teachers can learn how to be more autonomy supportive toward students; teachers most engage students when they offer high levels of both autonomy support and structure; and an autonomy‐supportive motivating style is an important element to a high‐quality teacher‐student relationship.")
            .withField(StandardField.PUBLISHER, "The University of Chicago Press")
            .withField(StandardField.NUMBER, "3")
            .withField(StandardField.PAGES, "225--236")
            .withField(StandardField.VOLUME, "106")
            .withField(StandardField.URL, "http://www.jstor.org/stable/10.1086/501484")
            .withField(StandardField.YEAR, "2006");

    @Test
    void searchByTitle() throws Exception {
        List<BibEntry> entries = fetcher.performSearch("title: \"Test Anxiety Analysis of Chinese College Students in Computer-based Spoken English Test\"");
        assertEquals(Collections.singletonList(bibEntry), entries);
    }

    @Test
    void searchById() throws FetcherException {
        assertEquals(Optional.of(bibEntry), fetcher.performSearchById("90002164"));
        assertEquals(Optional.of(doiEntry), fetcher.performSearchById("https://www.jstor.org/stable/10.1086/501484?seq=1"));
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
        // Does not provide articles and journals
        return "Test";
    }

    @Disabled("jstor does not support search only based on year")
    @Override
    public void supportsYearRangeSearch() throws Exception {

    }

    @Disabled("jstor does not provide articles with journals")
    @Override
    public void supportsJournalSearch() throws Exception {

    }

    @Disabled("jstor does not support search only based on year")
    @Override
    public void supportsYearSearch() throws Exception {

    }
}
