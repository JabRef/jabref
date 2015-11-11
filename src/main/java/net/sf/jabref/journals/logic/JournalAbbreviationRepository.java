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
package net.sf.jabref.journals.logic;

import com.google.common.base.Optional;

import net.sf.jabref.Globals;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A repository for all journal abbreviations, including add and find methods.
 */
public class JournalAbbreviationRepository {

    private final HashMap<String, Abbreviation> fullNameLowerCase2Abbreviation = new HashMap<String, Abbreviation>();
    private final HashMap<String, Abbreviation> isoLowerCase2Abbreviation = new HashMap<String, Abbreviation>();
    private final HashMap<String, Abbreviation> medlineLowerCase2Abbreviation = new HashMap<String, Abbreviation>();

    private final SortedSet<Abbreviation> abbreviations = new TreeSet<Abbreviation>();

    public void readJournalListFromResource(String resource) {
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromResource(checkNotNull(resource));
        for (Abbreviation abbreviation : parser.getAbbreviations()) {
            addEntry(abbreviation);
        }
    }

    public void readJournalListFromFile(File file) throws FileNotFoundException {
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(checkNotNull(file));
        for (Abbreviation abbreviation : parser.getAbbreviations()) {
            addEntry(abbreviation);
        }
    }

    public int size() {
        return abbreviations.size();
    }

    public boolean isKnownName(String journalName) {
        String s = checkNotNull(journalName).trim().toLowerCase();
        return fullNameLowerCase2Abbreviation.get(s) != null
                || isoLowerCase2Abbreviation.get(s) != null
                || medlineLowerCase2Abbreviation.get(s) != null;
    }

    public boolean isAbbreviatedName(String journalName) {
        String s = checkNotNull(journalName).trim().toLowerCase();
        return isoLowerCase2Abbreviation.get(s) != null
                || medlineLowerCase2Abbreviation.get(s) != null;
    }

    /**
     * Attempts to get the abbreviated name of the journal given. May contain dots.
     *
     * @param journalName The journal name to abbreviate.
     * @return The abbreviated name
     */
    public Optional<Abbreviation> getAbbreviation(String journalName) {
        String s = checkNotNull(journalName).toLowerCase().trim();

        if (fullNameLowerCase2Abbreviation.containsKey(s)) {
            return Optional.of(fullNameLowerCase2Abbreviation.get(s));
        } else if (isoLowerCase2Abbreviation.containsKey(s)) {
            return Optional.of(isoLowerCase2Abbreviation.get(s));
        } else if (medlineLowerCase2Abbreviation.containsKey(s)) {
            return Optional.of(medlineLowerCase2Abbreviation.get(s));
        } else {
            return Optional.absent();
        }
    }

    public void addEntry(Abbreviation abbreviation) {
        checkNotNull(abbreviation);

        if (isKnownName(abbreviation.getName())) {
            Abbreviation previous = getAbbreviation(abbreviation.getName()).get();
            abbreviations.remove(previous);
            // TODO logging strategy required
            // System.out.println(Globals.lang("Duplicate Journal Abbreviation - old one will be overwritten by new one\nOLD: %0\nNEW: %1", previous.toString(), abbreviation.toString()));
        }

        abbreviations.add(abbreviation);

        fullNameLowerCase2Abbreviation.put(abbreviation.getName().toLowerCase(), abbreviation);
        isoLowerCase2Abbreviation.put(abbreviation.getIsoAbbreviation().toLowerCase(), abbreviation);
        medlineLowerCase2Abbreviation.put(abbreviation.getMedlineAbbreviation().toLowerCase(), abbreviation);
    }

    public SortedSet<Abbreviation> getAbbreviations() {
        return Collections.unmodifiableSortedSet(abbreviations);
    }

    public Optional<String> getNextAbbreviation(String text) {
        Optional<Abbreviation> abbreviation = getAbbreviation(text);

        if (!abbreviation.isPresent()) {
            return Optional.absent();
        }

        Abbreviation abbr = abbreviation.get();
        return Optional.of(abbr.getNext(text));
    }

    public Optional<String> getMedlineAbbreviation(String text) {
        Optional<Abbreviation> abbreviation = getAbbreviation(text);

        if (!abbreviation.isPresent()) {
            return Optional.absent();
        }

        Abbreviation abbr = abbreviation.get();
        return Optional.of(abbr.getMedlineAbbreviation());
    }

    public Optional<String> getIsoAbbreviation(String text) {
        Optional<Abbreviation> abbreviation = getAbbreviation(text);

        if (!abbreviation.isPresent()) {
            return Optional.absent();
        }

        Abbreviation abbr = abbreviation.get();
        return Optional.of(abbr.getIsoAbbreviation());
    }

    public String toPropertiesString() {
        StringBuilder sb = new StringBuilder();
        for (Abbreviation abbreviation : getAbbreviations()) {
            sb.append(String.format("%s%n", abbreviation.toPropertiesLine()));
        }
        return sb.toString();
    }
}
