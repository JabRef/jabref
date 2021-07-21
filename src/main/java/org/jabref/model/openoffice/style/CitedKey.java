package org.jabref.model.openoffice.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.ootext.OOText;

/**
 * Cited keys are collected from the citations in citation groups.
 *
 * They contain backreferences to the corresponding citations in {@code where}. This allows the
 * extra information generated using CitedKeys to be distributed back to the in-text citations.
 */
public class CitedKey implements
                      ComparableCitedKey,
                      CitationMarkerNormEntry,
                      CitationMarkerNumericBibEntry {

    public final String citationKey;
    private final List<CitationPath> where;

    private Optional<CitationLookupResult> db;
    private Optional<Integer> number; // For Numbered citation styles.
    private Optional<String> uniqueLetter; // For AuthorYear citation styles.
    private Optional<OOText> normCitMarker;  // For AuthorYear citation styles.

    CitedKey(String citationKey, CitationPath path, Citation cit) {

        this.citationKey = citationKey;
        this.where = new ArrayList<>(); // remember order
        this.where.add(path);

        // synchronized with Citation
        this.db = cit.getLookupResult();
        this.number = cit.getNumber();
        this.uniqueLetter = cit.getUniqueLetter();

        // CitedKey only
        this.normCitMarker = Optional.empty();
    }

    /*
     * Implement ComparableCitedKey
     */
    @Override
    public String getCitationKey() {
        return citationKey;
    }

    @Override
    public Optional<BibEntry> getBibEntry() {
        return db.map(e -> e.entry);
    }

    /*
     * Implement CitationMarkerNormEntry
     */
    @Override
    public Optional<CitationLookupResult> getLookupResult() {
        return db;
    }

    /*
     * Implement CitationMarkerNumericBibEntry
     */
    @Override
    public Optional<Integer> getNumber() {
        return number;
    }

    public void setNumber(Optional<Integer> number) {
        this.number = number;
    }

    public List<CitationPath> getCitationPaths() {
        return new ArrayList<>(where);
    }

    public Optional<String> getUniqueLetter() {
        return uniqueLetter;
    }

    public void setUniqueLetter(Optional<String> uniqueLetter) {
        this.uniqueLetter = uniqueLetter;
    }

    public Optional<OOText> getNormalizedCitationMarker() {
        return normCitMarker;
    }

    public void setNormalizedCitationMarker(Optional<OOText> normCitMarker) {
        this.normCitMarker = normCitMarker;
    }

    /**
     * Appends to end of {@code where}
     */
    void addPath(CitationPath path, Citation cit) {
        this.where.add(path);

        // Check consistency
        if (!cit.getLookupResult().equals(this.db)) {
            throw new IllegalStateException("CitedKey.addPath: mismatch on cit.db");
        }
        if (!cit.getNumber().equals(this.number)) {
            throw new IllegalStateException("CitedKey.addPath: mismatch on cit.number");
        }
        if (!cit.getUniqueLetter().equals(this.uniqueLetter)) {
            throw new IllegalStateException("CitedKey.addPath: mismatch on cit.uniqueLetter");
        }
    }

    /*
     * Lookup
     */
    void lookupInDatabases(List<BibDatabase> databases) {
        this.db = Citation.lookup(databases, this.citationKey);
    }

    void distributeLookupResult(CitationGroups cgs) {
        cgs.distributeToCitations(where, Citation::setLookupResult, db);
    }

    /*
     * Make unique using a letter or by numbering
     */

    void distributeNumber(CitationGroups cgs) {
        cgs.distributeToCitations(where, Citation::setNumber, number);
    }

    void distributeUniqueLetter(CitationGroups cgs) {
        cgs.distributeToCitations(where, Citation::setUniqueLetter, uniqueLetter);
    }
}
