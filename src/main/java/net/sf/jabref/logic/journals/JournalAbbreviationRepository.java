package net.sf.jabref.logic.journals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A repository for all journal abbreviations, including add and find methods.
 */
public class JournalAbbreviationRepository {

    private final Map<String, Abbreviation> fullNameLowerCase2Abbreviation = new HashMap<>();
    private final Map<String, Abbreviation> isoLowerCase2Abbreviation = new HashMap<>();
    private final Map<String, Abbreviation> medlineLowerCase2Abbreviation = new HashMap<>();

    private final SortedSet<Abbreviation> abbreviations = new TreeSet<>();

    private static final Log LOGGER = LogFactory.getLog(JournalAbbreviationRepository.class);

    public int size() {
        return abbreviations.size();
    }

    public boolean isKnownName(String journalName) {
        String nameKey = Objects.requireNonNull(journalName).trim().toLowerCase(Locale.ENGLISH);
        return (fullNameLowerCase2Abbreviation.containsKey(nameKey)) || (isoLowerCase2Abbreviation.containsKey(nameKey))
                || (medlineLowerCase2Abbreviation.containsKey(nameKey));
    }

    public boolean isAbbreviatedName(String journalName) {
        String nameKey = Objects.requireNonNull(journalName).trim().toLowerCase(Locale.ENGLISH);
        return (isoLowerCase2Abbreviation.containsKey(nameKey)) || (medlineLowerCase2Abbreviation.containsKey(nameKey));
    }

    /**
     * Attempts to get the abbreviated name of the journal given. May contain dots.
     *
     * @param journalName The journal name to abbreviate.
     * @return The abbreviated name
     */
    public Optional<Abbreviation> getAbbreviation(String journalName) {
        String nameKey = Objects.requireNonNull(journalName).toLowerCase(Locale.ENGLISH).trim();

        if (fullNameLowerCase2Abbreviation.containsKey(nameKey)) {
            return Optional.of(fullNameLowerCase2Abbreviation.get(nameKey));
        } else if (isoLowerCase2Abbreviation.containsKey(nameKey)) {
            return Optional.of(isoLowerCase2Abbreviation.get(nameKey));
        } else if (medlineLowerCase2Abbreviation.containsKey(nameKey)) {
            return Optional.of(medlineLowerCase2Abbreviation.get(nameKey));
        } else {
            return Optional.empty();
        }
    }

    public void addEntry(Abbreviation abbreviation) {
        Objects.requireNonNull(abbreviation);

        if (isKnownName(abbreviation.getName())) {
            Abbreviation previous = getAbbreviation(abbreviation.getName()).get();
            abbreviations.remove(previous);
            LOGGER.info("Duplicate journal abbreviation - old one will be overwritten by new one\nOLD: "
                    + previous + "\nNEW: " + abbreviation);
        }

        abbreviations.add(abbreviation);

        fullNameLowerCase2Abbreviation.put(abbreviation.getName().toLowerCase(Locale.ENGLISH), abbreviation);
        isoLowerCase2Abbreviation.put(abbreviation.getIsoAbbreviation().toLowerCase(Locale.ENGLISH), abbreviation);
        medlineLowerCase2Abbreviation.put(abbreviation.getMedlineAbbreviation().toLowerCase(Locale.ENGLISH),
                abbreviation);
    }

    public void addEntries(List<Abbreviation> abbreviationsToAdd) {
        abbreviationsToAdd.forEach(this::addEntry);
    }

    public SortedSet<Abbreviation> getAbbreviations() {
        return Collections.unmodifiableSortedSet(abbreviations);
    }

    public Optional<String> getNextAbbreviation(String text) {
        Optional<Abbreviation> abbreviation = getAbbreviation(text);

        if (!abbreviation.isPresent()) {
            return Optional.empty();
        }

        Abbreviation abbr = abbreviation.get();
        return Optional.of(abbr.getNext(text));
    }

    public Optional<String> getMedlineAbbreviation(String text) {
        Optional<Abbreviation> abbreviation = getAbbreviation(text);

        if (!abbreviation.isPresent()) {
            return Optional.empty();
        }

        Abbreviation abbr = abbreviation.get();
        return Optional.of(abbr.getMedlineAbbreviation());
    }

    public Optional<String> getIsoAbbreviation(String text) {
        Optional<Abbreviation> abbreviation = getAbbreviation(text);

        if (!abbreviation.isPresent()) {
            return Optional.empty();
        }

        Abbreviation abbr = abbreviation.get();
        return Optional.of(abbr.getIsoAbbreviation());
    }
}
