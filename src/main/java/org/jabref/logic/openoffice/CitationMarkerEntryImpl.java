package org.jabref.logic.openoffice;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

/**
 * Implement CitationMarkerEntry by containing the data needed.
 *
 * {@see CitationMarkerEntry} for description.
 *
 */
public class CitationMarkerEntryImpl implements CitationMarkerEntry {
    final String citationKey;
    final BibEntry bibEntry;
    final BibDatabase database;
    final String uniqueLetter;
    final String pageInfo;
    final boolean isFirstAppearanceOfSource;

    public CitationMarkerEntryImpl(String citationKey,
                                   BibEntry bibEntry,
                                   BibDatabase database,
                                   String uniqueLetter,
                                   String pageInfo,
                                   boolean isFirstAppearanceOfSource) {
        this.citationKey = citationKey;

        if (bibEntry == null && database != null) {
            throw new RuntimeException("CitationMarkerEntryImpl:"
                                       + " bibEntry == null, but database != null");
        }
        if (bibEntry != null && database == null) {
            throw new RuntimeException("CitationMarkerEntryImpl:"
                                       + " bibEntry != null, but database == null");
        }

        this.bibEntry = bibEntry;
        this.database = database;
        this.uniqueLetter = uniqueLetter;
        this.pageInfo = pageInfo;
        this.isFirstAppearanceOfSource = isFirstAppearanceOfSource;
    }

    @Override
    public String getCitationKey() {
        return citationKey;
    }

    @Override
    public BibEntry getBibEntryOrNull() {
        return bibEntry;
    }

    @Override
    public BibDatabase getDatabaseOrNull() {
        return database;
    }

    @Override
    public String getUniqueLetterOrNull() {
        return uniqueLetter;
    }

    @Override
    public String getPageInfoOrNull() {
        return pageInfo;
    }

    @Override
    public boolean getIsFirstAppearanceOfSource() {
        return isFirstAppearanceOfSource;
    }
}
