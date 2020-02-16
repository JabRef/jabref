package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
public class GrobidCitationFetcherTest {

    static ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    static GrobidCitationFetcher grobidCitationFetcher = new GrobidCitationFetcher(importFormatPreferences);

    static String example1 = "Derwing, T. M., Rossiter, M. J., & Munro, M. J. (2002). Teaching native speakers to listen to foreign-accented speech. Journal of Multilingual and Multicultural Development, 23(4), 245-259.";
    static BibEntry example1AsBibEntry = new BibEntry(StandardEntryType.Article).withCiteKey("-1")
                .withField(StandardField.AUTHOR, "T Derwing and M Rossiter and M Munro")
                .withField(StandardField.TITLE, "Teaching native speakers to listen to foreign-accented speech")
                .withField(StandardField.JOURNAL, "Journal of Multilingual and Multicultural Development")
                .withField(StandardField.YEAR, "2002")
                .withField(StandardField.PAGES, "245--259")
                .withField(StandardField.VOLUME, "23")
                .withField(StandardField.NUMBER, "4");
    static String example2 = "Thomas, H. K. (2004). Training strategies for improving listeners' comprehension of foreign-accented speech (Doctoral dissertation). University of Colorado, Boulder.";
    static BibEntry example2AsBibEntry = new BibEntry(BibEntry.DEFAULT_TYPE).withCiteKey("-1")
            .withField(StandardField.AUTHOR, "H Thomas")
            .withField(StandardField.TITLE, "Training strategies for improving listeners' comprehension of foreign-accented speech (Doctoral dissertation)")
            .withField(StandardField.YEAR, "2004")
            .withField(StandardField.ADDRESS, "Boulder");
    static String example3 = "Turk, J., Graham, P., & Verhulst, F. (2007). Child and adolescent psychiatry : A developmental approach. Oxford, England: Oxford University Press.";
    static BibEntry example3AsBibEntry = new BibEntry(BibEntry.DEFAULT_TYPE).withCiteKey("-1")
            .withField(StandardField.AUTHOR, "J Turk and P Graham and F Verhulst")
            .withField(StandardField.TITLE, "Child and adolescent psychiatry : A developmental approach")
            .withField(StandardField.PUBLISHER, "Oxford University Press")
            .withField(StandardField.YEAR, "2007")
            .withField(StandardField.ADDRESS, "Oxford, England");
    static String example4 = "Carr, I., & Kidner, R. (2003). Statutes and conventions on international trade law (4th ed.). London, England: Cavendish.";
    static BibEntry example4AsBibEntry = new BibEntry(StandardEntryType.Article).withCiteKey("-1")
            .withField(StandardField.AUTHOR, "I Carr and R Kidner")
            .withField(StandardField.BOOKTITLE, "Statutes and conventions on international trade law")
            .withField(StandardField.PUBLISHER, "Cavendish")
            .withField(StandardField.YEAR, "2003")
            .withField(StandardField.ADDRESS, "London, England");
    static String invalidInput1 = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx________________________________";
    static String invalidInput2 = "¦@#¦@#¦@#¦@#¦@#¦@#¦@°#¦@¦°¦@°";

    @Test
    public void grobidPerformSearchCorrectResultTest() {
        List<BibEntry> entries = grobidCitationFetcher.performSearch(example1);
        assertEquals(List.of(example1AsBibEntry), entries);
        entries = grobidCitationFetcher.performSearch(example2);
        assertEquals(List.of(example2AsBibEntry), entries);
        entries = grobidCitationFetcher.performSearch(example3);
        assertEquals(List.of(example3AsBibEntry), entries);
        entries = grobidCitationFetcher.performSearch(example4);
        assertEquals(List.of(example4AsBibEntry), entries);
    }

    @Test
    public void grobidPerformSearchCorrectlySplitsStringTest() {
        List<BibEntry> entries = grobidCitationFetcher.performSearch(example1 + "\n" + example2 + "\r\n" + example3 + "\r" + example4);
        assertEquals(List.of(example1AsBibEntry, example2AsBibEntry, example3AsBibEntry, example4AsBibEntry), entries);
    }

    @Test
    public void grobidPerformSearchWithEmptyStringsTest() {
        List<BibEntry> entries = grobidCitationFetcher.performSearch("   \n   ");
        assertEquals(Collections.emptyList(), entries);
    }

    @Test
    public void grobidPerformSearchWithInvalidDataTest() {
        List<BibEntry> entries = grobidCitationFetcher.performSearch(invalidInput1);
        assertEquals(Collections.emptyList(), entries);
        entries = grobidCitationFetcher.performSearch(invalidInput2);
        assertEquals(Collections.emptyList(), entries);
    }

}
