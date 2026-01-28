package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class FieldFormatterCleanupMapper {
    private static final Pattern FIELD_FORMATTER_CLEANUP_PATTERN = Pattern.compile("([^\\[]+)\\[([^]]+)]");

    private FieldFormatterCleanupMapper() {
    }

    /// This parses the key/list map of fields and clean up actions for the field.
    ///
    /// General format for one key/list map: `...[...]` - `field[formatter1,formatter2,...]`
    /// Multiple are written as `...[...]...[...]...[...]`
    /// `field1[formatter1,formatter2,...]field2[formatter3,formatter4,...]`
    ///
    /// The idea is that characters are field names until `[` is reached and that formatter lists are terminated by `]`
    ///
    /// Example: `pages[normalize_page_numbers]title[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]`
    public static List<FieldFormatterCleanup> parseActions(String formatterString) {
        if ((formatterString == null) || formatterString.isEmpty()) {
            // no save actions defined in the metadata
            return List.of();
        }

        List<FieldFormatterCleanup> result = new ArrayList<>();

        // first remove all newlines for easier parsing
        String formatterStringWithoutLineBreaks = StringUtil.unifyLineBreaks(formatterString, "");

        Matcher matcher = FIELD_FORMATTER_CLEANUP_PATTERN.matcher(formatterStringWithoutLineBreaks);
        while (matcher.find()) {
            String fieldKey = matcher.group(1);
            Field field = FieldFactory.parseField(fieldKey);

            String fieldString = matcher.group(2);

            List<FieldFormatterCleanup> fieldFormatterCleanups = Arrays
                    .stream(fieldString.split(","))
                    .map(FieldFormatterCleanupActions::getFormatterFromString)
                    .map(formatter -> new FieldFormatterCleanup(field, formatter))
                    .toList();
            result.addAll(fieldFormatterCleanups);
        }
        return result;
    }

    public static String serializeActions(List<FieldFormatterCleanup> actionList, String newLineSeparator) {
        // First, group all formatters by the field for which they apply
        // Order of the list should be kept
        Map<Field, Set<String>> groupedByField = new LinkedHashMap<>();
        for (FieldFormatterCleanup cleanup : actionList) {
            // add the formatter to map if not already in there, order sensitive
            groupedByField.computeIfAbsent(cleanup.getField(), _ -> new LinkedHashSet<>())
                          .add(cleanup.getFormatter().getKey());
        }

        // convert the contents of the hashmap into the correct serialization
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Field, Set<String>> entry : groupedByField.entrySet()) {
            StringJoiner joiner = new StringJoiner(",", "[", "]" + newLineSeparator);
            entry.getValue().forEach(joiner::add);
            result.append(entry.getKey().getName()).append(joiner);
        }
        return result.toString();
    }
}
