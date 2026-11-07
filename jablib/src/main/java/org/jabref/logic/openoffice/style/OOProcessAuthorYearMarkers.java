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
import org.jabref.model.openoffice.style.CitedReference;
import org.jabref.model.openoffice.style.CitedReferences;
import org.jabref.model.openoffice.style.NonUniqueCitationMarker;
import org.jabref.model.openoffice.util.OOListUtil;

class OOProcessAuthorYearMarkers {

    private OOProcessAuthorYearMarkers() {
    }

    /// Fills `sortedCitedReferences//normalizedCitationMarker`
    private static void createNormalizedCitationMarkers(CitedReferences sortedCitedReferences, JStyle style) {
        for (CitedReference citedReference : sortedCitedReferences.values()) {
            citedReference.setNormalizedCitationMarker(Optional.of(style.getNormalizedCitationMarker(citedReference)));
        }
    }

    /// For each cited source make the citation keys unique by setting
    /// the uniqueLetter fields to letters ("a", "b") or Optional.empty()
    ///
    /// precondition: sortedCitedReferences already has normalized citation markers.
    /// precondition: sortedCitedReferences is sorted (according to the order we want the letters to be assigned)
    ///
    /// Expects to see data for all cited sources here.
    /// Clears uniqueLetters before filling.
    ///
    /// On return: Each citedReference in sortedCitedReferences has uniqueLetter set as needed.
    /// The same values are copied to the corresponding citations in citationGroups.
    ///
    /// Depends on: style, citations and their order.
    private static void createUniqueLetters(CitedReferences sortedCitedReferences, CitationGroups citationGroups) {
        // The entries in the clashingKeys lists preserve
        // firstAppearance order from sortedCitedReferences.values().
        //
        // The index of the citationKey in this order will decide
        // which unique letter it receives.
        //
        Map<String, List<String>> normalizedCitationMarkerToClashingKeys = new HashMap<>();
        for (CitedReference citedReference : sortedCitedReferences.values()) {
            String normalizedCitationMarker = OOText.toString(citedReference.getNormalizedCitationMarker().get());
            String citationKey = citedReference.citationKey;

            List<String> clashingKeys = normalizedCitationMarkerToClashingKeys.putIfAbsent(normalizedCitationMarker, new ArrayList<>(1));
            if (clashingKeys == null) {
                clashingKeys = normalizedCitationMarkerToClashingKeys.get(normalizedCitationMarker);
            }
            if (!clashingKeys.contains(citationKey)) {
                // First appearance of citationKey, add to list.
                clashingKeys.add(citationKey);
            }
        }

        // Clear old uniqueLetter values.
        for (CitedReference citedReference : sortedCitedReferences.values()) {
            citedReference.setUniqueLetter(Optional.empty());
        }

        // For sets of citation keys fighting for a normalizedCitationMarker
        // add unique letters to the year.
        for (List<String> clashingKeys : normalizedCitationMarkerToClashingKeys.values()) {
            if (clashingKeys.size() <= 1) {
                continue; // No fight, no letters.
            }
            // Multiple citation keys: they get their letters
            // according to their order in clashingKeys.
            int nextUniqueLetter = 'a';
            for (String citationKey : clashingKeys) {
                String uniqueLetter = String.valueOf((char) nextUniqueLetter);
                sortedCitedReferences.get(citationKey).setUniqueLetter(Optional.of(uniqueLetter));
                nextUniqueLetter++;
            }
        }
        sortedCitedReferences.distributeUniqueLetters(citationGroups);
    }

    /* ***************************************
     *
     *     Calculate presentation of citation groups
     *     (create citMarkers)
     *
     * **************************************/

    /// Set isFirstAppearanceOfSource in each citation.
    ///
    /// Preconditions: globalOrder, localOrder
    private static void setIsFirstAppearanceOfSourceInCitations(CitationGroups citationGroups) {
        Set<String> seenBefore = new HashSet<>();
        for (CitationGroup group : citationGroups.getCitationGroupsInGlobalOrder()) {
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

    /// Produce citMarkers for normal
    /// (!isCitationKeyCiteMarkers &amp;&amp; !isNumberEntries) styles.
    ///
    /// @param style Bibliography style.
    static void produceCitationMarkers(CitationGroups citationGroups, JStyle style) {
        assert !style.isCitationKeyCiteMarkers();
        assert !style.isNumberEntries();
        // Citations in (Au1, Au2 2000) form

        CitedReferences citedReferences = citationGroups.getCitedReferencesSortedInOrderOfAppearance();

        createNormalizedCitationMarkers(citedReferences, style);
        createUniqueLetters(citedReferences, citationGroups);
        citationGroups.createPlainBibliographySortedByComparator(OOProcess.AUTHOR_YEAR_TITLE_COMPARATOR);

        // Mark first appearance of each citationKey
        setIsFirstAppearanceOfSourceInCitations(citationGroups);

        for (CitationGroup group : citationGroups.getCitationGroupsInGlobalOrder()) {
            final NonUniqueCitationMarker strictlyUnique = NonUniqueCitationMarker.THROWS;

            List<Citation> citations = group.getCitationsInLocalOrder();
            List<CitationMarkerEntry> citationMarkerEntries = OOListUtil.map(citations, citation -> citation);
            OOText citMarker = style.createCitationMarker(citationMarkerEntries,
                    group.citationType,
                    strictlyUnique);
            group.setCitationMarker(Optional.of(citMarker));
        }
    }
}
