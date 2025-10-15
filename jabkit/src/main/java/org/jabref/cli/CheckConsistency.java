package org.jabref.cli;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.cli.converter.CygWinPathConverter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultCsvWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultTxtWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultWriter;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "check-consistency", description = "Check consistency of the library.")
class CheckConsistency implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckConsistency.class);

    @ParentCommand
    private JabKitArgumentProcessor argumentProcessor;

    @Mixin
    private JabKitArgumentProcessor.SharedOptions sharedOptions = new JabKitArgumentProcessor.SharedOptions();

    // [impl->req~jabkit.cli.input-flag~1]
    @Option(names = {"--input"}, converter = CygWinPathConverter.class, description = "Input BibTeX file", required = true)
    private Path inputFile;

    @Option(names = {"--output-format"}, description = "Output format: txt or csv", defaultValue = "txt")
    private String outputFormat;

    @Override
    public Integer call() {
        Optional<ParserResult> parserResult = JabKitArgumentProcessor.importFile(
                inputFile,
                "bibtex",
                argumentProcessor.cliPreferences,
                sharedOptions.porcelain);
        if (parserResult.isEmpty()) {
            System.out.println(Localization.lang("Unable to open file '%0'.", inputFile));
            return 2;
        }

        if (parserResult.get().isInvalid()) {
            System.out.println(Localization.lang("Input file '%0' is invalid and could not be parsed.", inputFile));
            return 2;
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Checking consistency of '%0'.", inputFile));
            System.out.flush();
        }

        BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();

        BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
        BibliographyConsistencyCheck.Result result = consistencyCheck.check(databaseContext, (count, total) -> {
            if (!sharedOptions.porcelain) {
                System.out.println(Localization.lang("Checking consistency for entry type %0 of %1", count + 1, total));
            }
        });

        return writeCheckResult(result, databaseContext);
    }

    private int writeCheckResult(BibliographyConsistencyCheck.Result result, BibDatabaseContext databaseContext) {
        Writer writer = new OutputStreamWriter(System.out);
        BibliographyConsistencyCheckResultWriter checkResultWriter;

        if ("txt".equalsIgnoreCase(outputFormat)) {
            checkResultWriter = new BibliographyConsistencyCheckResultTxtWriter(
                    result,
                    writer,
                    sharedOptions.porcelain,
                    argumentProcessor.entryTypesManager,
                    databaseContext.getMode());
        } else {
            checkResultWriter = new BibliographyConsistencyCheckResultCsvWriter(
                    result,
                    writer,
                    sharedOptions.porcelain,
                    argumentProcessor.entryTypesManager,
                    databaseContext.getMode());
        }

        // System.out should not be closed, therefore no try-with-resources
        try {
            checkResultWriter.writeFindings();
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("Error writing results", e);
            return 2;
        }

        if (!result.entryTypeToResultMap().isEmpty()) {
            return 1;
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Consistency check completed"));
        }
        return 0;
    }
}
