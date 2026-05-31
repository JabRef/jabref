package org.jabref.toolkit.commands;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jabref.logic.auxparser.AuxParser;
import org.jabref.logic.auxparser.AuxParserResult;
import org.jabref.logic.auxparser.AuxParserStatisticsProvider;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.cleanup.FieldFormatterCleanupMapper;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.toolkit.exception.ExportServiceException;
import org.jabref.toolkit.exception.ImportServiceException;
import org.jabref.toolkit.service.ExportService;
import org.jabref.toolkit.service.ImportService;

import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "generate-bib-from-aux", description = "Generate small bib from aux file.")
class GenerateBibFromAux implements Callable<Integer> {

    @ParentCommand
    private JabKit argumentProcessor;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @Option(names = "--aux", required = true)
    private Path auxFile;

    @Mixin
    private InputOption inputOption = new InputOption();

    @Option(names = "--output")
    private Path outputFile;

    @Option(names = {"--field-formatters"}, description = "Field Formatter")
    private String fieldFormatters;

    @Override
    public Integer call() throws ImportServiceException, ExportServiceException {
        Path inputFile = inputOption.getInputFile();
        ParserResult pr = ImportService.importBibTexFile(
                inputFile,
                argumentProcessor.cliPreferences,
                sharedOptions.porcelain);

        if (!Files.exists(auxFile)) {
            System.err.println(Localization.lang("Unable to open file '%0'.", auxFile));
            return 2;
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Creating excerpt of from '%0' with '%1'.", inputFile, auxFile.toAbsolutePath()));
        }

        AuxParser auxParser = new DefaultAuxParser(pr.getDatabase());
        AuxParserResult result = auxParser.parse(auxFile);

        if (!sharedOptions.porcelain) {
            System.out.println(new AuxParserStatisticsProvider(result).getInformation(true));
        }

        BibDatabase subDatabase = result.getGeneratedBibDatabase();
        if (subDatabase == null || !subDatabase.hasEntries()) {
            System.out.println(Localization.lang("No library generated."));
            return CommandLine.ExitCode.OK;
        }

        FieldFormatterCleanupMapper.applyFormatters(fieldFormatters, subDatabase.getEntries());

        if (outputFile == null) {
            System.out.println(subDatabase.getEntries().stream()
                                          .map(BibEntry::toString)
                                          .collect(Collectors.joining("\n\n")));
        } else {
            ExportService.create(argumentProcessor.cliPreferences).saveDatabase(subDatabase, outputFile);
            if (!sharedOptions.porcelain) {
                System.out.println(Localization.lang("Created library with '%0' entries.", subDatabase.getEntryCount()));
            }
        }
        return CommandLine.ExitCode.OK;
    }
}
