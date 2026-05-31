package org.jabref.toolkit.commands;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;

import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.toolkit.converter.KeySuffixConverter;
import org.jabref.toolkit.exception.ExportServiceException;
import org.jabref.toolkit.exception.ImportServiceException;
import org.jabref.toolkit.service.ExportService;
import org.jabref.toolkit.service.ImportService;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;

import static picocli.CommandLine.Command;
import static picocli.CommandLine.Mixin;
import static picocli.CommandLine.Option;
import static picocli.CommandLine.ParentCommand;

@Command(name = "generate", description = "Generate citation keys for entries in a .bib file.")
class GenerateCitationKeys implements Callable<Integer> {

    @ParentCommand
    private CitationKeys parentCommand;

    @Mixin
    private JabKit.SharedOptions sharedOptions = new JabKit.SharedOptions();

    @Mixin
    private InputOption inputOption = new InputOption();

    @Option(names = "--output", description = "Output .bib file")
    private Path outputFile;

    @Option(names = "--pattern", description = "Override the citation key pattern from the preferences")
    private String pattern;

    @Option(names = "--transliterate", description = "Transliterate fields for citation key generation")
    private Boolean transliterate;

    @Option(names = "--warn-before-overwrite", description = "Warn before overwriting existing citation keys")
    private Boolean warnBeforeOverwrite;

    @Option(names = "--suffix", description = "Key suffix strategy: ALWAYS, SECOND_WITH_A, SECOND_WITH_B", converter = KeySuffixConverter.class)
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

    @Option(names = "--key-patterns", description = "Key patterns for specific entry types")
    private Map<String, String> keyPatterns;

    @Override
    public Integer call() throws ImportServiceException, ExportServiceException {
        Path inputFile = inputOption.getInputFile();
        ParserResult parserResult = ImportService.importBibTexFile(
                inputFile,
                parentCommand.getParent().cliPreferences,
                sharedOptions.porcelain);

        BibDatabaseContext databaseContext = parserResult.getDatabaseContext();

        if (!sharedOptions.porcelain) {
            System.out.println(Localization.lang("Regenerating citation keys according to metadata."));
        }

        CitationKeyGenerator keyGenerator = getCitationKeyGenerator(databaseContext);
        for (BibEntry entry : databaseContext.getEntries()) {
            keyGenerator.generateAndSetKey(entry);
        }

        ExportService exportService = ExportService.create(parentCommand.getParent().cliPreferences);
        if (outputFile != null) {
            exportService.saveDatabase(parserResult.getDatabase(), outputFile);
        } else {
            exportService.printDatabaseContextToStdOut(parserResult.getDatabaseContext());
        }
        return CommandLine.ExitCode.OK;
    }

    private @NonNull CitationKeyGenerator getCitationKeyGenerator(BibDatabaseContext databaseContext) {
        CitationKeyPatternPreferences existingPreferences = parentCommand.getParent().cliPreferences.getCitationKeyPatternPreferences();

        CitationKeyPatternPreferences preferencesToUse = new CitationKeyPatternPreferences(
                transliterate != null ? transliterate : existingPreferences.shouldTransliterateFieldsForCitationKey(),
                avoidOverwrite != null ? avoidOverwrite : existingPreferences.shouldAvoidOverwriteCiteKey(),
                warnBeforeOverwrite != null ? warnBeforeOverwrite : existingPreferences.shouldWarnBeforeOverwriteCiteKey(),
                generateBeforeSaving != null ? generateBeforeSaving : existingPreferences.shouldGenerateCiteKeysBeforeSaving(),
                keySuffix != null ? keySuffix : existingPreferences.getKeySuffix(),
                keyPatternRegex != null ? keyPatternRegex : existingPreferences.getKeyPatternRegex(),
                keyPatternReplacement != null ? keyPatternReplacement : existingPreferences.getKeyPatternReplacement(),
                unwantedCharacters != null ? unwantedCharacters : existingPreferences.getUnwantedCharacters(),
                getKeyPatterns(keyPatterns, existingPreferences.getKeyPatterns()),
                new SimpleObjectProperty<>(keywordDelimiter != null ? keywordDelimiter : existingPreferences.getKeywordDelimiter())
        );
        return new CitationKeyGenerator(databaseContext, preferencesToUse);
    }

    /// Creates keyPatterns from preferences and --key-patterns option
    ///
    /// @param keyPatternsOption      patterns submitted by a user via --key-patterns option
    /// @param keyPatternsPreferences patterns from preferences
    /// @return keyPatterns from preferences or overridden by user-supplied patterns
    private GlobalCitationKeyPatterns getKeyPatterns(@Nullable Map<String, String> keyPatternsOption, GlobalCitationKeyPatterns keyPatternsPreferences) {
        GlobalCitationKeyPatterns patternsCopy = pattern != null
                                                 ? GlobalCitationKeyPatterns.fromPattern(pattern)
                                                 : new GlobalCitationKeyPatterns(keyPatternsPreferences.getDefaultValue());
        if (keyPatternsOption == null) {
            return patternsCopy;
        }
        keyPatternsOption.forEach((type, pattern) -> {
            EntryType passedEntryType = EntryTypeFactory.parse(type);
            if (passedEntryType instanceof UnknownEntryType) {
                System.err.println(Localization.lang("The default entry type will be used since the invalid key was passed."));
            }
            patternsCopy.addCitationKeyPattern(passedEntryType, pattern);
        });
        return patternsCopy;
    }
}
