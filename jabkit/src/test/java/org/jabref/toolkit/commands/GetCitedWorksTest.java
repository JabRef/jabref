package org.jabref.toolkit.commands;

import java.util.List;

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

    @BeforeEach
    void setupMocks() throws Exception {
        mockFetcherFactory = mock();
        mockExportService = mock();

        GetCitedWorks sut = new GetCitedWorks() {
            @Override
            protected void init() {
                this.citationFetcherFactory = mockFetcherFactory;
                this.exportService = mockExportService;
            }
        };

        JabKit jabKit = new JabKit(preferences, entryTypesManager);
        CommandFactory factory = new CommandFactory(sut);
        this.commandLine = new CommandLine(jabKit, factory);

        CitationFetcher citationFetcher = mock(CitationFetcher.class);
        when(mockFetcherFactory.getCitationFetcher(any())).thenReturn(citationFetcher);

        // mock the returned fetcher with sample results
        when(citationFetcher.getReferences(any()))
                .thenReturn(List.of(
                        new BibEntry()
                                .withField(StandardField.TITLE, "Cited Work 1")
                                .withField(StandardField.DOI, "10.1000/xyz123")
                                .withChanged(true),
                        new BibEntry()
                                .withField(StandardField.TITLE, "Cited Work 2")
                                .withField(StandardField.DOI, "10.1000/abc456")
                                .withChanged(true)
                ));
    }

    @Test
    void existingDoiPrintsToStdoutWithDefaults() {
        // mock the export service to do nothing and capture the argument
        ArgumentCaptor<List<BibEntry>> captor = ArgumentCaptor.captor();
        // TODO refactor outputEntries to only consume (not return int) -> use `doNothing()` instead
        doReturn(CommandLine.ExitCode.OK).when(mockExportService).outputEntries(captor.capture());

        int exitCode = commandLine.execute("get-cited-works", "10.3390/su131810256");

        assertEquals(0, exitCode);
        List<BibEntry> exported = captor.getValue();
        assertEquals(List.of(new BibEntry(), new BibEntry()), exported);
    }
}
