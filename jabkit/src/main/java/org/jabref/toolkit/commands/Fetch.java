package org.jabref.toolkit.commands;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.toolkit.exception.CliException;
import org.jabref.toolkit.service.ExportService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "fetch", description = "Fetch entries from a provider.")
class Fetch implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Fetch.class);

    record Provider(String name, String query) {
    }

    @ParentCommand
    private JabKit argumentProcessor;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @Option(names = "--provider", required = true)
    private String provider;

    @Option(names = "--query")
    private String query;

    @Option(names = "--output")
    private Path outputFile;

    @Override
    public Integer call() throws CliException {
        Set<SearchBasedFetcher> fetchers = WebFetchers.getSearchBasedFetchers(
                argumentProcessor.cliPreferences.getImportFormatPreferences(),
                argumentProcessor.cliPreferences.getImporterPreferences());
        SearchBasedFetcher selectedFetcher = fetchers.stream()
                                                     .filter(fetcher -> fetcher.getName().equalsIgnoreCase(provider))
                                                     .findFirst()
                                                     .orElseThrow(() -> new CliException(
                                                             "Could not find fetcher '" + provider + "'",
                                                             Localization.lang("Could not find fetcher '%0'", provider),
                                                             CommandLine.ExitCode.USAGE));

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Running query '%0' with fetcher '%1'.", query, provider));
            System.out.print(Localization.lang("Please wait..."));
        }

        try {
            List<BibEntry> matches = selectedFetcher.performSearch(query);
            if (matches.isEmpty()) {
                System.out.println("\r" + Localization.lang("No results found."));
                return CommandLine.ExitCode.OK;
            }

            if (!sharedOptions.porcelain) {
                System.out.println("\r" + Localization.lang("Found %0 results.", String.valueOf(matches.size())));
            }

            if (outputFile != null) {
                ExportService.create(argumentProcessor.cliPreferences, sharedOptions.porcelain).saveDatabase(
                        new BibDatabase(matches),
                        outputFile);
            } else {
                System.out.println(matches.stream().map(BibEntry::toString).collect(Collectors.joining("\n\n")));
            }
            return CommandLine.ExitCode.OK;
        } catch (FetcherException e) {
            LOGGER.error("Error while fetching", e);
            return 2;
        }
    }
}
