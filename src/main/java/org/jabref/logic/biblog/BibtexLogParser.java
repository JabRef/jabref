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

/**
 * Parses the contents of a .blg (BibTeX log) file to extract warning messages.
 */
public class BibtexLogParser {
    public List<BibWarning> parseBiblog(Path blgFilePath) throws IOException {
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
        Pattern compile = Pattern.compile("^Warning--([a-zA-Z ]+) in ([^\\s]+)$");
        Matcher matcher = compile.matcher(line);
        if (matcher.find()) {
            String message = matcher.group(1).trim();
            String entryKey = matcher.group(2);
            String fieldName = null;
            if (message.startsWith("empty")) {
                fieldName = message.substring("empty".length()).trim();
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
