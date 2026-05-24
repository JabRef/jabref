package org.jabref.toolkit.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import javafx.util.Pair;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportService.class);

    private final CliPreferences cliPreferences;

    private ExportService(CliPreferences cliPreferences) {
        this.cliPreferences = cliPreferences;
    }

    public static ExportService create(CliPreferences cliPreferences) {
        return new ExportService(cliPreferences);
    }

    public List<Pair<String, String>> getAvailableExportFormats() {
        ExporterFactory exporterFactory = ExporterFactory.create(cliPreferences);
        return exporterFactory.getExporters().stream()
                              .map(format -> new Pair<>(format.getName(), format.getId()))
                              .toList();
    }

    public int outputEntries(List<BibEntry> entries) {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(entries));
        return outputDatabaseContext(bibDatabaseContext);
    }

    /// Outputs to StdOut. Generates citation keys if missing.
    public int outputDatabaseContext(BibDatabaseContext bibDatabaseContext) {
        generateCitationKeys(bibDatabaseContext, cliPreferences.getCitationKeyPatternPreferences());

        try (OutputStreamWriter writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
            BibDatabaseWriter bibWriter = new BibDatabaseWriter(writer, bibDatabaseContext, cliPreferences);
            bibWriter.writeDatabase(bibDatabaseContext);
        } catch (IOException e) {
            LOGGER.error("Could not write BibTeX", e);
            System.err.println(Localization.lang("Unable to write to %0.", "stdout"));
            return 1;
        }
        return 0;
    }

    public void saveDatabase(BibEntryTypesManager entryTypesManager,
                             BibDatabase newBase,
                             Path outputFile) {
        saveDatabaseContext(entryTypesManager, new BibDatabaseContext(newBase), outputFile);
    }

    public void saveDatabaseContext(BibEntryTypesManager entryTypesManager,
                                    BibDatabaseContext bibDatabaseContext,
                                    Path outputFile) {
        try {
            if (!FileUtil.isBibFile(outputFile)) {
                System.err.println(Localization.lang("Invalid output file type provided."));
            }
            try (AtomicFileWriter fileWriter = new AtomicFileWriter(outputFile, StandardCharsets.UTF_8)) {
                BibWriter bibWriter = new BibWriter(fileWriter, OS.NEWLINE);
                SelfContainedSaveConfiguration saveConfiguration = (SelfContainedSaveConfiguration) new SelfContainedSaveConfiguration()
                        .withReformatOnSave(cliPreferences.getLibraryPreferences().shouldAlwaysReformatOnSave());
                BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                        bibWriter,
                        saveConfiguration,
                        cliPreferences.getFieldPreferences(),
                        cliPreferences.getCitationKeyPatternPreferences(),
                        entryTypesManager);
                databaseWriter.writeDatabase(bibDatabaseContext);

                // Show just a warning message if encoding did not work for all characters:
                if (fileWriter.hasEncodingProblems()) {
                    System.err.println(Localization.lang("Warning") + ": "
                            + Localization.lang("UTF-8 could not be used to encode the following characters: %0", fileWriter.getEncodingProblems()));
                }
                System.out.println(Localization.lang("Saved %0.", outputFile));
            }
        } catch (IOException ex) {
            System.err.println(Localization.lang("Could not save file.") + "\n" + ex.getLocalizedMessage());
        }
    }

    /// Generates a citation key if there is no key existing
    private static void generateCitationKeys(
            BibDatabaseContext databaseContext,
            CitationKeyPatternPreferences citationKeyPatternPreferences) {

        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(
                databaseContext,
                citationKeyPatternPreferences);
        for (BibEntry entry : databaseContext.getEntries()) {
            if (!entry.hasCitationKey()) {
                keyGenerator.generateAndSetKey(entry);
            }
        }
    }

    public int exportFile(
            @NonNull ParserResult parserResult,
            @NonNull Path outputFile,
            String format,
            boolean porcelain,
            BibEntryTypesManager entryTypesManager) {

        if (!porcelain) {
            System.out.println(Localization.lang("Exporting '%0'.", outputFile));
        }

        if ("bibtex".equalsIgnoreCase(format)) {
            saveDatabase(
                    entryTypesManager,
                    parserResult.getDatabase(),
                    outputFile);
            return 0;
        }

        Path path = parserResult.getPath().get().toAbsolutePath();
        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();
        databaseContext.setDatabasePath(path);
        List<Path> fileDirForDatabase = databaseContext
                .getFileDirectories(cliPreferences.getFilePreferences());

        ExporterFactory exporterFactory = ExporterFactory.create(cliPreferences);
        Optional<Exporter> exporter = exporterFactory.getExporterByName(format);
        if (exporter.isEmpty()) {
            System.err.println(Localization.lang("Unknown export format '%0'.", format));
            return 2;
        }

        try {
            exporter.get().export(
                    parserResult.getDatabaseContext(),
                    outputFile,
                    parserResult.getDatabaseContext().getDatabase().getEntries(),
                    fileDirForDatabase,
                    Injector.instantiateModelOrService(JournalAbbreviationRepository.class));
        } catch (IOException
                 | SaveException
                 | ParserConfigurationException
                 | TransformerException ex) {
            LOGGER.error("Could not export file '{}'.", outputFile, ex);
            return 2;
        }
        return 0;
    }

    public int exportToFile(
            BibDatabaseContext databaseContext,
            List<BibEntry> matches,
            String outputFormat1,
            Path outputFile1) {

        ExporterFactory exporterFactory = ExporterFactory.create(cliPreferences);
        Optional<Exporter> exporter = exporterFactory.getExporterByName(outputFormat1);

        if (exporter.isEmpty()) {
            System.err.println(Localization.lang("Unknown export format %0", outputFormat1));
            return 2;
        }

        try {
            System.out.println(Localization.lang("Exporting %0", outputFile1.toAbsolutePath().toString()));
            exporter.get().export(
                    databaseContext,
                    outputFile1,
                    matches,
                    List.of(),
                    Injector.instantiateModelOrService(JournalAbbreviationRepository.class));
        } catch (IOException
                 | SaveException
                 | ParserConfigurationException
                 | TransformerException ex) {
            LOGGER.error("Could not export file '{}}'", outputFile1.toAbsolutePath(), ex);
            return 2;
        }
        return 0;
    }

    public void saveDatabase(List<BibEntry> matches, BibEntryTypesManager entryTypesManager, Path outputFile1) {
        saveDatabase(entryTypesManager, new BibDatabase(matches), outputFile1);
    }
}
