package org.jabref.logic.icore;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;

public class ConferenceAcronymExtractor {
    // Regex that'll extract the string within the first deepest set of parentheses
    // A slight modification of: https://stackoverflow.com/a/17759264
    private static final Pattern PATTERN = Pattern.compile("\\(([^()]*)\\)");

    /**
     * Attempts to extract a conference acronym enclosed in the first deepest set of parentheses from the given input string.
     * <p>
     * This method uses a regular expression {@code \(([^()]*)\)} to find the innermost parenthesized substring.
     * Only the <strong>first match</strong> is considered; any additional matching substrings in the input are ignored.
     * </p>
     *
     * <p>
     * If a match is found, leading and trailing whitespace around the acronym is stripped. If the resulting string is not
     * empty, it is returned wrapped in an {@code Optional}. Otherwise, an empty {@code Optional} is returned.
     * </p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code "(SERA)"} → {@code Optional.of("SERA")}</li>
     *   <li>{@code "Conference ( ABC )"} → {@code Optional.of("ABC")}</li>
     *   <li>{@code "This (SERA) has multiple (CONF) acronyms"} → {@code Optional.of("SERA")}</li>
     *   <li>{@code "Input with empty () parentheses"} → {@code Optional.empty()}</li>
     *   <li>{@code "Input with empty (        ) whitespace in parens"} → {@code Optional.empty()}</li>
     *   <li>{@code ""} → {@code Optional.empty()}</li>
     * </ul>
     *
     * @param input the string to search, must not be {@code null}
     * @return an {@code Optional} containing the extracted and trimmed acronym string from the first set of parentheses,
     *         or {@code Optional.empty()} if no acronym is found
     */
    public static Optional<String> extract(@NonNull String input) {
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
