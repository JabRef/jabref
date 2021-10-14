package org.jabref.logic.openoffice.style;

import java.util.List;
import java.util.Optional;

import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationMarkerNumericEntry;
import org.jabref.model.openoffice.util.OOListUtil;

class OOProcessNumericMarkers {

    private OOProcessNumericMarkers() {
        /**/
    }

    /**
     * Produce citation markers for the case of numbered citations
     * with bibliography sorted by first appearance in the text.
     *
     * Numbered citation markers for each CitationGroup.
     * Numbering is according to first appearance.
     * Assumes global order and local order are already applied.
     *
     * @param cgs
     * @param style
     *
     */
    static void produceCitationMarkers(CitationGroups cgs, OOBibStyle style) {

        assert style.isNumberEntries();

        if (style.isSortByPosition()) {
            cgs.createNumberedBibliographySortedInOrderOfAppearance();
        } else {
            cgs.createNumberedBibliographySortedByComparator(OOProcess.AUTHOR_YEAR_TITLE_COMPARATOR);
        }

        for (CitationGroup group : cgs.getCitationGroupsInGlobalOrder()) {
            List<CitationMarkerNumericEntry> cits = OOListUtil.map(group.getCitationsInLocalOrder(), e -> e);
            OOText citMarker = style.getNumCitationMarker2(cits);
            group.setCitationMarker(Optional.of(citMarker));
        }
    }

}
