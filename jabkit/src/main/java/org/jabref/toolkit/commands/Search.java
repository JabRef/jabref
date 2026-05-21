package org.jabref.toolkit.commands;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.LibrarySearcher;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.search.inmemory.InMemoryLibrarySearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.toolkit.converter.CygWinPathConverter;
import org.jabref.toolkit.util.ExportService;
import org.jabref.toolkit.util.ImportService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "search", description = "Search in a library.")
class Search implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Search.class);

    @ParentCommand
    private JabKit argumentProcessor;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @Option(names = {"--query"}, description = "Search query", required = true)
    private String query;

    @Mixin
    private InputOption inputOption = new InputOption();

    @Option(names = {"--output"}, converter = CygWinPathConverter.class, description = "Output file")
    private Path outputFile;

    @Option(names = {"--output-format"}, description = "Output format: bib, txt, etc.")
    private String outputFormat = "bibtex";

    @Override
    public Integer call() {
        Path inputFile = inputOption.getInputFile();

        Optional<ParserResult> parserResult = ImportService.importBibTexFile(inputFile, argumentProcessor.cliPreferences, sharedOptions.porcelain);
        if (parserResult.isEmpty()) {
            return 2;
        }

        SearchPreferences searchPreferences = argumentProcessor.cliPreferences.getSearchPreferences();
        SearchQuery searchQuery = new SearchQuery(query, searchPreferences.getSearchFlags());

        BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();
        LibrarySearcher searcher = new InMemoryLibrarySearcher(databaseContext, argumentProcessor.cliPreferences.getBibEntryPreferences());
        List<BibEntry> matches = searcher.getMatches(searchQuery);

        if (matches.isEmpty()) {
            System.out.println(Localization.lang("No search matches."));
            return 0;
        }

        if ("bibtex".equals(outputFormat)) {
            ExportService.saveDatabase(matches, argumentProcessor.cliPreferences, argumentProcessor.entryTypesManager, outputFile);
            LOGGER.debug("Finished export");
        } else {
            return ExportService.exportToFile(databaseContext, matches, argumentProcessor.cliPreferences, outputFormat, outputFile);
        }
        return 0;
    }
}
