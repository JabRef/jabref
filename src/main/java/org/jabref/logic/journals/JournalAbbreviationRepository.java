package org.jabref.logic.journals;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A repository for all journal abbreviations, including add and find methods.
 */
public class JournalAbbreviationRepository {

    private static final Log LOGGER = LogFactory.getLog(JournalAbbreviationRepository.class);
    private final Set<Abbreviation> abbreviations = new HashSet<>(16000); // We have over 15.000 abbreviations in the built-in lists

    public JournalAbbreviationRepository(Abbreviation... abbreviations) {
        for (Abbreviation abbreviation : abbreviations) {
            addEntry(abbreviation);
        }
    }

    private static boolean isMatched(String name, Abbreviation abbreviation) {
        return name.equalsIgnoreCase(abbreviation.getName())
                || name.equalsIgnoreCase(abbreviation.getIsoAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getMedlineAbbreviation());
    }

    private static boolean isMatchedAbbreviated(String name, Abbreviation abbreviation) {
        return name.equalsIgnoreCase(abbreviation.getIsoAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getMedlineAbbreviation());
    }

    public int size() {
        return abbreviations.size();
    }

    public boolean isKnownName(String journalName) {
        return abbreviations.stream().anyMatch(abbreviation -> isMatched(journalName.trim(), abbreviation));
    }

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

        if (abbreviations.contains(abbreviation)) {
            Abbreviation previous = getAbbreviation(abbreviation.getName()).get();
            abbreviations.remove(previous);
            LOGGER.info("Duplicate journal abbreviation - old one will be overwritten by new one\nOLD: "
                    + previous + "\nNEW: " + abbreviation);
        }

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

    public Optional<String> getMedlineAbbreviation(String text) {
        return getAbbreviation(text).map(Abbreviation::getMedlineAbbreviation);
    }

    public Optional<String> getIsoAbbreviation(String text) {
        return getAbbreviation(text).map(Abbreviation::getIsoAbbreviation);
    }
}
