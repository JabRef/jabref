package org.jabref.toolkit.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.toolkit.exception.ExportException;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class ExportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportService.class);

    private final CliPreferences cliPreferences;
    private final ExporterFactory exporterFactory;
    private final Exporter bibtexExporter;
    private BibEntryTypesManager entryTypesManager;

    public ExportService(CliPreferences cliPreferences) {
        this.cliPreferences = cliPreferences;
        entryTypesManager = cliPreferences.getCustomEntryTypesRepository();
        exporterFactory = ExporterFactory.create(cliPreferences);
        bibtexExporter = createBibtexExporter();
    }

    private Exporter createBibtexExporter() {
        return new Exporter("bibtex", "BibTex", StandardFileType.BIBTEX_DB) {
            @Override
            public void export(@NonNull BibDatabaseContext databaseContext, Path file, @NonNull List<BibEntry> entries) throws IOException, TransformerException, ParserConfigurationException, SaveException {
                // TODO: not sure about the different options (alternative: save entries)
                internalSaveDatabaseContext(databaseContext, file);
            }
        };
    }

    public static ExportService create(CliPreferences cliPreferences) {
        return new ExportService(cliPreferences);
    }

    public List<Pair<String, String>> getAvailableExportFormats() {
        return Stream.concat(exporterFactory.getExporters().stream(), Stream.of(bibtexExporter))
                     .map(format -> new Pair<>(format.getName(), format.getId()))
                     .toList();
    }

    /// Outputs to StdOut. Generates citation keys if missing.
    public void printBibEntries(List<BibEntry> entries) throws ExportException {
        printDatabaseContext(new BibDatabaseContext(new BibDatabase(entries)));
    }

    /// Outputs to StdOut. Generates citation keys if missing.
    public void printDatabaseContext(BibDatabaseContext bibDatabaseContext) throws ExportException {
        try (OutputStreamWriter writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
            generateCitationKeys(bibDatabaseContext, cliPreferences.getCitationKeyPatternPreferences());
            BibDatabaseWriter bibWriter = new BibDatabaseWriter(writer, bibDatabaseContext, cliPreferences);
            bibWriter.writeDatabase(bibDatabaseContext);
        } catch (IOException ex) {
            throw new ExportException("Unable to write to stdout",
                    Localization.lang("Unable to write to %0.", "stdout"),
                    ex, CommandLine.ExitCode.SOFTWARE);
        }
    }

    public void saveBibEntries(List<BibEntry> matches, Path outputFile) throws ExportException {
        saveDatabase(new BibDatabase(matches), outputFile);
    }

    public void saveDatabase(BibDatabase newBase, Path outputFile) throws ExportException {
        saveDatabaseContext(new BibDatabaseContext(newBase), outputFile);
    }

    public void saveDatabaseContext(BibDatabaseContext bibDatabaseContext, Path outputFile) throws ExportException {
        try {
            internalSaveDatabaseContext(bibDatabaseContext, outputFile);
        } catch (IOException ex) {
            throw new ExportException("Unable to write to stdout",
                    Localization.lang("Unable to write to %0.", "stdout"),
                    ex, CommandLine.ExitCode.SOFTWARE);
        }
    }

    private void internalSaveDatabaseContext(BibDatabaseContext bibDatabaseContext, Path outputFile) throws IOException {
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
    }

    public void exportEntriesToFile(List<BibEntry> entries, String outputFormat, Path outputFile) throws ExportException {
        exportBibDatabaseContextToFile(
                new BibDatabaseContext(new BibDatabase(entries)),
                entries,
                outputFormat,
                outputFile);
    }

    public void exportBibDatabaseContextToFile(
            BibDatabaseContext databaseContext,
            List<BibEntry> matches,
            String format,
            Path outputFile) throws ExportException {

        Exporter exporter = getExporterByName(format);
        tryExportWithExporter(exporter, outputFile, databaseContext, matches, List.of());
    }

    public void exportParserResultToFile(
            @NonNull ParserResult parserResult,
            @NonNull Path outputFile,
            String format,
            boolean porcelain) throws ExportException {

        if (!porcelain) {
            System.out.println(Localization.lang("Exporting '%0'.", outputFile));
        }

        if ("bibtex".equalsIgnoreCase(format)) {
            // TODO marked for removal
            saveDatabase(parserResult.getDatabase(), outputFile);
            return;
        }

        Path path = parserResult.getPath().get().toAbsolutePath();
        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();
        databaseContext.setDatabasePath(path);
        List<Path> fileDirForDatabase = databaseContext
                .getFileDirectories(cliPreferences.getFilePreferences());

        List<BibEntry> entries = databaseContext.getDatabase().getEntries();

        Exporter exporter = getExporterByName(format);
        tryExportWithExporter(exporter, outputFile, databaseContext, entries, fileDirForDatabase);
    }

    private static void tryExportWithExporter(
            Exporter exporter, @NonNull Path outputFile,
            BibDatabaseContext databaseContext,
            List<BibEntry> entries,
            List<Path> fileDirForDatabase) throws ExportException {

        try {
            JournalAbbreviationRepository abbreviationRepository = Injector.instantiateModelOrService(JournalAbbreviationRepository.class);
            System.out.println(Localization.lang("Exporting %0", outputFile.toAbsolutePath().toString()));
            exporter.export(databaseContext, outputFile, entries, fileDirForDatabase,
                    abbreviationRepository);
        } catch (IOException | SaveException | ParserConfigurationException | TransformerException ex) {
            throw new ExportException("Failed to export file.",
                    Localization.lang("Failed to export file."),
                    ex, CommandLine.ExitCode.SOFTWARE);
        }
    }

    private Exporter getExporterByName(String exporterId) throws ExportException {
        return exporterFactory.getExporterByName(exporterId)
                              .or(() -> bibtexExporter.getId().equalsIgnoreCase(exporterId) ?
                                        Optional.of(bibtexExporter) :
                                        Optional.empty())
                              .orElseThrow(
                                      () -> new ExportException("Unknown export format '" + exporterId + "'.",
                                              Localization.lang("Unknown export format '%0'.", exporterId),
                                              CommandLine.ExitCode.USAGE));
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
}
