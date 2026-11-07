package org.jabref.model.openoffice.style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.ootext.OOText;

import org.jspecify.annotations.NullMarked;

/// Cited references are collected from the citations in citation groups.
///
/// They contain backreferences to the corresponding citations in `where`. This allows the extra information generated using [CitedReferences] to be distributed back to the in-text citations.
@NullMarked
public class CitedReference implements ComparableCitedReference, CitationMarkerNormEntry, CitationMarkerNumericBibEntry {

    public final String citationKey;
    private final List<CitationPath> where;

    private Optional<CitationLookupResult> citationLookupResult;
    private Optional<Integer> number; // For Numbered citation styles.
    private Optional<String> uniqueLetter; // For AuthorYear citation styles.
    private Optional<OOText> normalizedCitationMarker;  // For AuthorYear citation styles.

    CitedReference(String citationKey, CitationPath path, Citation citation) {
        this.citationKey = citationKey;
        this.where = new ArrayList<>(); // remember order
        this.where.add(path);

        // synchronized with Citation
        this.citationLookupResult = citation.getLookupResult();
        this.number = citation.getNumber();
        this.uniqueLetter = citation.getUniqueLetter();

        // CitedReference only
        this.normalizedCitationMarker = Optional.empty();
    }

    /*
     * Implement ComparableCitedReference
     */
    @Override
    public String getCitationKey() {
        return citationKey;
    }

    @Override
    public Optional<BibEntry> getBibEntry() {
        return citationLookupResult.map(citationLookupResult -> citationLookupResult.entry);
    }

    /*
     * Implement CitationMarkerNormEntry
     */
    @Override
    public Optional<CitationLookupResult> getLookupResult() {
        return citationLookupResult;
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
        return normalizedCitationMarker;
    }

    public void setNormalizedCitationMarker(Optional<OOText> normalizedCitationMarker) {
        this.normalizedCitationMarker = normalizedCitationMarker;
    }

    /// Appends to end of `where`
    void addPath(CitationPath path, Citation citation) {
        this.where.add(path);

        // Check consistency
        if (!citation.getLookupResult().equals(this.citationLookupResult)) {
            throw new IllegalStateException("CitedReference.addPath: mismatch on citation.citationLookupResult");
        }
        if (!citation.getNumber().equals(this.number)) {
            throw new IllegalStateException("CitedReference.addPath: mismatch on citation.number");
        }
        if (!citation.getUniqueLetter().equals(this.uniqueLetter)) {
            throw new IllegalStateException("CitedReference.addPath: mismatch on citation.uniqueLetter");
        }
    }

    /*
     * Lookup
     */
    void lookupInDatabases(List<BibDatabase> databases) {
        this.citationLookupResult = Citation.lookup(databases, this.citationKey);
    }

    void distributeLookupResult(CitationGroups citationGroups) {
        citationGroups.distributeToCitations(where, Citation::setLookupResult, citationLookupResult);
    }

    /*
     * Make unique using a letter or by numbering
     */

    void distributeNumber(CitationGroups citationGroups) {
        citationGroups.distributeToCitations(where, Citation::setNumber, number);
    }

    void distributeUniqueLetter(CitationGroups citationGroups) {
        citationGroups.distributeToCitations(where, Citation::setUniqueLetter, uniqueLetter);
    }
}
