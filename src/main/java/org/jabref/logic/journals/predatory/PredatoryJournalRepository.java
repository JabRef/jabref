package org.jabref.logic.journals.predatory;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.jabref.logic.util.strings.StringSimilarity;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A repository for all predatory journals and publishers, including add and find methods.
 */
public class PredatoryJournalRepository implements AutoCloseable {
    private final Logger LOGGER = LoggerFactory.getLogger(PredatoryJournalRepository.class);
    private final Map<String, PredatoryJournalInformation> predatoryJournals;
    private final StringSimilarity match = new StringSimilarity();
    private final MVStore store;

    /**
     * Initializes the internal data based on the predatory journals found in the given MV file
     */
    public PredatoryJournalRepository(Path mvStore) {
        MVMap<String, PredatoryJournalInformation> predatoryJournalsMap;
        store = new MVStore.Builder().readOnly().fileName(mvStore.toAbsolutePath().toString()).open();
        predatoryJournals = store.openMap("PredatoryJournals");
    }

    /**
     * Initializes the repository with demonstration data. Used if no abbreviation file is found.
     */
    public PredatoryJournalRepository() {
        store = null;
        predatoryJournals = Map.of("Demo", new PredatoryJournalInformation("Demo", "Demo", ""));
    }

    /**
     * Returns true if the given journal name is contained in the list in its full form
     */
    public boolean isKnownName(String journalName) {
        String journal = journalName.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");

        if (predatoryJournals.containsKey(journal)) {
            LOGGER.debug("Found predatory journal {}", journal);
            return true;
        }

        var matches = predatoryJournals.keySet().stream()
                                       .filter(key -> match.isSimilar(journal.toLowerCase(Locale.ROOT), key.toLowerCase(Locale.ROOT)))
                                       .collect(Collectors.toList());

        LOGGER.info("Found multiple possible predatory journals {}", String.join(", ", matches));
        return !matches.isEmpty();
    }

    @Override
    public void close() throws Exception {
        if (store != null) {
            store.close();
        }
    }
}
