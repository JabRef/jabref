package org.jabref.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.DatabaseSearcher;
import org.jabref.logic.search.PostgreServer;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;

import com.airhacks.afterburner.injection.Injector;
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
    private KitCommandLine kitCommandLine;

    @Mixin
    private KitCommandLine.SharedOptions sharedOptions = new KitCommandLine.SharedOptions();

    @Option(names = {"--query"}, description = "Search query", required = true)
    private String query;

    @Option(names = {"--input"}, description = "Input BibTeX file", required = true)
    private Path inputFile;

    @Option(names = {"--output"}, description = "Output file")
    private Path outputFile;

    @Option(names = {"--output-format"}, description = "Output format: bib, txt, etc.")
    private String outputFormat = "bibtex";

    @Override
    public Integer call() throws Exception {
        String searchTerm = query;
        Optional<ParserResult> pr = kitCommandLine.importFile(inputFile, "bibtex");
        if (pr.isEmpty()) {
            return 1;
        }

        BibDatabaseContext databaseContext = pr.get().getDatabaseContext();

        SearchPreferences searchPreferences = kitCommandLine.cliPreferences.getSearchPreferences();
        SearchQuery query = new SearchQuery(searchTerm, searchPreferences.getSearchFlags());

        List<BibEntry> matches;
        try {
            // extract current thread task executor from indexManager
            matches = new DatabaseSearcher(query,
                    databaseContext,
                    new CurrentThreadTaskExecutor(),
                    kitCommandLine.cliPreferences,
                    Injector.instantiateModelOrService(PostgreServer.class)
            ).getMatches();
        } catch (IOException ex) {
            LOGGER.error("Error occurred when searching", ex);
            return 1;
        }

        // export matches
        if (matches.isEmpty()) {
            System.out.println(Localization.lang("No search matches."));
            return 0;
        }

        if ("bibtex".equals(outputFormat)) {
            // output a bib file as default or if
            // provided exportFormat is "bib"
            kitCommandLine.saveDatabase(new BibDatabase(matches), outputFile);
            LOGGER.debug("Finished export");
        } else {
            // export new database
            ExporterFactory exporterFactory = ExporterFactory.create(kitCommandLine.cliPreferences);
            Optional<Exporter> exporter = exporterFactory.getExporterByName(outputFormat);
            if (exporter.isEmpty()) {
                System.err.println(Localization.lang("Unknown export format %0", outputFormat));
            } else {
                // We have an TemplateExporter instance:
                try {
                    System.out.println(Localization.lang("Exporting %0", outputFile.toAbsolutePath().toString()));
                    exporter.get().export(
                            databaseContext,
                            outputFile,
                            matches,
                            List.of(),
                            Injector.instantiateModelOrService(JournalAbbreviationRepository.class));
                } catch (Exception ex) {
                    LOGGER.error("Could not export file '{}}'", outputFile.toAbsolutePath(), ex);
                }
            }
        }

        return 0;
    }
}
