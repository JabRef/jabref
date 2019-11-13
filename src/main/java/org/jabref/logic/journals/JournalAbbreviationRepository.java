package org.jabref.logic.journals;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A repository for all journal abbreviations, including add and find methods.
 */
public class JournalAbbreviationRepository {

    // We have over 15.000 abbreviations in the built-in lists
    private final Set<Abbreviation> abbreviations = new HashSet<>(16000);

    public JournalAbbreviationRepository(Abbreviation... abbreviations) {
        for (Abbreviation abbreviation : abbreviations) {
            addEntry(abbreviation);
        }
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

    public int size() {
        return abbreviations.size();
    }

    /**
     * Returns true if the given journal name is contained in the list either in its full form (e.g Physical Review
     * Letters) or its abbreviated form (e.g. Phys. Rev. Lett.).
     */
    public boolean isKnownName(String journalName) {
        return abbreviations.stream().anyMatch(abbreviation -> isMatched(journalName.trim(), abbreviation));
    }

    /**
     * Returns true if the given journal name is in its abbreviated form (e.g. Phys. Rev. Lett.). The test is strict,
     * i.e. journals whose abbreviation is the same as the full name are not considered
     */
    public boolean isAbbreviatedName(String journalName) {
        return abbreviations.stream().anyMatch(abbreviation -> isMatchedAbbreviated(journalName.trim(), abbreviation));
    }

    /**
     * Attempts to get the abbreviated name of the journal given. May contain dots.
     *
     * @param journalName The journal name to abbreviate.
     * @return The abbreviated name
     */
    public Optional<Abbreviation> getAbbreviation(String journalName) {
        return abbreviations.stream().filter(abbreviation -> isMatched(journalName.trim(), abbreviation)).findFirst();
    }

    public void addEntry(Abbreviation abbreviation) {
        Objects.requireNonNull(abbreviation);

        // Abbreviation equality is tested on name only, so we might have to remove an old abbreviation
        abbreviations.remove(abbreviation);
        abbreviations.add(abbreviation);
    }

    public void addEntries(Collection<Abbreviation> abbreviationsToAdd) {
        abbreviationsToAdd.forEach(this::addEntry);
    }

    public Set<Abbreviation> getAbbreviations() {
        return Collections.unmodifiableSet(abbreviations);
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
