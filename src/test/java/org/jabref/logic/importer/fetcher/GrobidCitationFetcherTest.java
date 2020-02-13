package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
public class GrobidCitationFetcherTest {

    static ImportFormatPreferences importFormatPreferences;
    static FileUpdateMonitor fileUpdateMonitor;
    static GrobidCitationFetcher grobidCitationFetcher;

    static String example1 = "Derwing, T. M., Rossiter, M. J., & Munro, M. J. (2002). Teaching native speakers to listen to foreign-accented speech. Journal of Multilingual and Multicultural Development, 23(4), 245-259.";
    static String example1AsBibtex = "@article{-1,\n" +
            "author\t=\t\"T Derwing and M Rossiter and M Munro\",\n" +
            "title\t=\t\"Teaching native speakers to listen to foreign-accented speech\",\n" +
            "journal\t=\t\"Journal of Multilingual and Multicultural Development\",\n" +
            "year\t=\t\"2002\",\n" +
            "pages\t=\t\"245--259\",\n" +
            "volume\t=\t\"23\",\n" +
            "number\t=\t\"4\"\n" +
            "}";
    static String example2 = "Thomas, H. K. (2004). Training strategies for improving listeners' comprehension of foreign-accented speech (Doctoral dissertation). University of Colorado, Boulder.";
    static String example2AsBibtex = "@misc{-1,\n" +
            "author\t=\t\"H Thomas\",\n" +
            "title\t=\t\"Training strategies for improving listeners' comprehension of foreign-accented speech (Doctoral dissertation)\",\n"
            +
            "year\t=\t\"2004\",\n" +
            "address\t=\t\"Boulder\"\n" +
            "}";
    static String example3 = "Turk, J., Graham, P., & Verhulst, F. (2007). Child and adolescent psychiatry : A developmental approach. Oxford, England: Oxford University Press.";
    static String example3AsBibtex = "@misc{-1,\n" +
            "author\t=\t\"J Turk and P Graham and F Verhulst\",\n" +
            "title\t=\t\"Child and adolescent psychiatry : A developmental approach\",\n" +
            "publisher\t=\t\"Oxford University Press\",\n" +
            "year\t=\t\"2007\",\n" +
            "address\t=\t\"Oxford, England\"\n" +
            "}";
    static String example4 = "Carr, I., & Kidner, R. (2003). Statutes and conventions on international trade law (4th ed.). London, England: Cavendish.";
    static String example4AsBibtex = "@article{-1,\n" +
            "author\t=\t\"I Carr and R Kidner\",\n" +
            "booktitle\t=\t\"Statutes and conventions on international trade law\",\n" +
            "publisher\t=\t\"Cavendish\",\n" +
            "year\t=\t\"2003\",\n" +
            "address\t=\t\"London, England\"\n" +
            "}";
    static String invalidInput1 = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx________________________________";
    static String invalidInput2 = "¦@#¦@#¦@#¦@#¦@#¦@#¦@°#¦@¦°¦@°";
    static BibEntry example1AsBibEntry;
    static BibEntry example2AsBibEntry;
    static BibEntry example3AsBibEntry;
    static BibEntry example4AsBibEntry;

    @BeforeAll
    public static void setup() throws ParseException {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        fileUpdateMonitor = new DummyFileUpdateMonitor();
        grobidCitationFetcher = new GrobidCitationFetcher(importFormatPreferences);
        example1AsBibEntry = BibtexParser.singleFromString(example1AsBibtex, importFormatPreferences, fileUpdateMonitor).get();
        example2AsBibEntry = BibtexParser.singleFromString(example2AsBibtex, importFormatPreferences, fileUpdateMonitor).get();
        example3AsBibEntry = BibtexParser.singleFromString(example3AsBibtex, importFormatPreferences, fileUpdateMonitor).get();
        example4AsBibEntry = BibtexParser.singleFromString(example4AsBibtex, importFormatPreferences, fileUpdateMonitor).get();
    }

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
