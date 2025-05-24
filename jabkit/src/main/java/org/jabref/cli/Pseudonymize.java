package org.jabref.cli;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pseudonymization.Pseudonymization;
import org.jabref.logic.pseudonymization.PseudonymizationResultCsvWriter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "pseudonymize", description = "Anonymize the library")
public class Pseudonymize implements Runnable {
    private final static Logger LOGGER = LoggerFactory.getLogger(Pseudonymize.class);

    @ParentCommand
    private ArgumentProcessor argumentProcessor;

    @Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    @Parameters(index = "0", description = "File to be anonymized")
    private String inputFile;

    @Option(names = {"--output"}, description = "Output file")
    private String outputFile;

    @Option(names = {"--key"}, description = "Output keys file")
    private String key;

    @Override
    public void run() {
        String fileName = getFileName(inputFile);
        String outputFileName = fileName + ".bib";
        String keyFileName = fileName + "_keys.csv";

        // todo: case- output file already exist !

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

        System.out.println(Localization.lang("Anonymizing the library '%0' ...", fileName));
        Pseudonymization pseudonymization = new Pseudonymization();
        BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);

        ArgumentProcessor.saveDatabase(
                argumentProcessor.cliPreferences,
                argumentProcessor.entryTypesManager,
                new BibDatabase(result.bibDatabaseContext().getEntries()),
                Path.of(Objects.requireNonNullElse(outputFile, outputFileName)));

        try {
            Path keysPath = Path.of(Objects.requireNonNullElse(key, keyFileName));
            System.out.println(Localization.lang("Saving") + ": " + keysPath);
            PseudonymizationResultCsvWriter.writeValuesMappingAsCsv(keysPath, result);
        } catch (IOException ex) {
            System.err.println(Localization.lang("Unable to save keys: %0", ex.getMessage()));
            LOGGER.error("Unable to save keys of anonymized library");
        }
    }

    public static String getFileName(String inputFile) {
        String fileName = Path.of(inputFile).getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String suffix = "_pseudo";

        if (dotIndex != -1) {
            return fileName.substring(0, dotIndex) + suffix;
        } else {
            return fileName + suffix;
        }
    }
}
