package org.jabref.model.openoffice.style;

import java.util.Optional;

import org.jabref.model.openoffice.ootext.OOText;

/**
 * This is what we need for createCitationMarker to produce author-year citation markers.
 */
public interface CitationMarkerEntry extends CitationMarkerNormEntry {

    /**
     * uniqueLetter or Optional.empty() if not needed.
     */
    Optional<String> getUniqueLetter();

    /**
     * pageInfo for this citation, provided by the user.
     *          May be empty, for none.
     */
    Optional<OOText> getPageInfo();

    /**
     *  @return true if this citation is the first appearance of the source cited. Some styles use
     *               different limit on the number of authors shown in this case.
     */
    boolean getIsFirstAppearanceOfSource();
}
