package org.jabref.toolkit.commands;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Map;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.toolkit.converter.CygWinPathConverter;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "generate", description = "Generate citation keys for entries in a .bib file.")
class GenerateCitationKeys implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateCitationKeys.class);

    @ParentCommand
    private CitationKeys parentCommand;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    // [impl->req~jabkit.cli.input-flag~1]
    @Option(names = {"--input"}, converter = CygWinPathConverter.class, description = "Input .bib file", required = true)
    private Path inputFile;

    @Option(names = "--output", description = "Output .bib file")
    private Path outputFile;

    @Option(names = "--pattern", description = "Override the citation key pattern from the preferences")
    private String pattern;

    @Option(names = "--transliterate", description = "Transliterate fields for citation key generation")
    private Boolean transliterate;

    @Option(names = "--warn-before-overwrite", description = "Warn before overwriting existing citation keys")
    private Boolean warnBeforeOverwrite;

    @Option(names = "--suffix", description = "Key suffix strategy: ALWAYS, SECOND_WITH_A, SECOND_WITH_B")
    private CitationKeyPatternPreferences.KeySuffix keySuffix;

    @Option(names = "--regex", description = "Regular expression for key pattern matching")
    private String keyPatternRegex;

    @Option(names = "--regex-replacement", description = "Replacement string for regex matches")
    private String keyPatternReplacement;

    @Option(names = "--unwanted", description = "Unwanted characters to remove from generated keys")
    private String unwantedCharacters;

    @Option(names = "--keyword-delimiter", description = "Delimiter to use between keywords in citation key")
    private Character keywordDelimiter;

    @Option(names = "--avoid-overwrite", description = "Avoid overwriting existing citation keys")
    private Boolean avoidOverwrite;

    @Option(names = "--generate-before-saving", description = "Generate citation keys before saving")
    private Boolean generateBeforeSaving;

    @Option(names = "--entry-type-pattern", description = "Per-entry-type citation key pattern")
    private Map<String, String> entryTypePatterns;

    @Override
    public void run() {
        Optional<ParserResult> parserResult = JabKit.importFile(
                inputFile,
                "bibtex",
                parentCommand.getParent().cliPreferences,
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

        CitationKeyGenerator keyGenerator = getCitationKeyGenerator(databaseContext);
        for (BibEntry entry : databaseContext.getEntries()) {
            keyGenerator.generateAndSetKey(entry);
        }

        if (outputFile != null) {
            JabKit.saveDatabase(
                    parentCommand.getParent().cliPreferences,
                    parentCommand.getParent().entryTypesManager,
                    parserResult.get().getDatabase(),
                    outputFile);
        } else {
            JabKit.outputDatabaseContext(parentCommand.getParent().cliPreferences, parserResult.get().getDatabaseContext());
        }
    }

    private @NonNull CitationKeyGenerator getCitationKeyGenerator(BibDatabaseContext databaseContext) {
        CitationKeyPatternPreferences existingPreferences =
                parentCommand.getParent().cliPreferences.getCitationKeyPatternPreferences();

        String defaultPattern =
                pattern != null
                ? pattern
                : existingPreferences.getDefaultPattern();

        GlobalCitationKeyPatterns patterns =
                GlobalCitationKeyPatterns.fromPattern(defaultPattern);

        if (entryTypePatterns != null) {
            entryTypePatterns.forEach((type, pattern) ->
                    patterns.addCitationKeyPattern(
                            EntryTypeFactory.parse(type),
                            pattern
                    )
            );
        }

        CitationKeyPatternPreferences preferencesToUse =
                new CitationKeyPatternPreferences(
                        transliterate != null ? transliterate : existingPreferences.shouldTransliterateFieldsForCitationKey(),
                        avoidOverwrite != null ? avoidOverwrite : existingPreferences.shouldAvoidOverwriteCiteKey(),
                        warnBeforeOverwrite != null ? warnBeforeOverwrite : existingPreferences.shouldWarnBeforeOverwriteCiteKey(),
                        generateBeforeSaving != null ? generateBeforeSaving : existingPreferences.shouldGenerateCiteKeysBeforeSaving(),
                        keySuffix != null ? keySuffix : existingPreferences.getKeySuffix(),
                        keyPatternRegex != null ? keyPatternRegex : existingPreferences.getKeyPatternRegex(),
                        keyPatternReplacement != null ? keyPatternReplacement : existingPreferences.getKeyPatternReplacement(),
                        unwantedCharacters != null ? unwantedCharacters : existingPreferences.getUnwantedCharacters(),
                        patterns,
                        pattern != null ? pattern : existingPreferences.getDefaultPattern(),
                        keywordDelimiter != null ? keywordDelimiter : existingPreferences.getKeywordDelimiter()
                );

        return new CitationKeyGenerator(databaseContext, preferencesToUse);
    }
}
