package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
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
    private final String URL_PAGE = "https://www.researchgate.net/publication/4207355_Paranoid_a_global_secure_file_access_control_system";
    private ResearchGate fetcher;
    private BibEntry entry;

    @BeforeEach
    public void setUp() {
        fetcher = new ResearchGate(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
        entry = new BibEntry(StandardEntryType.InProceedings);
        entry.setField(StandardField.DOI, "10.1109/CSAC.2005.42");
        entry.setField(StandardField.TITLE, "Paranoid: a global secure file access control system");
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextFoundByDOI() throws IOException, FetcherException {
        assertEquals(Optional.of(new URL(URL_PDF)), fetcher.findFullText(entry));
    }

    @Test
    @DisabledOnCIServer("CI server is unreliable")
    void fullTextNotFoundByDOI() throws IOException, FetcherException {
        BibEntry entry2 = new BibEntry().withField(StandardField.DOI, "10.1021/bk-2006-WWW.ch014");
        assertEquals(Optional.empty(), fetcher.findFullText(entry2));
    }

    @Test
    void getDocumentByTitle() throws IOException, FetcherException {
        String source = fetcher.getURLByString(entry.getTitle().get());
        assertTrue(source.startsWith(URL_PAGE));
    }

    @Test
    void getDocumentByDOI() throws IOException, FetcherException {
        String source = fetcher.getURLByDoi(entry.getDOI().get());
        assertEquals(URL_PAGE, source);
    }

    @Test
    void trustLevel() {
        assertEquals(TrustLevel.META_SEARCH, fetcher.getTrustLevel());
    }

    @Test
    void performSearchWithString() throws Exception {
        BibEntry master = new BibEntry(StandardEntryType.PhdThesis)
                .withCitationKey("phdthesis")
                .withField(StandardField.AUTHOR, "Diez, Tobias")
                .withField(StandardField.TITLE, "Slice theorem for Fréchet group actions and covariant symplectic field theory")
                .withField(StandardField.MONTH, "10")
                .withField(StandardField.YEAR, "2013");
        List<BibEntry> fetchedEntries = fetcher.performSearch("Slice theorem for Fréchet group actions and covariant symplectic");
        assertEquals(Optional.of(master), fetchedEntries.stream().findFirst());
    }

    @Test
    void performSearchWithLuceneQuery() throws Exception {
        BibEntry master = new BibEntry(StandardEntryType.Article)
                .withCitationKey("article")
                .withField(StandardField.TITLE, "Wine Microbiology and Predictive Microbiology: " +
                        "A Short Overview on Application, and Perspectives")
                .withField(StandardField.DOI, "10.3390/microorganisms10020421")
                .withField(StandardField.JOURNAL, "Microorganisms")
                .withField(StandardField.MONTH, "02")
                .withField(StandardField.PAGES, "421")
                .withField(StandardField.VOLUME, "10")
                .withField(StandardField.YEAR, "2022")
                .withField(StandardField.AUTHOR, "Petruzzi, Leonardo and Campaniello, Daniela and Corbo," +
                        " Maria and Speranza, Barbara and Altieri, Clelia and Sinigaglia, Milena and Bevilacqua, Antonio");

        QueryNode queryNode = new StandardSyntaxParser().parse("Wine Microbiology and Predictive " +
                "Microbiology: A Short Overview on Application, and Perspectives", NO_EXPLICIT_FIELD);
        assertEquals(Optional.of(master), fetcher.performSearch(queryNode).stream().findFirst());
    }

    @Test
    void performSearchWithBibEntry() throws FetcherException {
        entry.withCitationKey("inproceedings")
             .withField(StandardField.ISBN, "0-7695-2461-3")
             .withField(StandardField.MONTH, "01")
             .withField(StandardField.PAGES, "9 pp.-")
             .withField(StandardField.YEAR, "2006")
             .withField(StandardField.AUTHOR, "Zaffar, F. and Kedem, G. and Gehani, Abdurrazzak");
        assertEquals(Optional.of(entry), fetcher.performSearch(entry).stream().findFirst());
    }
}
