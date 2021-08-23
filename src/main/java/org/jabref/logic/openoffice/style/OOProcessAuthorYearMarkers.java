package org.jabref.logic.openoffice.style;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationMarkerEntry;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.style.CitedKey;
import org.jabref.model.openoffice.style.CitedKeys;
import org.jabref.model.openoffice.style.NonUniqueCitationMarker;
import org.jabref.model.openoffice.util.OOListUtil;

class OOProcessAuthorYearMarkers {

    private OOProcessAuthorYearMarkers() {
        /**/
    }

    /**
     *  Fills {@code sortedCitedKeys//normCitMarker}
     */
    private static void createNormalizedCitationMarkers(CitedKeys sortedCitedKeys, OOBibStyle style) {

        for (CitedKey ck : sortedCitedKeys.values()) {
            ck.setNormalizedCitationMarker(Optional.of(style.getNormalizedCitationMarker(ck)));
        }
    }

    /**
     *  For each cited source make the citation keys unique by setting
     *  the uniqueLetter fields to letters ("a", "b") or Optional.empty()
     *
     * precondition: sortedCitedKeys already has normalized citation markers.
     * precondition: sortedCitedKeys is sorted (according to the order we want the letters to be assigned)
     *
     * Expects to see data for all cited sources here.
     * Clears uniqueLetters before filling.
     *
     * On return: Each citedKey in sortedCitedKeys has uniqueLetter set as needed.
     *            The same values are copied to the corresponding citations in cgs.
     *
     *  Depends on: style, citations and their order.
     */
    private static void createUniqueLetters(CitedKeys sortedCitedKeys, CitationGroups cgs) {

        // The entries in the clashingKeys lists preserve
        // firstAppearance order from sortedCitedKeys.values().
        //
        // The index of the citationKey in this order will decide
        // which unique letter it receives.
        //
        Map<String, List<String>> normCitMarkerToClachingKeys = new HashMap<>();
        for (CitedKey citedKey : sortedCitedKeys.values()) {
            String normCitMarker = OOText.toString(citedKey.getNormalizedCitationMarker().get());
            String citationKey = citedKey.citationKey;

            List<String> clashingKeys = normCitMarkerToClachingKeys.putIfAbsent(normCitMarker, new ArrayList<>(1));
            if (clashingKeys == null) {
                clashingKeys = normCitMarkerToClachingKeys.get(normCitMarker);
            }
            if (!clashingKeys.contains(citationKey)) {
                // First appearance of citationKey, add to list.
                clashingKeys.add(citationKey);
            }
        }

        // Clear old uniqueLetter values.
        for (CitedKey citedKey : sortedCitedKeys.values()) {
            citedKey.setUniqueLetter(Optional.empty());
        }

        // For sets of citation keys figthing for a normCitMarker
        // add unique letters to the year.
        for (List<String> clashingKeys : normCitMarkerToClachingKeys.values()) {
            if (clashingKeys.size() <= 1) {
                continue; // No fight, no letters.
            }
            // Multiple citation keys: they get their letters
            // according to their order in clashingKeys.
            int nextUniqueLetter = 'a';
            for (String citationKey : clashingKeys) {
                String uniqueLetter = String.valueOf((char) nextUniqueLetter);
                sortedCitedKeys.get(citationKey).setUniqueLetter(Optional.of(uniqueLetter));
                nextUniqueLetter++;
            }
        }
        sortedCitedKeys.distributeUniqueLetters(cgs);
    }

    /* ***************************************
     *
     *     Calculate presentation of citation groups
     *     (create citMarkers)
     *
     * **************************************/

    /**
     * Set isFirstAppearanceOfSource in each citation.
     *
     * Preconditions: globalOrder, localOrder
     */
    private static void setIsFirstAppearanceOfSourceInCitations(CitationGroups cgs) {
        Set<String> seenBefore = new HashSet<>();
        for (CitationGroup group : cgs.getCitationGroupsInGlobalOrder()) {
            for (Citation cit : group.getCitationsInLocalOrder()) {
                String currentKey = cit.citationKey;
                if (!seenBefore.contains(currentKey)) {
                    cit.setIsFirstAppearanceOfSource(true);
                    seenBefore.add(currentKey);
                } else {
                    cit.setIsFirstAppearanceOfSource(false);
                }
            }
        }
    }

    /**
     * Produce citMarkers for normal
     * (!isCitationKeyCiteMarkers &amp;&amp; !isNumberEntries) styles.
     *
     * @param cgs
     * @param style              Bibliography style.
     */
    static void produceCitationMarkers(CitationGroups cgs, OOBibStyle style) {

        assert !style.isCitationKeyCiteMarkers();
        assert !style.isNumberEntries();
        // Citations in (Au1, Au2 2000) form

        CitedKeys citedKeys = cgs.getCitedKeysSortedInOrderOfAppearance();

        createNormalizedCitationMarkers(citedKeys, style);
        createUniqueLetters(citedKeys, cgs);
        cgs.createPlainBibliographySortedByComparator(OOProcess.AUTHOR_YEAR_TITLE_COMPARATOR);

        // Mark first appearance of each citationKey
        setIsFirstAppearanceOfSourceInCitations(cgs);

        for (CitationGroup group : cgs.getCitationGroupsInGlobalOrder()) {

            final boolean inParenthesis = (group.citationType == CitationType.AUTHORYEAR_PAR);
            final NonUniqueCitationMarker strictlyUnique = NonUniqueCitationMarker.THROWS;

            List<Citation> cits = group.getCitationsInLocalOrder();
            List<CitationMarkerEntry> citationMarkerEntries = OOListUtil.map(cits, e -> e);
            OOText citMarker = style.createCitationMarker(citationMarkerEntries,
                                                          inParenthesis,
                                                          strictlyUnique);
            group.setCitationMarker(Optional.of(citMarker));
        }
    }

}
