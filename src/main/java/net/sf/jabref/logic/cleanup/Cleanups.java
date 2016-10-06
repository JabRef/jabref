package net.sf.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.formatter.Formatters;
import net.sf.jabref.logic.formatter.IdentityFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.OrdinalsToSuperscriptFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.model.cleanup.FieldFormatterCleanup;
import net.sf.jabref.model.cleanup.FieldFormatterCleanups;
import net.sf.jabref.model.cleanup.Formatter;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.strings.StringUtil;

public class Cleanups {

    public static final FieldFormatterCleanups DEFAULT_SAVE_ACTIONS;
    public static List<Formatter> availableFormatters;


    static {
        availableFormatters = new ArrayList<>();
        availableFormatters.addAll(Formatters.ALL);

        List<FieldFormatterCleanup> defaultFormatters = new ArrayList<>();
        defaultFormatters.add(new FieldFormatterCleanup(FieldName.PAGES, new NormalizePagesFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup(FieldName.DATE, new NormalizeDateFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup(FieldName.MONTH, new NormalizeMonthFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup(FieldName.TITLE, new ProtectTermsFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup(FieldName.TITLE, new UnitsToLatexFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup(FieldName.TITLE, new LatexCleanupFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup(FieldName.TITLE, new HtmlToLatexFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup(FieldName.ABSTRACT_ALL_TEXT_FIELDS_FIELD, new OrdinalsToSuperscriptFormatter()));
        DEFAULT_SAVE_ACTIONS = new FieldFormatterCleanups(false, defaultFormatters);
    }

    public static List<Formatter> getAvailableFormatters() {
        return Collections.unmodifiableList(availableFormatters);
    }

    public static List<FieldFormatterCleanup> parse(String formatterString) {

        if ((formatterString == null) || formatterString.isEmpty()) {
            // no save actions defined in the meta data
            return new ArrayList<>();
        }

        List<FieldFormatterCleanup> actions = new ArrayList<>();

        //read concrete actions
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

                //read each formatter
                int tokenIndex = remainingString.indexOf(',');
                do {
                    boolean doBreak = false;
                    if ((tokenIndex == -1) || (tokenIndex > endIndex)) {
                        tokenIndex = remainingString.indexOf(']');
                        doBreak = true;
                    }

                    String formatterKey = remainingString.substring(currentIndex + 1, tokenIndex);
                    actions.add(new FieldFormatterCleanup(fieldKey, getFormatterFromString(formatterKey)));

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
        for (Formatter formatter : availableFormatters) {
            if (formatterName.equals(formatter.getKey())) {
                return formatter;
            }
        }
        return new IdentityFormatter();
    }
}
