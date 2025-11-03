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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldFormatterCleanupActions {

    public static final List<FieldFormatterCleanup> DEFAULT_SAVE_ACTIONS;
    public static final List<FieldFormatterCleanup> RECOMMEND_BIBTEX_ACTIONS;
    public static final List<FieldFormatterCleanup> RECOMMEND_BIBLATEX_ACTIONS;

    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";

    private static final Logger LOGGER = LoggerFactory.getLogger(FieldFormatterCleanupActions.class);

    /**
     * This parses the key/list map of fields and clean up actions for the field.
     * <p>
     * General format for one key/list map: <code>...[...]</code> - <code>field[formatter1,formatter2,...]</code>
     * Multiple are written as <code>...[...]...[...]...[...]</code>
     * <code>field1[formatter1,formatter2,...]field2[formatter3,formatter4,...]</code>
     * <p>
     * The idea is that characters are field names until <code>[</code> is reached and that formatter lists are terminated by <code>]</code>
     * <p>
     * Example: <code>pages[normalize_page_numbers]title[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]</code>
     */

    static {
        DEFAULT_SAVE_ACTIONS = List.of(
                new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()),
                new FieldFormatterCleanup(StandardField.DATE, new NormalizeDateFormatter()),
                new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()),
                new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new ReplaceUnicodeLigaturesFormatter()),
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

    /**
     * Note: String parsing is done at {@link FieldFormatterCleanupActions#parse(String)}
     */
    public static String getMetaDataString(List<FieldFormatterCleanup> actionList, String newLineSeparator) {
        return FieldFormatterCleanupParser.serializeActions(actionList, newLineSeparator);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<FieldFormatterCleanup> getConfiguredActions() {
        return Collections.unmodifiableList(actions);
    }

    public List<FieldChange> applySaveActions(BibEntry entry) {
        if (enabled) {
            return applyAllActions(entry);
        } else {
            return List.of();
        }
    }

    private List<FieldChange> applyAllActions(BibEntry entry) {
        List<FieldChange> result = new ArrayList<>();

        for (FieldFormatterCleanup action : actions) {
            result.addAll(action.cleanup(entry));
        }

        return result;
    }

    public static List<FieldFormatterCleanup> parse(String formatterString) {
        return FieldFormatterCleanupParser.parseAction(formatterString);
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
