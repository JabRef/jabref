package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XFootnote;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CitedKey implements CitationSort.ComparableCitation {
    String citationKey;
    LinkedHashSet<CitationPath> where;
    Optional<CitationDatabaseLookup.Result> db;
    Optional<Integer> number; // For Numbered citation styles.
    Optional<String> uniqueLetter; // For AuthorYear citation styles.
    Optional<String> normCitMarker;  // For AuthorYear citation styles.

    CitedKey(String citationKey, CitationPath p, Citation cit) {
        this.citationKey = citationKey;
        this.where = new LinkedHashSet<>(); // remember order
        this.where.add(p);
        this.db = cit.db;
        this.number = cit.number;
        this.uniqueLetter = cit.uniqueLetter;
        this.normCitMarker = Optional.empty();
    }

    @Override
    public String getCitationKey(){
        return citationKey;
    }

    @Override
    public Optional<BibEntry> getBibEntry(){
        return (db.isPresent()
                ? Optional.of(db.get().entry)
                : Optional.empty());
    }

    /** No pageInfo is needed for sorting the bibliography,
     *  getPageInfoOrNull always returns null. Only exists to implement ComparableCitation.
     *
     *  @return null
     */
    @Override
    public String getPageInfoOrNull(){
        return null;
    }

    /**
     * Appends to end of {@code where}
     */
    void addPath(CitationPath p, Citation cit) {
        this.where.add(p);
        if (cit.db != this.db) {
            throw new RuntimeException("CitedKey.addPath: mismatch on cit.db");
        }
        if (cit.number != this.number) {
            throw new RuntimeException("CitedKey.addPath: mismatch on cit.number");
        }
        if (cit.uniqueLetter != this.uniqueLetter) {
            throw new RuntimeException("CitedKey.addPath: mismatch on cit.uniqueLetter");
        }
    }

    void lookupInDatabases(List<BibDatabase> databases) {
        this.db = CitationDatabaseLookup.lookup(databases, this.citationKey);
    }

    void distributeDatabaseLookupResult(CitationGroups cgs) {
        cgs.setDatabaseLookupResults(where, db);
    }

    void distributeNumber(CitationGroups cgs) {
        cgs.setNumbers(where, number);
    }

    void distributeUniqueLetter(CitationGroups cgs) {
        cgs.setUniqueLetters(where, uniqueLetter);
    }
} // class CitedKey
