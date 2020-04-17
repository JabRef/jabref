package org.jabref.logic.journals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

/**
 * A repository for all journal abbreviations, including add and find methods.
 */
public class JournalAbbreviationRepository {

    private final MVMap<String, String> fullToAbbreviation;
    private final MVMap<String, String> abbreviationToFull;
    private final List<Abbreviation> customAbbreviations;

    public JournalAbbreviationRepository() {
        MVStore store = new MVStore.Builder().readOnly().fileName("journalList.mv").open();
        this.fullToAbbreviation = store.openMap("FullToAbbreviation");
        this.abbreviationToFull = store.openMap("AbbreviationToFull");
        this.customAbbreviations = new ArrayList<>();
    }

    private static boolean isMatched(String name, Abbreviation abbreviation) {
        return name.equalsIgnoreCase(abbreviation.getName())
                || name.equalsIgnoreCase(abbreviation.getAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getMedlineAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getShortestUniqueAbbreviation());
    }

    private static boolean isMatchedAbbreviated(String name, Abbreviation abbreviation) {
        boolean isAbbreviated = name.equalsIgnoreCase(abbreviation.getAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getMedlineAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getShortestUniqueAbbreviation());
        boolean isExpanded = name.equalsIgnoreCase(abbreviation.getName());
        return isAbbreviated && !isExpanded;
    }

    /**
     * Returns true if the given journal name is contained in the list either in its full form (e.g Physical Review
     * Letters) or its abbreviated form (e.g. Phys. Rev. Lett.).
     */
    public boolean isKnownName(String journalName) {
        String journal = journalName.trim();

        boolean isKnown = customAbbreviations.stream().anyMatch(abbreviation -> isMatched(journal, abbreviation));
        if (isKnown) {
            return true;
        }

        return fullToAbbreviation.containsKey(journal) || abbreviationToFull.containsKey(journal);
    }

    /**
     * Returns true if the given journal name is in its abbreviated form (e.g. Phys. Rev. Lett.). The test is strict,
     * i.e. journals whose abbreviation is the same as the full name are not considered
     */
    public boolean isAbbreviatedName(String journalName) {
        String journal = journalName.trim();

        boolean isAbbreviated = customAbbreviations.stream().anyMatch(abbreviation -> isMatchedAbbreviated(journal, abbreviation));
        if (isAbbreviated) {
            return true;
        }

        return abbreviationToFull.containsKey(journal);
    }

    /**
     * Attempts to get the abbreviated name of the journal given. May contain dots.
     *
     * @param journalName The journal name to abbreviate.
     * @return The abbreviated name
     */
    public Optional<Abbreviation> getAbbreviation(String journalName) {
        String journal = journalName.trim();

        Optional<Abbreviation> customAbbreviation = customAbbreviations.stream()
                                                                       .filter(abbreviation -> isMatched(journal, abbreviation))
                                                                       .findAny();
        if (customAbbreviation.isPresent()) {
            return customAbbreviation;
        }

        return Optional.ofNullable(fullToAbbreviation.get(journal))
                       .map(abbreviation -> new Abbreviation(journal, abbreviation));
    }

    public void addCustomAbbreviation(Abbreviation abbreviation) {
        Objects.requireNonNull(abbreviation);

        // We do not want to keep duplicates, thus remove the old abbreviation
        customAbbreviations.remove(abbreviation);
        customAbbreviations.add(abbreviation);
    }

    public List<Abbreviation> getCustomAbbreviations() {
        return customAbbreviations;
    }

    public void addCustomAbbreviations(Collection<Abbreviation> abbreviationsToAdd) {
        abbreviationsToAdd.forEach(this::addCustomAbbreviation);
    }

    public Optional<String> getNextAbbreviation(String text) {
        return getAbbreviation(text).map(abbreviation -> abbreviation.getNext(text));
    }

    public Optional<String> getDefaultAbbreviation(String text) {
        return getAbbreviation(text).map(Abbreviation::getAbbreviation);
    }

    public Optional<String> getMedlineAbbreviation(String text) {
        return getAbbreviation(text).map(Abbreviation::getMedlineAbbreviation);
    }

    public Optional<String> getShortestUniqueAbbreviation(String text) {
        return getAbbreviation(text).map(Abbreviation::getShortestUniqueAbbreviation);
    }
}
