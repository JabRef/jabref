package org.jabref.logic.journals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

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

    private static final Set<Pair<String, String>> ALLOWED_MISMATCHES = Set.of(
            Pair.of("Polish Academy of Sciences", "Acta Phys. Polon."),
            Pair.of("Jagellonian University", "Acta Phys. Polon."),
            Pair.of("Universităţii din Timișoara", "An. Univ."),
            Pair.of("Universităţii \"Ovidius\" Constanţa", "An. Ştiinţ.")
    );

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

    /**
     * Checks if the abbreviation starts with the same letter as the full name
     */
    public ValidationResult checkStartingLetters(String fullName, String abbreviation, int lineNumber) {
        fullName = fullName.trim();
        abbreviation = abbreviation.trim();

        if (isAllowedException(fullName, abbreviation)) {
            return new ValidationResult(
                    true,
                    "Allowed abbreviation exception",
                    ValidationType.ERROR,
                    fullName,
                    abbreviation,
                    lineNumber
            );
        }

        String fullFirst = getFirstSignificantWord(fullName);
        String abbrFirst = getFirstSignificantWord(abbreviation);

        // Validate initials
        if (!abbrFirst.isEmpty() &&
                !fullFirst.isEmpty() &&
                !abbrFirst.toLowerCase().startsWith(fullFirst.substring(0, 1).toLowerCase())) {
            return new ValidationResult(
                    false,
                    "Abbreviation does not begin with same letter as full journal name",
                    ValidationType.ERROR,
                    fullName,
                    abbreviation,
                    lineNumber,
                    String.format("Should start with '%c' (from '%s')",
                            fullFirst.toLowerCase().charAt(0),
                            fullFirst)
            );
        }

        return new ValidationResult(
                true,
                "",
                ValidationType.ERROR,
                fullName,
                abbreviation,
                lineNumber
        );
    }

    private boolean isAllowedException(String fullName, String abbreviation) {
        return ALLOWED_MISMATCHES.stream()
                                 .anyMatch(pair ->
                                         fullName.equals(pair.getKey()) &&
                                                 abbreviation.startsWith(pair.getValue())
                                 );
    }

    private String getFirstSignificantWord(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "";
        }
        return s.trim().split("[\\s\\-–.,:;'\"]+")[0];
    }

    /**
     * Checks if the abbreviation is the same as the full text
     */
    public ValidationResult checkAbbreviationEqualsFullText(String fullName, String abbreviation, int lineNumber) {
        if (fullName.equalsIgnoreCase(abbreviation) && fullName.trim().split("\\s+").length > 1) {
            return new ValidationResult(false,
                    "Abbreviation is the same as the full text",
                    ValidationType.WARNING,
                    fullName,
                    abbreviation,
                    lineNumber,
                    "Consider using a shorter abbreviation to distinguish it from the full name");
        }

        return new ValidationResult(true, "", ValidationType.ERROR, fullName, abbreviation, lineNumber);
    }

    /**
     * Checks if the abbreviation uses outdated "Manage." instead of "Manag."
     */
    public ValidationResult checkOutdatedManagementAbbreviation(String fullName, String abbreviation, int lineNumber) {
        if (fullName.contains("Management") && abbreviation.contains("Manage.")) {
            return new ValidationResult(false,
                    "Management is abbreviated with outdated \"Manage.\" instead of \"Manag.\"",
                    ValidationType.WARNING,
                    fullName,
                    abbreviation,
                    lineNumber,
                    "Update to use the standard abbreviation \"Manag.\"");
        }
        return new ValidationResult(true, "", ValidationType.WARNING, fullName, abbreviation, lineNumber);
    }

    /**
     * Check for duplicate full names with different abbreviations
     */
    private void checkDuplicateFullNames() {
        for (Map.Entry<String, List<String>> entry : fullNameToAbbrev.entrySet()) {
            if (entry.getValue().size() > 1) {
                String fullName = entry.getKey();
                List<String> abbreviations = entry.getValue();

                issues.add(new ValidationResult(false,
                        String.format("Duplicate full name '%s' with different abbreviations: %s",
                                fullName, String.join(", ", abbreviations)),
                        ValidationType.WARNING,
                        fullName,
                        abbreviations.get(0),
                        -1,
                        "Consider consolidating abbreviations or using more specific full names"));
            }
        }
    }

    /**
     * Check for duplicate abbreviations with different full names
     */
    private void checkDuplicateAbbreviations() {
        for (Map.Entry<String, List<String>> entry : abbrevToFullName.entrySet()) {
            if (entry.getValue().size() > 1) {
                String abbreviation = entry.getKey();
                List<String> fullNames = entry.getValue();

                issues.add(new ValidationResult(false,
                        String.format("Duplicate abbreviation '%s' used for different journals: %s",
                                abbreviation, String.join("; ", fullNames)),
                        ValidationType.WARNING,
                        fullNames.get(0),
                        abbreviation,
                        -1,
                        "Consider using more specific abbreviations to avoid ambiguity"));
            }
        }
    }

    /**
     * Validates a journal entry against all rules
     */
    public List<ValidationResult> validate(String fullName, String abbreviation, int lineNumber) {
        List<ValidationResult> results = new ArrayList<>();

        // Error checks
        results.add(checkWrongEscape(fullName, abbreviation, lineNumber));
        results.add(checkNonUtf8(fullName, abbreviation, lineNumber));
        results.add(checkStartingLetters(fullName, abbreviation, lineNumber));

        // Warning checks
        results.add(checkAbbreviationEqualsFullText(fullName, abbreviation, lineNumber));
        results.add(checkOutdatedManagementAbbreviation(fullName, abbreviation, lineNumber));

        // Track for duplicate checks
        fullNameToAbbrev.computeIfAbsent(fullName, k -> new ArrayList<>()).add(abbreviation);
        abbrevToFullName.computeIfAbsent(abbreviation, k -> new ArrayList<>()).add(fullName);

        return results;
    }

    /**
     * Get all validation issues found
     */
    public List<ValidationResult> getIssues() {
        // Check for duplicates
        checkDuplicateFullNames();
        checkDuplicateAbbreviations();
        return issues;
    }
}
