package org.jabref.toolkit.commands;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "get-cited-works", description = "Outputs a list of works cited (\"bibliography\")")
class GetCitedWorks implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetCitedWorks.class);

    @CommandLine.ParentCommand
    private JabKit argumentProcessor;

    @CommandLine.Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @CommandLine.Parameters(description = "DOI to check")
    private String doi;

    @Override
    public Integer call() {
        CitationFetcher citationFetcher = new SemanticScholarCitationFetcher(argumentProcessor.cliPreferences.getImporterPreferences());

        List<BibEntry> entries;

        try {
             entries = citationFetcher.searchCiting(new BibEntry().withField(StandardField.DOI, doi));
        } catch (FetcherException e) {
            LOGGER.error("Could not fetch citation information based on DOI", e);
            System.err.print(Localization.lang("No data was found for the identifier"));
            System.err.println(" - " + doi);
            System.err.println(e.getLocalizedMessage());
            System.err.println();
            return 2;
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
            BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(entries));
            BibDatabaseWriter bibWriter = new BibDatabaseWriter(writer, context, argumentProcessor.cliPreferences);
            bibWriter.writeDatabase(context);
        } catch (IOException e) {
            LOGGER.error("Could not write BibTeX", e);
            System.err.println(Localization.lang("Unable to write to %0.", "stdout"));
            return 1;
        }
        return 0;
    }
}
