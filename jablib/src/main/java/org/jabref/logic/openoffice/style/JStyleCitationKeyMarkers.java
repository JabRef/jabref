package org.jabref.logic.openoffice.style;

import java.util.Optional;

import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;

@NullMarked
class JStyleCitationKeyMarkers {

    private JStyleCitationKeyMarkers() {
    }

    /// Produce citation markers for the case when the citation
    /// markers are the citation keys themselves, separated by commas.
    static void produceCitationMarkers(CitationGroups citationGroups, JStyle style) {
        assert style.isCitationKeyCiteMarkers();

        citationGroups.createPlainBibliographySortedByComparator(JStyleCitationProcessor.AUTHOR_YEAR_TITLE_COMPARATOR);

        for (CitationGroup group : citationGroups.getCitationGroupsInGlobalOrder()) {
            OOText marker = group.citationType == CitationType.INVISIBLE_CIT
                            ? OOText.fromString("")
                            : style.formatCitationKeys(group.getCitationsInLocalOrder().stream().map(Citation::getCitationKey).toList());
            group.setCitationMarker(Optional.of(marker));
        }
    }
}
