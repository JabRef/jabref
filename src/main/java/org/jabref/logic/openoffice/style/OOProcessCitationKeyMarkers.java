package org.jabref.logic.openoffice.style;

import java.util.Optional;

import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.util.OOListUtil;

class OOProcessCitationKeyMarkers {

    private OOProcessCitationKeyMarkers() {
        /**/
    }

    /**
     *  Produce citation markers for the case when the citation
     *  markers are the citation keys themselves, separated by commas.
     */
    static void produceCitationMarkers(CitationGroups cgs, OOBibStyle style) {

        assert style.isCitationKeyCiteMarkers();

        cgs.createPlainBibliographySortedByComparator(OOProcess.AUTHOR_YEAR_TITLE_COMPARATOR);

        for (CitationGroup group : cgs.getCitationGroupsInGlobalOrder()) {
            String citMarker =
                style.getCitationGroupMarkupBefore()
                + String.join(",", OOListUtil.map(group.getCitationsInLocalOrder(), Citation::getCitationKey))
                + style.getCitationGroupMarkupAfter();
            group.setCitationMarker(Optional.of(OOText.fromString(citMarker)));
        }
    }
}
