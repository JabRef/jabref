package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A repository for all predatory journals and publishers, including add and find methods.
 */
public class PredatoryJournalRepository {

    private final MVMap<String, List<String>> predatoryJournals;
    private final Logger LOGGER = LoggerFactory.getLogger(PredatoryJournalRepository.class);

    public PredatoryJournalRepository(Path predatoryJournalList) {
        MVStore store = new MVStore.Builder().fileName(predatoryJournalList.toAbsolutePath().toString()).open();
        this.predatoryJournals = store.openMap("predatoryJournals");
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

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("matches: " + String.join(", ", matches));
        }
        return !matches.isEmpty();
    }

    public void addToPredatoryJournals(String name, String abbr, String url) {
        // computeIfAbsent -- more efficient if key is already present as list only created if absent
        // predatoryJournals.computeIfAbsent(decode(name), (k, v) -> new ArrayList<String>()).addAll(List.of(decode(abbr), url));

        predatoryJournals.put(decode(name), List.of(decode(abbr), url));
    }

    private String decode(String s) {
        if (s == null) {
            return "";
        }

        return s.replace(",", "")
                .replace("&amp;", "&")
                .replace("&#8217;", "'")
                .replace("&#8211;", "-");
    }

    private double cosineSimilarity(String a, String b) {
        List<String> a_bigrams = getBigrams(a);
        List<String> b_bigrams = getBigrams(b);
        Set<String> union = getUnion(a_bigrams, b_bigrams);

        Map<String, Integer> a_freqs = countNgramFreq(a_bigrams, union);
        Map<String, Integer> b_freqs = countNgramFreq(b_bigrams, union);

        List<Integer> av = new ArrayList<>(a_freqs.values());
        List<Integer> bv = new ArrayList<>(b_freqs.values());

        return dot(av, bv) / (norm(av) * norm(bv));
    }

    private static List<String> getBigrams(String s) {
        String[] s_chars = s.toLowerCase().replace(" ", "").split("");
        for (int i = 0; i < s_chars.length - 1; i++) {
            s_chars[i] += s_chars[i + 1];
        }
        return List.of(s_chars).subList(0, s_chars.length - 1);
    }

    private Map<String, Integer> countNgramFreq(List<String> ngrams, Set<String> union) {
        var freqs = new HashMap<String, Integer>();
        for (String n : union) {
            freqs.put(n, Collections.frequency(ngrams, n));
        }
        return freqs;
    }

    private Set<String> getUnion(List<String> a, List<String> b) {
        return Stream.of(a, b).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    private double dot(List<Integer> av, List<Integer> bv) {
        double sum = 0;
        for (int i = 0; i < av.size() && i < bv.size(); i++) {
            sum += av.get(i) * bv.get(i);
        }
        return sum;
    }

    private double norm(List<Integer> v) {
        double sum = 0;
        for (int x : v) {
            sum += x * x;
        }
        return Math.sqrt(sum);
    }
}
