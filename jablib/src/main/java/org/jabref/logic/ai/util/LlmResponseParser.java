package org.jabref.logic.ai.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LlmResponseParser {
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^\\s*\\d+\\.[ \\t]*(.+)$", Pattern.MULTILINE);
    private static final Pattern BULLET_LINE_PATTERN = Pattern.compile("^[-*•]\\s*");
    private static final String QUOTE_REMOVAL_PATTERN = "^[\"']|[\"']$";

    private LlmResponseParser() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /// Extracts a numbered or bulleted list from an LLM response.
    ///
    /// @param response the raw LLM response
    /// @return a list of extracted non-blank items; empty list if no valid items found
    public static List<String> extractNumberedList(String response) {
        List<String> items = new ArrayList<>();

        // Try numbered format first (multiline regex)
        Matcher matcher = NUMBERED_PATTERN.matcher(response);

        while (matcher.find()) {
            String item = matcher.group(1).trim();
            item = item.replaceAll(QUOTE_REMOVAL_PATTERN, "");

            if (isValidItem(item)) {
                items.add(item);
            }
        }

        // If numbered format didn't match, try group-based line parsing
        if (items.isEmpty()) {
            items.addAll(extractByGroups(response));
        }

        return items;
    }

    private static List<String> extractByGroups(String response) {
        // Split into groups of consecutive non-blank lines
        List<List<String>> groups = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String line : response.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (!current.isEmpty()) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
            } else {
                current.add(trimmed);
            }
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }

        boolean anyBullets = groups.stream()
                                   .anyMatch(g -> g.stream().anyMatch(l -> BULLET_LINE_PATTERN.matcher(l).find()));

        List<String> result = new ArrayList<>();
        for (List<String> group : groups) {
            boolean groupHasBullets = group.stream().anyMatch(l -> BULLET_LINE_PATTERN.matcher(l).find());
            if (anyBullets && !groupHasBullets) {
                continue;
            }
            for (String line : group) {
                String item = BULLET_LINE_PATTERN.matcher(line).replaceFirst("")
                                                 .replaceAll("^\\d+\\.\\s*", "")
                                                 .replaceAll(QUOTE_REMOVAL_PATTERN, "");
                if (isValidItem(item)) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    private static boolean isValidItem(String item) {
        return item != null && !item.isBlank();
    }
}
