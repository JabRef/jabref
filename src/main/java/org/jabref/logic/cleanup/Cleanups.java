package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;

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
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

public class Cleanups {

    public static final FieldFormatterCleanups DEFAULT_SAVE_ACTIONS;
    public static final FieldFormatterCleanups RECOMMEND_BIBTEX_ACTIONS;
    public static final FieldFormatterCleanups RECOMMEND_BIBLATEX_ACTIONS;

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
            return new ArrayList<>();
        }

        List<FieldFormatterCleanup> actions = new ArrayList<>();

        // read concrete actions
        int startIndex = 0;

        // first remove all newlines for easier parsing
        String remainingString = formatterString;

        remainingString = StringUtil.unifyLineBreaks(remainingString, "");
        try {
            while (startIndex < formatterString.length()) {
                // read the field name
                int currentIndex = remainingString.indexOf('[');
                String fieldKey = remainingString.substring(0, currentIndex);
                int endIndex = remainingString.indexOf(']');
                startIndex += endIndex + 1;

                // read each formatter
                int tokenIndex = remainingString.indexOf(',');
                do {
                    boolean doBreak = false;
                    if ((tokenIndex == -1) || (tokenIndex > endIndex)) {
                        tokenIndex = remainingString.indexOf(']');
                        doBreak = true;
                    }

                    String formatterKey = remainingString.substring(currentIndex + 1, tokenIndex);
                    actions.add(new FieldFormatterCleanup(FieldFactory.parseField(fieldKey), getFormatterFromString(formatterKey)));

                    remainingString = remainingString.substring(tokenIndex + 1);
                    if (remainingString.startsWith("]") || doBreak) {
                        break;
                    }
                    tokenIndex = remainingString.indexOf(',');

                    currentIndex = -1;
                } while (true);
            }
        } catch (StringIndexOutOfBoundsException ignore) {
            // if this exception occurs, the remaining part of the save actions string is invalid.
            // Thus we stop parsing and take what we have parsed until now
            return actions;
        }
        return actions;
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
