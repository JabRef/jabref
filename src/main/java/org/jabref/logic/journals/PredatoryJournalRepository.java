package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A repository for all predatory journals and publishers, including add and find methods.
 */
public class PredatoryJournalRepository {
    private final Map<String, List<String>> predatoryJournals = new HashMap<>();
    private final Logger LOGGER = LoggerFactory.getLogger(PredatoryJournalRepository.class);

    /**
     * Initializes the internal data based on the predatory journals found in the given MV file
     */
    public PredatoryJournalRepository(Path PJList) {
        MVMap<String, List<String>> mvFullToPredatoryJournalObject;
        try (MVStore store = new MVStore.Builder().readOnly().fileName(PJList.toAbsolutePath().toString()).open()) {
            mvFullToPredatoryJournalObject = store.openMap("FullToPredatoryJournal");
            predatoryJournals.putAll(mvFullToPredatoryJournalObject);
        }
    }

    /**
     * Initializes the repository with demonstration data. Used if no abbreviation file is found.
     */
    public PredatoryJournalRepository() {
        predatoryJournals.put("Demo", List.of("Demo", "Demo"));
    }

    /**
     * Returns true if the given journal name is contained in the list in its full form
     */
    public boolean isKnownName(String journalName, double similarityScore) {
        String journal = journalName.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");

        if (predatoryJournals.containsKey(journal)) {
            return true;
        }

        var matches = predatoryJournals.keySet().stream()
                                       .filter(key -> cosineSimilarity(key, journal) > similarityScore)
                                       .collect(Collectors.toList());

        LOGGER.info("matches: " + String.join(", ", matches));
        return !matches.isEmpty();
    }

    public void addToPredatoryJournals(String name, String abbr, String url) {
        predatoryJournals.put(decode(name), List.of(decode(abbr), url));
    }

    private String decode(String s) {
        return Optional.ofNullable(s)
                       .orElse("")
                       .replace(",", "")
                       .replace("&amp;", "&")
                       .replace("&#8217;", "'")
                       .replace("&#8211;", "-");
    }

    private double cosineSimilarity(String a, String b) {
        List<String> a_bigrams = getBigrams(a);
        List<String> b_bigrams = getBigrams(b);
        Set<String> union = getUnion(a_bigrams, b_bigrams);

        Map<String, Integer> a_freqs = countNgramFrequency(a_bigrams, union);
        Map<String, Integer> b_freqs = countNgramFrequency(b_bigrams, union);

        List<Integer> av = new ArrayList<>(a_freqs.values());
        List<Integer> bv = new ArrayList<>(b_freqs.values());

        return dotProduct(av, bv) / (magnitude(av) * magnitude(bv));
    }

    private static List<String> getBigrams(String s) {
        String cleanStr = s.toLowerCase().replace(" ", "");
        return IntStream.range(0, cleanStr.length() - 1)
                        .mapToObj(i -> cleanStr.substring(i, i + 2))
                        .collect(Collectors.toList());
    }

    private Map<String, Integer> countNgramFrequency(List<String> ngrams, Set<String> union) {
        return union.stream().collect(Collectors.toMap(n -> n, n -> Collections.frequency(ngrams, n)));
    }

    private Set<String> getUnion(List<String> a, List<String> b) {
        return Stream.concat(a.stream(), b.stream()).collect(Collectors.toSet());
    }

    private double dotProduct(List<Integer> av, List<Integer> bv) {
        return IntStream.range(0, Math.min(av.size(), bv.size()))
                        .mapToDouble(i -> av.get(i) * bv.get(i))
                        .sum();
    }

    private double magnitude(List<Integer> v) {
        return Math.sqrt(v.stream().mapToDouble(x -> x * x).sum());
    }
}

