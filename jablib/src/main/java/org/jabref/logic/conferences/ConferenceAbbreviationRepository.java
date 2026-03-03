package org.jabref.logic.conferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class provides a repository for conference abbreviation data,
 * similar in structure and purpose to JournalAbbreviationRepository.
 *
 * Purpose:
 * The goal is to support abbreviation of conference proceedings titles,
 * typically stored in the BibTeX/BibLaTeX field "booktitle".
 *
 * Architectural Role:
 * - It is responsible ONLY for loading and providing abbreviation data.
 *
 * Data Source:
 * The repository loads abbreviations from a CSV file with the format:
 * Full Conference Name,Abbreviation
 */
public class ConferenceAbbreviationRepository {

    /**
     * Default classpath location of the CSV file.
     */
    private static final String DEFAULT_RESOURCE = "/conference-abbreviations.csv";

    private final Map<String, String> fullToAbbrev = new HashMap<>();
    private final Map<String, String> abbrevToFull = new HashMap<>();

    /// Creates an empty repository.
    public ConferenceAbbreviationRepository() {
    }

    /**
     * Constructor used to load abbreviation data from a CSV InputStream.
     */
    public ConferenceAbbreviationRepository(InputStream csvStream) throws IOException {
        load(csvStream);
    }

    /**
     * Factory method to load the repository from the default classpath resource.
     * This method:
     * 1. Locates the CSV file inside src/main/resources
     * 2. Opens it as an InputStream
     * 3. Delegates parsing to the constructor
     */
    public static ConferenceAbbreviationRepository loadFromClasspath() throws IOException {
        InputStream stream = ConferenceAbbreviationRepository.class.getResourceAsStream(DEFAULT_RESOURCE);

        // If the file cannot be found, fail it.
        if (stream == null) {
            throw new IOException("Could not find resource " + DEFAULT_RESOURCE + " on classpath");
        }

        return new ConferenceAbbreviationRepository(stream);
    }

    /**
     * Internal method responsible for parsing the CSV file.
     * Steps:
     * 1. Read file line-by-line.
     * 2. Ignore empty lines and comment lines.
     * 3. Split each line into exactly two columns (full name + abbreviation).
     * 4. Store mappings in both directions.
     */
    private void load(InputStream csvStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();

                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split(",", 2);
                if (parts.length != 2) {
                    continue;
                }

                String fullName = parts[0].trim();
                String abbreviation = parts[1].trim();
                if (fullName.isEmpty() || abbreviation.isEmpty()) {
                    continue;
                }

                fullToAbbrev.put(fullName, abbreviation);
                abbrevToFull.put(abbreviation, fullName);
            }
        }
    }

    /**
     * Returns the abbreviation for a given full conference name.
     * Example:
     * Input:  "International Conference on Software Engineering"
     * Output: "ICSE"
     */
    public Optional<String> getAbbreviation(String fullName) {
        return Optional.ofNullable(fullToAbbrev.get(fullName));
    }

    /**
     * Returns the full conference name for a given abbreviation.
     * Example:
     *Input:  "ICSE"
     *Output: "International Conference on Software Engineering"
     */
    public Optional<String> getFullName(String abbreviation) {
        return Optional.ofNullable(abbrevToFull.get(abbreviation));
    }

    /**
     * Toggle behavior similar to JournalAbbreviationRepository.
     *
     * If input is:
     * - a full conference name -> return abbreviation
     * - an abbreviation -> return full conference name
     * - unknown -> return empty Optional
     */
    public Optional<String> getNextAbbreviation(String input) {
        // Case 1: input is a full conference name
        String asAbbrev = fullToAbbrev.get(input);
        if (asAbbrev != null) {
            return Optional.of(asAbbrev);
        }

        // Case 2: input is an abbreviation
        String asFull = abbrevToFull.get(input);
        if (asFull != null) {
            return Optional.of(asFull);
        }

        // Case 3: no known mapping
        return Optional.empty();
    }
}
