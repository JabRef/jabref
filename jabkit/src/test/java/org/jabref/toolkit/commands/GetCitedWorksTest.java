package org.jabref.toolkit.commands;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.toolkit.service.CitationFetcherFactory;
import org.jabref.toolkit.service.ExportService;
import org.jabref.toolkit.util.CommandFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetCitedWorksTest extends AbstractJabKitTest {

    private CitationFetcherFactory mockFetcherFactory;
    private ExportService mockExportService;
    private CitationFetcher citationFetcher;

    @BeforeEach
    void setupMocks() {
        mockFetcherFactory = mock();
        mockExportService = mock();

        GetCitedWorks sut = new GetCitedWorks() {
            @Override
            protected void initFields() {
                this.citationFetcherFactory = mockFetcherFactory;
                this.exportService = mockExportService;
            }
        };

        JabKit jabKit = new JabKit(preferences, entryTypesManager);
        CommandFactory factory = new CommandFactory(sut);
        this.commandLine = new CommandLine(jabKit, factory);

        citationFetcher = mock(CitationFetcher.class);
        when(mockFetcherFactory.getCitationFetcher(any())).thenReturn(citationFetcher);
    }

    @Test
    void simpleDoiLookupPrintsFetcherResults() throws Exception {
        // mock the citation fetcher with sample results
        List<BibEntry> citedWorks = List.of(
                creatBibEntry("Cited Work 1", "10.1000/xyz123"),
                creatBibEntry("Cited Work 2", "10.1000/abc456")
        );
        when(citationFetcher.getReferences(any())).thenReturn(citedWorks);

        // mock the export service to do nothing and capture the argument
        ArgumentCaptor<List<BibEntry>> captor = ArgumentCaptor.captor();
        // TODO refactor outputEntries to only consume (not return int) -> use `doNothing()` instead
        doReturn(CommandLine.ExitCode.OK).when(mockExportService).printBibEntries(captor.capture());

        int exitCode = commandLine.execute("get-cited-works", "10.3390/su131810256");

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        List<BibEntry> exported = captor.getValue();
        assertEquals(citedWorks, exported);
    }

    @Test
    void unknownDoiLookupExitsSuccessfully() throws Exception {
        List<BibEntry> citedWorks = List.of();
        when(citationFetcher.getReferences(any())).thenReturn(citedWorks);

        int exitCode = commandLine.execute("get-cited-works", "10.1000/2045-climate101");

        assertEquals(CommandLine.ExitCode.OK, exitCode);
    }

    @Test
    void fetcherExceptionExitsSoftwareError() throws Exception {
        when(citationFetcher.getReferences(any())).thenThrow(new FetcherException("Failed to fetch"));

        int exitCode = commandLine.execute("get-cited-works", "10.1000/2026valid01");

        assertEquals(CommandLine.ExitCode.SOFTWARE, exitCode);
    }

    private static BibEntry creatBibEntry(String title, String doi) {
        return new BibEntry()
                .withField(StandardField.TITLE, title)
                .withField(StandardField.DOI, doi)
                .withChanged(true);
    }
}
