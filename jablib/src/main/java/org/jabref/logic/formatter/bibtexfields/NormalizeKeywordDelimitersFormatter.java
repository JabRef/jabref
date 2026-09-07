package org.jabref.logic.formatter.bibtexfields;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.cleanup.KeywordSeparatorAware;
import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.importer.KeywordImportNormalizer;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.KeywordList;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Rewrites a keyword field that uses one of the accepted import delimiters to the keyword separator of the library.
///
/// A field that already uses the separator is returned unchanged, including its spacing, so applying this
/// formatter to a consistent library never produces a diff.
///
/// [impl->req~save.keywords.normalize-delimiters~1]
@NullMarked
public class NormalizeKeywordDelimitersFormatter extends Formatter implements KeywordSeparatorAware {

    private final @Nullable BibEntryPreferences bibEntryPreferences;
    private final @Nullable Character keywordSeparator;

    /// Unbound: preferences and separator are resolved from the global preferences when formatting.
    public NormalizeKeywordDelimitersFormatter() {
        this.bibEntryPreferences = null;
        this.keywordSeparator = null;
    }

    /// @param keywordSeparator the separator to serialize with; `null` uses the one from the preferences
    public NormalizeKeywordDelimitersFormatter(@Nullable BibEntryPreferences bibEntryPreferences, @Nullable Character keywordSeparator) {
        this.bibEntryPreferences = bibEntryPreferences;
        this.keywordSeparator = keywordSeparator;
    }

    @Override
    public Formatter withKeywordSeparator(Character keywordSeparator) {
        return new NormalizeKeywordDelimitersFormatter(bibEntryPreferences, keywordSeparator);
    }

    @Override
    public String getName() {
        return Localization.lang("Normalize keyword delimiters");
    }

    @Override
    public String getKey() {
        return "normalize_keyword_delimiters";
    }

    @Override
    public String format(@NonNull String value) {
        if (value.isBlank()) {
            return value;
        }
        BibEntryPreferences preferences = getBibEntryPreferences();
        Character separator = Optional.ofNullable(keywordSeparator)
                                      .or(() -> Optional.ofNullable(preferences.getKeywordSeparator()))
                                      .orElse(BibEntryPreferences.getDefault().getKeywordSeparator());
        List<Character> delimiters = Stream.concat(
                                                   KeywordImportNormalizer.parseConfiguredDelimiters(preferences.getImportKeywordDelimiters()).stream(),
                                                   Stream.of(separator))
                                           .distinct()
                                           .toList();
        BibEntryPreferences.ImportDelimiterParsingStrategy strategy = Optional.ofNullable(preferences.getImportDelimiterParsingStrategy())
                                                                              .orElse(BibEntryPreferences.ImportDelimiterParsingStrategy.SPLIT_ON_ALL_DELIMITERS);

        KeywordList keywords = switch (strategy) {
            case SPLIT_ON_ALL_DELIMITERS ->
                    KeywordList.parseWithMultipleDelimiters(value, delimiters);
            case INFER_DELIMITER_BY_PRIORITY ->
                    KeywordList.parseWithPrioritizedDelimiters(value, delimiters);
        };
        if (keywords.equals(KeywordList.parse(value, separator))) {
            return value;
        }
        return KeywordList.serializeWithSpaces(keywords.stream().toList(), separator);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Rewrite keywords separated by an accepted import delimiter to the keyword separator of the library.");
    }

    @Override
    public String getExampleInput() {
        return "keywordOne; keywordTwo, keywordThree";
    }

    private BibEntryPreferences getBibEntryPreferences() {
        if (bibEntryPreferences != null) {
            return bibEntryPreferences;
        }
        return Injector.instantiateModelOrService(JabRefCliPreferences.class).getBibEntryPreferences();
    }
}
