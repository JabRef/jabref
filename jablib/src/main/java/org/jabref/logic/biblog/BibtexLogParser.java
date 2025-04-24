package org.jabref.logic.biblog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
    private static final Pattern WARNING_PATTERN = Pattern.compile("^Warning--(?<message>[a-zA-Z ]+) in (?<entryKey>[^\\s]+)$");
    private static final String EMPTY_FIELD_PREFIX = "empty";

    public List<BibWarning> parseBiblog(@NonNull Path blgFilePath) throws IOException {
        List<BibWarning> warnings = new ArrayList<>();
        List<String> lines = Files.readAllLines(blgFilePath);
        for (String line : lines) {
            Optional<BibWarning> potentialWarning = parseWarningLine(line);
            potentialWarning.ifPresent(warnings::add);
        }
        return warnings;
    }

    /**
     * Parses a single line from the .blg file to identify a warning.
     * <p>
     * Currently supports parsing warnings of the format:
     * <pre>
     * Warning--[message] in [entryKey]
     * </pre>
     * For example: {@code Warning--empty journal in Scholey_2013}
     *
     * @param line a single line from the .blg file
     * @return an Optional containing a {@link BibWarning} if a match is found, or empty otherwise
     */
    private Optional<BibWarning> parseWarningLine(String line) {
        // TODO: Support additional warning formats
        Matcher matcher = WARNING_PATTERN.matcher(line);
        if (!matcher.find()) {
            return Optional.empty();
        }

        String message = matcher.group("message").trim();
        String entryKey = matcher.group("entryKey");
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
}
