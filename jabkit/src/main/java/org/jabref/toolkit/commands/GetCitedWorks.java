package org.jabref.toolkit.commands;

import java.util.List;
import java.util.concurrent.Callable;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.importer.fetcher.citation.crossref.CrossRefCitationFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
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

    @CommandLine.Option(
            names = "--provider",
            description = "Metadata provider: ${COMPLETION-CANDIDATES}"
    )
    private Provider provider = Provider.crossref;

    @CommandLine.Parameters(description = "DOI to check")
    private String doi;

    @Override
    public Integer call() {
        CitationFetcher citationFetcher = switch (provider) {
            case crossref -> {
                CliPreferences preferences = argumentProcessor.cliPreferences;
                AiService aiService = new AiService(
                        preferences.getAiPreferences(),
                        preferences.getFilePreferences(),
                        preferences.getCitationKeyPatternPreferences(),
                        LOGGER::info,
                        new CurrentThreadTaskExecutor());
                yield new CrossRefCitationFetcher(
                        preferences.getImporterPreferences(),
                        preferences.getImportFormatPreferences(),
                        preferences.getCitationKeyPatternPreferences(),
                        preferences.getGrobidPreferences(),
                        aiService);
            }
            case semanticscholar ->
                    new SemanticScholarCitationFetcher(
                            argumentProcessor.cliPreferences.getImporterPreferences()
                    );
        };

        List<BibEntry> entries;

        try {
            entries = citationFetcher.getReferences(new BibEntry().withField(StandardField.DOI, doi));
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

    private enum Provider {
        crossref,
        semanticscholar
    }
}
