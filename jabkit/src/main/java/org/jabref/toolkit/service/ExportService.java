package org.jabref.toolkit.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import org.jabref.toolkit.exception.ExportServiceException;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@NullMarked
public class ExportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportService.class);

    private final CliPreferences cliPreferences;
    private final ExporterFactory exporterFactory;
    private final Exporter bibtexExporter;
    private final BibEntryTypesManager entryTypesManager;

    private final boolean porcelain;

    public ExportService(CliPreferences cliPreferences, boolean porcelain) {
        this.cliPreferences = cliPreferences;
        entryTypesManager = cliPreferences.getCustomEntryTypesRepository();
        exporterFactory = ExporterFactory.create(cliPreferences);
        this.porcelain = porcelain;
        bibtexExporter = createBibtexExporter();
    }

    private Exporter createBibtexExporter() {
        return new Exporter("bibtex", "BibTex", StandardFileType.BIBTEX_DB) {
            @Override
            public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) throws IOException {
                internalSaveDatabaseContext(new BibDatabaseContext(new BibDatabase(entries)), file);
            }
        };
    }

    public static ExportService create(CliPreferences cliPreferences, boolean porcelain) {
        return new ExportService(cliPreferences, porcelain);
    }

    public List<Pair<String, String>> getAvailableExportFormats() {
        return Stream.concat(exporterFactory.getExporters().stream(), Stream.of(bibtexExporter))
                     .map(format -> new Pair<>(format.getName(), format.getId()))
                     .toList();
    }

    public void printBibEntriesToStdOut(List<BibEntry> entries) throws ExportServiceException {
        printDatabaseContextToStdOut(new BibDatabaseContext(new BibDatabase(entries)));
    }

    public void printDatabaseContextToStdOut(BibDatabaseContext bibDatabaseContext) throws ExportServiceException {
        try (OutputStreamWriter writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
            generateCitationKeys(bibDatabaseContext, cliPreferences.getCitationKeyPatternPreferences());
            BibDatabaseWriter bibWriter = new BibDatabaseWriter(writer, bibDatabaseContext, cliPreferences);
            bibWriter.writeDatabase(bibDatabaseContext);
        } catch (IOException ex) {
            throw new ExportServiceException("Unable to write to stdout",
                    Localization.lang("Unable to write to %0.", "stdout"),
                    ex, CommandLine.ExitCode.SOFTWARE);
        }
    }

    public void saveBibEntries(
            List<BibEntry> matches,
            Path outputFile) throws ExportServiceException {

        saveDatabase(new BibDatabase(matches), outputFile);
    }

    public void saveDatabase(
            BibDatabase newBase,
            Path outputFile) throws ExportServiceException {

        saveDatabaseContext(new BibDatabaseContext(newBase), outputFile);
    }

    public void saveDatabaseContext(
            BibDatabaseContext bibDatabaseContext,
            Path outputFile) throws ExportServiceException {

        try {
            internalSaveDatabaseContext(bibDatabaseContext, outputFile);
        } catch (IOException ex) {
            throw new ExportServiceException("Unable to write to " + outputFile,
                    Localization.lang("Unable to write to %0.", outputFile),
                    ex, CommandLine.ExitCode.SOFTWARE);
        }
    }

    private void internalSaveDatabaseContext(
            BibDatabaseContext bibDatabaseContext,
            Path outputFile) throws IOException {

        if (!FileUtil.isBibFile(outputFile)) {
            printOut(Localization.lang("Invalid output file type provided."));
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
                printErr(Localization.lang("UTF-8 could not be used to encode the following characters: %0",
                        fileWriter.getEncodingProblems()));
            }
            printOut(Localization.lang("Saved %0.", outputFile));
        }
    }

    public void exportEntriesToFile(
            List<BibEntry> entries,
            Path outputFile, String outputFormat) throws ExportServiceException {

        exportBibDatabaseContextToFile(
                new BibDatabaseContext(new BibDatabase(entries)),
                entries,
                outputFile,
                outputFormat
        );
    }

    public void exportBibDatabaseContextToFile(
            BibDatabaseContext databaseContext,
            List<BibEntry> matches,
            Path outputFile,
            String outputFormat) throws ExportServiceException {

        Exporter exporter = getExporterByName(outputFormat);
        tryExportWithExporter(exporter, outputFile, databaseContext, matches, List.of());
    }

    public void exportParserResultToFile(
            ParserResult parserResult,
            Path outputFile,
            String format) throws ExportServiceException {

        Optional<Path> path = parserResult.getPath().map(Path::toAbsolutePath);
        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();
        path.ifPresent(databaseContext::setDatabasePath);
        List<Path> fileDirForDatabase = databaseContext
                .getFileDirectories(cliPreferences.getFilePreferences());

        List<BibEntry> entries = databaseContext.getDatabase().getEntries();

        Exporter exporter = getExporterByName(format);
        tryExportWithExporter(exporter, outputFile, databaseContext, entries, fileDirForDatabase);
    }

    public void exportParserResultToStdOut(
            ParserResult parserResult,
            String format) throws ExportServiceException {

        Optional<Path> path = parserResult.getPath().map(Path::toAbsolutePath);
        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();
        path.ifPresent(databaseContext::setDatabasePath);
        List<Path> fileDirForDatabase = databaseContext
                .getFileDirectories(cliPreferences.getFilePreferences());
        List<BibEntry> entries = databaseContext.getDatabase().getEntries();
        Exporter exporter = getExporterByName(format);
        Path temporaryOutput = null;

        try {
            String extension = exporter.getFileType().getExtensions().getFirst();
            temporaryOutput = Files.createTempFile("jabkit-", "." + extension);
            JournalAbbreviationRepository abbreviationRepository = Injector.instantiateModelOrService(JournalAbbreviationRepository.class);
            exporter.export(databaseContext, temporaryOutput, entries, fileDirForDatabase, abbreviationRepository);
            Files.copy(temporaryOutput, System.out);
            System.out.flush();
        } catch (IOException | SaveException | ParserConfigurationException | TransformerException ex) {
            throw new ExportServiceException("Unable to write to stdout",
                    Localization.lang("Unable to write to %0.", "stdout"),
                    ex, CommandLine.ExitCode.SOFTWARE);
        } finally {
            if (temporaryOutput != null) {
                try {
                    Files.deleteIfExists(temporaryOutput);
                } catch (IOException ex) {
                    LOGGER.debug("Unable to delete temporary export file {}", temporaryOutput, ex);
                }
            }
        }
    }

    private void tryExportWithExporter(
            Exporter exporter,
            Path outputFile,
            BibDatabaseContext databaseContext,
            List<BibEntry> entries,
            List<Path> fileDirForDatabase) throws ExportServiceException {

        try {
            JournalAbbreviationRepository abbreviationRepository = Injector.instantiateModelOrService(JournalAbbreviationRepository.class);
            printOut(Localization.lang("Exporting '%0'.", outputFile.toAbsolutePath().toString()));
            exporter.export(databaseContext, outputFile, entries, fileDirForDatabase, abbreviationRepository);
        } catch (IOException | SaveException | ParserConfigurationException | TransformerException ex) {
            throw new ExportServiceException("Failed to export file.",
                    Localization.lang("Failed to export file."),
                    ex, CommandLine.ExitCode.SOFTWARE);
        }
    }

    private Exporter getExporterByName(String exporterId) throws ExportServiceException {
        return exporterFactory.getExporterByName(exporterId)
                              .or(() -> bibtexExporter.getId().equalsIgnoreCase(exporterId)
                                        ? Optional.of(bibtexExporter)
                                        : Optional.empty())
                              .orElseThrow(
                                      () -> new ExportServiceException("Unknown export format '" + exporterId + "'.",
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

    private void printOut(String s) {
        if (!this.porcelain) {
            System.out.println(s);
        }
    }

    private void printErr(String s) {
        // We print actual errors independent of porcelain flag
        System.err.println(s);
    }
}
