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
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.toolkit.converter.CygWinPathConverter;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "convert", description = "Convert between bibliography formats.")
class Convert implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Convert.class);

    @ParentCommand
    private JabKit jabKit;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    // [impl->req~jabkit.cli.input-flag~1]
    @Option(names = {"--input"}, converter = CygWinPathConverter.class, description = "Input file", required = true)
    private Path inputFile;

    @Option(names = {"--input-format"}, description = "Input format")
    private String inputFormat;

    @Option(names = {"--output"}, converter = CygWinPathConverter.class, description = "Output file")
    private Path outputFile;

    @Option(names = {"--output-format"}, description = "Output format")
    private String outputFormat = "bibtex";

    @Override
    public void run() {
        Optional<ParserResult> parserResult = JabKit.importFile(inputFile, inputFormat, jabKit.cliPreferences, sharedOptions.porcelain);
        if (parserResult.isEmpty()) {
            System.out.println(Localization.lang("Unable to open file '%0'.", inputFile));
            return;
        }

        if (parserResult.get().isInvalid()) {
            System.out.println(Localization.lang("Input file '%0' is invalid and could not be parsed.", inputFile));
            return;
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Converting '%0' to '%1'.", inputFile, outputFormat));
        }

        if (outputFile == null) {
            System.out.println(parserResult.get().getDatabase());
            return;
        }

        exportFile(parserResult.get(), outputFile, outputFormat);
    }

    protected void exportFile(@NonNull ParserResult parserResult, @NonNull Path outputFile, String format) {
        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Exporting '%0'.", outputFile));
        }

        if ("bibtex".equalsIgnoreCase(format)) {
            JabKit.saveDatabase(
                    jabKit.cliPreferences,
                    jabKit.entryTypesManager,
                    parserResult.getDatabase(),
                    outputFile);
            return;
        }

        Path path = parserResult.getPath().get().toAbsolutePath();
        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();
        databaseContext.setDatabasePath(path);
        List<Path> fileDirForDatabase = databaseContext
                .getFileDirectories(jabKit.cliPreferences.getFilePreferences());

        ExporterFactory exporterFactory = ExporterFactory.create(jabKit.cliPreferences);
        Optional<Exporter> exporter = exporterFactory.getExporterByName(format);
        if (exporter.isEmpty()) {
            System.out.println(Localization.lang("Unknown export format '%0'.", format));
            return;
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
        }
    }
}
