package org.jabref.logic.importer.fetcher;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.transformers.AbstractQueryTransformer;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Disabled for experimental status.
@Disabled
@FetcherTest
public class BvbFetcherTest {

    BvbFetcher fetcher = new BvbFetcher();
    BibEntry bibEntryISBN0321356683;
    BibEntry bibEntryISBN9783960886402;

    @Test
    void testPerformTest() throws Exception {
        List<BibEntry> result = fetcher.performSearch("effective java bloch");

        result.forEach(entry -> System.out.println(entry.toString()));
    }

    @BeforeEach
    public void setUp() {
        fetcher = new BvbFetcher();

        bibEntryISBN9783960886402 = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.DATE, "2018")
                .withField(StandardField.SUBTITLE, "best practices für die Java-Plattform")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.TITLEADDON, "Joshua Bloch")
                .withField(StandardField.EDITION, "3. Auflage")
                .withField(StandardField.FILE, "ParsedFileField{description='', link='http://search.ebscohost.com/login.aspx?direct=true&scope=site&db=nlebk&db=nlabk&AN=1906353', fileType='PDF'}")
                .withField(StandardField.ISBN, "9783960886402")
                .withField(StandardField.KEYWORDS, "Klassen, Interfaces, Generics, Enums, Annotationen, Lambdas, Streams, Module, parallel, Parallele Programmierung, Serialisierung, funktional, funktionale Programmierung, Java EE, Jakarta EE")
                .withField(StandardField.LOCATION, "Heidelberg")
                .withField(StandardField.PUBLISHER, "{dpunkt.verlag} and {Dpunkt. Verlag (Heidelberg)}");

        bibEntryISBN0321356683 = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.DATE, "2011")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.SERIES, "\u0098The\u009C Java series")
                .withField(StandardField.TITLEADDON, "Joshua Bloch")
                .withField(StandardField.EDITION, "2. ed., [Nachdr.]")
                .withField(StandardField.ISBN, "0321356683")
                .withField(StandardField.LOCATION, "Upper Saddle River, NJ [u.a.]")
                .withField(StandardField.PUBLISHER, "{Addison-Wesley}");
    }

    @Test
    public void testGetName() {
        assertEquals("Bibliotheksverbund Bayern (Experimental)", fetcher.getName());
    }

    @Test
    public void simpleSearchQueryURLCorrect() throws Exception {
        String query = "java jdk";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(query, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        URL url = fetcher.getURLForQuery(luceneQuery);
        assertEquals("http://bvbr.bib-bvb.de:5661/bvb01sru?version=1.1&recordSchema=marcxml&operation=searchRetrieve&query=java+and+jdk&maximumRecords=10", url.toString());
    }

    @Test
    public void complexSearchQueryURLCorrect() throws Exception {
        String query = "tit:jdk";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(query, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        URL url = fetcher.getURLForQuery(luceneQuery);
        assertEquals("http://bvbr.bib-bvb.de:5661/bvb01sru?version=1.1&recordSchema=marcxml&operation=searchRetrieve&query=marcxml.kon%3D%22java%22+and+marcxml.tit%3D%22jdk%22&maximumRecords=10", url.toString());
    }

    @Test
    public void testPerformSearchMatchingMultipleEntries() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("title:\"effective java bloch\"");
        assertTrue(searchResult.contains(bibEntryISBN9783960886402));
        assertTrue(searchResult.contains(bibEntryISBN0321356683));
    }

    @Test
    public void testPerformSearch591166003() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("isbn:9783960886402");
        assertEquals(Collections.singletonList(bibEntryISBN9783960886402), searchResult);
    }

    @Test
    public void testPerformSearch66391437X() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("isbn:bibEntryISBN0321356683");
        assertEquals(Collections.singletonList(bibEntryISBN0321356683), searchResult);
    }

    @Test
    public void testPerformSearchEmpty() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("");
        assertEquals(Collections.emptyList(), searchResult);
    }
}
