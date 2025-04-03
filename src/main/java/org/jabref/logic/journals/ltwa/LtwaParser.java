package org.jabref.logic.journals.ltwa;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parser for the LTWA (List of Title Word Abbreviations) CSV file.
 * Converts CSV data into a list of LtwaEntry objects.
 */
public class LtwaParser {
    private static final Pattern ANNOTATION = Pattern.compile("\\s*\\(.*?\\)");
    private static final Pattern LINE_FORMAT = Pattern.compile("\"\\s*(.*?)\\s*\";\"\\s*(.*?)\\s*\";\"\\s*(.*?)\\s*\"");
    private static final Set<String> NA = Set.of("n.a.", "n. a.", "n.a");
    private final Path file;

    public LtwaParser(Path file) {
        this.file = file;
    }

    /**
     * Parse LTWA entries from the given file.
     *
     * @return List of LtwaEntry objects
     * @throws IOException If an I/O error occurs
     */
    public List<LtwaEntry> parse() throws IOException {
        List<LtwaEntry> entries = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            boolean isFirstRow = true;
            String line;
            while ((line = reader.readLine()) != null) {
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }

                var matcher = LINE_FORMAT.matcher(line);
                if (!matcher.find()) {
                    continue;
                }

                var word = matcher.group(1);
                var abbreviationStr = matcher.group(2);
                var languageStr = matcher.group(3);

                word = NormalizeUtils.normalize(ANNOTATION.matcher(word).replaceAll("").strip());
                var abbreviationPresent = NA.contains(word);
                var abbreviation = abbreviationPresent ? abbreviationStr : null;
                List<String> languages = Arrays.stream(languageStr.split("\\s+")).map(String::trim)
                        .filter(s -> !s.isEmpty()).toList();

                entries.add(new LtwaEntry(word, abbreviation, languages));
            }
        }

        return entries;
    }
}
