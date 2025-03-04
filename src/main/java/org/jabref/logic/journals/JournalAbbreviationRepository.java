package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

/**
 * A repository for all journal abbreviations, including add and find methods.
 */
public class JournalAbbreviationRepository {
    static final Pattern QUESTION_MARK = Pattern.compile("\\?");

    private static final String FULL_TO_ABBREVIATION_MAP_NAME = "FullToAbbreviation";
    private static final String ABBREVIATION_TO_ABBREVIATION_MAP_NAME = "AbbreviationToAbbreviation";
    private static final String DOTLESS_TO_ABBREVIATION_MAP_NAME = "DotlessToAbbreviation";
    private static final String SHORTEST_UNIQUE_TO_ABBREVIATION_MAP_NAME = "ShortestUniqueToAbbreviation";

    private final MVStore store;
    private MVMap<String, Abbreviation> fullToAbbreviationMap;
    private MVMap<String, Abbreviation> abbreviationToAbbreviationMap;
    private MVMap<String, Abbreviation> dotlessToAbbreviationMap;
    private MVMap<String, Abbreviation> shortestUniqueToAbbreviationMap;

    private final TreeSet<Abbreviation> customAbbreviations = new TreeSet<>();

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
        abbreviationToAbbreviationMap.put("Demo", newAbbreviation);
        dotlessToAbbreviationMap.put("Demo", newAbbreviation);
        shortestUniqueToAbbreviationMap.put("Dem", newAbbreviation);
    }

    private static boolean isMatched(String name, Abbreviation abbreviation) {
        return name.equalsIgnoreCase(abbreviation.getName())
                || name.equalsIgnoreCase(abbreviation.getAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getDotlessAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getShortestUniqueAbbreviation());
    }

    private static boolean isMatchedAbbreviated(String name, Abbreviation abbreviation) {
        if (name.equalsIgnoreCase(abbreviation.getName())) {
            return false;
        }
        return name.equalsIgnoreCase(abbreviation.getAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getDotlessAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getShortestUniqueAbbreviation());
    }

    /**
     * Returns true if the given journal name is contained in the list either in its full form
     * (e.g., Physical Review Letters) or its abbreviated form (e.g., Phys. Rev. Lett.).
     */
    public boolean isKnownName(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }
        String journal = journalName.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");

        return customAbbreviations.stream().anyMatch(abbreviation -> isMatched(journal, abbreviation))
                || fullToAbbreviationMap.containsKey(journal)
                || abbreviationToAbbreviationMap.containsKey(journal)
                || dotlessToAbbreviationMap.containsKey(journal)
                || shortestUniqueToAbbreviationMap.containsKey(journal);
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
                || abbreviationToAbbreviationMap.containsKey(journal)
                || dotlessToAbbreviationMap.containsKey(journal)
                || shortestUniqueToAbbreviationMap.containsKey(journal);
    }

    /**
     * Attempts to get the abbreviation of the journal given.
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

        Abbreviation abbr = Optional.ofNullable(fullToAbbreviationMap.get(journal))
                                    .or(() -> Optional.ofNullable(abbreviationToAbbreviationMap.get(journal)))
                                    .or(() -> Optional.ofNullable(dotlessToAbbreviationMap.get(journal)))
                                    .or(() -> Optional.ofNullable(shortestUniqueToAbbreviationMap.get(journal)))
                                    .orElse(null);
        if (abbr != null) {
            // Recreate the Abbreviation so that the dotless field is derived from the abbreviation.
            abbr = new Abbreviation(abbr.getName(), abbr.getAbbreviation(), abbr.getShortestUniqueAbbreviation());
        }
        return Optional.ofNullable(abbr);
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
        abbreviationToAbbreviationMap = store.openMap(ABBREVIATION_TO_ABBREVIATION_MAP_NAME);
        dotlessToAbbreviationMap = store.openMap(DOTLESS_TO_ABBREVIATION_MAP_NAME);
        shortestUniqueToAbbreviationMap = store.openMap(SHORTEST_UNIQUE_TO_ABBREVIATION_MAP_NAME);
    }
}
