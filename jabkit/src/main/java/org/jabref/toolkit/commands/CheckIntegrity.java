package org.jabref.toolkit.commands;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.integrity.IntegrityCheckResultCsvWriter;
import org.jabref.logic.integrity.IntegrityCheckResultErrorFormatWriter;
import org.jabref.logic.integrity.IntegrityCheckResultTxtWriter;
import org.jabref.logic.integrity.IntegrityCheckResultWriter;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.toolkit.converter.CygWinPathConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;

@Command(name = "check-integrity", description = "Check integrity of the database.")
class CheckIntegrity implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckIntegrity.class);

    @CommandLine.ParentCommand
    private JabKit jabKit;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    // [impl->req~jabkit.cli.input-flag~1]
    @Option(names = {"--input"}, converter = CygWinPathConverter.class, description = "Input BibTeX file", required = true)
    private Path inputFile;

    @Option(names = {"--output-format"}, description = "Output format: errorformat, txt or csv", defaultValue = "errorformat")
    private String outputFormat;

    // in BibTeX it could be preferences.getEntryEditorPreferences().shouldAllowIntegerEditionBibtex()
    @Option(names = {"--allow-integer-edition"}, description = "Allows Integer edition", negatable = true, defaultValue = "true", fallbackValue = "true")
    private boolean allowIntegerEdition;

    @Override
    public Integer call() {
        Optional<ParserResult> parserResult = JabKit.importFile(
                inputFile,
                "bibtex",
                jabKit.cliPreferences,
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
            System.out.println(Localization.lang("Checking integrity of '%0'.", inputFile));
            System.out.flush();
        }

        BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();

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
            case "errorformat" ->
                    checkResultWriter = new IntegrityCheckResultErrorFormatWriter(writer, messages, parserResult.get(), inputFile);
            case "txt" ->
                    checkResultWriter = new IntegrityCheckResultTxtWriter(writer, messages);
            case "csv" ->
                    checkResultWriter = new IntegrityCheckResultCsvWriter(writer, messages);
            default -> {
                System.out.println(Localization.lang("Unknown output format '%0'.", outputFormat));
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
        return 0;
    }
}
