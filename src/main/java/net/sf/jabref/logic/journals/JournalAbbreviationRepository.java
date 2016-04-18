/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.journals;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

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
        abbreviationsToAdd.forEach(abbreviation -> addEntry(abbreviation));
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
