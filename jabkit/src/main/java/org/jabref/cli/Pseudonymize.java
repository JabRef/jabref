package org.jabref.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pseudonymization.Pseudonymization;
import org.jabref.logic.pseudonymization.PseudonymizationResultCsvWriter;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "pseudonymize", description = "Perform pseudonymization of the library")
public class Pseudonymize implements Runnable {
    private final static Logger LOGGER = LoggerFactory.getLogger(Pseudonymize.class);
    private static final String PSEUDO_SUFFIX = ".pseudo";

    @ParentCommand
    private ArgumentProcessor argumentProcessor;

    @Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    // ADR 0045
    @Option(names = {"--input"}, description = "BibTex file to be pseudonymize", required = true)
    private String inputFile;

    @Option(names = {"--output"}, description = "Output pseudo-bib file")
    private String outputFile;

    @Option(names = {"--key"}, description = "Output pseudo-keys file")
    private String keyFile;

    @Option(names = {"-f", "--force"}, description = "Overwrite output file(s) if it exist")
    private boolean force;

    @Override
    public void run() {
        String fileName = FileUtil.getBaseName(inputFile);
        String pseudoBib = fileName + PSEUDO_SUFFIX + ".bib";
        String pseudoKeys = fileName + PSEUDO_SUFFIX + ".csv";
        Path pseudoBibPath = Path.of(outputFile != null ? outputFile : pseudoBib);
        Path pseudoKeyPath = Path.of(keyFile != null ? keyFile : pseudoKeys);

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

        System.out.println(Localization.lang("Pseudonymizing the library '%0'.", fileName));
        Pseudonymization pseudonymization = new Pseudonymization();
        BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);

        if (Files.exists(pseudoBibPath) && !force) {
            System.out.println(Localization.lang("'%0' file already exists! Use -f or --force to overwrite.", pseudoBib));
            return;
        } else if (Files.exists(pseudoBibPath) && force) {
            System.out.println(Localization.lang("'%0' file already exists. Overwriting.", pseudoBib));
        }

        ArgumentProcessor.saveDatabaseContext(
                argumentProcessor.cliPreferences,
                argumentProcessor.entryTypesManager,
                result.bibDatabaseContext(),
                pseudoBibPath);

        try {
            if (Files.exists(pseudoKeyPath) && !force) {
                System.out.println(Localization.lang("'%0' file already exists! Use -f or --force to overwrite.", pseudoKeys));
                return;
            } else if (Files.exists(pseudoKeyPath) && force) {
                System.out.println(Localization.lang("'%0' file already exists. Overwriting.", pseudoKeys));
            }

            System.out.println(Localization.lang("Saving: %0.", pseudoKeyPath));
            PseudonymizationResultCsvWriter.writeValuesMappingAsCsv(pseudoKeyPath, result);
        } catch (IOException ex) {
            LOGGER.error("Unable to save keys for pseudonymized library", ex);
        }
    }
}
