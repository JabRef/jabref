package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.util.strings.StringSimilarity;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

/**
 * A repository for all journal abbreviations, including add and find methods.
 */
public class JournalAbbreviationRepository {
    static final Pattern QUESTION_MARK = Pattern.compile("\\?");

    // These fields need to be public to be used in JournalMvGenerator.java
    private static final String FULL_TO_ABBREVIATION_MAP_NAME = "FullToAbbreviation";
    private static final String ABBREVIATION_TO_ABBREVIATION_MAP_NAME = "AbbreviationToName";
    private static final String DOTLESS_TO_ABBREVIATION_MAP_NAME = "DotlessToName";
    private static final String SHORTEST_UNIQUE_TO_ABBREVIATION_MAP_NAME = "ShortestUniqueToName";

    private final MVStore store;
    private MVMap<String, Abbreviation> fullToAbbreviationMap;
    private MVMap<String, String> abbreviationToNameMap;
    private MVMap<String, String> dotlessToNameMap;
    private MVMap<String, String> shortestUniqueToNameMap;
    private final TreeSet<Abbreviation> customAbbreviations = new TreeSet<>();
    private final StringSimilarity similarity = new StringSimilarity();

    /**
     * Initializes the internal data based on the abbreviations found in the given MV file
     */
    public JournalAbbreviationRepository(Path journalList) {
        String journalPath = journalList.toAbsolutePath().toString();
        store = new MVStore.Builder().fileName(journalPath).open();

        openMaps(store);
    }

    /**
     * Initializes the repository with demonstration data. Used if no abbreviation file is found.
     */
    public JournalAbbreviationRepository() {
        // this will persist in memory
        store = new MVStore.Builder().open();

        openMaps(store);

        Abbreviation newAbbreviation = new Abbreviation(
                "Demonstration",
                "Demo",
                "Dem"
        );

        fullToAbbreviationMap.put("Demonstration", newAbbreviation);
        abbreviationToNameMap.put("Demo", "Demonstration");
        dotlessToNameMap.put("Demo", "Demonstration");
        shortestUniqueToNameMap.put("Dem", "Demonstration");
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

    /**
     * Returns true if the given journal name is contained in the list either in its full form
     * (e.g., Physical Review Letters) or its abbreviated form (e.g., Phys. Rev. Lett.).
     * If the exact match is not found, attempts a fuzzy match to recognize minor input errors.
     */
    public boolean isKnownName(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }
        return get(journalName).isPresent();
    }

    /**
     * Returns true if the given journal name is in its abbreviated form (e.g. Phys. Rev. Lett.). The test is strict,
     * i.e., journals whose abbreviation is the same as the full name are not considered
     */
    public boolean isAbbreviatedName(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }
        String journal = journalName.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");
        return customAbbreviations.stream().anyMatch(abbreviation -> isMatchedAbbreviated(journal, abbreviation))
                || abbreviationToNameMap.containsKey(journal)
                || dotlessToNameMap.containsKey(journal)
                || shortestUniqueToNameMap.containsKey(journal);
    }

    /**
     * Attempts to get the abbreviation of the journal given.
     * if no exact match is found, attempts a fuzzy match on full journal names.
     *
     * @param input The journal name (either full name or abbreviated name).
     */
    public Optional<Abbreviation> get(String input) {
        // Clean up input: trim and unescape ampersand
        String journal = input.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");

        Optional<Abbreviation> customAbbreviation = customAbbreviations.stream()
                                                                       .filter(abbreviation -> isMatched(journal, abbreviation))
                                                                       .findFirst();
        if (customAbbreviation.isPresent()) {
            return customAbbreviation;
        }

        // If the abbreviation is coming from fullToAbbreviationMap, then it's the name
        Optional<Abbreviation> abbreviation = Optional.ofNullable(fullToAbbreviationMap.get(journal));

        if (abbreviation.isEmpty()) {
            String name = abbreviationToNameMap.get(journal);
            if (name == null) {
                name = dotlessToNameMap.get(journal);
            }
            if (name == null) {
                name = shortestUniqueToNameMap.get(journal);
            }
            if (name != null) {
                abbreviation = Optional.ofNullable(fullToAbbreviationMap.get(name));
            }
        }

        if (abbreviation.isEmpty()) {
            abbreviation = findAbbreviationFuzzyMatched(journal);
        }

        return abbreviation;
    }

    private Optional<Abbreviation> findAbbreviationFuzzyMatched(String input) {
        Optional<Abbreviation> customMatch = findBestFuzzyMatched(customAbbreviations, input);
        if (customMatch.isPresent()) {
            return customMatch;
        }

        return findBestFuzzyMatched(fullToAbbreviationMap.values(), input);
    }

    private Optional<Abbreviation> findBestFuzzyMatched(Collection<Abbreviation> abbreviations, String input) {
        // threshold for edit distance similarity comparison
        final double SIMILARITY_THRESHOLD = 1.0;

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

            // If there is a very close match of two abbreviations, do not use any of them, because they are too close.
            if (Math.abs(bestDistance - secondDistance) < SIMILARITY_THRESHOLD) {
                return Optional.empty();
            }
        }

        return Optional.of(candidates.getFirst());
    }

    public void addCustomAbbreviation(Abbreviation abbreviation) {
        Objects.requireNonNull(abbreviation);

        // We do NOT want to keep duplicates
        // The set automatically "removes" duplicates
        // What is a duplicate? An abbreviation is NOT the same if any field is NOT equal (e.g., if the shortest unique differs, the abbreviation is NOT the same)
        customAbbreviations.add(abbreviation);
    }

    public Collection<Abbreviation> getCustomAbbreviations() {
        return customAbbreviations;
    }

    public void addCustomAbbreviations(Collection<Abbreviation> abbreviationsToAdd) {
        abbreviationsToAdd.forEach(this::addCustomAbbreviation);
    }

    public Optional<String> getNextAbbreviation(String text) {
        return get(text).map(abbreviation -> abbreviation.getNext(text));
    }

    public Optional<String> getDefaultAbbreviation(String text) {
        return get(text).map(Abbreviation::getAbbreviation);
    }

    public Optional<String> getDotless(String text) {
        return get(text).map(Abbreviation::getDotlessAbbreviation);
    }

    public Optional<String> getShortestUniqueAbbreviation(String text) {
        return get(text).map(Abbreviation::getShortestUniqueAbbreviation);
    }

    public Set<String> getFullNames() {
        return fullToAbbreviationMap.keySet();
    }

    public Collection<Abbreviation> getAllLoaded() {
        return fullToAbbreviationMap.values();
    }

    private void openMaps(MVStore store) {
        fullToAbbreviationMap = store.openMap(FULL_TO_ABBREVIATION_MAP_NAME);
        abbreviationToNameMap = store.openMap(ABBREVIATION_TO_ABBREVIATION_MAP_NAME);
        dotlessToNameMap = store.openMap(DOTLESS_TO_ABBREVIATION_MAP_NAME);
        shortestUniqueToNameMap = store.openMap(SHORTEST_UNIQUE_TO_ABBREVIATION_MAP_NAME);
    }
}
