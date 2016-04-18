package net.sf.jabref.exporter;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.formatter.CaseChangers;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.formatter.IdentityFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.OrdinalsToSuperscriptFormatter;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;

import java.util.*;

public class FieldFormatterCleanups {

    private final List<FieldFormatterCleanup> actions;

    private static List<Formatter> availableFormatters;

    private boolean enabled;

    public static final FieldFormatterCleanups DEFAULT_SAVE_ACTIONS;

    static {
        availableFormatters = new ArrayList<>();
        availableFormatters.addAll(BibtexFieldFormatters.ALL);
        availableFormatters.addAll(CaseChangers.ALL);

        List<FieldFormatterCleanup> defaultFormatters = new ArrayList<>();
        defaultFormatters.add(new FieldFormatterCleanup("pages", new NormalizePagesFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup("month", new NormalizeMonthFormatter()));
        defaultFormatters.add(new FieldFormatterCleanup("booktitle", new OrdinalsToSuperscriptFormatter()));
        DEFAULT_SAVE_ACTIONS = new FieldFormatterCleanups(false, defaultFormatters);
    }

    public FieldFormatterCleanups(boolean enabled, String formatterString) {
        this(enabled, parse(formatterString));
    }

    public FieldFormatterCleanups(boolean enabled, List<FieldFormatterCleanup> actions) {
        this.enabled = enabled;
        this.actions = Objects.requireNonNull(actions);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<FieldFormatterCleanup> getConfiguredActions() {
        return Collections.unmodifiableList(actions);
    }

    public List<Formatter> getAvailableFormatters() {
        return Collections.unmodifiableList(availableFormatters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        FieldFormatterCleanups that = (FieldFormatterCleanups) o;

        if (enabled != that.enabled) {
            return false;
        }
        return actions.equals(that.actions);

    }

    @Override
    public int hashCode() {
        return Objects.hash(actions, enabled);
    }

    private static List<FieldFormatterCleanup> parse(String formatterString) {

        if ((formatterString == null) || formatterString.isEmpty()) {
            // no save actions defined in the meta data
            return new ArrayList<>();
        }

        List<FieldFormatterCleanup> actions = new ArrayList<>();

        //read concrete actions
        int startIndex = 0;

        // first remove all newlines for easier parsing
        String remainingString = formatterString;

        remainingString = StringUtil.unifyLineBreaksToConfiguredLineBreaks(remainingString).replaceAll(Globals.NEWLINE, "");
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

    public List<FieldChange> applySaveActions(BibEntry entry) {
        if (enabled) {
            return applyAllActions(entry);
        } else {
            return new ArrayList<>();
        }
    }

    private List<FieldChange> applyAllActions(BibEntry entry) {
        List<FieldChange> result = new ArrayList<>();

        for (FieldFormatterCleanup action : actions) {
            result.addAll(action.cleanup(entry));
        }

        return result;
    }

    private static Formatter getFormatterFromString(String formatterName) {
        for (Formatter formatter : availableFormatters) {
            if (formatterName.equals(formatter.getKey())) {
                return formatter;
            }
        }
        return new IdentityFormatter();
    }

    private static String getMetaDataString(List<FieldFormatterCleanup> actionList) {
        //first, group all formatters by the field for which they apply
        Map<String, List<String>> groupedByField = new HashMap<>();
        for (FieldFormatterCleanup cleanup : actionList) {
            String key = cleanup.getField();

            // add new list into the hashmap if needed
            if (!groupedByField.containsKey(key)) {
                groupedByField.put(key, new ArrayList<>());
            }

            //add the formatter to the map if it is not already there
            List<String> formattersForKey = groupedByField.get(key);
            if (!formattersForKey.contains(cleanup.getFormatter().getKey())) {
                formattersForKey.add(cleanup.getFormatter().getKey());
            }
        }

        // convert the contents of the hashmap into the correct serialization
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : groupedByField.entrySet()) {
            result.append(entry.getKey());

            StringJoiner joiner = new StringJoiner(",", "[", "]" + Globals.NEWLINE);
            entry.getValue().forEach(joiner::add);
            result.append(joiner.toString());
        }

        return result.toString();
    }

    public List<String> convertToString() {
        List<String> stringRepresentation = new ArrayList<>();

        if (enabled) {
            stringRepresentation.add("enabled");
        } else {
            stringRepresentation.add("disabled");
        }

        String formatterString = FieldFormatterCleanups.getMetaDataString(actions);
        stringRepresentation.add(formatterString);
        return stringRepresentation;
    }

    public static FieldFormatterCleanups parseFromString(List<String> formatterMetaList) {

        if (formatterMetaList != null && formatterMetaList.size() >= 2) {
            boolean enablementStatus = "enabled".equals(formatterMetaList.get(0));
            String formatterString = formatterMetaList.get(1);
            return new FieldFormatterCleanups(enablementStatus, formatterString);
        } else {
            // return default actions
            return FieldFormatterCleanups.DEFAULT_SAVE_ACTIONS;
        }

    }
}
