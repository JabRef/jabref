package org.jabref.logic.icore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;

public final class ConferenceUtils {
    // Regex that'll extract the string within the first deepest set of parentheses
    // A slight modification of: https://stackoverflow.com/a/17759264
    private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(([^()]*)\\)");
    // Regex that will match all years of type 19XX or 20XX;
    private static final String YEAR_REGEX = "(19|20)\\d{2}";
    /*
        Regex that will match:
        - ordinals of form [number][st|nd|rd|th] as in 1st, 2nd, 3rd, 4th, and so on
        - ordinals in LaTeX syntax of [number]\textsuperscript{[st|nd|rd|th]} as in 3\textsuperscript{rd},
          17\textsuperscript{th}, etc. These are just the same ordinals as above but with the added LaTeX text syntax.
     */
    private static final String ORDINAL_REGEX = "\\d+(\\\\textsuperscript\\{)?(st|nd|rd|th)}?";
    private static final Pattern YEAR_OR_ORDINAL_PATTERN = Pattern.compile(YEAR_REGEX + "|" + ORDINAL_REGEX);
    // Stopwords must not be contained in the ICORE data
    private static final Set<String> TITLE_STOPWORDS = Set.of(
            "proceedings", "volume", "part", "papers",
            "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"
    );
    private static final int MAX_CANDIDATES_THRESHOLD = 50;
    private static final int DELIMITER_START = -1;

    private ConferenceUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Attempts to extract the string enclosed in the first deepest set of parentheses from the given input string.
     * <p>
     * This method uses a regular expression {@code \(([^()]*)\)} to find the innermost parenthesized substring.
     * Only the <strong>first match</strong> is considered; any additional matching substrings in the input are ignored.
     * </p>
     *
     * <p>
     * If a match is found, leading and trailing whitespace around the string is stripped. If the resulting string is not
     * empty, it is returned wrapped in an {@code Optional}. Otherwise, an empty {@code Optional} is returned.
     * </p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code "(SERA)"} -> {@code Optional.of("SERA")}</li>
     *   <li>{@code "Conference ( ABC )"} -> {@code Optional.of("ABC")}</li>
     *   <li>{@code "This (SERA) has multiple (CONF) acronyms"} -> {@code Optional.of("SERA")}</li>
     *   <li>{@code "Input with empty () parentheses"} -> {@code Optional.empty()}</li>
     *   <li>{@code "Input with empty (        ) whitespace in parens"} -> {@code Optional.empty()}</li>
     *   <li>{@code ""} -> {@code Optional.empty()}</li>
     * </ul>
     *
     * @param input the string to search, must not be {@code null}
     * @return an {@code Optional} containing the extracted and trimmed string from the first set of parentheses,
     * or {@code Optional.empty()} if no string is found
     */
    public static Optional<String> extractStringFromParentheses(@NonNull String input) {
        if (input.indexOf('(') < 0) {
            return Optional.empty();
        }

        Matcher matcher = PARENTHESES_PATTERN.matcher(input);

        if (matcher.find()) {
            String match = matcher.group(1).strip();
            if (!match.isEmpty()) {
                return Optional.of(match);
            }
        }

        return Optional.empty();
    }

    /**
     * Generates possible acronym candidates from the given input string by splitting on common delimiters and extracting
     * substrings within the specified cutoff length.
     * <p>
     * Candidates are ordered in a {@link TreeSet} such that longer strings are positioned before shorter ones, with
     * lexicographical ordering used to break ties. This prevents overfitting on composite acronyms during lookup (like
     * between {@code IEEE-IV} and {@code IV}) by pushing the shorter substrings to the end.
     * A maximum of 50 candidates are generated to avoid excessive expansion.
     * The splitting delimiters are {@code whitespace}, {@code ,}, {@code '}, {@code _}, {@code :}, {@code .}, and {@code -}.
     * Delimiters between acronyms are kept, if the cutoff length allows.
     * </p>
     * <p>
     * For example, given the input string {@code "IEEE-IV'2022"} and a cutoff of {@code 11}, this method generates the
     * following candidates in order: {@code "IEEE-IV", "IV'2022", "2022", "IEEE", "IV"}. Notice that {@code "IEEE-IV"}
     * is positioned ahead and retains the {@code -} in between.
     * </p>
     *
     * @param input  the raw string to extract acronym candidates from, must not be {@code null}
     * @param cutoff the maximum allowed length of each candidate substring; candidates longer than this are discarded
     * @return a set of acronym candidates ordered by descending length and then lexicographically,
     * or an empty set if no valid candidates are found
     */
    public static Set<String> generateAcronymCandidates(@NonNull String input, int cutoff) {
        if (input.isEmpty() || cutoff <= 0) {
            return Set.of();
        }

        List<Integer> bounds = new ArrayList<>();
        // Collect delimiter boundaries: -1 (start), every delimiter index, and input length (end).
        bounds.add(DELIMITER_START);
        for (int i = 0; i < input.length(); i++) {
            if (isAcronymDelimiter(input.charAt(i))) {
                bounds.add(i);
            }
        }
        bounds.add(input.length());

        // TreeSet to ensure ordering; with longer strings positioned ahead
        Set<String> candidates = new TreeSet<>((a, b) -> {
            int lengthCompare = Integer.compare(b.length(), a.length());
            return lengthCompare != 0 ? lengthCompare : a.compareTo(b);
        });
        // Process bounds and generate candidates
        for (int i = 0; i < bounds.size() - 1; i++) {
            for (int j = i + 1; j < bounds.size(); j++) {
                if (candidates.size() >= MAX_CANDIDATES_THRESHOLD) {
                    return candidates;
                }
                int start = bounds.get(i) + 1;
                int end = bounds.get(j);
                int len = end - start;
                if (len > 0 && len <= cutoff) {
                    String candidate = trimDelimiters(input.substring(start, end));
                    if (!candidate.isEmpty()) {
                        candidates.add(candidate);
                    }
                }
            }
        }

        return candidates;
    }

    private static String trimDelimiters(String candidate) {
        int left = 0;
        int right = candidate.length() - 1;

        // Move left pointer until we find a non-delimiter
        while (left <= right && isAcronymDelimiter(candidate.charAt(left))) {
            left++;
        }

        // Move right pointer until we find a non-delimiter
        while (right >= left && isAcronymDelimiter(candidate.charAt(right))) {
            right--;
        }

        return left <= right ? candidate.substring(left, right + 1) : "";
    }

    private static boolean isAcronymDelimiter(char c) {
        return Character.isWhitespace(c) ||
                c == '\'' || c == ',' || c == '_' ||
                c == ':' || c == '.' || c == '-';
    }

    /**
     * Normalizes a raw conference title query string into a simplified form suitable for fuzzy matching.
     * <p>
     * The normalization process performs the following steps:
     * </p>
     * <ol>
     *     <li>Removes all substrings enclosed in parentheses, e.g., {@code "proceedings (ICSE 2022)"} -> {@code "Proceedings"}.</li>
     *     <li>Removes all years of form {@code 19XX} or {@code 20xx} (e.g., {@code 1999}, {@code 2022}) and ordinals in
     *         regular form (e.g., {@code 1st}, {@code 2nd}, {@code 3rd}) as well as in LaTeX syntax (e.g.,
     *         {@code 3\textsuperscript{rd}} or {@code 17\textsuperscript{th}}).</li>
     *     <li>Splits the input into alphanumeric tokens, discarding stopwords found in the {@code TITLE_STOPWORDS} set
     *          (which includes months, or other common stopwords like {@code proceedings}, {@code papers}, etc.)</li>
     *     <li>Concatenates the remaining tokens into a normalized string without delimiters.</li>
     *     <li>Removes leading false-start tokens like {@code "ofthe"}, {@code "of"}, or {@code "the"}.</li>
     * </ol>
     * <p>
     * Note that the input is expected to already be lowercased before calling this method.
     * </p>
     * <p>
     * An example:
     * {@code "proceedings of the 3rd international conference on machine learning (icml 2018)"} ->
     * {@code "internationalconferenceonmachinelearning"}
     * </p>
     *
     * @param input the pre-lowercased raw string to normalize, must not be {@code null}
     * @return a normalized string representation of the input
     */
    public static String normalize(@NonNull String input) {
        StringBuilder normalized = new StringBuilder();
        StringBuilder currentToken = new StringBuilder();

        input = removeAllParenthesesWithContent(input);
        input = YEAR_OR_ORDINAL_PATTERN.matcher(input).replaceAll("");

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);

            if (Character.isLetterOrDigit(currentChar)) {
                currentToken.append(currentChar);
                continue;
            }

            normalizeTokenAndFlush(currentToken, normalized);
        }

        normalizeTokenAndFlush(currentToken, normalized);

        return normalized.toString()
                         .replaceFirst("^(ofthe|of|the)+", "");   // remove any false starts
    }

    private static void normalizeTokenAndFlush(StringBuilder currentToken, StringBuilder output) {
        if (currentToken.isEmpty()) {
            return;
        }

        String token = currentToken.toString();
        currentToken.setLength(0);

        if (TITLE_STOPWORDS.contains(token)) {
            return;
        }

        output.append(token);
    }

    public static String removeAllParenthesesWithContent(String input) {
        Matcher parenthesesMatcher = PARENTHESES_PATTERN.matcher(input);

        while (parenthesesMatcher.find()) {
            input = parenthesesMatcher.replaceAll("");
            parenthesesMatcher = PARENTHESES_PATTERN.matcher(input);
        }

        return input;
    }
}
