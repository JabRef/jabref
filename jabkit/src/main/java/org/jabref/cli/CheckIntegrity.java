package org.jabref.cli;

import java.util.List;
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
            System.out.flush();
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
                .flatMap(entry -> integrityCheck.checkEntry(entry).stream())
                .toList();

        switch (outputFormat) {
            case "errorformat" -> messages.forEach(message -> {
                if (message.field() != null) {
                    System.out.println(String.format("%s:%d: %s: %s",
                            inputFile,
                            message.getLineNumber().orElse(0),
                            message.field().getName(),
                            message.message()));
                } else {
                    System.out.println(String.format("%s:%d: %s",
                            inputFile,
                            message.getLineNumber().orElse(0),
                            message.message()));
                }
            });
            case "txt" -> {
                if (messages.isEmpty()) {
                    System.out.println(Localization.lang("No integrity problems found."));
                } else {
                    messages.forEach(message -> {
                        if (message.field() != null) {
                            System.out.println(String.format("- %s: %s", message.field().getName(), message.message()));
                        } else {
                            System.out.println(String.format("- %s", message.message()));
                        }
                    });
                    System.out.println();
                    System.out.println(Localization.lang("Total integrity problems found: %0.", String.valueOf(messages.size())));
                }
            }
            case "csv" -> {
                System.out.println("file,line,field,message");
                messages.forEach(message -> {
                    String line = message.getLineNumber().map(Object::toString).orElse("");
                    String field = message.field() != null ? message.field().getName() : "";
                    System.out.println(String.format("%s,%s,%s,%s",
                            inputFile,
                            line,
                            field,
                            message.message().replace("\"", "\"\"")));
                });
            }
            default -> {
                System.out.println(Localization.lang("Unknown output format '%0'.", outputFormat));
                return 3;
            }
        }
    }

    private void outputErrorFormat(List<IntegrityMessage> messages) {
        messages.forEach(message -> {
            if (message.field() != null) {
                System.out.println(String.format("%s:%d: %s: %s",
                        inputFile,
                        message.getLineNumber().orElse(0),
                        message.field().getName(),
                        message.message()));
            } else {
                System.out.println(String.format("%s:%d: %s",
                        inputFile,
                        message.getLineNumber().orElse(0),
                        message.message()));
            }
        });
    }
}
