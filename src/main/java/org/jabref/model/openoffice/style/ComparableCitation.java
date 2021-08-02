package org.jabref.model.openoffice.style;

import java.util.Optional;

import org.jabref.model.openoffice.ootext.OOText;

/**
 * When sorting citations (in a group), we also consider pageInfo.
 * Otherwise we sort citations as cited keys.
 */
public interface ComparableCitation extends ComparableCitedKey {
    Optional<OOText> getPageInfo();
}
