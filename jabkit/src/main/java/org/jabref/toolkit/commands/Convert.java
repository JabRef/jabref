package org.jabref.toolkit.commands;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.cleanup.FieldFormatterCleanupMapper;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.toolkit.converter.CygWinPathConverter;
import org.jabref.toolkit.exception.ExportException;
import org.jabref.toolkit.service.ExportService;
import org.jabref.toolkit.service.ImportService;

import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "convert", description = "Convert between bibliography formats.")
class Convert implements Callable<Integer> {

    @ParentCommand
    private JabKit jabKit;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @Mixin
    private InputOption inputOption = new InputOption();

    @Option(names = {"--input-format"}, description = "Input format")
    private String inputFormat;

    @Option(names = {"--output"}, converter = CygWinPathConverter.class, description = "Output file")
    private Path outputFile;

    @Option(names = {"--output-format"}, description = "Output format")
    private String outputFormat = "bibtex";

    @Option(names = {"--field-formatters"}, description = "Field Formatter")
    private String fieldFormatters;

    @Override
    public Integer call() {
        Path inputFile = inputOption.getInputFile();
        Optional<ParserResult> parserResult = ImportService.importFile(inputFile, inputFormat, jabKit.cliPreferences, sharedOptions.porcelain);
        if (parserResult.isEmpty()) {
            System.err.println(Localization.lang("Unable to open file '%0'.", inputFile));
            return 2;
        }

        if (parserResult.get().isInvalid()) {
            System.err.println(Localization.lang("Input file '%0' is invalid and could not be parsed.", inputFile));
            return 2;
        }

        FieldFormatterCleanupMapper.applyFormatters(fieldFormatters, parserResult.get().getDatabase().getEntries());

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Converting '%0' to '%1'.", inputFile, outputFormat));
        }

        if (outputFile == null) {
            System.out.println(parserResult.get().getDatabase());
            return 0;
        }

        try {
            ExportService.create(jabKit.cliPreferences)
                                .exportParserResultToFile(parserResult.get(), outputFile, outputFormat, sharedOptions.porcelain);
            return CommandLine.ExitCode.OK;
        } catch (ExportException ex) {
            // TODO this just informs the user, maybe to lax?
            System.err.println(ex.getLocalizedMessage() + " (" + (ex.getCause() == null ? "" : ex.getCause().getLocalizedMessage()) + ")");
            return ex.getExitCode();
        }
    }
}
