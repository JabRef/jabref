package org.jabref.model.openoffice.style;

import java.util.Optional;

import org.jabref.model.openoffice.ootext.OOText;

/**
 * This is what we need for numeric citation markers.
 */
public interface CitationMarkerNumericEntry {

    String getCitationKey();

    /**
     * @return Optional.empty() for unresolved
     */
    Optional<Integer> getNumber();

    Optional<OOText> getPageInfo();
}
