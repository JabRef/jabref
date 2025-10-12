package org.jabref.cli;

import io.github.adr.embedded.ADR;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.jabref.cli.converter.CygWinPathConverter;
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
  private static final String BIB_EXTENSION = ".bib";
  private static final String CSV_EXTENSION = ".csv";

  @ParentCommand
  private ArgumentProcessor argumentProcessor;

  @Mixin
  private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

  @ADR(45)
  @Option(names = {
      "--input"}, converter = CygWinPathConverter.class, description = "BibTeX file to be pseudonymized", required = true)
  private Path inputPath;

  @Option(names = {
      "--output"}, converter = CygWinPathConverter.class, description = "Output pseudo-bib file")
  private Path outputFile;

  @Option(names = {"--key"}, description = "Output pseudo-keys file")
  private String keyFile;

  @Option(names = {"-f", "--force"}, description = "Overwrite output file(s) if any exist(s)")
  private boolean force;

  @Override
  public void run() {
    String fileName = FileUtil.getBaseName(inputPath);
    Path pseudoBibPath = resolveOutputPath(outputFile.toString(), inputPath,
        fileName + PSEUDO_SUFFIX + BIB_EXTENSION);
    Path pseudoKeyPath = resolveOutputPath(keyFile, inputPath,
        fileName + PSEUDO_SUFFIX + CSV_EXTENSION);

    Optional<ParserResult> parserResult = ArgumentProcessor.importFile(
        inputPath,
        "bibtex",
        argumentProcessor.cliPreferences,
        sharedOptions.porcelain);

    if (parserResult.isEmpty()) {
      System.out.println(Localization.lang("Unable to open file '%0'.", inputPath));
      return;
    }

    if (parserResult.get().isInvalid()) {
      System.out.println(
          Localization.lang("Input file '%0' is invalid and could not be parsed.", inputPath));
      return;
    }

    System.out.println(Localization.lang("Pseudonymizing library '%0'...", fileName));
    Pseudonymization pseudonymization = new Pseudonymization();
    BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();
    Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);

    if (!fileOverwriteCheck(pseudoBibPath)) {
      return;
    }

    ArgumentProcessor.saveDatabaseContext(
        argumentProcessor.cliPreferences,
        argumentProcessor.entryTypesManager,
        result.bibDatabaseContext(),
        pseudoBibPath);

    if (!fileOverwriteCheck(pseudoKeyPath)) {
      return;
    }

    try {
      PseudonymizationResultCsvWriter.writeValuesMappingAsCsv(pseudoKeyPath, result);
      System.out.println(Localization.lang("Saved %0.", pseudoKeyPath));
    } catch (IOException ex) {
      LOGGER.error("Unable to save keys for pseudonymized library", ex);
    }
  }

  private Path resolveOutputPath(String customPath, Path inputPath, String defaultFileName) {
    return customPath != null ? Path.of(customPath)
        : inputPath.getParent().resolve(defaultFileName);
  }

  private boolean fileOverwriteCheck(Path filePath) {
    if (!Files.exists(filePath)) {
      return true;
    }

    String fileName = filePath.getFileName().toString();

    if (!force) {
      System.out.println(
          Localization.lang("File '%0' already exists. Use -f or --force to overwrite.", fileName));
      return false;
    }

    System.out.println(Localization.lang("File '%0' already exists. Overwriting.", fileName));
    return true;
  }
}
