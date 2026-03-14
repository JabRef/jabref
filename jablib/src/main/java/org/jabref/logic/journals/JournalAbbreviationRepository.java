package org.jabref.logic.journals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.jabref.logic.journals.ltwa.LtwaRepository;
import org.jabref.logic.util.strings.StringSimilarity;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A repository for all journal abbreviations backed by Postgres for built in abbreviations.
/// Custom user defined abbreviations are kept in memory.
/// Fuzzy matching uses Postgres `pg_trgm` extension for efficient trigram similarity search.
public class JournalAbbreviationRepository {

    static final Pattern QUESTION_MARK = Pattern.compile("\\?");

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationRepository.class);

    /// Minimum trigram similarity score for a fuzzy match candidate (pg_trgm scale: 0.0 to 1.0)
    private static final double FUZZY_SIMILARITY_MIN = 0.3;

    /// If the top two fuzzy candidates have similarity scores within this threshold, the match is considered
    /// ambiguous and no result is returned. This prevents wrong matches such as "Nutrients" → "Nutrition".
    private static final double FUZZY_AMBIGUITY_THRESHOLD = 0.05;

    private static final String FIND_EXACT_SQL = """
            SELECT name, abbreviation, shortest_unique_abbreviation
            FROM journal_abbreviation
            WHERE name = ? OR abbreviation = ? OR dotless_abbreviation = ? OR shortest_unique_abbreviation = ?
            LIMIT 1""";

    private static final String IS_ABBREVIATED_SQL = """
            SELECT 1 FROM journal_abbreviation
            WHERE abbreviation = ? OR dotless_abbreviation = ? OR shortest_unique_abbreviation = ?
            LIMIT 1""";

    private static final String FIND_FUZZY_SQL = """
            SELECT name, abbreviation, shortest_unique_abbreviation,
                   similarity(name, ?) AS sim
            FROM journal_abbreviation
            WHERE similarity(name, ?) > ?
            ORDER BY sim DESC
            LIMIT 2""";

    private static final String GET_ALL_SQL = """
            SELECT name, abbreviation, shortest_unique_abbreviation
            FROM journal_abbreviation""";

    private static final String GET_ALL_NAMES_SQL = """
            SELECT name FROM journal_abbreviation""";

    private final DataSource dataSource;
    private final TreeSet<Abbreviation> customAbbreviations = new TreeSet<>();
    private final StringSimilarity similarity = new StringSimilarity();
    private final LtwaRepository ltwaRepository;

    /// Initializes the repository backed by the given Postgres data source.
    ///
    /// @param dataSource     The Postgres data source containing built-in journal abbreviations.
    /// @param ltwaRepository The LTWA repository to use for LTWA-based abbreviations.
    public JournalAbbreviationRepository(DataSource dataSource, LtwaRepository ltwaRepository) {
        this.dataSource = dataSource;
        this.ltwaRepository = ltwaRepository;
    }

    /// Initializes the repository with demonstration data. Used if no database is available.
    public JournalAbbreviationRepository() {
        this.dataSource = null;
        this.ltwaRepository = new LtwaRepository();
        customAbbreviations.add(new Abbreviation("Demonstration", "Demo", "Dem"));
    }

    private static String sanitizeInput(String input) {
        String result = input.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");
        // Strip surrounding curly braces added by LaTeX protection
        if (result.startsWith("{") && result.endsWith("}")) {
            result = result.substring(1, result.length() - 1);
        }
        return result;
    }

    private static boolean isMatched(String name, Abbreviation abbreviation) {
        return name.equalsIgnoreCase(abbreviation.getName())
                || name.equalsIgnoreCase(abbreviation.getAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getDotlessAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getShortestUniqueAbbreviation());
    }

    private static boolean isMatchedAbbreviated(String name, Abbreviation abbreviation) {
        boolean isExpanded = name.equalsIgnoreCase(abbreviation.getName());
        if (isExpanded) {
            return false;
        }
        return name.equalsIgnoreCase(abbreviation.getAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getDotlessAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getShortestUniqueAbbreviation());
    }

    /// Checks custom abbreviations first (exact, then fuzzy), then the database (exact, then fuzzy).
    ///
    /// @param input The journal name (full name, abbreviated, dotless, or shortest unique form).
    private Optional<Abbreviation> find(String input) {
        String journal = sanitizeInput(input);

        // case-insensitive as per existing behavior
        Optional<Abbreviation> custom = customAbbreviations.stream()
                                                           .filter(abbreviation -> isMatched(journal, abbreviation))
                                                           .findFirst();
        if (custom.isPresent()) {
            return custom;
        }

        Optional<Abbreviation> exact = queryOne(FIND_EXACT_SQL, journal, journal, journal, journal);
        if (exact.isPresent()) {
            return exact;
        }

        Optional<Abbreviation> customFuzzy = findBestFuzzyMatchedInMemory(customAbbreviations, journal);
        if (customFuzzy.isPresent()) {
            return customFuzzy;
        }

        if (hasCustomFuzzyCandidates(journal)) {
            return Optional.empty();
        }

        return findFuzzyInDatabase(journal);
    }

    private boolean hasCustomFuzzyCandidates(String input) {
        return customAbbreviations.stream()
                                  .anyMatch(abbreviation -> similarity.isSimilar(input, abbreviation.getName()));
    }

    /// Fuzzy matches against an in-memory collection using StringSimilarity
    /// Used for custom abbreviations typically < 20 entries
    private Optional<Abbreviation> findBestFuzzyMatchedInMemory(Collection<Abbreviation> abbreviations, String input) {
        if (input.trim().split("\\s+").length <= 1) {
            boolean isAscii = input.chars().allMatch(c -> c <= 0x7F);
            if (isAscii) {
                return Optional.empty();
            }
        }

        final double editDistanceThreshold = 1.0;

        List<Abbreviation> candidates = abbreviations.stream()
                                                     .filter(abbreviation -> similarity.isSimilar(input, abbreviation.getName()))
                                                     .sorted(Comparator.comparingDouble(abbreviation -> similarity.editDistanceIgnoreCase(input, abbreviation.getName())))
                                                     .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        if (candidates.size() > 1) {
            double bestDistance = similarity.editDistanceIgnoreCase(input, candidates.getFirst().getName());
            double secondDistance = similarity.editDistanceIgnoreCase(input, candidates.get(1).getName());

            if (Math.abs(bestDistance - secondDistance) < editDistanceThreshold) {
                return Optional.empty();
            }
        }

        return Optional.of(candidates.getFirst());
    }

    /// Fuzzy matches in the database using Postgres pg_trgm trigram similarity
    /// To prevent wrong matches, fuzzy matching is skipped for single word ASCII inputs
    private Optional<Abbreviation> findFuzzyInDatabase(String input) {
        if (dataSource == null) {
            return Optional.empty();
        }

        if (input.trim().split("\\s+").length <= 1) {
            boolean isAscii = input.chars().allMatch(c -> c <= 0x7F);
            if (isAscii) {
                return Optional.empty();
            }
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(FIND_FUZZY_SQL)) {
            ps.setString(1, input);
            ps.setString(2, input);
            ps.setDouble(3, FUZZY_SIMILARITY_MIN);

            try (ResultSet rs = ps.executeQuery()) {
                List<Abbreviation> candidates = new ArrayList<>(2);
                List<Double> similarities = new ArrayList<>(2);
                while (rs.next()) {
                    candidates.add(toAbbreviation(rs));
                    similarities.add(rs.getDouble("sim"));
                }

                if (candidates.isEmpty()) {
                    return Optional.empty();
                }

                // If top two candidates are too close in similarity, the match is ambiguous
                if (candidates.size() > 1
                        && Math.abs(similarities.getFirst() - similarities.get(1)) < FUZZY_AMBIGUITY_THRESHOLD) {
                    return Optional.empty();
                }

                return Optional.of(candidates.getFirst());
            }
        } catch (SQLException e) {
            LOGGER.error("Error during fuzzy search for journal abbreviation", e);
        }
        return Optional.empty();
    }

    /// Returns true if the given journal name is contained in the repository either in its full form
    /// If no exact match is found, attempts a fuzzy match to recognize minor input errors
    public boolean isKnownName(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }
        return find(journalName).isPresent();
    }

    /// Returns true if the given journal name is in its abbreviated form
    public boolean isAbbreviatedName(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }
        String journal = sanitizeInput(journalName);

        // Check custom abbreviations (case-insensitive)
        if (customAbbreviations.stream().anyMatch(abbreviation -> isMatchedAbbreviated(journal, abbreviation))) {
            return true;
        }

        // Check database (excludes full name — only abbreviated forms)
        return queryExists(IS_ABBREVIATED_SQL, journal, journal, journal);
    }

    public Optional<String> getLtwaAbbreviation(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return Optional.of(journalName);
        }
        return ltwaRepository.abbreviate(journalName);
    }

    /// Returns the full unabbreviated journal name for the given input
    public Optional<String> getFullName(String text) {
        return find(text).map(Abbreviation::getName);
    }

    public Optional<String> getDefaultAbbreviation(String text) {
        return find(text).map(Abbreviation::getAbbreviation);
    }

    public Optional<String> getDotless(String text) {
        return find(text).map(Abbreviation::getDotlessAbbreviation);
    }

    public Optional<String> getShortestUniqueAbbreviation(String text) {
        return find(text).map(Abbreviation::getShortestUniqueAbbreviation);
    }

    /// Cycles to the next abbreviation form for the given text.
    ///
    /// The cycle is name -> abbreviation → dotless -> [shortest unique if different] > name.
    /// This inlines the logic previously in {@link Abbreviation#getNext(String)}.
    public Optional<String> getNextAbbreviation(String text) {
        return find(text).map(abbreviation -> abbreviation.getNext(text));
    }

    /// Attempts to get the abbreviation entry for the given input.
    /// Prefer using the String-returning methods ({@link #getDefaultAbbreviation}, {@link #getDotless}, etc.)
    /// for logic operations. This method exists primarily for UI display needs.
    ///
    /// @param input The journal name (full, abbreviated, dotless, or shortest unique form).
    public Optional<Abbreviation> get(String input) {
        return find(input);
    }

    /// Returns all built in abbreviations from the database, used for UI display in the preferences panel
    public Collection<Abbreviation> getAllLoaded() {
        List<Abbreviation> all = new ArrayList<>();
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(GET_ALL_SQL);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    all.add(toAbbreviation(rs));
                }
            } catch (SQLException e) {
                LOGGER.error("Error fetching all journal abbreviations", e);
            }
        }
        return all;
    }

    /// Returns the full names of all built in journals, used for auto-completion
    public Set<String> getFullNames() {
        Set<String> names = new HashSet<>();
        customAbbreviations.forEach(abbreviation -> names.add(abbreviation.getName()));
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(GET_ALL_NAMES_SQL);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            } catch (SQLException e) {
                LOGGER.error("Error fetching journal full names", e);
            }
        }
        return names;
    }

    public void addCustomAbbreviation(@NonNull Abbreviation abbreviation) {
        customAbbreviations.add(abbreviation);
    }

    public Collection<Abbreviation> getCustomAbbreviations() {
        return customAbbreviations;
    }

    public void addCustomAbbreviations(Collection<Abbreviation> abbreviationsToAdd) {
        abbreviationsToAdd.forEach(this::addCustomAbbreviation);
    }

    private Optional<Abbreviation> queryOne(String sql, String... params) {
        if (dataSource == null) {
            return Optional.empty();
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(toAbbreviation(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Error querying journal abbreviation", e);
        }
        return Optional.empty();
    }

    private boolean queryExists(String sql, String... params) {
        if (dataSource == null) {
            return false;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.error("Error checking journal abbreviation existence", e);
        }
        return false;
    }

    private static Abbreviation toAbbreviation(ResultSet rs) throws SQLException {
        return new Abbreviation(
                rs.getString("name"),
                rs.getString("abbreviation"),
                rs.getString("shortest_unique_abbreviation")
        );
    }
}
