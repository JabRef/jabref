package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.util.strings.StringSimilarity;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A repository for all journal abbreviations, including add and find methods.
 */
public class JournalAbbreviationRepository {
    static final Pattern QUESTION_MARK = Pattern.compile("\\?");
    
    /**
     * Identifier for the built-in abbreviation list
     */
    public static final String BUILTIN_LIST_ID = "BUILTIN_LIST";

    private final Map<String, Abbreviation> fullToAbbreviationObject = new HashMap<>();
    private final Map<String, Abbreviation> abbreviationToAbbreviationObject = new HashMap<>();
    private final Map<String, Abbreviation> dotlessToAbbreviationObject = new HashMap<>();
    private final Map<String, Abbreviation> shortestUniqueToAbbreviationObject = new HashMap<>();
    private final TreeSet<Abbreviation> customAbbreviations = new TreeSet<>();
    private final StringSimilarity similarity = new StringSimilarity();
    
    private final Map<String, Boolean> enabledSources = new HashMap<>();
    private final Map<Abbreviation, String> abbreviationSources = new HashMap<>();

    /**
     * Initializes the internal data based on the abbreviations found in the given MV file
     */
    public JournalAbbreviationRepository(Path journalList) {
        MVMap<String, Abbreviation> mvFullToAbbreviationObject;
        try (MVStore store = new MVStore.Builder().readOnly().fileName(journalList.toAbsolutePath().toString()).open()) {
            mvFullToAbbreviationObject = store.openMap("FullToAbbreviation");
            mvFullToAbbreviationObject.forEach((name, abbreviation) -> {
                String abbrevationString = abbreviation.getAbbreviation();
                String shortestUniqueAbbreviation = abbreviation.getShortestUniqueAbbreviation();
                Abbreviation newAbbreviation = new Abbreviation(
                        name,
                        abbrevationString,
                        shortestUniqueAbbreviation
                );
                fullToAbbreviationObject.put(name, newAbbreviation);
                abbreviationToAbbreviationObject.put(abbrevationString, newAbbreviation);
                dotlessToAbbreviationObject.put(newAbbreviation.getDotlessAbbreviation(), newAbbreviation);
                shortestUniqueToAbbreviationObject.put(shortestUniqueAbbreviation, newAbbreviation);
                
                abbreviationSources.put(newAbbreviation, BUILTIN_LIST_ID);
            });
        }
        
        enabledSources.put(BUILTIN_LIST_ID, true);
    }

    /**
     * Initializes the repository with demonstration data. Used if no abbreviation file is found.
     */
    public JournalAbbreviationRepository() {
        Abbreviation newAbbreviation = new Abbreviation(
                "Demonstration",
                "Demo",
                "Dem"
        );
        fullToAbbreviationObject.put("Demonstration", newAbbreviation);
        abbreviationToAbbreviationObject.put("Demo", newAbbreviation);
        dotlessToAbbreviationObject.put("Demo", newAbbreviation);
        shortestUniqueToAbbreviationObject.put("Dem", newAbbreviation);
        
        abbreviationSources.put(newAbbreviation, BUILTIN_LIST_ID);
        enabledSources.put(BUILTIN_LIST_ID, true);
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
     * Returns true if the given journal name is in its abbreviated form (e.g. Phys. Rev. Lett.). The test is strict,
     * i.e., journals whose abbreviation is the same as the full name are not considered.
     * Respects the enabled/disabled state of abbreviation sources.
     */
    public boolean isAbbreviatedName(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }
        String journal = journalName.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");
        
        boolean isCustomAbbreviated = customAbbreviations.stream()
                .filter(abbreviation -> isSourceEnabled(abbreviationSources.getOrDefault(abbreviation, BUILTIN_LIST_ID)))
                .anyMatch(abbreviation -> isMatchedAbbreviated(journal, abbreviation));
                
        if (!isSourceEnabled(BUILTIN_LIST_ID)) {
            return isCustomAbbreviated;
        }
        
        return isCustomAbbreviated || 
               abbreviationToAbbreviationObject.containsKey(journal) ||
               dotlessToAbbreviationObject.containsKey(journal) ||
               shortestUniqueToAbbreviationObject.containsKey(journal);
    }

    /**
     * Returns true if the given journal name is contained in the list either in its full form
     * (e.g., Physical Review Letters) or its abbreviated form (e.g., Phys. Rev. Lett.).
     * If the exact match is not found, attempts a fuzzy match to recognize minor input errors.
     * Respects the enabled/disabled state of abbreviation sources.
     */
    public boolean isKnownName(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }

        return get(journalName).isPresent();
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
                .filter(abbreviation -> isSourceEnabled(abbreviationSources.getOrDefault(abbreviation, BUILTIN_LIST_ID)))
                .filter(abbreviation -> isMatched(journal, abbreviation))
                .findFirst();
                
        if (customAbbreviation.isPresent()) {
            return customAbbreviation;
        }

        if (!isSourceEnabled(BUILTIN_LIST_ID)) {
            return Optional.empty();
        }
        
        Optional<Abbreviation> builtInAbbreviation = Optional.empty();
        
        if (isSourceEnabled(BUILTIN_LIST_ID)) {
            builtInAbbreviation = Optional.ofNullable(fullToAbbreviationObject.get(journal))
                    .or(() -> Optional.ofNullable(abbreviationToAbbreviationObject.get(journal)))
                    .or(() -> Optional.ofNullable(dotlessToAbbreviationObject.get(journal)))
                    .or(() -> Optional.ofNullable(shortestUniqueToAbbreviationObject.get(journal)));
        }

        if (builtInAbbreviation.isPresent()) {
            return builtInAbbreviation;
        }

        return findAbbreviationFuzzyMatched(journal);
    }

    private Optional<Abbreviation> findAbbreviationFuzzyMatched(String input) {
        Collection<Abbreviation> enabledCustomAbbreviations = customAbbreviations.stream()
                .filter(abbreviation -> isSourceEnabled(abbreviationSources.getOrDefault(abbreviation, BUILTIN_LIST_ID)))
                .collect(Collectors.toList());
                
        Optional<Abbreviation> customMatch = findBestFuzzyMatched(enabledCustomAbbreviations, input);
        if (customMatch.isPresent()) {
            return customMatch;
        }

        if (!isSourceEnabled(BUILTIN_LIST_ID)) {
            return Optional.empty();
        }
        
        return findBestFuzzyMatched(fullToAbbreviationObject.values(), input);
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

    /**
     * Adds a custom abbreviation to the repository
     * 
     * @param abbreviation The abbreviation to add
     */
    public void addCustomAbbreviation(Abbreviation abbreviation) {
        Objects.requireNonNull(abbreviation);

        // We do NOT want to keep duplicates
        // The set automatically "removes" duplicates
        // What is a duplicate? An abbreviation is NOT the same if any field is NOT equal (e.g., if the shortest unique differs, the abbreviation is NOT the same)
        customAbbreviations.add(abbreviation);
        
        abbreviationSources.put(abbreviation, BUILTIN_LIST_ID);
    }

    /**
     * Adds a custom abbreviation to the repository with source tracking
     * 
     * @param abbreviation The abbreviation to add
     * @param sourcePath The path or identifier of the source
     * @param enabled Whether the source is enabled
     */
    public void addCustomAbbreviation(Abbreviation abbreviation, String sourcePath, boolean enabled) {
        Objects.requireNonNull(abbreviation);
        Objects.requireNonNull(sourcePath);
        
        customAbbreviations.add(abbreviation);
        abbreviationSources.put(abbreviation, sourcePath);
        
        enabledSources.put(sourcePath, enabled);
    }

    public Collection<Abbreviation> getCustomAbbreviations() {
        return customAbbreviations;
    }

    /**
     * Adds multiple custom abbreviations to the repository
     * 
     * @param abbreviationsToAdd The abbreviations to add
     */
    public void addCustomAbbreviations(Collection<Abbreviation> abbreviationsToAdd) {
        abbreviationsToAdd.forEach(this::addCustomAbbreviation);
    }
    
    /**
     * Adds abbreviations with a specific source key and enabled state
     * 
     * @param abbreviationsToAdd Collection of abbreviations to add
     * @param sourceKey The key identifying the source of these abbreviations
     * @param enabled Whether the source should be enabled initially
     */
    public void addCustomAbbreviations(Collection<Abbreviation> abbreviationsToAdd, String sourceKey, boolean enabled) {
        enabledSources.put(sourceKey, enabled);
        
        for (Abbreviation abbreviation : abbreviationsToAdd) {
            customAbbreviations.add(abbreviation);
            abbreviationSources.put(abbreviation, sourceKey);
        }
    }
    
    /**
     * Checks if a journal abbreviation source is enabled
     * 
     * @param sourceKey The key identifying the source
     * @return true if the source is enabled or has no explicit state (default is enabled)
     */
    public boolean isSourceEnabled(String sourceKey) {
        return enabledSources.getOrDefault(sourceKey, true);
    }
    
    /**
     * Sets the enabled state for a journal abbreviation source
     * 
     * @param sourceKey The key identifying the source
     * @param enabled Whether the source should be enabled
     */
    public void setSourceEnabled(String sourceKey, boolean enabled) {
        enabledSources.put(sourceKey, enabled);
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
        return fullToAbbreviationObject.keySet();
    }

    public Collection<Abbreviation> getAllLoaded() {
        return fullToAbbreviationObject.values();
    }
}
