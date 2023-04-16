package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.formatter.Formatters;
import org.jabref.logic.formatter.IdentityFormatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToUnicodeFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.bibtexfields.OrdinalsToSuperscriptFormatter;
import org.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.layout.format.ReplaceUnicodeLigaturesFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

public class FieldFormatterCleanups {

    public static final List<FieldFormatterCleanup> DEFAULT_SAVE_ACTIONS;
    public static final List<FieldFormatterCleanup> RECOMMEND_BIBTEX_ACTIONS;
    public static final List<FieldFormatterCleanup> RECOMMEND_BIBLATEX_ACTIONS;

    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";

    /**
     * This parses the key/list map of fields and clean up actions for the field.
     *
     * General format for one key/list map: <code>...[...]</code> - <code>field[formatter1,formatter2,...]</code>
     * Multiple are written as <code>...[...]...[...]...[...]</code>
     *   <code>field1[formatter1,formatter2,...]field2[formatter3,formatter4,...]</code>
     *
     * The idea is that characters are field names until <code>[</code> is reached and that formatter lists are terminated by <code>]</code>
     *
     * Example: <code>pages[normalize_page_numbers]title[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]</code>
     */
    private static final Pattern FIELD_FORMATTER_CLEANUP_PATTERN = Pattern.compile("([^\\[]+)\\[([^]]+)]");

    static {
        DEFAULT_SAVE_ACTIONS = List.of(
            new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()),
            new FieldFormatterCleanup(StandardField.DATE, new NormalizeDateFormatter()),
            new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()),
            new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new ReplaceUnicodeLigaturesFormatter()));

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

    public FieldFormatterCleanups(boolean enabled, List<FieldFormatterCleanup> actions) {
        this.enabled = enabled;
        this.actions = Objects.requireNonNull(actions);
    }

    /**
     * Note: String parsing is done at {@link FieldFormatterCleanups#parse(String)}
     */
    public static String getMetaDataString(List<FieldFormatterCleanup> actionList, String newLineSeparator) {
        // first, group all formatters by the field for which they apply
        Map<Field, List<String>> groupedByField = new TreeMap<>(Comparator.comparing(Field::getName));
        for (FieldFormatterCleanup cleanup : actionList) {
            Field key = cleanup.getField();

            // add new list into the hashmap if needed
            if (!groupedByField.containsKey(key)) {
                groupedByField.put(key, new ArrayList<>());
            }

            // add the formatter to the map if it is not already there
            List<String> formattersForKey = groupedByField.get(key);
            if (!formattersForKey.contains(cleanup.getFormatter().getKey())) {
                formattersForKey.add(cleanup.getFormatter().getKey());
            }
        }

        // convert the contents of the hashmap into the correct serialization
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Field, List<String>> entry : groupedByField.entrySet()) {
            result.append(entry.getKey().getName());

            StringJoiner joiner = new StringJoiner(",", "[", "]" + newLineSeparator);
            entry.getValue().forEach(joiner::add);
            result.append(joiner);
        }

        return result.toString();
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
            return Collections.emptyList();
        }
    }

    private List<FieldChange> applyAllActions(BibEntry entry) {
        List<FieldChange> result = new ArrayList<>();

        for (FieldFormatterCleanup action : actions) {
            result.addAll(action.cleanup(entry));
        }

        return result;
    }

    // ToDo: This should reside in MetaDataSerializer
    public List<String> getAsStringList(String delimiter) {
        List<String> stringRepresentation = new ArrayList<>();

        if (enabled) {
            stringRepresentation.add(ENABLED);
        } else {
            stringRepresentation.add(DISABLED);
        }

        String formatterString = getMetaDataString(actions, delimiter);
        stringRepresentation.add(formatterString);
        return stringRepresentation;
    }

    public static List<FieldFormatterCleanup> parse(String formatterString) {
        if ((formatterString == null) || formatterString.isEmpty()) {
            // no save actions defined in the meta data
            return Collections.emptyList();
        }

        List<FieldFormatterCleanup> result = new ArrayList<>();

        // first remove all newlines for easier parsing
        String formatterStringWithoutLineBreaks = StringUtil.unifyLineBreaks(formatterString, "");

        Matcher matcher = FIELD_FORMATTER_CLEANUP_PATTERN.matcher(formatterStringWithoutLineBreaks);
        while (matcher.find()) {
            String fieldKey = matcher.group(1);
            Field field = FieldFactory.parseField(fieldKey);

            String fieldString = matcher.group(2);

            List<FieldFormatterCleanup> fieldFormatterCleanups = Arrays.stream(fieldString.split(","))
                                                                       .map(FieldFormatterCleanups::getFormatterFromString)
                                                                       .map(formatter -> new FieldFormatterCleanup(field, formatter))
                                                                       .toList();
            result.addAll(fieldFormatterCleanups);
        }
        return result;
    }

    // ToDo: This should reside in MetaDataParser
    public static FieldFormatterCleanups parse(List<String> formatterMetaList) {
        if ((formatterMetaList != null) && (formatterMetaList.size() >= 2)) {
            boolean enablementStatus = FieldFormatterCleanups.ENABLED.equals(formatterMetaList.get(0));
            String formatterString = formatterMetaList.get(1);

            return new FieldFormatterCleanups(enablementStatus, parse(formatterString));
        } else {
            // return default actions
            return new FieldFormatterCleanups(false, DEFAULT_SAVE_ACTIONS);
        }
    }

    private static Formatter getFormatterFromString(String formatterName) {
        for (Formatter formatter : Formatters.getAll()) {
            if (formatterName.equals(formatter.getKey())) {
                return formatter;
            }
        }
        return new IdentityFormatter();
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
        if (obj instanceof FieldFormatterCleanups other) {
            return Objects.equals(actions, other.actions) && (enabled == other.enabled);
        }
        return false;
    }

    @Override
    public String toString() {
        return "FieldFormatterCleanups{" +
                "enabled=" + enabled + "," +
                "actions=" + actions +
                "}";
    }
}
