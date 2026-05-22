package org.jabref.toolkit.commands;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.integrity.IntegrityCheckResultCsvWriter;
import org.jabref.logic.integrity.IntegrityCheckResultErrorFormatWriter;
import org.jabref.logic.integrity.IntegrityCheckResultWriter;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;

@Command(name = "integrity", description = "Check integrity of the library.")
class CheckIntegrity implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckIntegrity.class);

    @CommandLine.ParentCommand
    private Check check;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @Mixin
    private InputOption inputOption = new InputOption();

    @Option(names = {"--output-format"}, description = "Output format: errorformat, txt or csv", defaultValue = Check.FORMAT_ERRORFORMAT)
    private String outputFormat;

    // in BibTeX it could be preferences.getEntryEditorPreferences().shouldAllowIntegerEditionBibtex()
    @Option(names = {"--allow-integer-edition"}, description = "Allows Integer edition", negatable = true, defaultValue = "true", fallbackValue = "true")
    private boolean allowIntegerEdition;

    @Override
    public Integer call() {
        return execute(inputOption.getInputFile(), outputFormat, allowIntegerEdition, sharedOptions.porcelain, check.jabKit);
    }

    /// Runs the integrity check on `inputFile` and writes the findings to `System.out`.
    ///
    /// Shared with the parent `check` command, which runs both checks at once.
    ///
    /// @return the exit code (0 = no findings, 1 = integrity findings present, 2/3 = error)
    static int execute(Path inputFile, String outputFormat, boolean allowIntegerEdition, boolean porcelain, JabKit jabKit) {
        JabKit.ImportOutcome importOutcome = JabKit.importBibtexLibrary(inputFile, jabKit.cliPreferences, porcelain);
        ParserResult parserResult = importOutcome.parserResult();
        if (parserResult == null) {
            return importOutcome.exitCode();
        }

        if (!porcelain) {
            System.out.println(Localization.lang("Checking integrity of '%0'.", inputFile));
            System.out.flush();
        }

        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();

        IntegrityCheck integrityCheck = new IntegrityCheck(
                databaseContext,
                jabKit.cliPreferences.getFilePreferences(),
                jabKit.cliPreferences.getCitationKeyPatternPreferences(),
                JournalAbbreviationLoader.loadRepository(jabKit.cliPreferences.getJournalAbbreviationPreferences()),
                allowIntegerEdition
        );

        List<IntegrityMessage> messages = databaseContext.getEntries().stream()
                                                         .flatMap(entry -> integrityCheck.checkEntry(entry).stream())
                                                         .collect(Collectors.toList());

        messages.addAll(integrityCheck.checkDatabase(databaseContext.getDatabase()));

        Writer writer = new OutputStreamWriter(System.out);
        IntegrityCheckResultWriter checkResultWriter;
        switch (outputFormat.toLowerCase(Locale.ROOT)) {
            // "txt" is kept as an alias for the errorformat output.
            case Check.FORMAT_ERRORFORMAT,
                 Check.FORMAT_TXT ->
                    checkResultWriter = new IntegrityCheckResultErrorFormatWriter(writer, messages, parserResult, inputFile);
            case Check.FORMAT_CSV ->
                    checkResultWriter = new IntegrityCheckResultCsvWriter(writer, messages);
            default -> {
                System.err.println(Localization.lang("Unknown output format '%0'.", outputFormat));
                return 3;
            }
        }

        try {
            checkResultWriter.writeFindings();
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("Error writing results", e);
            return 2;
        }

        // Signal integrity findings via a non-zero exit so CI can fail on them.
        return messages.isEmpty() ? 0 : 1;
    }
}
