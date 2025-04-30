package org.jabref.cli;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Option;

/**
 * Run an entry fetcher from the command line.
 *
 * Takes a string containing both the name of the fetcher to use and the search query, separated by a :
 * Outputs a TODO: @return A parser result containing the entries fetched or null if an error occurred.
 */
@Command(name = "fetch", description = "Fetch entries from a provider.")
class Fetch implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArgumentProcessor.class);

    @CommandLine.ParentCommand
    private KitCommandLine kitCommandLine;

    @Option(names = "--provider", required = true)
    private String provider;

    @Option(names = "--query")
    private String query;

    @Option(names = "--output", required = true)
    private String output;

    @Option(names = "--append", description = "Append to existing file")
    private boolean append;

    @Override
    public Integer call() {
        if ((provider == null) || !provider.contains(":")) {
            System.out.println(Localization.lang("Expected syntax for --fetch='<name of fetcher>:<query>'"));
            System.out.println(Localization.lang("The following fetchers are available:"));
            System.out.println(kitCommandLine.)

            return 1;
        }

        String[] split = fetchCommand.split(":");
        String engine = split[0];
        String query = split[1];

        Set<SearchBasedFetcher> fetchers = WebFetchers.getSearchBasedFetchers(
                cliPreferences.getImportFormatPreferences(),
                cliPreferences.getImporterPreferences());
        Optional<SearchBasedFetcher> selectedFetcher = fetchers.stream()
                                                               .filter(fetcher -> fetcher.getName().equalsIgnoreCase(engine))
                                                               .findFirst();
        if (selectedFetcher.isEmpty()) {
            System.out.println(Localization.lang("Could not find fetcher '%0'", engine));

            System.out.println(Localization.lang("The following fetchers are available:"));
            fetchers.forEach(fetcher -> System.out.println("  " + fetcher.getName()));

            return Optional.empty();
        } else {
            System.out.println(Localization.lang("Running query '%0' with fetcher '%1'.", query, engine));
            System.out.print(Localization.lang("Please wait..."));
            try {
                List<BibEntry> matches = selectedFetcher.get().performSearch(query);
                if (matches.isEmpty()) {
                    System.out.println("\r" + Localization.lang("No results found."));
                    return Optional.empty();
                } else {
                    System.out.println("\r" + Localization.lang("Found %0 results.", String.valueOf(matches.size())));
                    return Optional.of(new ParserResult(matches));
                }
            } catch (
                    FetcherException e) {
                LOGGER.error("Error while fetching", e);
                return Optional.empty();
            }
        }

        System.out.println("Not yet implemented");
        return 0;
    }
}
