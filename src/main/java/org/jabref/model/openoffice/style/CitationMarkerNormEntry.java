package org.jabref.model.openoffice.style;

import java.util.Optional;

/**
 * This is what we need to produce normalized author-year citation markers.
 */
public interface CitationMarkerNormEntry {

    /** Citation key. This is what we usually get from the document.
     *
     *  Used if getLookupResult() returns empty, which indicates failure to lookup in the databases.
     */
    String getCitationKey();

    /** Result of looking up citation key in databases.
     *
     * Optional.empty() indicates unresolved citation.
     */
    Optional<CitationLookupResult> getLookupResult();
}
