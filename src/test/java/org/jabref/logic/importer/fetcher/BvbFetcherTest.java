package org.jabref.logic.importer.fetcher;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jabref.logic.importer.fetcher.transformers.AbstractQueryTransformer.NO_EXPLICIT_FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class BvbFetcherTest {

    BvbFetcher fetcher = new BvbFetcher();
    BibEntry bibEntryISBN0134685997;
    BibEntry bibEntryISBN9783960886402;

    @Test
    void testPerformTest() throws Exception {
        String searchquery = "effective java author:bloch";
        List<BibEntry> result = fetcher.performSearch(searchquery);
        assertTrue(result.size() > 0);

//        System.out.println("Query:\n");
//        System.out.println(fetcher.getURLForQuery(new StandardSyntaxParser().parse(searchquery, NO_EXPLICIT_FIELD)));
//        System.out.println("Test result:\n");
//        result.forEach(entry -> System.out.println(entry.toString()));
    }

    @BeforeEach
    public void setUp() {
        fetcher = new BvbFetcher();

        bibEntryISBN9783960886402 = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.SUBTITLE, "best practices für die Java-Plattform")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.TITLEADDON, "Joshua Bloch")
                .withField(StandardField.EDITION, "3. Auflage, Übersetzung der englischsprachigen 3. Originalausgabe 2018")
                .withField(StandardField.FILE, "ParsedFileField{description='', link='http://search.ebscohost.com/login.aspx?direct=true&scope=site&db=nlebk&db=nlabk&AN=1906353', fileType='PDF'}")
                .withField(StandardField.ISBN, "9783960886402")
                .withField(StandardField.KEYWORDS, "Klassen, Interfaces, Generics, Enums, Annotationen, Lambdas, Streams, Module, parallel, Parallele Programmierung, Serialisierung, funktional, funktionale Programmierung, Java EE, Jakarta EE")
                .withField(StandardField.LOCATION, "Heidelberg")
                .withField(StandardField.PUBLISHER, "{dpunkt.verlag} and {Dpunkt. Verlag (Heidelberg)}");

        bibEntryISBN0134685997 = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.YEAR, "2018")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.TITLEADDON, "Joshua Bloch")
                .withField(StandardField.EDITION, "Third edition")
                .withField(StandardField.ISBN, "0134685997")
                .withField(StandardField.LOCATION, "Boston")
                .withField(StandardField.PUBLISHER, "{Addison-Wesley}");
    }

    @Test
    public void testGetName() {
        assertEquals("Bibliotheksverbund Bayern (Experimental)", fetcher.getName());
    }

    @Test
    public void simpleSearchQueryURLCorrect() throws Exception {
        String query = "java jdk";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(query, NO_EXPLICIT_FIELD);
        URL url = fetcher.getURLForQuery(luceneQuery);
        assertEquals("http://bvbr.bib-bvb.de:5661/bvb01sru?version=1.1&recordSchema=marcxml&operation=searchRetrieve&query=java+jdk&maximumRecords=30", url.toString());
    }

    @Test
    public void complexSearchQueryURLCorrect() throws Exception {
        String query = "title:jdk";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(query, NO_EXPLICIT_FIELD);
        URL url = fetcher.getURLForQuery(luceneQuery);
        assertEquals("http://bvbr.bib-bvb.de:5661/bvb01sru?version=1.1&recordSchema=marcxml&operation=searchRetrieve&query=jdk&maximumRecords=30", url.toString());
    }

    @Test
    public void testPerformSearchMatchingMultipleEntries() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("effective java bloch");
        assertEquals(bibEntryISBN9783960886402, searchResult.get(0));
        assertEquals(bibEntryISBN0134685997, searchResult.get(1));
    }

    @Test
    public void testPerformSearchEmpty() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("");
        assertEquals(Collections.emptyList(), searchResult);
    }
}
