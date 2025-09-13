package org.jabref.logic.icore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.JabRefException;
import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.icore.ConferenceEntry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.jspecify.annotations.NonNull;
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
 * </p>
 */
public class ConferenceRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConferenceRepository.class);
    private static final String ICORE_RANK_DATA_FILE = "/icore/ICORE2023.csv";
    private static final double LEVENSHTEIN_THRESHOLD = 0.9;
    private static final double COMBINED_LCS_LEV_THRESHOLD = 0.75;
    private static final double EPSILON = 1e-6;
    private static final StringSimilarity LEVENSHTEIN_MATCHER = new StringSimilarity();

    private final Map<String, ConferenceEntry> acronymToConference = new HashMap<>();
    private final Map<String, ConferenceEntry> titleToConference = new HashMap<>();
    private final Map<String, ConferenceEntry> normalizedTitleToConference = new HashMap<>();
    private int maxAcronymLength = 0;

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
                String acronym = record.get("Acronym").strip().toLowerCase();
                String rank = record.get("Rank").strip();

                if (id.isEmpty() || title.isEmpty() || acronym.isEmpty() || rank.isEmpty()) {
                    LOGGER.warn("Missing fields in row in ICORE rank data: {}", record);
                    continue;
                }

                if (title.indexOf('(') >= 0) {
                    // remove any extra alias strings in parentheses
                    title = ConferenceUtils.removeAllParenthesesWithContent(title);
                }
                ConferenceEntry conferenceEntry = new ConferenceEntry(id, title, acronym, rank);
                acronymToConference.put(acronym, conferenceEntry);
                titleToConference.put(title, conferenceEntry);
                normalizedTitleToConference.put(ConferenceUtils.normalize(title), conferenceEntry);
                if (acronym.length() >= maxAcronymLength) {
                    maxAcronymLength = acronym.length();
                }
            }
        } catch (IOException e) {
            throw new JabRefException("I/O Error while reading ICORE data from resource", e);
        }
        LOGGER.debug("Max acronym length seen in data: {}", maxAcronymLength);
    }

    /**
     * Searches the given query against the ICORE conference ranking data and returns a match, if found.
     * <p>
     * While searching, we first look for a conference acronym present inside parentheses, like <code>(ICSE)</code>
     * or <code>(ICSE 2022)</code>. If acronym lookup fails, the query is processed further and matched against the list
     * of conference titles.
     * </p>
     *
     * @param bookTitle the string to search, must not be {@code null}
     * @return an {@code Optional} conference entry, if found
     * or {@code Optional.empty()} if no conference entry is found
     * @implNote see {@link ConferenceRepository#fuzzySearchConferenceTitles} for more details on matching
     */
    public Optional<ConferenceEntry> getConferenceFromBookTitle(@NonNull String bookTitle) {
        String query = bookTitle.strip().toLowerCase();
        ConferenceEntry conference;

        conference = acronymToConference.get(query);
        if (conference != null) {
            return Optional.of(conference);
        }

        conference = titleToConference.get(query);
        if (conference != null) {
            return Optional.of(conference);
        }

        Optional<ConferenceEntry> acronymConference = getConferenceFromAcronym(query);
        if (acronymConference.isPresent()) {
            return acronymConference;
        }

        return fuzzySearchConferenceTitles(query);
    }

    private Optional<ConferenceEntry> getConferenceFromAcronym(String query) {
        Optional<String> acronym = ConferenceUtils.extractStringFromParentheses(query);

        if (acronym.isPresent()) {
            ConferenceEntry conference;
            Set<String> acronymCandidates = ConferenceUtils.generateAcronymCandidates(acronym.get(), maxAcronymLength);
            LOGGER.debug("Extracted acronym string: {}, Acronym candidates: {}", acronym.get(), acronymCandidates);
            for (String candidate : acronymCandidates) {
                conference = acronymToConference.get(candidate);
                if (conference != null) {
                    return Optional.of(conference);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Searches the conference data for the given query string using a combination of Levenshtein similarity
     * {@link StringSimilarity#similarity} and Longest Common Substring (LCS) similarity {@link StringSimilarity#LCSSimilarity}.
     * <p>
     * The input query is first fed through the normalizer at {@link ConferenceUtils#normalize} which strips away much of the
     * noise.
     * </p>
     * <p>
     * While searching, the function computes Levenshtein similarity and LCS similarity between the query and the current conference
     * title (also normalized) and prioritizes them in the following order:
     * <ol>
     *     <li>Whenever LCS similarity returns <code>1.0</code>, i.e., a conference title is found entirely as a substring in the query.</li>
     *     <li>Whenever Levenshtein similarity exceeds the threshold defined by the <code>LEVENSHTEIN_THRESHOLD</code> constant.</li>
     *     <li>The combined weighted score of both LCS and Levenshtein similarities exceeds the <code>COMBINED_LCS_LEV_THRESHOLD</code>.</li>
     * </ol>
     * <p>
     * The combined score is calculated as follows:
     * <code>(0.6 * Levenshtein similarity) + (0.4 * LCS similarity)</code>
     * </p>
     *
     * @param query The query string to be searched
     * @return an {@code Optional} conference entry, if found
     * or {@code Optional.empty()} if no conference entry is found
     */
    private Optional<ConferenceEntry> fuzzySearchConferenceTitles(String query) {
        String bestMatch = "";
        double bestScore = 0.0;
        String normalizedQuery = ConferenceUtils.normalize(query);

        if (normalizedQuery.isEmpty()) {
            return Optional.empty();
        }

        ConferenceEntry acronymConference = acronymToConference.get(normalizedQuery);
        if (acronymConference != null) {
            return Optional.of(acronymConference);
        }

        acronymConference = normalizedTitleToConference.get(normalizedQuery);
        if (acronymConference != null) {
            return Optional.of(acronymConference);
        }

        for (String conferenceTitle : normalizedTitleToConference.keySet()) {
            // only match for queries longer than the current conference title
            // this will safeguard against overfitting common prefixes
            if (normalizedQuery.length() >= conferenceTitle.length()) {
                double levSimilarity = LEVENSHTEIN_MATCHER.similarity(normalizedQuery, conferenceTitle);
                double LCSSimilarity = StringSimilarity.LCSSimilarity(normalizedQuery, conferenceTitle);
                double combinedScore = levSimilarity * 0.6 + LCSSimilarity * 0.4;
                boolean exactSubstringMatch = Math.abs(LCSSimilarity - 1.0) <= EPSILON;

                if (exactSubstringMatch) {
                    return Optional.of(normalizedTitleToConference.get(conferenceTitle));
                }

                if (levSimilarity >= LEVENSHTEIN_THRESHOLD) {
                    return Optional.of(normalizedTitleToConference.get(conferenceTitle));
                }

                if (combinedScore >= COMBINED_LCS_LEV_THRESHOLD && combinedScore >= bestScore) {
                    bestMatch = conferenceTitle;
                    bestScore = combinedScore;
                    LOGGER.debug("Matched query: {} with title: {} with combinedScore: {} and LEV: {} and LCS: {}",
                            normalizedQuery, conferenceTitle, combinedScore, levSimilarity, LCSSimilarity);
                }
            }
        }

        if (!bestMatch.isEmpty()) {
            return Optional.of(normalizedTitleToConference.get(bestMatch));
        }

        return Optional.empty();
    }
}
