package org.jabref.model.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;

public class FieldFormatterCleanups {

    public static final String ENABLED = "enabled";
    public static final String DISABLED = "disabled";

    private final List<FieldFormatterCleanup> actions;

    private final boolean enabled;

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

    public List<String> getAsStringList(String newline) {
        List<String> stringRepresentation = new ArrayList<>();

        if (enabled) {
            stringRepresentation.add(ENABLED);
        } else {
            stringRepresentation.add(DISABLED);
        }

        String formatterString = getMetaDataString(actions, newline);
        stringRepresentation.add(formatterString);
        return stringRepresentation;
    }

    private static String getMetaDataString(List<FieldFormatterCleanup> actionList, String newline) {
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

            StringJoiner joiner = new StringJoiner(",", "[", "]" + newline);
            entry.getValue().forEach(joiner::add);
            result.append(joiner.toString());
        }

        return result.toString();
    }

}
