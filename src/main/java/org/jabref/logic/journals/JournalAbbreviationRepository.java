package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private static final String ABBREVIATION_TO_ABBREVIATION_MAP_NAME = "AbbreviationToName";
    private static final String DOTLESS_TO_ABBREVIATION_MAP_NAME = "DotlessToName";
    private static final String SHORTEST_UNIQUE_TO_ABBREVIATION_MAP_NAME = "ShortestUniqueToName";

    private final MVStore store;
    private MVMap<String, Abbreviation> fullToAbbreviationMap;
    private MVMap<String, String> abbreviationToNameMap;
    private MVMap<String, String> dotlessToNameMap;
    private MVMap<String, String> shortestUniqueToNameMap;

    private final TreeSet<Abbreviation> customAbbreviations = new TreeSet<>();

    /**
     * Initializes the internal data based on the abbreviations found in the given MV file
     */
    public JournalAbbreviationRepository(Path journalList) {
        String journalPath = journalList.toAbsolutePath().toString();
        store = new MVStore.Builder().fileName(journalPath).cacheSize(128).open();

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
                || abbreviationToNameMap.containsKey(journal)
                || dotlessToNameMap.containsKey(journal)
                || shortestUniqueToNameMap.containsKey(journal);
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
        String name = journal;
        Abbreviation abbr = fullToAbbreviationMap.get(journal);

        if (abbr == null) {
            name = abbreviationToNameMap.get(journal);
            if (name == null) {
                name = dotlessToNameMap.get(journal);
            }
            if (name == null) {
                name = shortestUniqueToNameMap.get(journal);
            }
            if (name != null) {
                abbr = fullToAbbreviationMap.get(name);
            }
        }

        if (abbr != null) {
            // Recreate the Abbreviation so that the transient fields (like name) are recalculated.
            abbr = new Abbreviation(name, abbr.getAbbreviation(), abbr.getShortestUniqueAbbreviation());
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
        List<Abbreviation> values = new ArrayList<>();
        fullToAbbreviationMap.forEach((name, abbr) -> {
            String abbreviationString = abbr.getAbbreviation();
            String shortestUniqueAbbreviation = abbr.getShortestUniqueAbbreviation();
            Abbreviation newAbbreviation = new Abbreviation(
                    name,
                    abbreviationString,
                    shortestUniqueAbbreviation
            );
            values.add(newAbbreviation);
        });
        return values;
    }

    private void openMaps(MVStore store) {
        fullToAbbreviationMap = store.openMap(FULL_TO_ABBREVIATION_MAP_NAME);
        abbreviationToNameMap = store.openMap(ABBREVIATION_TO_ABBREVIATION_MAP_NAME);
        dotlessToNameMap = store.openMap(DOTLESS_TO_ABBREVIATION_MAP_NAME);
        shortestUniqueToNameMap = store.openMap(SHORTEST_UNIQUE_TO_ABBREVIATION_MAP_NAME);
    }
}
