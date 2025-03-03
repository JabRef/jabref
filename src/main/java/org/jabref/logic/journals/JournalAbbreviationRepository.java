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

public class JournalAbbreviationRepository {
    static final Pattern QUESTION_MARK = Pattern.compile("\\?");

    private final MVStore store;
    private MVMap<String, Abbreviation> fullToAbbreviationMap;
    private MVMap<String, Abbreviation> abbreviationToAbbreviationMap;
    private MVMap<String, Abbreviation> dotlessToAbbreviationMap;
    private MVMap<String, Abbreviation> shortestUniqueToAbbreviationMap;

    private final TreeSet<Abbreviation> customAbbreviations = new TreeSet<>();

    public JournalAbbreviationRepository(Path journalList) {
        String journalPath = journalList.toAbsolutePath().toString();
        store = new MVStore.Builder().fileName(journalPath).cacheSize(64).open();

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

    // Keep the store open and reuse the maps for all queries.
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

    public Optional<Abbreviation> get(String input) {
        String journal = input.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");

        Optional<Abbreviation> customAbbreviation = customAbbreviations.stream()
                                                                       .filter(abbreviation -> isMatched(journal, abbreviation))
                                                                       .findFirst();
        if (customAbbreviation.isPresent()) {
            return customAbbreviation;
        }
        return Optional.ofNullable(fullToAbbreviationMap.get(journal))
                       .or(() -> Optional.ofNullable(abbreviationToAbbreviationMap.get(journal)))
                       .or(() -> Optional.ofNullable(dotlessToAbbreviationMap.get(journal)))
                       .or(() -> Optional.ofNullable(shortestUniqueToAbbreviationMap.get(journal)));
    }

    public void addCustomAbbreviation(Abbreviation abbreviation) {
        Objects.requireNonNull(abbreviation);
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

    // Helper methods to match abbreviation variants.
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

    private void openMaps(MVStore store) {
        String fullToAbbreviationMapName = "FullToAbbreviation";
        String abbreviationToAbbreviationMapName = "AbbreviationToAbbreviation";
        String dotlessToAbbreviationMapName = "DotlessToAbbreviation";
        String shortestUniqueToAbbreviationMapName = "ShortestUniqueToAbbreviation";

        fullToAbbreviationMap = store.openMap(fullToAbbreviationMapName);
        abbreviationToAbbreviationMap = store.openMap(abbreviationToAbbreviationMapName);
        dotlessToAbbreviationMap = store.openMap(dotlessToAbbreviationMapName);
        shortestUniqueToAbbreviationMap = store.openMap(shortestUniqueToAbbreviationMapName);
    }
}
