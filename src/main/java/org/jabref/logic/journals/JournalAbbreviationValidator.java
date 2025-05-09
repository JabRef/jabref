package org.jabref.logic.journals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JournalAbbreviationValidator {

    public static class ValidationResult {
        private final boolean isValid;
        private final String message;
        private final ValidationType type;
        private final String fullName;
        private final String abbreviation;
        private final int lineNumber;
        private final String suggestion;

        public ValidationResult(boolean isValid, String message, ValidationType type,
                              String fullName, String abbreviation, int lineNumber) {
            this(isValid, message, type, fullName, abbreviation, lineNumber, "");
        }

        public ValidationResult(boolean isValid, String message, ValidationType type,
                              String fullName, String abbreviation, int lineNumber, String suggestion) {
            this.isValid = isValid;
            this.message = message;
            this.type = type;
            this.fullName = fullName;
            this.abbreviation = abbreviation;
            this.lineNumber = lineNumber;
            this.suggestion = suggestion;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getMessage() {
            return message;
        }

        public ValidationType getType() {
            return type;
        }

        public String getFullName() {
            return fullName;
        }

        public String getAbbreviation() {
            return abbreviation;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getSuggestion() {
            return suggestion;
        }

        @Override
        public String toString() {
            return type + " at line " + lineNumber + ": " + message +
                   (suggestion.isEmpty() ? "" : " Suggestion: " + suggestion) +
                   " [" + fullName + " -> " + abbreviation + "]";
        }
    }

    public enum ValidationType {
        ERROR,
        WARNING
    }

    // Updated pattern to include more valid escape sequences
    private static final Pattern INVALID_ESCAPE_PATTERN = Pattern.compile("(?<!\\\\)\\\\(?![\\\\\"ntrbfu])|\\\\$");

    private final List<ValidationResult> issues = new ArrayList<>();
    private final Map<String, List<String>> fullNameToAbbrev = new HashMap<>();
    private final Map<String, List<String>> abbrevToFullName = new HashMap<>();

    /**
     * Checks if the journal name or abbreviation contains wrong escape characters
     */
    public ValidationResult checkWrongEscape(String fullName, String abbreviation, int lineNumber) {
        List<ValidationResult> escapeIssues = new ArrayList<>();

        // Check full name
        Matcher fullNameMatcher = INVALID_ESCAPE_PATTERN.matcher(fullName);
        if (fullNameMatcher.find()) {
            escapeIssues.add(new ValidationResult(false,
                String.format("Invalid escape sequence in full name at position %d", fullNameMatcher.start()),
                ValidationType.ERROR,
                fullName,
                abbreviation,
                lineNumber,
                "Use valid escape sequences: \\\\, \\\", \\n, \\t, \\r, \\b, \\f, \\uXXXX"));
        }

        // Check abbreviation
        Matcher abbrevMatcher = INVALID_ESCAPE_PATTERN.matcher(abbreviation);
        if (abbrevMatcher.find()) {
            escapeIssues.add(new ValidationResult(false,
                String.format("Invalid escape sequence in abbreviation at position %d", abbrevMatcher.start()),
                ValidationType.ERROR,
                fullName,
                abbreviation,
                lineNumber,
                "Use valid escape sequences: \\\\, \\\", \\n, \\t, \\r, \\b, \\f, \\uXXXX"));
        }

        return escapeIssues.isEmpty() ?
            new ValidationResult(true, "", ValidationType.ERROR, fullName, abbreviation, lineNumber) :
            escapeIssues.get(0);
    }

    /**
     * Checks if the journal name or abbreviation contains non-UTF8 characters
     */
    public ValidationResult checkNonUtf8(String fullName, String abbreviation, int lineNumber) {
        if (!isValidUtf8(fullName) || !isValidUtf8(abbreviation)) {
            return new ValidationResult(false,
                    "Journal name or abbreviation contains invalid UTF-8 sequences",
                    ValidationType.ERROR,
                    fullName,
                    abbreviation,
                    lineNumber,
                    "Ensure all characters are valid UTF-8. Remove or replace any invalid characters.");
        }
        return new ValidationResult(true, "", ValidationType.ERROR, fullName, abbreviation, lineNumber);
    }

    private boolean isValidUtf8(String str) {
        try {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            return new String(bytes, StandardCharsets.UTF_8).equals(str);
        } catch (Exception e) {
            return false;
        }
    }
}
