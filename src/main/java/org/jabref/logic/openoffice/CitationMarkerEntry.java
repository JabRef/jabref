package org.jabref.logic.openoffice;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

/**
 * This is what we need for getCitationMarker to produce author-year
 * citation markers.
 *
 * Details of BibEntry are accessed via getBibEntryOrNull()
 *
 * org/jabref/gui/openoffice.Citation and org/jabref/gui/openoffice.CitedKey
 *
 * are used as actual sources.
 *
 * Citation misses two things
 *   - isFirstAppearanceOfSource : could be extended to provide this.
 *   - pageInfo under DataModel JabRef52 needs CitationGroup
 *
 * CitedKey is used for creating normalizedCitationMarker, so we do
 * not need pageInfo, uniqueLetter and isFirstAppearanceOfSource.
 *
 */
public interface CitationMarkerEntry {

    /** Citation key. This is what we usually get from the document.
     *
     *  Used if getBibEntryOrNull() and/or getDatabaseOrNull() returns
     *  null, which indicates failure to lookup in the databases.
     *  The marker created is "Unresolved({citationKey})".
     *
     */
    String getCitationKey();

    /** Bibliography entry looked up from databases.
     *
     * May be null if not found. In this case getDatabaseOrNull()
     * should also return null.
     */
    BibEntry getBibEntryOrNull();

    /**
     * The database where BibEntry was found.
     * May be null, if not found (otherwise not).
     */
    BibDatabase getDatabaseOrNull();

    /**
     * uniqueLetter or null if not needed.
     */
    String getUniqueLetterOrNull();

    /**
     * pageInfo for this citation, provided by the user.
     * May be null, for none.
     */
    String getPageInfoOrNull();

    /**
     *  @return true if this citation is the first appearance of the
     *  source cited. Some styles use different limit on the number of
     *  authors shown in this case.
     */
    boolean getIsFirstAppearanceOfSource();
}
