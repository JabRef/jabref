package org.jabref.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.cli.converter.CygWinPathConverter;
import org.jabref.logic.auxparser.AuxParser;
import org.jabref.logic.auxparser.AuxParserResult;
import org.jabref.logic.auxparser.AuxParserStatisticsProvider;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "generate-bib-from-aux", description = "Generate small bib from aux file.")
class GenerateBibFromAux implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateBibFromAux.class);

    @ParentCommand
    private ArgumentProcessor argumentProcessor;

    @Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    @Option(names = "--aux", required = true)
    private Path auxFile;

    // [impl->req~jabkit.cli.input-flag~1]
    @Option(names = {"--input"}, converter = CygWinPathConverter.class, description = "Input BibTeX file", required = true)
    private Path inputFile;

    @Option(names = "--output")
    private Path outputFile;

    @Override
    public void run() {
        Optional<ParserResult> pr = ArgumentProcessor.importFile(
                inputFile,
                "bibtex",
                argumentProcessor.cliPreferences,
                sharedOptions.porcelain);
        if (pr.isEmpty()) {
            System.out.println(Localization.lang("Unable to open file '%0'.", inputFile));
            return;
        }

        if (!Files.exists(auxFile)) {
            System.out.println(Localization.lang("Unable to open file '%0'.", auxFile));
            return;
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Creating excerpt of from '%0' with '%1'.", inputFile, auxFile.toAbsolutePath()));
        }

        AuxParser auxParser = new DefaultAuxParser(pr.get().getDatabase());
        AuxParserResult result = auxParser.parse(auxFile);

        if (!sharedOptions.porcelain) {
            System.out.println(new AuxParserStatisticsProvider(result).getInformation(true));
        }

        BibDatabase subDatabase = result.getGeneratedBibDatabase();
        if (subDatabase == null || !subDatabase.hasEntries()) {
            System.out.println(Localization.lang("No library generated."));
            return;
        }

        if (outputFile == null) {
            System.out.println(subDatabase.getEntries().stream()
                                          .map(BibEntry::toString)
                                          .collect(Collectors.joining("\n\n")));
            return;
        } else {
            ArgumentProcessor.saveDatabase(
                    argumentProcessor.cliPreferences,
                    argumentProcessor.entryTypesManager,
                    subDatabase,
                    outputFile);
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Created library with '%0' entries.", subDatabase.getEntryCount()));
        }
    }
}
