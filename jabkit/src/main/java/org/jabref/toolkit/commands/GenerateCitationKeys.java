package org.jabref.toolkit.commands;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.toolkit.converter.CygWinPathConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "generate-citation-keys", description = "Generate citation keys for entries in a .bib file.")
class GenerateCitationKeys implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateCitationKeys.class);

    @ParentCommand
    private JabKit argumentProcessor;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    // [impl->req~jabkit.cli.input-flag~1]
    @Option(names = {"--input"}, converter = CygWinPathConverter.class, description = "Input BibTeX file", required = true)
    private Path inputFile;

    @Option(names = "--output", description = "The output .bib file.")
    private Path outputFile;

    @Override
    public void run() {
        Optional<ParserResult> parserResult = JabKit.importFile(
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

        BibDatabaseContext databaseContext = parserResult.get().getDatabaseContext();

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Regenerating citation keys according to metadata."));
        }

        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(
                databaseContext,
                argumentProcessor.cliPreferences.getCitationKeyPatternPreferences());
        for (BibEntry entry : databaseContext.getEntries()) {
            keyGenerator.generateAndSetKey(entry);
        }

        if (outputFile != null) {
            JabKit.saveDatabase(
                    argumentProcessor.cliPreferences,
                    argumentProcessor.entryTypesManager,
                    parserResult.get().getDatabase(),
                    outputFile);
        } else {
            JabKit.outputDatabaseContext(argumentProcessor.cliPreferences, parserResult.get().getDatabaseContext());
        }
    }
}
