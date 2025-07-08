package org.jabref.logic.icore;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.util.strings.StringSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ICoreRankingRepository {

    final Map<String, String> acronymToRank = new HashMap<>();
    private final Map<String, String> nameToRank = new HashMap<>();
    private final Map<String, ConferenceRankingEntry> acronymMap = new HashMap<>();
    private final Map<String, ConferenceRankingEntry> nameMap = new HashMap<>();
    private final StringSimilarity similarity = new StringSimilarity();
    private static final Logger LOGGER = LoggerFactory.getLogger(ICoreRankingRepository.class);

    public ICoreRankingRepository() {
        InputStream inputStream = getClass().getResourceAsStream("/ICORE.csv");
        if (inputStream == null) {
            LOGGER.error("ICORE.csv not found in resources.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            reader.lines().skip(1).forEach(line -> {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length >= 9) {
                    String name = parts[0].trim().toLowerCase();
                    String acronym = parts[1].trim().toLowerCase();
                    String rank = parts[3].trim();
                    acronymToRank.put(acronym, rank);
                    nameToRank.put(name, rank);
                }
                ConferenceRankingEntry entry = new ConferenceRankingEntry(
                        parts[0].trim(), // title
                        parts[1].trim(), // acronym
                        parts[2].trim(), // source
                        parts[3].trim(), // rank
                        parts[4].trim(), // note
                        parts[5].trim(), // dblp
                        parts[6].trim(), // primaryFor
                        parts[7].trim(), // comments
                        parts[8].trim()  // averageRating
                );
                acronymMap.put(entry.acronym.toLowerCase(), entry);
                nameMap.put(entry.title.toLowerCase(), entry);
            });

//            System.out.println("Loaded entries:");
//            acronymToRank.forEach((key, val) -> System.out.println("Acronym: " + key + " -> " + val));
//            nameToRank.forEach((key, val) -> System.out.println("Name: " + key + " -> " + val));
        } catch (Exception e) {
            LOGGER.error("Failed to load ICORE ranking data", e);
        }
    }

    public Optional<String> getRankingFor(String acronymOrName) {
        String key = acronymOrName.trim().toLowerCase();

        // 1. Try exact acronym match
        if (acronymToRank.containsKey(key)) {
            return Optional.of(acronymToRank.get(key));
        }

        // 2. Try exact name match
        if (nameToRank.containsKey(key)) {
            return Optional.of(nameToRank.get(key));
        }

        // 3. Skip fuzzy matching for short strings (e.g., "icse")
        if (key.length() < 6) {
            LOGGER.info("Skipped fuzzy fallback for short string: " + key);
            return Optional.empty();
        }

        // 4. Fallback: fuzzy match with strict threshold
        LOGGER.info("Fuzzy match fallback triggered for: " + key);
        return nameToRank.entrySet().stream()
                .filter(e -> similarity.editDistanceIgnoreCase(e.getKey(), key) < 0.01)
                .peek(e -> LOGGER.info("Fuzzy match candidate: " + e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public Optional<ConferenceRankingEntry> getFullEntry(String acronymOrName) {
        String key = acronymOrName.trim().toLowerCase();
        if (acronymMap.containsKey(key)) {
            return Optional.of(acronymMap.get(key));
        }
        if (nameMap.containsKey(key)) {
            return Optional.of(nameMap.get(key));
        }
        return Optional.empty();
    }
}
