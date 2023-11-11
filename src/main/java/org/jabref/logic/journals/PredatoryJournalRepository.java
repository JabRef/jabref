package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import info.debatty.java.stringsimilarity.Cosine;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A repository for all predatory journals and publishers, including add and find methods.
 */
public class PredatoryJournalRepository {
    private final Logger LOGGER = LoggerFactory.getLogger(PredatoryJournalRepository.class);
    private final Map<String, PredatoryJournalInformation> predatoryJournals = new HashMap<>();
    private final Cosine COSINE_METRIC = new Cosine();

    /**
     * Initializes the internal data based on the predatory journals found in the given MV file
     */
    public PredatoryJournalRepository(Path pjlist) {
        MVMap<String, PredatoryJournalInformation> predatoryJournalsMap;
        try (MVStore store = new MVStore.Builder().readOnly().fileName(pjlist.toAbsolutePath().toString()).open()) {
            predatoryJournalsMap = store.openMap("PredatoryJournals");
            predatoryJournals.putAll(predatoryJournalsMap);
        }
    }

    /**
     * Initializes the repository with demonstration data. Used if no abbreviation file is found.
     */
    public PredatoryJournalRepository() {
        predatoryJournals.put("Demo", new PredatoryJournalInformation("Demo", "Demo", ""));
    }

    /**
     * Returns true if the given journal name is contained in the list in its full form
     */
    public boolean isKnownName(String journalName, double similarityScore) {
        String journal = journalName.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");

        if (predatoryJournals.containsKey(journal)) {
            LOGGER.info("match: " + journal);
            return true;
        }

        var matches = predatoryJournals.keySet().stream()
                                       .filter(key -> COSINE_METRIC.distance(key.toLowerCase(Locale.ROOT), journal.toLowerCase(Locale.ROOT)) > similarityScore)
                                       .collect(Collectors.toList());

        LOGGER.info("matches: " + String.join(", ", matches));
        return !matches.isEmpty();
    }

    public void addToPredatoryJournals(String name, String abbr, String url) {
        predatoryJournals.put(decode(name), new PredatoryJournalInformation(decode((name)), decode(abbr), url));
    }

    private String decode(String s) {
        return Optional.ofNullable(s)
                       .orElse("")
                       .replace(",", "")
                       .replace("&amp;", "&")
                       .replace("&#8217;", "'")
                       .replace("&#8211;", "-");
    }

    public Map<String, PredatoryJournalInformation> getPredatoryJournals() {
        return Collections.unmodifiableMap(predatoryJournals);
    }
}
