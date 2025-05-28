package org.jabref.cli;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultCsvWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultTxtWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultWriter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "check-consistency", description = "Check consistency of the library.")
class CheckConsistency implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckConsistency.class);

    @ParentCommand
    private ArgumentProcessor argumentProcessor;

    @Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    @Option(names = {"--input"}, description = "Input BibTeX file", required = true)
    private String inputFile;

    @Option(names = {"--output-format"}, description = "Output format: txt or csv", defaultValue = "txt")
    private String outputFormat;

    @Override
    public void run() {
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

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Checking consistency of '%0'.", inputFile));
            System.out.flush();
        }

        BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();
        List<BibEntry> entries = databaseContext.getDatabase().getEntries();

        BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
        BibliographyConsistencyCheck.Result result = consistencyCheck.check(entries, (count, total) -> {
            System.out.println(Localization.lang("Checking consistency for entry type %0 of %1", count + 1, total));
        });

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
            return;
        }

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Consistency check completed"));
        }
    }
}
