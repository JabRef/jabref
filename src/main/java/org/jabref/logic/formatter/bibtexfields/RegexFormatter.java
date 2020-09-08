package org.jabref.logic.formatter.bibtexfields;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class RegexFormatter extends Formatter {

    public static final String KEY = "regex";
    private static final Pattern PATTERN_ESCAPED_OPENING_CURLY_BRACE = Pattern.compile("\\\\\\{");
    private static final Pattern PATTERN_ESCAPED_CLOSING_CURLY_BRACE = Pattern.compile("\\\\\\}");
    // RegEx to match {...}
    // \\ is required to have the { interpreted as character
    // ? is required to disable the aggressive match
    private static final Pattern PATTERN_ENCLOSED_IN_CURLY_BRACES = Pattern.compile("(\\{.*?})");
    // Magic arbitrary unicode char, which will never appear in bibtex files
    private static final String PLACEHOLDER_FOR_PROTECTED_GROUP = Character.toString('\u0A14');
    private static final String PLACEHOLDER_FOR_OPENING_CURLY_BRACE = Character.toString('\u0A15');
    private static final String PLACEHOLDER_FOR_CLOSING_CURLY_BRACE = Character.toString('\u0A16');
    private static final String QUOTE_AND_OPENING_BRACE = "\"(";
    private static final int LENGTH_OF_QUOTE_AND_OPENING_BRACE = QUOTE_AND_OPENING_BRACE.length();
    private static final String CLOSING_BRACE_AND_QUOTE = ")\"";
    private static final int LENGTH_OF_CLOSING_BRACE_AND_QUOTE = CLOSING_BRACE_AND_QUOTE.length();
    private static String regex;
    private String replacement;

    /**
     * Constructs a new regular expression-based formatter with the given RegEx.
     *
     * @param input the regular expressions for matching and replacing given in the form {@code (<regex>, <replace>)}.
     */
    public RegexFormatter(String input) {
        // formatting is like ("exp1","exp2"), we want to first remove (" and ")
        String rexToSet = input.substring(LENGTH_OF_QUOTE_AND_OPENING_BRACE, input.length() - LENGTH_OF_CLOSING_BRACE_AND_QUOTE);
        String[] parts = rexToSet.split("\",\"");
        regex = parts[0];
        replacement = parts[1];
    }

    @Override
    public String getName() {
        return Localization.lang("regular expression");
    }

    @Override
    public String getKey() {
        return KEY;
    }

    private String replaceHonoringProtectedGroups(final String input) {
        Matcher matcher = PATTERN_ENCLOSED_IN_CURLY_BRACES.matcher(input);

        List<String> replaced = new ArrayList<>();
        while (matcher.find()) {
            replaced.add(matcher.group(1));
        }
        String workingString = matcher.replaceAll(PLACEHOLDER_FOR_PROTECTED_GROUP);
        workingString = workingString.replaceAll(regex, replacement);

        for (String r : replaced) {
            workingString = workingString.replaceFirst(PLACEHOLDER_FOR_PROTECTED_GROUP, r);
        }
        return workingString;
    }

    @Override
    public String format(final String input) {
        Objects.requireNonNull(input);
        if (regex == null) {
            return input;
        }

        Matcher matcherOpeningCurlyBrace = PATTERN_ESCAPED_OPENING_CURLY_BRACE.matcher(input);
        final String openingCurlyBraceReplaced = matcherOpeningCurlyBrace.replaceAll(PLACEHOLDER_FOR_OPENING_CURLY_BRACE);

        Matcher matcherClosingCurlyBrace = PATTERN_ESCAPED_CLOSING_CURLY_BRACE.matcher(openingCurlyBraceReplaced);
        final String closingCurlyBraceReplaced = matcherClosingCurlyBrace.replaceAll(PLACEHOLDER_FOR_CLOSING_CURLY_BRACE);

        final String regexApplied = replaceHonoringProtectedGroups(closingCurlyBraceReplaced);

        return regexApplied
                .replaceAll(PLACEHOLDER_FOR_OPENING_CURLY_BRACE, "\\\\{")
                .replaceAll(PLACEHOLDER_FOR_CLOSING_CURLY_BRACE, "\\\\}");
    }

    @Override
    public String getDescription() {
        return Localization.lang("Add a regular expression for the key pattern.");
    }

    @Override
    public String getExampleInput() {
        return "Please replace the spaces";
    }
}
