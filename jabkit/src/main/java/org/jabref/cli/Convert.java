package org.jabref.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "convert", description = "Convert between bibliography formats.")
public class Convert implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Convert.class);

    @ParentCommand
    private ArgumentProcessor argumentProcessor;

    @Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    @Option(names = {"--input"}, description = "Input file", required = true)
    private Path inputFile;

    @Option(names = {"--input-format"}, description = "Input format")
    private String inputFormat;

    @Option(names = {"--output"}, description = "Output file")
    private Path outputFile;

    @Option(names = {"--output-format"}, description = "Output format")
    private String outputFormat;

    @Override
    public void run() {
        if (inputFile == null
                || !Files.exists(inputFile)
                || outputFile == null) {
            return;
        }

        Optional<ParserResult> pr = ArgumentProcessor.importFile(argumentProcessor.cliPreferences, inputFile, inputFormat);
        if (pr.isPresent()) {
            exportFile(pr.get(), outputFile, outputFormat);
        } else {
            LOGGER.error("Unable to export input file {}", inputFile);
        }
    }

    protected void exportFile(ParserResult pr, Path outputFile, String format) {
        if (pr == null || outputFile == null) {
            return;
        }

        if (pr.isInvalid()) {
            System.err.println(Localization.lang("The output option depends on a valid import option."));
            return;
        }

        if ("bibtex".equalsIgnoreCase(format)) {
            ArgumentProcessor.saveDatabase(argumentProcessor.cliPreferences, argumentProcessor.entryTypesManager, pr.getDatabase(), outputFile);
            return;
        }

        // This signals that the latest import should be stored in the given
        // format to the given file.
        Path path = pr.getPath().get().toAbsolutePath();
        BibDatabaseContext databaseContext = pr.getDatabaseContext();
        databaseContext.setDatabasePath(path);
        List<Path> fileDirForDatabase = databaseContext
                .getFileDirectories(argumentProcessor.cliPreferences.getFilePreferences());
        System.out.println(Localization.lang("Exporting %0", outputFile));
        ExporterFactory exporterFactory = ExporterFactory.create(argumentProcessor.cliPreferences);
        Optional<Exporter> exporter = exporterFactory.getExporterByName(format);
        if (exporter.isEmpty()) {
            System.err.println(Localization.lang("Unknown export format %0", format));
        } else {
            // We have an exporter:
            try {
                exporter.get().export(
                        pr.getDatabaseContext(),
                        outputFile,
                        pr.getDatabaseContext().getDatabase().getEntries(),
                        fileDirForDatabase,
                        Injector.instantiateModelOrService(JournalAbbreviationRepository.class));
            } catch (Exception ex) {
                System.err.println(Localization.lang("Could not export file '%0' (reason: %1)", outputFile, Throwables.getStackTraceAsString(ex)));
            }
        }
    }
}
