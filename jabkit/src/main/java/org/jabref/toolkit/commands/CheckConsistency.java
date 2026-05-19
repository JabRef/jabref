package org.jabref.toolkit.commands;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultCsvWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultErrorFormatWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultTxtWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultWriter;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "consistency", description = "Check consistency of the library.")
class CheckConsistency implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckConsistency.class);

    @ParentCommand
    private Check check;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @Mixin
    private InputOption inputOption = new InputOption();

    @Option(names = {"--output-format"}, description = "Output format: errorformat, txt or csv", defaultValue = Check.FORMAT_ERRORFORMAT)
    private String outputFormat;

    @Override
    public Integer call() {
        return execute(inputOption.getInputFile(), outputFormat, sharedOptions.porcelain, check.jabKit);
    }

    /// Runs the consistency check on `inputFile` and writes the findings to `System.out`.
    ///
    /// Shared with the parent `check` command, which runs both checks at once.
    ///
    /// @return the exit code (0 = consistent, 1 = inconsistencies found, 2/3 = error)
    static int execute(Path inputFile, String outputFormat, boolean porcelain, JabKit jabKit) {
        JabKit.ImportOutcome importOutcome = JabKit.importBibtexLibrary(inputFile, jabKit.cliPreferences, porcelain);
        ParserResult parserResult = importOutcome.parserResult();
        if (parserResult == null) {
            return importOutcome.exitCode();
        }

        if (!porcelain) {
            System.out.println(Localization.lang("Checking consistency of '%0'.", inputFile));
            System.out.flush();
        }

        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();

        BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
        BibliographyConsistencyCheck.Result result = consistencyCheck.check(databaseContext, jabKit.entryTypesManager, (count, total) -> {
            if (!porcelain) {
                System.out.println(Localization.lang("Checking consistency for entry type %0 of %1", count + 1, total));
            }
        });

        return writeCheckResult(result, databaseContext, parserResult, inputFile, outputFormat, porcelain, jabKit);
    }

    private static int writeCheckResult(BibliographyConsistencyCheck.Result result,
                                        BibDatabaseContext databaseContext,
                                        ParserResult parserResult,
                                        Path inputFile,
                                        String outputFormat,
                                        boolean porcelain,
                                        JabKit jabKit) {
        Writer writer = new OutputStreamWriter(System.out);
        BibliographyConsistencyCheckResultWriter checkResultWriter;

        if (Check.FORMAT_ERRORFORMAT.equalsIgnoreCase(outputFormat)) {
            checkResultWriter = new BibliographyConsistencyCheckResultErrorFormatWriter(
                    result,
                    writer,
                    porcelain,
                    jabKit.entryTypesManager,
                    databaseContext.getMode(),
                    parserResult,
                    inputFile);
        } else if (Check.FORMAT_TXT.equalsIgnoreCase(outputFormat)) {
            checkResultWriter = new BibliographyConsistencyCheckResultTxtWriter(
                    result,
                    writer,
                    porcelain,
                    jabKit.entryTypesManager,
                    databaseContext.getMode());
        } else {
            checkResultWriter = new BibliographyConsistencyCheckResultCsvWriter(
                    result,
                    writer,
                    porcelain,
                    jabKit.entryTypesManager,
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

        if (!porcelain) {
            System.out.println(Localization.lang("Consistency check completed"));
        }
        return 0;
    }
}
