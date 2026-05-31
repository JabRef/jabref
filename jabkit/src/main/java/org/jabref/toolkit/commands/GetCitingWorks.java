package org.jabref.toolkit.commands;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.toolkit.converter.CitationFetcherTypeConverter;
import org.jabref.toolkit.exception.ExportException;
import org.jabref.toolkit.service.CitationFetcherFactory;
import org.jabref.toolkit.service.ExportService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "get-citing-works", description = "Outputs a list of works citing the work at hand")
class GetCitingWorks implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetCitingWorks.class);

    protected CitationFetcherFactory citationFetcherFactory;

    protected ExportService exportService;

    @CommandLine.ParentCommand
    private JabKit argumentProcessor;

    @CommandLine.Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @CommandLine.Option(
            names = "--output",
            description = "Write output to this file (e.g. --output=out.bib)"
    )
    private Path outputFile;

    @CommandLine.Option(
            names = "--output-format",
            description = "Output format (e.g. bibtex)",
            defaultValue = "bibtex"
    )
    private String outputFormat;

    @CommandLine.Option(
            names = "--provider",
            converter = CitationFetcherTypeConverter.class,
            description = "Metadata provider: ${COMPLETION-CANDIDATES}"
    )
    private CitationFetcherType citationFetcherType = CitationFetcherType.OPEN_CITATIONS;

    @CommandLine.Parameters(description = "DOI to check")
    private String doi;

    void initFields() {
        citationFetcherFactory = CitationFetcherFactory.create(argumentProcessor.cliPreferences);
        exportService = ExportService.create(argumentProcessor.cliPreferences);
    }

    @Override
    public Integer call() throws ExportException, FetcherException {
        initFields();

        CitationFetcher citationFetcher = citationFetcherFactory.getCitationFetcher(citationFetcherType);

        List<BibEntry> entries = citationFetcher.getCitations(new BibEntry().withField(StandardField.DOI, doi));

        if (outputFile == null) {
            exportService.printBibEntriesToStdOut(entries);
        } else {
            exportService.exportEntriesToFile(entries, outputFile, outputFormat);
        }
        return CommandLine.ExitCode.OK;
    }
}
