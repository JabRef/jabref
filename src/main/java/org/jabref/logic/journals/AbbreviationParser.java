package org.jabref.logic.journals;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Reads abbreviation files (CSV format) into a list of Abbreviations.
 */
public class AbbreviationParser {
    private static final Character[] DELIMITERS = {';', ','};
    private static final char NO_DELIMITER = '\0'; // empty char

    // Ensures ordering while preventing duplicates
    private final LinkedHashSet<Abbreviation> abbreviations = new LinkedHashSet<>();

    /*
     * Read the given file, which should contain a list of journal names and their abbreviations. Each line should be
     * formatted as: "Full Journal Name,Abbr. Journal Name[,Shortest Unique Abbreviation]"
     * Tries to detect the delimiter, if comma or semicolon is used to ensure backwards compatibility
     *
     * @param file Path the given file
     */
    void readJournalListFromFile(Path file) throws IOException {
        char delimiter = detectDelimiter(file);

        try (CSVParser csvParser = new CSVParser(Files.newBufferedReader(file, StandardCharsets.UTF_8), AbbreviationFormat.getCSVFormatWithDelimiter(delimiter))) {
            for (CSVRecord csvRecord : csvParser) {
                String name = csvRecord.size() > 0 ? csvRecord.get(0) : "";
                String abbreviation = csvRecord.size() > 1 ? csvRecord.get(1) : "";
                String shortestUniqueAbbreviation = csvRecord.size() > 2 ? csvRecord.get(2) : "";

                // Check name and abbreviation
                if (name.isEmpty() || abbreviation.isEmpty()) {
                    return;
                }

                Abbreviation abbreviationToAdd = new Abbreviation(name, abbreviation, shortestUniqueAbbreviation);
                abbreviations.add(abbreviationToAdd);
            }
        }
    }

    private char detectDelimiter(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = reader.readLine();

            if (line == null) {
                return NO_DELIMITER;
            }
            return Arrays.stream(DELIMITERS)
                         .filter(s -> line.contains(s.toString()))
                         .findFirst()
                         .orElse(NO_DELIMITER);
        }
    }

    public Collection<Abbreviation> getAbbreviations() {
        return abbreviations;
    }
}
