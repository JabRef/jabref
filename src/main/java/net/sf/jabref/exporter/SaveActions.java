package net.sf.jabref.exporter;

import net.sf.jabref.MetaData;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.formatter.CaseChangers;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.formatter.IdentityFormatter;
import net.sf.jabref.model.entry.BibEntry;

import java.util.*;

public class SaveActions {

    private final List<FieldFormatterCleanup> actions;

    private List<Formatter> availableFormatters;

    public static final String META_KEY = "saveActions";

    private boolean enabled;

    public SaveActions(MetaData metaData) {
        Objects.requireNonNull(metaData);

        actions = new ArrayList<>();
        setAvailableFormatters();

        List<String> formatters = metaData.getData(META_KEY);
        if (formatters == null) {
            // no save actions defined in the meta data
            return;
        } else {
            parseEnabledStatus(formatters.get(0));

            parseSaveActions(formatters.get(1));
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
        int index = 0;
        String remainingString = formatterString;
        try {
            while (startIndex < formatterString.length()) {
                // read the field name
                index = remainingString.indexOf("[");
                String fieldKey = remainingString.substring(0, index);
                int endIndex = remainingString.indexOf("]");
                startIndex += endIndex + 1;

                //read each formatter
                int commaIndex = remainingString.indexOf(",");
                do {
                    String formatterKey = remainingString.substring(index + 1, commaIndex);
                    actions.add(new FieldFormatterCleanup(fieldKey, getFormatterFromString(formatterKey)));

                    remainingString = remainingString.substring(commaIndex + 1);
                    if (remainingString.startsWith("]")) {
                        break;
                    }
                    commaIndex = remainingString.indexOf(",");
                    index = -1;
                } while (commaIndex != -1 && commaIndex < endIndex);

                if ("]".equals(remainingString)) {
                    return;
                } else {
                    remainingString = remainingString.substring(1, remainingString.length());
                }

            }
        } catch (StringIndexOutOfBoundsException ignore) {
            // if this exception occurs, the remaining part of the save actions string is invalid.
            // Thus we stop parsing and take what we have parsed until now
            return;
        }
    }

    private void parseEnabledStatus(String enablementString) {
        //read if save actions should be enabled
        enabled = "enabled".equals(enablementString);
    }

    private void setAvailableFormatters() {
        availableFormatters = new ArrayList<>();

        availableFormatters.addAll(BibtexFieldFormatters.ALL);
        availableFormatters.addAll(CaseChangers.ALL);
    }

    public BibEntry applySaveActions(BibEntry entry) {
        if (enabled) {
            applyAllActions(entry);
        }
        return entry;
    }

    private void applyAllActions(BibEntry entry) {
        for (FieldFormatterCleanup action : actions) {
            action.cleanup(entry);
        }
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
        HashMap<String, List<String>> groupedByField = new HashMap<>();
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
        for (String fieldKey : groupedByField.keySet()) {
            result.append(fieldKey);

            StringJoiner joiner = new StringJoiner(",","[","]");
            for (String formatterKey : groupedByField.get(fieldKey)) {
                joiner.add(formatterKey);
            }
            result.append(joiner.toString());
        }

        return result.toString();
    }


}
