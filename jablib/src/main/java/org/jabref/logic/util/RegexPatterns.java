package org.jabref.logic.util;

import java.util.regex.Pattern;

/**
 * A collection of pre-compiled regex patterns which are used repeatedly across multiple classes.
 */
public final class RegexPatterns {
    /**
     * Matches years in range 16XX to 20XX as standalone words.
     * <p>
     * Examples of matches:
     * <ul>
     *   <li>"1699" matches</li>
     *   <li>"2024" matches</li>
     *   <li>"1599" doesn't match (too early)</li>
     *   <li>"2100" doesn't (too late)</li>
     * </ul>
     *
     * Pattern: {@code \\b(1[6-9]|20)\\d{2}\\b}
     */
    public static final Pattern YEAR_PATTERN = Pattern.compile("\\b(1[6-9]|20)\\d{2}\\b");

    /**
     * Matches month names (full or abbreviated) case-insensitively.
     * <p>
     * Matches: january, feb, MARCH, Apr, etc.
     * <br>
     * Pattern: {@code \\b(january|february|...|jan|feb|...)\\b}
     */
    public static final Pattern MONTHS_PATTERN = Pattern
            .compile("\\b(january|february|march|april|may|june|july|august|september|october|november|december|" +
                    "jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Matches page ranges of the form {@code <number>-<number>} (like 45-67) as well as {@code <number>--<number>} (like 45--67).
     * <p>
     * Whitespace around the hyphen(s) is also matched
     * <br>
     * Pattern: {@code \\b\\d+\\s*--?\\s*\\d+\\b}
     */
    public static final Pattern PAGE_RANGE_PATTERN = Pattern.compile("\\b\\d+\\s*--?\\s*\\d+\\b");

    private RegexPatterns() {
        // Prevent instantiation
    }
}
