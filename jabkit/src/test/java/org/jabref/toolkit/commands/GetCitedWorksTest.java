package org.jabref.toolkit.commands;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.toolkit.exception.CliExceptionHandler;
import org.jabref.toolkit.service.CitationFetcherFactory;
import org.jabref.toolkit.service.ExportService;
import org.jabref.toolkit.util.CommandFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
        // TODO: redirect stdout/stderr for capturing and clean unit test output
        commandLine.setExecutionExceptionHandler(new CliExceptionHandler(commandLine.getExecutionExceptionHandler()));

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
        doNothing().when(mockExportService).printBibEntriesToStdOut(captor.capture());

        int exitCode = commandLine.execute("get-cited-works", "10.3390/su131810256");

        assertEquals(CommandLine.ExitCode.OK, exitCode);
        List<BibEntry> exported = captor.getValue();
        assertEquals(citedWorks, exported);
    }

    @Test
    void doiLookupWithOutputFileAndFormatExportsFetcherResults() throws Exception {
        // mock the citation fetcher with sample results
        List<BibEntry> citedWorks = List.of(
                creatBibEntry("Cited Work 1", "10.1000/xyz123"),
                creatBibEntry("Cited Work 2", "10.1000/abc456")
        );
        when(citationFetcher.getReferences(any())).thenReturn(citedWorks);

        // mock the export service to do nothing and capture the argument
        ArgumentCaptor<List<BibEntry>> entries = ArgumentCaptor.captor();
        ArgumentCaptor<Path> file = ArgumentCaptor.captor();
        ArgumentCaptor<String> format = ArgumentCaptor.captor();
        doNothing().when(mockExportService).exportEntriesToFile(entries.capture(), file.capture(), format.capture());

        int exitCode = commandLine.execute(
                "get-cited-works", "10.3390/su131810256",
                "--output", "out.file",
                "--output-format", "Klingon"
        );

        assertEquals(CommandLine.ExitCode.OK, exitCode);

        assertEquals(citedWorks, entries.getValue());
        assertEquals(Path.of("out.file"), file.getValue());
        assertEquals("Klingon", format.getValue());
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
        when(citationFetcher.getReferences(any())).thenThrow(
                new FetcherException("Fetcher: Failed to fetch (negative test)"));

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
