package org.jabref.logic.icore;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConferenceAcronymExtractor {
    // Regex that'll extract the string within the first deepest set of parentheses
    // A slight modification of: https://stackoverflow.com/a/17759264
    private static final Pattern PATTERN = Pattern.compile("\\(([^()]*)\\)");

    public static Optional<String> extract(String input) {
        Matcher matcher = PATTERN.matcher(input);

        if (matcher.find()) {
            String match = matcher.group(1).strip();
            if (!match.isEmpty()) {
                return Optional.of(match);
            }
        }

        return Optional.empty();
    }
}
