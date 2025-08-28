package org.jabref.logic.icore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.icore.ConferenceEntry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Repository that loads and stores the latest ICORE Conference Ranking Data and allows lookups using a conference's
 * acronym or title.
 * <p>
 * The ranking data is sourced from <a href="https://portal.core.edu.au/conf-ranks/">the ICORE Conference Ranking Portal</a>.
 * Since the website does not expose an API endpoint to fetch this data programmatically, it must be manually exported
 * from the website and stored as a resource. This means that when new ranking data is released, the old data must be
 * replaced and the <code>ICORE_RANK_DATA_FILE</code> variable must be modified to point to the new data file.
 */
public class ConferenceRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConferenceRepository.class);
    private static final String ICORE_RANK_DATA_FILE = "/icore/ICORE2023.csv";
    private static final double FUZZY_SEARCH_THRESHOLD = 0.9;
    private static final StringSimilarity MATCHER = new StringSimilarity();

    private final Map<String, ConferenceEntry> acronymToConference = new HashMap<>();
    private final Map<String, ConferenceEntry> titleToConference = new HashMap<>();

    public ConferenceRepository() throws JabRefException {
        InputStream inputStream = getClass().getResourceAsStream(ICORE_RANK_DATA_FILE);

        if (inputStream == null) {
            throw new JabRefException("ICORE rank data file not found in resources");
        }

        loadConferenceDataFromInputStream(inputStream);
    }

    /// Constructor to allow loading in test data
    public ConferenceRepository(InputStream testFileInputStream) throws JabRefException {
        loadConferenceDataFromInputStream(testFileInputStream);
    }

    private void loadConferenceDataFromInputStream(InputStream inputStream) throws JabRefException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        try (inputStream; reader) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .get()
                    .parse(reader);

            for (CSVRecord record : records) {
                String id = record.get("Id").strip();
                String title = record.get("Title").strip().toLowerCase();
                String acronym = record.get("Acronym").strip().toUpperCase();
                String rank = record.get("Rank").strip();

                if (id.isEmpty() || title.isEmpty() || acronym.isEmpty() || rank.isEmpty()) {
                    LOGGER.warn("Missing fields in row in ICORE rank data: {}", record);
                    continue;
                }

                ConferenceEntry conferenceEntry = new ConferenceEntry(id, title, acronym, rank);
                acronymToConference.put(acronym, conferenceEntry);
                titleToConference.put(title, conferenceEntry);
            }
        } catch (IOException e) {
            throw new JabRefException("I/O Error while reading ICORE data from resource", e);
        }
    }

    public Optional<ConferenceEntry> getConferenceFromAcronym(String acronym) {
        String query = acronym.strip().toUpperCase();

        ConferenceEntry conference = acronymToConference.get(query);

        if (conference == null) {
            return Optional.empty();
        }

        return Optional.of(conference);
    }

    public Optional<ConferenceEntry> getConferenceFromBookTitle(String bookTitle) {
        String query = bookTitle.strip().toLowerCase();

        ConferenceEntry conference = titleToConference.get(query);
        if (conference != null) {
            return Optional.of(conference);
        }

        String bestMatch = fuzzySearchConferenceTitles(query);
        if (bestMatch.isEmpty()) {
            return Optional.empty();
        }

        conference = titleToConference.get(bestMatch);

        return Optional.of(conference);
    }

    /**
     * Searches all conference titles for the given query string using {@link StringSimilarity#similarity} as a MATCHER.
     * <p>
     * The threshold for matching is set at 0.9. This function will always return the conference title with the highest
     * similarity rating.
     *
     * @param query The query string to be searched
     * @return The conference title, if found. Otherwise, an empty string is returned.
     */
    private String fuzzySearchConferenceTitles(String query) {
        String bestMatch = "";
        double bestSimilarity = 0.0;

        for (String conferenceTitle : titleToConference.keySet()) {
            double similarity = MATCHER.similarity(query, conferenceTitle);
            if (similarity >= FUZZY_SEARCH_THRESHOLD && similarity > bestSimilarity) {
                bestMatch = conferenceTitle;
                bestSimilarity = similarity;
            }
        }

        return bestMatch;
    }
}
