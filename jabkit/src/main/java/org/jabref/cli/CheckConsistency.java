package org.jabref.cli;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultCsvWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultTxtWriter;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheckResultWriter;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "check-consistency", description = "Check consistency of the database.")
class CheckConsistency implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckConsistency.class);

    @ParentCommand
    private ArgumentProcessor argumentProcessor;

    @Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    @Option(names = {"--input"}, description = "Input BibTeX file", required = true)
    private Path inputFile;

    @Option(names = {"--output-format"}, description = "Output format: txt or csv", defaultValue = "txt")
    private String outputFormat;

    @Override
    public void run() {
        if (inputFile == null) {
            System.out.println(Localization.lang("No file specified for consistency check."));
            return;
        }

        if (!FileUtil.isBibFile(inputFile)) {
            System.out.println(Localization.lang("Only bib files for consistency check."));
            return;
        }

        ParserResult pr;
        try {
            pr = OpenDatabase.loadDatabase(
                    inputFile,
                    argumentProcessor.cliPreferences.getImportFormatPreferences(),
                    new DummyFileUpdateMonitor()
            );
        } catch (IOException ex) {
            LOGGER.error("Error reading '{}'.", inputFile, ex);
            return;
        }
        BibDatabaseContext databaseContext = pr.getDatabaseContext();
        List<BibEntry> entries = databaseContext.getDatabase().getEntries();

        BibliographyConsistencyCheck consistencyCheck = new BibliographyConsistencyCheck();
        BibliographyConsistencyCheck.Result result = consistencyCheck.check(entries);

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
        }
        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Consistency check completed"));
        }
    }
}
