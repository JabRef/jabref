package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.DatabaseSearcher;
import org.jabref.logic.search.IndexManager;
import org.jabref.logic.search.PostgreServer;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.toolkit.ArgumentProcessor;
import org.jabref.toolkit.converter.CygWinPathConverter;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "search", description = "Search in a library.")
class Search implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Search.class);

    @ParentCommand
    private ArgumentProcessor argumentProcessor;

    @Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    @Option(names = {"--query"}, description = "Search query", required = true)
    private String query;

    // [impl->req~jabkit.cli.input-flag~1]
    @Option(names = {"--input"}, converter = CygWinPathConverter.class, description = "Input BibTeX file", required = true)
    private Path inputFile;

    @Option(names = {"--output"}, converter = CygWinPathConverter.class, description = "Output file")
    private Path outputFile;

    @Option(names = {"--output-format"}, description = "Output format: bib, txt, etc.")
    private String outputFormat = "bibtex";

    @Override
    public void run() {
        Optional<ParserResult> parserResult = ArgumentProcessor.importFile(
                inputFile,
                "bibtex",
                argumentProcessor.cliPreferences,
                sharedOptions.porcelain);
        if (parserResult.isEmpty()) {
            System.out.println(Localization.lang("Unable to open file '%0'.", inputFile));
            return;
        }

        if (parserResult.get().isInvalid()) {
            System.out.println(Localization.lang("Input file '%0' is invalid and could not be parsed.", inputFile));
            return;
        }

        PostgreServer postgreServer = new PostgreServer();
        IndexManager.clearOldSearchIndices();

        SearchPreferences searchPreferences = argumentProcessor.cliPreferences.getSearchPreferences();
        SearchQuery searchQuery = new SearchQuery(query, searchPreferences.getSearchFlags());

        BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();
        List<BibEntry> matches;
        try {
            // extract current thread task executor from indexManager
            matches = new DatabaseSearcher(
                    databaseContext,
                    new CurrentThreadTaskExecutor(),
                    argumentProcessor.cliPreferences,
                    postgreServer
            ).getMatches(searchQuery);
        } catch (IOException ex) {
            LOGGER.error("Error occurred when searching", ex);
            return;
        }

        // export matches
        if (matches.isEmpty()) {
            System.out.println(Localization.lang("No search matches."));
            return;
        }

        if ("bibtex".equals(outputFormat)) {
            // output a bib file as default or if
            // provided exportFormat is "bib"
            ArgumentProcessor.saveDatabase(
                    argumentProcessor.cliPreferences,
                    argumentProcessor.entryTypesManager,
                    new BibDatabase(matches),
                    outputFile);
            LOGGER.debug("Finished export");
        } else {
            // export new database
            ExporterFactory exporterFactory = ExporterFactory.create(argumentProcessor.cliPreferences);
            Optional<Exporter> exporter = exporterFactory.getExporterByName(outputFormat);

            if (exporter.isEmpty()) {
                System.out.println(Localization.lang("Unknown export format %0", outputFormat));
                return;
            }

            // We have an TemplateExporter instance:
            try {
                System.out.println(Localization.lang("Exporting %0", outputFile.toAbsolutePath().toString()));
                exporter.get().export(
                        databaseContext,
                        outputFile,
                        matches,
                        List.of(),
                        Injector.instantiateModelOrService(JournalAbbreviationRepository.class));
            } catch (IOException
                     | SaveException
                     | ParserConfigurationException
                     | TransformerException ex) {
                LOGGER.error("Could not export file '{}}'", outputFile.toAbsolutePath(), ex);
            }
        }
    }
}
