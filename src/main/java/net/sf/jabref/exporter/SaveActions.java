package net.sf.jabref.exporter;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.formatter.CaseChangers;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.formatter.IdentityFormatter;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;

import java.util.*;

public class SaveActions {

    private final List<FieldFormatterCleanup> actions;

    private List<Formatter> availableFormatters;

    public static final String META_KEY = "saveActions";

    private boolean enabled;

    public SaveActions(boolean enabled, String formatterString) {

        actions = new ArrayList<>();
        setAvailableFormatters();
        this.enabled = enabled;

        if (formatterString == null || "".equals(formatterString)) {
            // no save actions defined in the meta data
            return;
        } else {
            parseSaveActions(formatterString);
        }

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SaveActions that = (SaveActions) o;

        if (enabled != that.enabled) return false;
        return actions.equals(that.actions);

    }

    @Override
    public int hashCode() {
        int result = actions.hashCode();
        result = 31 * result + (enabled ? 1 : 0);
        return result;
    }

    private void parseSaveActions(String formatterString) {
        //read concrete actions
        int startIndex = 0;

        // first remove all newlines for easier parsing
        formatterString = StringUtil.unifyLineBreaksToConfiguredLineBreaks(formatterString).replaceAll(Globals.NEWLINE, "");

        String remainingString = formatterString;
        try {
            while (startIndex < formatterString.length()) {
                // read the field name
                int currentIndex = remainingString.indexOf("[");
                String fieldKey = remainingString.substring(0, currentIndex);
                int endIndex = remainingString.indexOf("]");
                startIndex += endIndex + 1;

                //read each formatter
                int tokenIndex = remainingString.indexOf(",");
                do {
                    boolean doBreak = false;
                    if (tokenIndex == -1 || tokenIndex > endIndex) {
                        tokenIndex = remainingString.indexOf("]");
                        doBreak = true;
                    }

                    String formatterKey = remainingString.substring(currentIndex + 1, tokenIndex);
                    actions.add(new FieldFormatterCleanup(fieldKey, getFormatterFromString(formatterKey)));

                    remainingString = remainingString.substring(tokenIndex + 1);
                    if (remainingString.startsWith("]") || doBreak) {
                        break;
                    }
                    tokenIndex = remainingString.indexOf(",");

                    currentIndex = -1;
                } while (true);


            }
        } catch (StringIndexOutOfBoundsException ignore) {
            // if this exception occurs, the remaining part of the save actions string is invalid.
            // Thus we stop parsing and take what we have parsed until now
            return;
        }
    }

    private void setAvailableFormatters() {
        availableFormatters = new ArrayList<>();

        availableFormatters.addAll(BibtexFieldFormatters.ALL);
        availableFormatters.addAll(CaseChangers.ALL);
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

    private Formatter getFormatterFromString(String formatterName) {
        for (Formatter formatter : availableFormatters) {
            if (formatterName.equals(formatter.getKey())) {
                return formatter;
            }
        }
        return new IdentityFormatter();
    }

    public static String getMetaDataString(List<FieldFormatterCleanup> actionList) {
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


}
