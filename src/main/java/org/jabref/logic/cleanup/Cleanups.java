package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

public class Cleanups {

    public static final FieldFormatterCleanups DEFAULT_SAVE_ACTIONS;
    public static final FieldFormatterCleanups RECOMMEND_BIBTEX_ACTIONS;
    public static final FieldFormatterCleanups RECOMMEND_BIBLATEX_ACTIONS;

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
    private static final Pattern FIELD_FORMATTER_CLEANUP_PATTERN = Pattern.compile("([^\\[]+)\\[([^\\]]+)\\]");

    static {
        List<FieldFormatterCleanup> defaultFormatters = new ArrayList<>();
        defaultFormatters.add(new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup(StandardField.DATE, new NormalizeDateFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new ReplaceUnicodeLigaturesFormatter()));
        DEFAULT_SAVE_ACTIONS = new FieldFormatterCleanups(false, defaultFormatters);

        List<FieldFormatterCleanup> recommendedBibTeXFormatters = new ArrayList<>();
        recommendedBibTeXFormatters.addAll(defaultFormatters);
        recommendedBibTeXFormatters.add(new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new HtmlToLatexFormatter()));
        recommendedBibTeXFormatters.add(new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new UnicodeToLatexFormatter()));
        recommendedBibTeXFormatters.add(new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new OrdinalsToSuperscriptFormatter()));
        RECOMMEND_BIBTEX_ACTIONS = new FieldFormatterCleanups(false, recommendedBibTeXFormatters);

        List<FieldFormatterCleanup> recommendedBiblatexFormatters = new ArrayList<>();
        recommendedBiblatexFormatters.addAll(defaultFormatters);
        recommendedBiblatexFormatters.add(new FieldFormatterCleanup(StandardField.TITLE, new HtmlToUnicodeFormatter()));
        recommendedBiblatexFormatters.add(new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new LatexToUnicodeFormatter()));
        // DO NOT ADD OrdinalsToSuperscriptFormatter here, because this causes issues. See https://github.com/JabRef/jabref/issues/2596.
        RECOMMEND_BIBLATEX_ACTIONS = new FieldFormatterCleanups(false, recommendedBiblatexFormatters);
    }

    private Cleanups() {
    }

    public static List<Formatter> getBuiltInFormatters() {
        return Formatters.getAll();
    }

    public static List<FieldFormatterCleanup> parse(String formatterString) {
        if ((formatterString == null) || formatterString.isEmpty()) {
            // no save actions defined in the meta data
            return Collections.emptyList();
        }

        // first remove all newlines for easier parsing
        String formatterStringWithoutLineBreaks = StringUtil.unifyLineBreaks(formatterString, "");

        List<FieldFormatterCleanup> result = new ArrayList<>();

        Matcher matcher = FIELD_FORMATTER_CLEANUP_PATTERN.matcher(formatterStringWithoutLineBreaks);
        while (matcher.find()) {
            String fieldKey = matcher.group(1);
            Field field = FieldFactory.parseField(fieldKey);

            String fieldString = matcher.group(2);

            List<FieldFormatterCleanup> fieldFormatterCleanups = Arrays.stream(fieldString.split(","))
                                                                       .map(formatterKey -> getFormatterFromString(formatterKey))
                                                                       .map(formatter -> new FieldFormatterCleanup(field, formatter))
                                                                       .toList();
            result.addAll(fieldFormatterCleanups);
        }
        return result;
    }

    public static FieldFormatterCleanups parse(List<String> formatterMetaList) {
        if ((formatterMetaList != null) && (formatterMetaList.size() >= 2)) {
            boolean enablementStatus = FieldFormatterCleanups.ENABLED.equals(formatterMetaList.get(0));
            String formatterString = formatterMetaList.get(1);
            return new FieldFormatterCleanups(enablementStatus, parse(formatterString));
        } else {
            // return default actions
            return DEFAULT_SAVE_ACTIONS;
        }
    }

    private static Formatter getFormatterFromString(String formatterName) {
        for (Formatter formatter : getBuiltInFormatters()) {
            if (formatterName.equals(formatter.getKey())) {
                return formatter;
            }
        }
        return new IdentityFormatter();
    }
}
