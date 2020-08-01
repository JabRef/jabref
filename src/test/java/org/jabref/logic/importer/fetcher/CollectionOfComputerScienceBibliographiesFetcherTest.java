package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class CollectionOfComputerScienceBibliographiesFetcherTest {
    private CollectionOfComputerScienceBibliographiesFetcher fetcher;

    @BeforeEach
    public void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        fetcher = new CollectionOfComputerScienceBibliographiesFetcher(importFormatPreferences);
    }

    @Test
    public void getNameReturnsCorrectName() {
        assertEquals("Collection of Computer Science Bibliographies", fetcher.getName());
    }

    @Test
    public void getUrlForQueryReturnsCorrectUrl() throws MalformedURLException, URISyntaxException, FetcherException {
        String query = "java jdk";
        URL url = fetcher.getURLForQuery(query);
        assertEquals("http://liinwww.ira.uka.de/bibliography/rss?query=java+jdk&sort=score", url.toString());
    }

    @Test
    public void performSearchReturnsMatchingMultipleEntries() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("jabref");

        BibEntry firstBibEntry = new BibEntry(StandardEntryType.InProceedings)
                .withCiteKey("conf/ecsa/OlssonEW17")
                .withField(StandardField.AUTHOR, "Tobias Olsson and Morgan Ericsson and Anna Wingkvist")
                .withField(StandardField.EDITOR, "Rog{\\~A}{\\copyright}rio de Lemos")
                .withField(StandardField.ISBN, "978-1-4503-5217-8")
                .withField(StandardField.PAGES, "152--158")
                .withField(StandardField.PUBLISHER, "ACM")
                .withField(StandardField.TITLE, "The relationship of code churn and architectural violations in the open source software JabRef")
                .withField(StandardField.URL, "http://dl.acm.org/citation.cfm?id=3129790")
                .withField(StandardField.YEAR, "2017")
                .withField(StandardField.BOOKTITLE, "11th European Conference on Software Architecture, ECSA 2017, Companion Proceedings, Canterbury, United Kingdom, September 11-15, 2017")
                .withField(new UnknownField("bibsource"), "DBLP, http://dblp.uni-trier.de/https://doi.org/10.1145/3129790.3129810; DBLP, http://dblp.uni-trier.de/db/conf/ecsa/ecsa2017c.html#OlssonEW17")
                .withField(new UnknownField("bibdate"), "2018-11-06");

        BibEntry secondBibEntry = new BibEntry(StandardEntryType.Article)
                .withCiteKey("oai:DiVA.org:lnu-68408")
                .withField(new UnknownField("identifier"), "urn:isbn:978-1-4503-5217-8; doi:10.1145/3129790.3129810; ISI:000426556400034")
                .withField(new UnknownField("subject"), "Software Architecture; Code Churn; Open Source; Architecrual Erosion; Technical Debt; Software Engineering; Programvaruteknik")
                .withField(new UnknownField("relation"), "ACM International Conference Proceeding Series; ECSA '17~Proceedings of the 11th European Conference on Software Architecture : Companion Proceedings, p. 152-158")
                .withField(StandardField.ABSTRACT, "The open source application JabRef has existed since" +
                        " 2003. In 2015, the developers decided to make an" +
                        " architectural refactoring as continued development was" +
                        " deemed too demanding. The developers also introduced" +
                        " Static Architecture Conformance Checking (SACC) to" +
                        " prevent violations to the intended architecture." +
                        " Measurements mined from source code repositories such" +
                        " as code churn and code ownership has been linked to" +
                        " several problems, for example fault proneness, security" +
                        " vulnerabilities, code smells, and degraded" +
                        " maintainability. The root cause of such problems can be" +
                        " architectural. To determine the impact of the" +
                        " refactoring of JabRef, we measure the code churn and" +
                        " code ownership before and after the refactoring and" +
                        " find that large files with violations had a" +
                        " significantly higher code churn than large files" +
                        " without violations before the refactoring. After the" +
                        " refactoring, the files that had violations show a more" +
                        " normal code churn. We find no such effect on code" +
                        " ownership. We conclude that files that contain" +
                        " violations detectable by SACC methods are connected to" +
                        " higher than normal code churn.")
                .withField(StandardField.TYPE, "info:eu-repo/semantics/conferenceObject")
                .withField(new UnknownField("description"), "Information and Software Qualtiy")
                .withField(StandardField.PAGES, "152--158")
                .withField(new UnknownField("bibsource"), "OAI-PMH server at www.diva-portal.org")
                .withField(new UnknownField("rights"), "info:eu-repo/semantics/openAccess")
                .withField(StandardField.URL, "http://urn.kb.se/resolve?urn=urn:nbn:se:lnu:diva-68408")
                .withField(new UnknownField("oai"), "oai:DiVA.org:lnu-68408")
                .withField(StandardField.TITLE, "The relationship of code churn and architectural violations in the open source software JabRef")
                .withField(StandardField.PUBLISHER, "Linn{\\'e}universitetet, Institutionen f{\\\"o}r datavetenskap (DV); Linn{\\'e}universitetet, Institutionen f{\\\"o}r datavetenskap (DV); Linn{\\'e}universitetet, Institutionen f{\\\"o}r datavetenskap (DV); New York, NY, USA")
                .withField(StandardField.LANGUAGE, "eng")
                .withField(StandardField.AUTHOR, "Tobias Olsson and Morgan Ericsson and Anna Wingkvist")
                .withField(StandardField.YEAR, "2017");

        // Checking entries in the set as the query is generic and returns a changing result set
        assertTrue(searchResult.contains(firstBibEntry));
        assertTrue(searchResult.contains(secondBibEntry));
    }

    @Test
    public void performSearchReturnsEmptyListForEmptySearch() throws FetcherException {
        List<BibEntry> searchResult = fetcher.performSearch("");
        assertEquals(Collections.emptyList(), searchResult);
    }
}
