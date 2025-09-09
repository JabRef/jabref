package org.jabref.cli;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.integrity.IntegrityCheck;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.Parameters;

@Command(name = "check-integrity", description = "Check integrity of the database.")
class CheckIntegrity implements Callable<Integer> {

    @CommandLine.ParentCommand
    private ArgumentProcessor argumentProcessor;

    @Mixin
    private ArgumentProcessor.SharedOptions sharedOptions = new ArgumentProcessor.SharedOptions();

    @Parameters(description = "BibTeX file to check", arity = "1")
    private String inputFile;

    @Option(names = {"--output-format"}, description = "Output format: errorformat, txt or csv", defaultValue = "errorformat")
    private String outputFormat;

    @Option(names = {"--allow-integer-edition"}, description = "Allows Integer edition: true or false", defaultValue = "true")
    private boolean allowIntegerEdition = true;

    @Override
    public Integer call() {
        Optional<ParserResult> parserResult = ArgumentProcessor.importFile(
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
            System.out.println(Localization.lang("Checking integrity of '%0'.", inputFile));
        }

        BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();

        IntegrityCheck integrityCheck = new IntegrityCheck(
                databaseContext,
                argumentProcessor.cliPreferences.getFilePreferences(),
                argumentProcessor.cliPreferences.getCitationKeyPatternPreferences(),
                JournalAbbreviationLoader.loadRepository(argumentProcessor.cliPreferences.getJournalAbbreviationPreferences()),
                allowIntegerEdition
        );

        List<IntegrityMessage> messages = databaseContext.getEntries().stream()
                                                         .flatMap(entry -> {
                                                             if (!sharedOptions.porcelain) {
                                                                 System.out.println(Localization.lang("Checking entry with citation key '%0'.", entry.getCitationKey().orElse("")));
                                                             }
                                                             return integrityCheck.checkEntry(entry).stream();
                                                         })
                                                         .toList();

        return switch (outputFormat.toLowerCase(Locale.ROOT)) {
            case "errorformat" ->
                    outputErrorFormat(messages);
            case "txt" ->
                    outputTxt(messages);
            case "csv" ->
                    outputCsv(messages);
            default -> {
                System.out.println(Localization.lang("Unknown output format '%0'.", outputFormat));
                yield 3;
            }
        };
    }

    private int outputCsv(List<IntegrityMessage> messages) {
        System.out.println("Citation Key,Field,Message");
        for (IntegrityMessage message : messages) {
            String citationKey = message.entry().getCitationKey().orElse("");
            String field = message.field() != null ? message.field().getDisplayName() : "";
            String msg = message.message().replace("\"", "\\\"");
            if (msg.contains(",")) {
                msg = "\"" + msg + "\"";
            }
            System.out.printf("%s,%s,%s%n", citationKey, field, msg);
        }
        return 0;
    }

    private int outputTxt(List<IntegrityMessage> messages) {
        messages.forEach(System.out::println);
        return 0;
    }

    private int outputErrorFormat(List<IntegrityMessage> messages) {
        for (IntegrityMessage message : messages) {
            BibEntry.FieldRange fieldRange = message.entry().getFieldRangeFromField(message.field());
            System.out.printf("%s:%d:%d: %s\n".formatted(
                    inputFile,
                    fieldRange.startLine(),
                    fieldRange.startColumn(),
                    message.message()));
        }
        return 0;
    }
}
