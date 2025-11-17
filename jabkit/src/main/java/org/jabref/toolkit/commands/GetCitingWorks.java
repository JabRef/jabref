package org.jabref.toolkit.commands;

import java.util.List;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "get-citing-works", description = "Outputs a list of works citting the work at hand")
class GetCitingWorks implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetCitingWorks.class);

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
             entries = citationFetcher.searchCitedBy(new BibEntry().withField(StandardField.DOI, doi));
        } catch (FetcherException e) {
            LOGGER.error("Could not fetch citation information based on DOI", e);
            System.err.print(Localization.lang("No data was found for the identifier"));
            System.err.println(" - " + doi);
            System.err.println(e.getLocalizedMessage());
            System.err.println();
            return 2;
        }

        return JabKit.outputEntries(argumentProcessor.cliPreferences, entries);
    }
}
