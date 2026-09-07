package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.formatter.IdentityFormatter;
import org.jabref.logic.formatter.bibtexfields.ConvertMSCCodesFormatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToUnicodeFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeIssn;
import org.jabref.logic.formatter.bibtexfields.NormalizeKeywordDelimitersFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.bibtexfields.OrdinalsToSuperscriptFormatter;
import org.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.layout.format.ReplaceUnicodeLigaturesFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldFormatterCleanupActions {

    public static final List<FieldFormatterCleanup> DEFAULT_SAVE_ACTIONS;
    public static final List<FieldFormatterCleanup> RECOMMEND_BIBTEX_ACTIONS;
    public static final List<FieldFormatterCleanup> RECOMMEND_BIBLATEX_ACTIONS;

    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldFormatterCleanupActions.class);

    static {
        DEFAULT_SAVE_ACTIONS = List.of(
                new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()),
                new FieldFormatterCleanup(StandardField.DATE, new NormalizeDateFormatter()),
                new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()),
                new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new ReplaceUnicodeLigaturesFormatter()),
                new FieldFormatterCleanup(StandardField.KEYWORDS, new NormalizeKeywordDelimitersFormatter()),
                new FieldFormatterCleanup(StandardField.KEYWORDS, new ConvertMSCCodesFormatter()),
                new FieldFormatterCleanup(StandardField.ISSN, new NormalizeIssn()));

        List<FieldFormatterCleanup> recommendedBibtexFormatters = new ArrayList<>(DEFAULT_SAVE_ACTIONS);
        recommendedBibtexFormatters.addAll(List.of(
                new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new HtmlToLatexFormatter()),
                new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new UnicodeToLatexFormatter()),
                new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new OrdinalsToSuperscriptFormatter())));
        RECOMMEND_BIBTEX_ACTIONS = Collections.unmodifiableList(recommendedBibtexFormatters);

        List<FieldFormatterCleanup> recommendedBiblatexFormatters = new ArrayList<>(DEFAULT_SAVE_ACTIONS);
        recommendedBiblatexFormatters.addAll(List.of(
                new FieldFormatterCleanup(StandardField.TITLE, new HtmlToUnicodeFormatter()),
                new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new LatexToUnicodeFormatter())));
        // DO NOT ADD OrdinalsToSuperscriptFormatter here, because this causes issues. See https://github.com/JabRef/jabref/issues/2596.
        RECOMMEND_BIBLATEX_ACTIONS = Collections.unmodifiableList(recommendedBiblatexFormatters);
    }

    private final boolean enabled;
    private final List<FieldFormatterCleanup> actions;

    public FieldFormatterCleanupActions(boolean enabled, @NonNull List<FieldFormatterCleanup> actions) {
        this.enabled = enabled;
        this.actions = actions;
    }

    /// Note: String parsing is done at [FieldFormatterCleanupMapper#parseActions(String)]
    public static String getMetaDataString(List<FieldFormatterCleanup> actionList, String newLineSeparator) {
        return FieldFormatterCleanupMapper.serializeActions(actionList, newLineSeparator);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<FieldFormatterCleanup> getConfiguredActions() {
        return Collections.unmodifiableList(actions);
    }

    /// The configured actions with every [KeywordSeparatorAware] formatter bound to the given library separator.
    /// `null` leaves the actions unbound, so those formatters use the global preference.
    public List<FieldFormatterCleanup> getConfiguredActions(@Nullable Character libraryKeywordSeparator) {
        if (libraryKeywordSeparator == null) {
            return getConfiguredActions();
        }
        return actions.stream()
                      .map(action -> action.getFormatter() instanceof KeywordSeparatorAware aware
                                     ? new FieldFormatterCleanup(action.getField(), aware.withKeywordSeparator(libraryKeywordSeparator))
                                     : action)
                      .toList();
    }

    public List<FieldChange> applySaveActions(BibEntry entry) {
        return applySaveActions(entry, null);
    }

    /// @param libraryKeywordSeparator the separator declared by the library the entry belongs to, or `null` for the global preference
    public List<FieldChange> applySaveActions(BibEntry entry, @Nullable Character libraryKeywordSeparator) {
        if (enabled) {
            return applyAllActions(entry, libraryKeywordSeparator);
        } else {
            return List.of();
        }
    }

    private List<FieldChange> applyAllActions(BibEntry entry, @Nullable Character libraryKeywordSeparator) {
        List<FieldChange> result = new ArrayList<>();

        for (FieldFormatterCleanup action : getConfiguredActions(libraryKeywordSeparator)) {
            result.addAll(action.cleanup(entry));
        }

        return result;
    }

    static Formatter getFormatterFromString(String formatterName) {
        return Formatters
                .getFormatterForKey(formatterName)
                .orElseGet(() -> {
                    if (!"identity".equals(formatterName)) {
                        // The identity formatter is not listed in the formatters list, but is still valid
                        // Therefore, we log errors in other cases only
                        LOGGER.info("Formatter {} not found.", formatterName);
                    }
                    return new IdentityFormatter();
                });
    }

    @Override
    public int hashCode() {
        return Objects.hash(actions, enabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FieldFormatterCleanupActions other) {
            return Objects.equals(actions, other.actions) && (enabled == other.enabled);
        }
        return false;
    }

    @Override
    public String toString() {
        return "FieldFormatterCleanupActions{" +
                "enabled=" + enabled + "," +
                "actions=" + actions +
                "}";
    }
}
