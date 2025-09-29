package org.jabref.logic.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.JabRefException;
import org.jabref.logic.journals.ltwa.NormalizeUtils;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton utility class for detecting and extracting city and country names from given text.
 *
 * <p>
 * The location data is loaded from the resource file {@code /util/countries_cities1000.txt}
 * and is processed to optimize matching performance by tracking minimum and maximum location
 * name lengths.
 * </p>
 *
 * <p>
 * The data is sourced from <a href="https://www.geonames.org/export/">GeoNames</a> and lists all
 * cities with population > 1000. Country names are manually appended at the end to get the consolidated
 * list of locations.
 * </p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * LocationDetector detector = LocationDetector.getInstance();
 * Set<String> locations = detector.extractLocations("I traveled from New York to Paris");
 * // Returns: {"new york", "paris"}
 * }</pre>
 */
public class LocationDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationDetector.class);
    private static final String LOCATIONS_FILE = "/util/countries_cities1000.txt";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}.'-]+");

    private final Set<String> locations = new HashSet<>();
    private int maxLocationLength = 0;
    private int minLocationLength = Integer.MAX_VALUE;

    private LocationDetector() throws JabRefException {
        InputStream inputStream = getClass().getResourceAsStream(LOCATIONS_FILE);

        if (inputStream == null) {
            throw new JabRefException("Locations file not in resources");
        }

        loadLocationDataFromInputStream(inputStream);
    }

    ///  Constructor to allow loading in test data
    private LocationDetector(InputStream testFileInputStream) throws JabRefException {
        loadLocationDataFromInputStream(testFileInputStream);
    }

    static class InstanceHolder {
        private static final LocationDetector INSTANCE;

        static {
            try {
                INSTANCE = new LocationDetector();
            } catch (JabRefException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    /**
     * Gets the singleton instance of LocationDetector.
     *
     * @return the singleton instance
     * @throws ExceptionInInitializerError if the instance could not be created
     */
    public static LocationDetector getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Return a test instance of LocationDetector using the given {@code InputStream} as location data.
     * <p>
     * This method should only be used in testing.
     *
     * @param testInputStream the input stream containing the location data
     * @return a new LocationDetector instance for testing
     * @throws JabRefException if there's an error loading the test data
     */
    public static LocationDetector createTestInstance(InputStream testInputStream) throws JabRefException {
        return new LocationDetector(testInputStream);
    }

    private void loadLocationDataFromInputStream(InputStream inputStream) throws JabRefException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try (inputStream; reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = NormalizeUtils.normalize(line).orElse("");
                if (line.isEmpty()) {
                    continue;
                }
                line = line.strip().toLowerCase();
                locations.add(line);
                maxLocationLength = Math.max(maxLocationLength, line.length());
                minLocationLength = Math.min(minLocationLength, line.length());
            }
        } catch (IOException e) {
            throw new JabRefException("I/O error while reading location data from resources", e);
        }
        LOGGER.debug("Max location length seen in data: {}", maxLocationLength);
    }

    /**
     * Extracts all known locations from the given input string.
     * <p>
     * The method normalizes the input, tokenizes it, and searches for both single-token
     * and multi-token location matches. It performs greedy matching to find the longest
     * possible location names.
     *
     * @param input the input string to search for locations (must not be null)
     * @return a set of location names found in the input string, or an empty set if none found
     */
    public Set<String> extractLocations(@NonNull String input) {
        if (input.isEmpty()) {
            return Set.of();
        }

        input = NormalizeUtils.normalize(input).orElse("");
        input = input.strip().toLowerCase();
        if (input.isEmpty()) {
            return Set.of();
        }

        List<String> tokens = tokenize(input);
        if (tokens.isEmpty()) {
            return Set.of();
        }

        Set<String> foundLocations = new HashSet<>();
        StringBuilder accumulator = new StringBuilder();

        for (int i = 0; i < tokens.size(); i++) {
            String currentToken = tokens.get(i);
            String foundLocation = "";

            if (locations.contains(currentToken)) {
                foundLocation = currentToken;
            }

            accumulator.setLength(0);
            accumulator.append(currentToken);
            for (int j = i + 1; j < tokens.size(); j++) {
                accumulator.append(" ");
                accumulator.append(tokens.get(j));

                if (accumulator.length() > maxLocationLength) {
                    break;
                }

                String nextToken = accumulator.toString();
                if (locations.contains(nextToken)) {
                    foundLocation = nextToken;
                }
            }

            if (!foundLocation.isEmpty()) {
                foundLocations.add(foundLocation);
            }
        }

        return foundLocations;
    }

    private static List<String> tokenize(String input) {
        Matcher m = TOKEN_PATTERN.matcher(input);
        List<String> tokens = new ArrayList<>();
        while (m.find()) {
            tokens.add(m.group());
        }
        return tokens;
    }
}
