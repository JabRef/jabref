package org.jabref.logic.biblog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.biblog.BibWarning;
import org.jabref.model.biblog.SeverityType;
import org.jabref.model.entry.field.FieldFactory;

import org.jspecify.annotations.NonNull;

/**
 * Parses the contents of a .blg (BibTeX log) file to extract warning messages.
 */
public class BibtexLogParser {
    private static final Pattern BIBTEX_WARNING_PATTERN = Pattern.compile("^Warning--(?<message>[a-zA-Z ]+) in (?<entryKey>[^\\s]+)$");
    private static final Pattern BIBLATEX_WARNING_PATTERN = Pattern.compile(
            "(?:(?:\\[\\d+\\] )?Biber\\.pm:\\d+> )?WARN - Datamodel: [a-z]+ entry '(?<entryKey>[^']+)' \\((?<fileName>[^)]+)\\): (?<message>.+)");

    private static final String EMPTY_FIELD_PREFIX = "empty";
    private static final String INVALID_FIELD_PREFIX = "field '";
    private static final String MULTI_INVALID_FIELD_PREFIX = "field - one of '";

    public List<BibWarning> parseBiblog(@NonNull Path blgFilePath) throws IOException {
        return Files.lines(blgFilePath)
                    .map(this::parseWarningLine)
                    .flatMap(Optional::stream)
                    .toList();
    }

    /// Parses a single line from a .blg file to identify a warning.
    ///
    /// This method supports two warning formats:
    ///
    /// 1.  **BibTeX Warnings:** Simple warnings from the legacy BibTeX backend.
    ///     `Warning--[message] in [entryKey]`
    ///     For example: `Warning--empty journal in Scholey_2013`
    ///
    /// 2.  **BibLaTeX Datamodel Warnings:** Detailed warnings from the Biber backend, including datamodel validation issues.
    ///     `[Log line] > WARN - Datamodel: [entry type] entry '[entryKey]' ([fileName]): [message]`
    ///     For example: `Biber.pm:123> WARN - Datamodel: article entry 'Scholey_2013' (file.bib): Invalid field 'journal'`
    ///
    /// @param line The single line from the .blg file to parse.
    /// @returns An `Optional` containing a `BibWarning` if a match is found, or an empty `Optional` otherwise.
    Optional<BibWarning> parseWarningLine(String line) {
        Matcher bibtexMatcher = BIBTEX_WARNING_PATTERN.matcher(line);
        if (bibtexMatcher.find()) {
            String message = bibtexMatcher.group("message").trim();
            String entryKey = bibtexMatcher.group("entryKey");
            // Extract field name for warnings related to empty fields  (e.g., "empty journal" -> fieldName = "journal")
            String fieldName = null;
            if (message.startsWith(EMPTY_FIELD_PREFIX)) {
                fieldName = message.substring(EMPTY_FIELD_PREFIX.length()).trim();
                fieldName = FieldFactory.parseField(fieldName).getName();
            }

            return Optional.of(new BibWarning(
                    SeverityType.WARNING,
                    message,
                    fieldName,
                    entryKey
            ));
        }

        Matcher biblatexMatcher = BIBLATEX_WARNING_PATTERN.matcher(line);
        if (biblatexMatcher.find()) {
            String message = biblatexMatcher.group("message").trim();
            String entryKey = biblatexMatcher.group("entryKey");
            String fieldName = null;

            // Extract field name for warnings related to invalid fields (e.g., "Invalid field 'publisher' for entrytype 'article'" -> fieldName = "publisher")
            String lowerCaseMessage = message.toLowerCase();
            if (lowerCaseMessage.contains(INVALID_FIELD_PREFIX)) {
                int startIndex = lowerCaseMessage.indexOf(INVALID_FIELD_PREFIX) + INVALID_FIELD_PREFIX.length();
                int endIndex = lowerCaseMessage.indexOf('\'', startIndex);
                if (endIndex != -1) {
                    fieldName = lowerCaseMessage.substring(startIndex, endIndex).trim();
                    fieldName = FieldFactory.parseField(fieldName).getName();
                }
            } else if (lowerCaseMessage.contains(MULTI_INVALID_FIELD_PREFIX)) {
                int startIndex = lowerCaseMessage.indexOf(MULTI_INVALID_FIELD_PREFIX) + MULTI_INVALID_FIELD_PREFIX.length();
                int endIndex = lowerCaseMessage.indexOf('\'', startIndex);
                if (endIndex != -1) {
                    fieldName = lowerCaseMessage.substring(startIndex, endIndex).trim().split(",")[0].trim();
                    fieldName = FieldFactory.parseField(fieldName).getName();
                }
            }

            return Optional.of(new BibWarning(
                    SeverityType.WARNING,
                    message,
                    fieldName,
                    entryKey
            ));
        }

        return Optional.empty();
    }
}
