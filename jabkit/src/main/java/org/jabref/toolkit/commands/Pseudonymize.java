package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pseudonymization.Pseudonymization;
import org.jabref.logic.pseudonymization.PseudonymizationResultCsvWriter;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.toolkit.converter.CygWinPathConverter;
import org.jabref.toolkit.exception.ExportException;
import org.jabref.toolkit.service.ExportService;
import org.jabref.toolkit.service.ImportService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "pseudonymize", description = "Perform pseudonymization of the library")
class Pseudonymize implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Pseudonymize.class);
    private static final String PSEUDO_SUFFIX = ".pseudo";
    private static final String BIB_EXTENSION = ".bib";
    private static final String CSV_EXTENSION = ".csv";

    @ParentCommand
    private JabKit argumentProcessor;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @Mixin
    private InputOption inputOption = new InputOption();

    @Option(names = {"--output"}, converter = CygWinPathConverter.class, description = "Output pseudo-bib file")
    private Path outputFile;

    @Option(names = {"--key"}, converter = CygWinPathConverter.class, description = "Output pseudo-keys file")
    private Path keyFile;

    @Option(names = {"-f", "--force"}, description = "Overwrite output file(s) if any exist(s)")
    private boolean force;

    @Override
    public Integer call() {
        Path inputPath = inputOption.getInputFile();
        String fileName = FileUtil.getBaseName(inputPath);
        Path pseudoBibPath = resolveOutputPath(outputFile, inputPath, fileName + PSEUDO_SUFFIX + BIB_EXTENSION);
        Path pseudoKeyPath = resolveOutputPath(keyFile, inputPath, fileName + PSEUDO_SUFFIX + CSV_EXTENSION);

        Optional<ParserResult> parserResult = ImportService.importBibTexFile(inputPath, argumentProcessor.cliPreferences, sharedOptions.porcelain);
        if (parserResult.isEmpty()) {
            return 2;
        }

        System.out.println(Localization.lang("Pseudonymizing library '%0'...", fileName));
        Pseudonymization pseudonymization = new Pseudonymization();
        BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);

        if (!fileOverwriteCheck(pseudoBibPath)) {
            return 2;
        }

        try {
            ExportService.create(argumentProcessor.cliPreferences).saveDatabaseContext(
                    result.bibDatabaseContext(),
                    pseudoBibPath);
        } catch (ExportException ex) {
            // TODO this just informs the user, maybe to lax?
            System.err.println(Localization.lang("Could not save file.") + "\n" + ex.getLocalizedMessage());
        }

        if (!fileOverwriteCheck(pseudoKeyPath)) {
            return 2;
        }

        try {
            PseudonymizationResultCsvWriter.writeValuesMappingAsCsv(pseudoKeyPath, result);
            System.out.println(Localization.lang("Saved %0.", pseudoKeyPath));
        } catch (IOException ex) {
            LOGGER.error("Unable to save keys for pseudonymized library", ex);
            return 2;
        }
        return 0;
    }

    private Path resolveOutputPath(Path customPath, Path inputPath, String defaultFileName) {
        return customPath != null ? customPath : inputPath.toAbsolutePath().getParent().resolve(defaultFileName);
    }

    private boolean fileOverwriteCheck(Path filePath) {
        if (!Files.exists(filePath)) {
            return true;
        }

        String fileName = filePath.getFileName().toString();

        if (!force) {
            System.err.println(Localization.lang("File '%0' already exists. Use -f or --force to overwrite.", fileName));
            return false;
        }

        System.out.println(Localization.lang("File '%0' already exists. Overwriting.", fileName));
        return true;
    }
}
