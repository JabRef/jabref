package org.jabref.toolkit.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.toolkit.exception.ExportServiceException;
import org.jabref.toolkit.service.ExportService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/// Headless equivalent of the "fetch by identifier" button next to the DOI/ISBN/ArXiv fields in
/// the entry editor. Unlike that button, there is no interactive merge step here: each identifier
/// always produces a freshly fetched entry.
@Command(name = "fetch-by-id", description = "Fetches a BibTeX entry for one or more identifiers (DOI, ISBN, ArXiv ID, ISSN, ...)")
class FetchByIdentifier implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FetchByIdentifier.class);

    @CommandLine.ParentCommand
    private JabKit argumentProcessor;

    @CommandLine.Mixin
    private JabKit.SharedOptions sharedOptions;

    @Parameters(paramLabel = "IDENTIFIER", description = "one or more identifiers to fetch (DOI, ISBN, ArXiv ID, ISSN, ...)", arity = "1..*")
    private String[] identifiers;

    @Override
    public Integer call() throws ExportServiceException {
        CompositeIdFetcher fetcher = new CompositeIdFetcher(argumentProcessor.cliPreferences.getImportFormatPreferences());
        List<BibEntry> entries = new ArrayList<>(identifiers.length);

        for (String identifier : identifiers) {
            if (!CompositeIdFetcher.containsValidId(identifier)) {
                LOGGER.warn("Skipped {}, because it is not a recognized identifier", identifier);
                System.err.println(Localization.lang("Identifier %0 is invalid", identifier));
                System.err.println();
                continue;
            }

            Optional<BibEntry> entry;
            try {
                entry = fetcher.performSearchById(identifier);
            } catch (FetcherException e) {
                LOGGER.error("Could not fetch BibTeX based on identifier", e);
                System.err.print(Localization.lang("No data was found for the identifier"));
                System.err.println(" - " + identifier);
                System.err.println(e.getLocalizedMessage());
                System.err.println();
                continue;
            }

            if (entry.isEmpty()) {
                LOGGER.error("Could not fetch BibTeX based on identifier - entry is empty");
                System.err.print(Localization.lang("No data was found for the identifier"));
                System.err.println(" - " + identifier);
                System.err.println();
                continue;
            }

            entries.add(entry.get());
        }

        ExportService.create(argumentProcessor.cliPreferences, sharedOptions.porcelain).printBibEntriesToStdOut(entries);
        return CommandLine.ExitCode.OK;
    }
}
