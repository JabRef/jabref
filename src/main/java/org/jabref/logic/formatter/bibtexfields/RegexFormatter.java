package org.jabref.logic.formatter.bibtexfields;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegexFormatter extends Formatter {
    public static final String KEY = "regex";
    private static final Logger LOGGER = LoggerFactory.getLogger(RegexFormatter.class);
    private static final Pattern PATTERN_ESCAPED_OPENING_CURLY_BRACE = Pattern.compile("\\\\\\{");
    private static final Pattern PATTERN_ESCAPED_CLOSING_CURLY_BRACE = Pattern.compile("\\\\\\}");
    // RegEx to match {...}
    // \\ is required to have the { interpreted as character
    // ? is required to disable the aggressive match
    private static final Pattern PATTERN_ENCLOSED_IN_CURLY_BRACES = Pattern.compile("(\\{.*?})");
    private static final String REGEX_CAPTURING_GROUP = "regex";
    private static final String REPLACEMENT_CAPTURING_GROUP = "replacement";
    /**
     * Matches a valid argument to the constructor. Two capturing groups are used to parse the {@link
     * RegexFormatter#regex} and {@link RegexFormatter#replacement} used in {@link RegexFormatter#format(String)}
     */
    private static final Pattern CONSTRUCTOR_ARGUMENT = Pattern.compile(
            "^\\(\"(?<" + REGEX_CAPTURING_GROUP + ">.*?)\" *?, *?\"(?<" + REPLACEMENT_CAPTURING_GROUP + ">.*?)\"\\)$");
    // Magic arbitrary unicode char, which will never appear in bibtex files
    private static final String PLACEHOLDER_FOR_PROTECTED_GROUP = Character.toString('\u0A14');
    private static final String PLACEHOLDER_FOR_OPENING_CURLY_BRACE = Character.toString('\u0A15');
    private static final String PLACEHOLDER_FOR_CLOSING_CURLY_BRACE = Character.toString('\u0A16');
    private final String regex;
    private final String replacement;

    /**
     * Constructs a new regular expression-based formatter with the given RegEx.
     *
     * @param input the regular expressions for matching and replacing given in the form {@code ("<regex>",
     *              "<replace>")}.
     */
    public RegexFormatter(String input) {
        Objects.requireNonNull(input);
        input = input.trim();
        Matcher constructorArgument = CONSTRUCTOR_ARGUMENT.matcher(input);
        if (constructorArgument.matches()) {
            regex = constructorArgument.group(REGEX_CAPTURING_GROUP);
            replacement = constructorArgument.group(REPLACEMENT_CAPTURING_GROUP);
        } else {
            regex = null;
            replacement = null;
            LOGGER.warn("RegexFormatter could not parse the input: " + input);
        }
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
        try {
            workingString = workingString.replaceAll(regex, replacement);
        } catch (PatternSyntaxException e) {
            LOGGER.warn("There is a syntax error in the regular expression, " +
                    regex + ", used by the regex modifier", e);
            return input;
        }

        for (String r : replaced) {
            workingString = workingString.replaceFirst(PLACEHOLDER_FOR_PROTECTED_GROUP, r);
        }
        return workingString;
    }

    @Override
    public String format(final String input) {
        Objects.requireNonNull(input);
        if (regex == null || replacement == null) {
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
