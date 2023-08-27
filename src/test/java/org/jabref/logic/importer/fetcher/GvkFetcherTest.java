package org.jabref.logic.importer.fetcher;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.transformers.AbstractQueryTransformer;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@FetcherTest
public class GvkFetcherTest {

    private GvkFetcher fetcher;
    private BibEntry bibEntryPPN591166003;
    private BibEntry bibEntryPPN66391437X;

    @BeforeEach
    public void setUp() {
        fetcher = new GvkFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

        bibEntryPPN591166003 = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.PUBLISHER, "Addison-Wesley")
                .withField(StandardField.YEAR, "2008")
                .withField(StandardField.AUTHOR, "Bloch, Joshua")
                .withField(StandardField.SERIES, "The Java series ... from the source")
                .withField(StandardField.ADDRESS, "Upper Saddle River, NJ")
                .withField(StandardField.EDITION, "2. ed., 5. print.")
                .withField(StandardField.NOTE, "Includes bibliographical references and index. - Previous ed.: 2001. - Hier auch später erschienene, unveränderte Nachdrucke")
                .withField(StandardField.ISBN, "9780321356680")
                .withField(StandardField.PAGETOTAL, "346")
                .withField(new UnknownField("ppn_gvk"), "591166003")
                .withField(StandardField.SUBTITLE, "[revised and updated for JAVA SE 6]");

        bibEntryPPN66391437X = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Effective unit testing")
                .withField(StandardField.PUBLISHER, "Manning")
                .withField(StandardField.YEAR, "2013")
                .withField(StandardField.AUTHOR, "Koskela, Lasse")
                .withField(StandardField.ADDRESS, "Shelter Island, NY")
                .withField(StandardField.ISBN, "9781935182573")
                .withField(StandardField.PAGETOTAL, "223")
                .withField(new UnknownField("ppn_gvk"), "66391437X")
                .withField(StandardField.SUBTITLE, "A guide for Java developers");
    }

    @Test
    public void testGetName() {
        assertEquals("GVK", fetcher.getName());
    }

    @Test
    public void simpleSearchQueryURLCorrect() throws Exception {
        String query = "java jdk";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(query, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        URL url = fetcher.getURLForQuery(luceneQuery);
        assertEquals("https://sru.k10plus.de/opac-de-627?version=1.1&operation=searchRetrieve&query=pica.all%3Djava+and+pica.all%3Djdk&maximumRecords=50&recordSchema=picaxml&sortKeys=Year%2C%2C1", url.toString());
    }

    @Test
    public void complexSearchQueryURLCorrect() throws Exception {
        String query = "kon:java tit:jdk";
        QueryNode luceneQuery = new StandardSyntaxParser().parse(query, AbstractQueryTransformer.NO_EXPLICIT_FIELD);
        URL url = fetcher.getURLForQuery(luceneQuery);
        assertEquals("https://sru.k10plus.de/opac-de-627?version=1.1&operation=searchRetrieve&query=pica.kon%3Djava+and+pica.tit%3Djdk&maximumRecords=50&recordSchema=picaxml&sortKeys=Year%2C%2C1", url.toString());
    }

    @Test
    public void testPerformSearchMatchingMultipleEntries() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("title:\"effective java\"");
        assertTrue(searchResult.contains(bibEntryPPN591166003));
        assertTrue(searchResult.contains(bibEntryPPN66391437X));
    }

    @Test
    public void testPerformSearch591166003() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("ppn:591166003");
        assertEquals(Collections.singletonList(bibEntryPPN591166003), searchResult);
    }

    @Test
    public void testPerformSearch66391437X() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("ppn:66391437X");
        assertEquals(Collections.singletonList(bibEntryPPN66391437X), searchResult);
    }

    @Test
    public void testPerformSearchEmpty() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("");
        assertEquals(Collections.emptyList(), searchResult);
    }
}
