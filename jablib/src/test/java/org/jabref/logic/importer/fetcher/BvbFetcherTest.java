package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.search.query.SearchQueryVisitor;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@FetcherTest
class BvbFetcherTest {

    BvbFetcher fetcher = new BvbFetcher();
    BibEntry bibEntryISBN9783960886402 = new BibEntry(StandardEntryType.Misc)
            .withField(StandardField.TITLE, "Effective Java")
            .withField(StandardField.YEAR, "2018")
            .withField(StandardField.SUBTITLE, "best practices für die Java-Plattform")
            .withField(StandardField.AUTHOR, "Bloch, Joshua")
            .withField(StandardField.TITLEADDON, "Joshua Bloch")
            .withField(StandardField.EDITION, "3. Auflage, Übersetzung der englischsprachigen 3. Originalausgabe 2018")
            .withFiles(List.of(new LinkedFile("", "http://search.ebscohost.com/login.aspx?direct=true&scope=site&db=nlebk&db=nlabk&AN=1906353", StandardFileType.PDF)))
            .withField(StandardField.ISBN, "9783960886402")
            .withField(StandardField.KEYWORDS, "Klassen, Interfaces, Generics, Enums, Annotationen, Lambdas, Streams, Module, parallel, Parallele Programmierung, Serialisierung, funktional, funktionale Programmierung, Java EE, Jakarta EE")
            .withField(StandardField.ADDRESS, "Heidelberg")
            .withField(StandardField.PAGETOTAL, "396")
            .withField(StandardField.PUBLISHER, "{dpunkt.verlag} and {Dpunkt. Verlag (Heidelberg)}");

    BibEntry bibEntryISBN0134685997 = new BibEntry(StandardEntryType.Misc)
            .withField(StandardField.TITLE, "Effective Java")
            .withField(StandardField.YEAR, "2018")
            .withField(StandardField.AUTHOR, "Bloch, Joshua")
            .withField(StandardField.TITLEADDON, "Joshua Bloch")
            .withField(StandardField.EDITION, "Third edition")
            .withField(StandardField.ISBN, "0134685997")
            .withField(StandardField.PAGETOTAL, "392")
            .withField(StandardField.ADDRESS, "Boston")
            .withField(StandardField.PUBLISHER, "{Addison-Wesley}");

    @Test
    void performTest() throws FetcherException {
        String searchquery = "effective java author=bloch";
        List<BibEntry> result = fetcher.performSearch(searchquery);
        assertFalse(result.isEmpty());

        //        System.out.println("Query:\n");
        //        System.out.println(fetcher.getURLForQuery(new StandardSyntaxParser().parse(searchquery, NO_EXPLICIT_FIELD)));
        //        System.out.println("Test result:\n");
        //        result.forEach(entry -> System.out.println(entry.toString()));
    }

    @Test
    void simpleSearchQueryURLCorrect() throws MalformedURLException, URISyntaxException {
        String query = "java jdk";
        SearchQuery searchQueryObject = new SearchQuery(query);
        SearchQueryVisitor visitor = new SearchQueryVisitor(searchQueryObject.getSearchFlags());
        URL url = fetcher.getURLForQuery(visitor.visitStart(searchQueryObject.getContext()));
        assertEquals("http://bvbr.bib-bvb.de:5661/bvb01sru?version=1.1&recordSchema=marcxml&operation=searchRetrieve&query=java%20jdk&maximumRecords=30", url.toString());
    }

    @Test
    void complexSearchQueryURLCorrect() throws MalformedURLException, URISyntaxException {
        String query = "title=jdk";
        SearchQuery searchQueryObject = new SearchQuery(query);
        SearchQueryVisitor visitor = new SearchQueryVisitor(searchQueryObject.getSearchFlags());
        URL url = fetcher.getURLForQuery(visitor.visitStart(searchQueryObject.getContext()));
        assertEquals("http://bvbr.bib-bvb.de:5661/bvb01sru?version=1.1&recordSchema=marcxml&operation=searchRetrieve&query=jdk&maximumRecords=30", url.toString());
    }

    @Test
    void performSearchMatchingMultipleEntries() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("effective java bloch");
        assertEquals(List.of(bibEntryISBN9783960886402, bibEntryISBN0134685997), searchResult.subList(0, 2));
    }

    @Test
    void performSearchEmpty() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("");
        assertEquals(List.of(), searchResult);
    }
}
