package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A repository for all journal abbreviations, including add and find methods.
 */
public class PredatoryJournalRepository {
    private final MVMap<String, List<String>> predatoryJournals;
    private final Logger LOGGER = LoggerFactory.getLogger(PredatoryJournalRepository.class);

    public PredatoryJournalRepository(Path predatoryJournalList) {
        MVStore store = new MVStore.Builder().fileName(predatoryJournalList.toAbsolutePath().toString()).open();
        this.predatoryJournals = store.openMap("FileToPredatoryJournal");
    }

    public boolean isKnownName(String journalName, double similarityScore) {
        String journal = journalName.trim().replace("\\&", "&");

        return Optional.ofNullable(predatoryJournals.get(journal))
                       .or(() -> {
                           boolean matchFound = predatoryJournals.keySet().stream()
                                                                 .anyMatch(key -> cosineSimilarity(key, journal) > similarityScore);
                           if (matchFound) {
                               // If there's a match, return the original list (though it's not used directly).
                               return Optional.of(predatoryJournals.get(journal));
                           }
                           return Optional.empty();
                       })
                       .isPresent();
    }

    public void addToPredatoryJournals(String name, String abbr, String url) {
        predatoryJournals.computeIfAbsent(decode(name), k -> new ArrayList<>()).addAll(List.of(decode(abbr), url));
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

        Map<String, Integer> a_freqs = countNgramFreq(a_bigrams, union);
        Map<String, Integer> b_freqs = countNgramFreq(b_bigrams, union);

        List<Integer> av = new ArrayList<>(a_freqs.values());
        List<Integer> bv = new ArrayList<>(b_freqs.values());

        return dot(av, bv) / (norm(av) * norm(bv));
    }

    private static List<String> getBigrams(String s) {
        String cleanStr = s.toLowerCase().replace(" ", "");
        return IntStream.range(0, cleanStr.length() - 1)
                        .mapToObj(i -> cleanStr.substring(i, i + 2))
                        .collect(Collectors.toList());
    }

    private Map<String, Integer> countNgramFreq(List<String> ngrams, Set<String> union) {
        return union.stream().collect(Collectors.toMap(n -> n, n -> Collections.frequency(ngrams, n)));
    }

    private Set<String> getUnion(List<String> a, List<String> b) {
        return Stream.concat(a.stream(), b.stream()).collect(Collectors.toSet());
    }

    private double dot(List<Integer> av, List<Integer> bv) {
        return IntStream.range(0, Math.min(av.size(), bv.size()))
                        .mapToDouble(i -> av.get(i) * bv.get(i))
                        .sum();
    }

    private double norm(List<Integer> v) {
        return Math.sqrt(v.stream().mapToDouble(x -> x * x).sum());
    }
}

