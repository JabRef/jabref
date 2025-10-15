package org.jabref.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "fetch", description = "Fetch entries from a provider.")
class Fetch implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Fetch.class);

    record Provider(String name, String query) {
    }

    @ParentCommand
    private JabKitArgumentProcessor argumentProcessor;

    @Mixin
    private JabKitArgumentProcessor.SharedOptions sharedOptions = new JabKitArgumentProcessor.SharedOptions();

    @Option(names = "--provider", required = true)
    private String provider;

    @Option(names = "--query")
    private String query;

    @Option(names = "--output")
    private Path outputFile;

    @Override
    public void run() {
        Set<SearchBasedFetcher> fetchers = WebFetchers.getSearchBasedFetchers(
                argumentProcessor.cliPreferences.getImportFormatPreferences(),
                argumentProcessor.cliPreferences.getImporterPreferences());
        Optional<SearchBasedFetcher> selectedFetcher = fetchers.stream()
                                                               .filter(fetcher -> fetcher.getName().equalsIgnoreCase(provider))
                                                               .findFirst();
        if (selectedFetcher.isEmpty()) {
            System.out.println(Localization.lang("Could not find fetcher '%0'", provider));
            return;
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Running query '%0' with fetcher '%1'.", query, provider));
            System.out.print(Localization.lang("Please wait..."));
        }

        try {
            List<BibEntry> matches = selectedFetcher.get().performSearch(query);
            if (matches.isEmpty()) {
                System.out.println("\r" + Localization.lang("No results found."));
                return;
            }

            if (!sharedOptions.porcelain) {
                System.out.println("\r" + Localization.lang("Found %0 results.", String.valueOf(matches.size())));
            }

            if (outputFile != null) {
                JabKitArgumentProcessor.saveDatabase(
                        argumentProcessor.cliPreferences,
                        argumentProcessor.entryTypesManager,
                        new BibDatabase(matches),
                        outputFile);
            } else {
                System.out.println(matches.stream().map(BibEntry::toString).collect(Collectors.joining("\n\n")));
            }
        } catch (FetcherException e) {
            LOGGER.error("Error while fetching", e);
        }
    }
}
