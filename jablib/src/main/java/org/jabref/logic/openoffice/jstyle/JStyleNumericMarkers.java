package org.jabref.logic.openoffice.jstyle;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationMarkerNumericEntry;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOListUtil;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;

@NullMarked
class JStyleNumericMarkers {

    private JStyleNumericMarkers() {
    }

    /// Produce citation markers for the case of numbered citations
    /// with bibliography sorted by first appearance in the text.
    ///
    /// Numbered citation markers for each CitationGroup.
    /// Numbering is according to first appearance.
    /// Assumes global order and local order are already applied.
    static void produceCitationMarkers(CitationGroups citationGroups, JStyle style) {
        assert style.isNumberEntries();

        if (style.isSortByPosition()) {
            citationGroups.createNumberedBibliographySortedInOrderOfAppearance();
        } else {
            citationGroups.createNumberedBibliographySortedByComparator(JStyleCitationProcessor.AUTHOR_YEAR_TITLE_COMPARATOR);
        }

        JStyleCitationProcessor.setIsFirstAppearanceOfSourceInCitations(citationGroups);

        for (CitationGroup group : citationGroups.getCitationGroupsInGlobalOrder()) {
            OOText citMarker;
            if (group.citationType == CitationType.AUTHORYEAR_INTEXT) {
                citMarker = formatInTextCitation(group.getCitationsInLocalOrder(), style);
            } else {
                List<CitationMarkerNumericEntry> citations = OOListUtil.map(group.getCitationsInLocalOrder(), citation -> citation);
                citMarker = style.formatNumericCitation(citations);
            }
            group.setCitationMarker(Optional.of(citMarker));
        }
    }

    // [impl->req~openoffice.narrative-citations~1]
    private static OOText formatInTextCitation(List<Citation> citations, JStyle style) {
        StringJoiner text = new StringJoiner(style.getCitationSeparator(),
                style.getCitationGroupMarkupBefore(), style.getCitationGroupMarkupAfter());
        for (Citation citation : citations) {
            if (citation.isUnresolved()) {
                text.add(StringEscapeUtils.escapeHtml4("Unresolved(" + citation.citationKey + ")"));
            } else {
                String authors = JStyleCitationFormatter.formatInTextAuthors(style, citation);
                String authorPrefix = authors.isEmpty() ? "" : authors + style.getYearSeparatorInText();
                text.add(authorPrefix + style.formatNumericCitation(List.of(citation)));
            }
        }
        return OOText.fromString(text.toString());
    }
}
