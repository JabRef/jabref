package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.LibrarySearcher;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.search.inmemory.InMemoryLibrarySearcher;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.toolkit.converter.CygWinPathConverter;

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
        Optional<ParserResult> parserResult = JabKit.importFile(
                inputFile,
                "bibtex",
                argumentProcessor.cliPreferences,
                sharedOptions.porcelain);
        if (parserResult.isEmpty()) {
            System.err.println(Localization.lang("Unable to open file '%0'.", inputFile));
            return 2;
        }

        if (parserResult.get().isInvalid()) {
            System.err.println(Localization.lang("Input file '%0' is invalid and could not be parsed.", inputFile));
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
            JabKit.saveDatabase(
                    argumentProcessor.cliPreferences,
                    argumentProcessor.entryTypesManager,
                    new BibDatabase(matches),
                    outputFile);
            LOGGER.debug("Finished export");
        } else {
            ExporterFactory exporterFactory = ExporterFactory.create(argumentProcessor.cliPreferences);
            Optional<Exporter> exporter = exporterFactory.getExporterByName(outputFormat);

            if (exporter.isEmpty()) {
                System.err.println(Localization.lang("Unknown export format %0", outputFormat));
                return 2;
            }

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
                return 2;
            }
        }
        return 0;
    }
}
