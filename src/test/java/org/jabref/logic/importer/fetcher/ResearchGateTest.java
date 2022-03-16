package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.jabref.logic.importer.fetcher.transformers.AbstractQueryTransformer.NO_EXPLICIT_FIELD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@FetcherTest
public class ResearchGateTest {

    private static final String URL_PDF = "https://www.researchgate.net/profile/Abdurrazzak-Gehani/publication/4207355_Paranoid_a_global_secure_file_access_control_system/links/5457747d0cf2cf516480995e/Paranoid-a-global-secure-file-access-control-system.pdf";
    private final String URL_PAGE = "https://www.researchgate.net/lite.publication.PublicationDownloadCitationModal.downloadCitation.html?fileType=BibTeX&citation=citationAndAbstract&publicationUid=4207355";
    private final String URL_TITLE = "https://www.researchgate.net/search/publication?q=Paranoid%253A%2Ba%2Bglobal%2Bsecure%2Bfile%2Baccess%2Bcontrol%2Bsystem&_sg=hAMDrqbG_CEiuRGM6Rk6Ljc__OnhF8x3j3y5p4vnpRQ5zkYbGq6Y5zOhHyM0NzH0-CVuHnSPYkEbcb0xVaZPM-DQ6cA";
    private ResearchGate fetcher;
    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        fetcher = new ResearchGate(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
        entry = new BibEntry();
        entry.setField(StandardField.DOI, "10.1109/CSAC.2005.42");
        entry.setField(StandardField.TITLE, "Paranoid: a global secure file access control system");
    }

    @Test
    void getDocumentByDOI() throws IOException {
        String source = fetcher.getURLByDoi(entry.getDOI().get());
        assertEquals(URL_PAGE, source);
    }

    @Test
    void getURLByQuery() throws IOException, QueryNodeParseException, FetcherException, URISyntaxException {
        SyntaxParser parser = new StandardSyntaxParser();
        QueryNode queryNode;
        queryNode = parser.parse("Paranoid: a global secure file access control system", NO_EXPLICIT_FIELD);

//        URL source = fetcher.getURLForQuery(queryNode);
//        assertEquals(URL_PAGE, source.toString());
    }

    @Test
    void getDocumentByTitle() throws IOException {
        String source = fetcher.getURLByString(entry.getTitle().get());
        assertTrue(source.startsWith(URL_PAGE));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextFoundByDOI() throws IOException {
        assertEquals(
                Optional.of(new URL(URL_PDF)),
                fetcher.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextNotFoundByDOI() throws IOException {
        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.DOI, "10.1021/bk-2006-WWW.ch014");

        assertEquals(Optional.empty(), fetcher.findFullText(entry2));
    }

    @Test
    void trustLevel() {
        assertEquals(TrustLevel.META_SEARCH, fetcher.getTrustLevel());
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry master = new BibEntry(StandardEntryType.PhdThesis)
                .withField(StandardField.AUTHOR, "Diez, Tobias")
                .withField(StandardField.TITLE, "Slice theorem for Fréchet group actions and covariant symplectic field theory")
                .withField(StandardField.MONTH, "10")
                .withField(StandardField.YEAR, "2013");
        List<BibEntry> fetchedEntries = fetcher.performSearch("Slice theorem for Fréchet group actions and covariant symplectic");
        // Abstract should not be included in JabRef tests
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertEquals(Collections.singletonList(master), fetchedEntries);
    }

    @Test
    void searchByQueryFindsMultipleEntries() throws Exception {
        BibEntry master = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Wine Microbiology and Predictive Microbiology: A Short Overview on Application, and Perspectives")
                .withField(StandardField.DOI, "10.3390/microorganisms10020421")
                .withField(StandardField.JOURNAL, "Microorganisms")
                .withField(StandardField.MONTH, "02")
                .withField(StandardField.PAGES, "421")
                .withField(StandardField.VOLUME, "10")
                .withField(StandardField.YEAR, "2022")
                .withField(StandardField.AUTHOR, "Petruzzi, Leonardo and Campaniello, Daniela and Corbo, Maria and Speranza, Barbara and Altieri, Clelia and Sinigaglia, Milena and Bevilacqua, Antonio");
        List<BibEntry> fetchedEntries = fetcher.performSearch("Wine Microbiology and Predictive Microbiology: A Short Overview on Application, and Perspectives");
        // Abstract should not be included in JabRef tests
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertEquals(Collections.singletonList(master), fetchedEntries);
    }
}
