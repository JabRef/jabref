package org.jabref.model.openoffice.style;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.util.OOPair;

public class Citation implements ComparableCitation, CitationMarkerEntry, CitationMarkerNumericEntry {

    /** key in database */
    public final String citationKey;

    /** Result from database lookup. Optional.empty() if not found. */
    private Optional<CitationLookupResult> db;

    /** The number used for numbered citation styles . */
    private Optional<Integer> number;

    /** Letter that makes the in-text citation unique. */
    private Optional<String> uniqueLetter;

    /** pageInfo */
    private Optional<OOText> pageInfo;

    /** isFirstAppearanceOfSource */
    private boolean isFirstAppearanceOfSource;

    /**
     *
     */
    public Citation(String citationKey) {
        this.citationKey = citationKey;
        this.db = Optional.empty();
        this.number = Optional.empty();
        this.uniqueLetter = Optional.empty();
        this.pageInfo = Optional.empty();
        this.isFirstAppearanceOfSource = false;
    }

    @Override
    public String getCitationKey() {
        return citationKey;
    }

    @Override
    public Optional<OOText> getPageInfo() {
        return pageInfo;
    }

    @Override
    public boolean getIsFirstAppearanceOfSource() {
        return isFirstAppearanceOfSource;
    }

    @Override
    public Optional<BibEntry> getBibEntry() {
        return (db.isPresent()
                ? Optional.of(db.get().entry)
                : Optional.empty());
    }

    public static Optional<CitationLookupResult> lookup(BibDatabase database, String key) {
        return (database
                .getEntryByCitationKey(key)
                .map(bibEntry -> new CitationLookupResult(bibEntry, database)));
    }

    public static Optional<CitationLookupResult> lookup(List<BibDatabase> databases, String key) {
        return (databases.stream()
                .map(database -> Citation.lookup(database, key))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst());
    }

    public void lookupInDatabases(List<BibDatabase> databases) {
        db = Citation.lookup(databases, citationKey);
    }

    public Optional<CitationLookupResult> getLookupResult() {
        return db;
    }

    public void setLookupResult(Optional<CitationLookupResult> db) {
        this.db = db;
    }

    public boolean isUnresolved() {
        return db.isEmpty();
    }

    @Override
    public Optional<Integer> getNumber() {
        return number;
    }

    public void setNumber(Optional<Integer> number) {
        this.number = number;
    }

    public int getNumberOrThrow() {
        return number.get();
    }

    public Optional<String> getUniqueLetter() {
        return uniqueLetter;
    }

    public void setUniqueLetter(Optional<String> uniqueLetter) {
        this.uniqueLetter = uniqueLetter;
    }

    public void setPageInfo(Optional<OOText> pageInfo) {
        Optional<OOText> normalizedPageInfo = PageInfo.normalizePageInfo(pageInfo);
        if (!normalizedPageInfo.equals(pageInfo)) {
            throw new IllegalArgumentException("setPageInfo argument is not normalized");
        }
        this.pageInfo = normalizedPageInfo;
    }

    public void setIsFirstAppearanceOfSource(boolean value) {
        isFirstAppearanceOfSource = value;
    }

    /*
     * Setters for CitationGroups.distribute()
     */
    public static void setLookupResult(OOPair<Citation, Optional<CitationLookupResult>> pair) {
        Citation cit = pair.a;
        cit.db = pair.b;
    }

    public static void setNumber(OOPair<Citation, Optional<Integer>> pair) {
        Citation cit = pair.a;
        cit.number = pair.b;
    }

    public static void setUniqueLetter(OOPair<Citation, Optional<String>> pair) {
        Citation cit = pair.a;
        cit.uniqueLetter = pair.b;
    }

}
