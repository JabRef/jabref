package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleBibliographyFormatter;
import org.jabref.logic.openoffice.style.JStyleCitationProcessor;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationMarkerNumericEntry;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.style.CitedReference;
import org.jabref.model.openoffice.style.CitedReferences;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Runs the existing JStyle processor on private groups and entry copies. Even temporary bibliography
/// fields such as `uniq` never touch the selected libraries. Page-reference markup is prepared here;
/// only refreshing those references is left to the document writer.
@NullMarked
public final class JStyleRenderer implements CitationRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JStyleRenderer.class);

    private final JStyle style;

    public JStyleRenderer(JStyle style) {
        this.style = style;
    }

    @Override
    public StyleCapabilities capabilities() {
        Set<CitationType> types = style.isNumberEntries() || style.isCitationKeyCiteMarkers()
                                  ? Set.of(CitationType.AUTHORYEAR_PAR, CitationType.INVISIBLE_CIT)
                                  : EnumSet.allOf(CitationType.class);
        return new StyleCapabilities(types, true, true, true);
    }

    @Override
    public RenderResult render(RenderRequest request) throws RenderException {
        RenderData data = new RenderData(request);
        if (!style.isValid()) {
            throw new RenderException("Cannot render an invalid JStyle");
        }
        Map<CitationGroupId, CitationGroup> copies = new LinkedHashMap<>();
        for (CitationGroup group : request.groupsInDocumentOrder()) {
            List<Citation> citations = new ArrayList<>();
            for (Citation original : group.citationsInStorageOrder) {
                Citation copy = new Citation(original.citationKey);
                copy.setPageInfo(original.getPageInfo());
                citations.add(copy);
            }
            copies.put(group.groupId, new CitationGroup(group.groupId,
                    capabilities().coerceForInsert(group.citationType), citations, group.getReferenceMarkNameForLinking()));
        }
        CitationGroups groups = new CitationGroups(copies);
        groups.setGlobalOrder(request.groupsInDocumentOrder().stream().map(group -> group.groupId).toList());
        try {
            JStyleCitationProcessor.produceCitationMarkers(groups, RenderData.copyDatabases(request.databases()), style);
            Map<CitationGroupId, OOText> markers = new LinkedHashMap<>();
            for (CitationGroup group : groups.getCitationGroupsInGlobalOrder()) {
                OOText text = group.citationType == CitationType.INVISIBLE_CIT
                              ? OOText.fromString("")
                              : style.decorateCitationMarker(marker(group));
                markers.put(group.groupId, text);
            }
            List<OOText> bibliography = new ArrayList<>();
            boolean refreshCitedOnPages = false;
            if (request.options().includeBibliography()) {
                CitedReferences references = groups.getBibliography().orElseThrow(() -> new RenderException("JStyle did not prepare a bibliography"));
                if (request.options().bibliographyTitle().isEmpty()) {
                    bibliography.add(style.getFormattedBibliographyTitle());
                } else {
                    bibliography.addAll(BibliographyText.start(request.options()));
                }
                for (CitedReference reference : references.values()) {
                    bibliography.add(JStyleBibliographyFormatter.formatBibliographyEntry(groups, reference, style, request.options().alwaysAddCitedOnPages()));
                }
                refreshCitedOnPages = groups.citationGroupsProvideReferenceMarkNameForLinking()
                        && (request.options().alwaysAddCitedOnPages() || !data.unresolvedKeys().isEmpty());
            }
            return new RenderResult(markers, bibliography, data.unresolvedKeys(), refreshCitedOnPages);
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.debug("JStyle rendering failed", e);
            throw new RenderException("Cannot render JStyle citations", e);
        }
    }

    private OOText marker(CitationGroup group) throws RenderException {
        if ((style.isNumberEntries() || style.isCitationKeyCiteMarkers()) && group.citationsInStorageOrder.stream().anyMatch(Citation::isUnresolved)) {
            StringJoiner text = new StringJoiner("; ");
            List<Citation> resolved = group.getCitationsInLocalOrder().stream().filter(citation -> !citation.isUnresolved()).toList();
            if (!resolved.isEmpty()) {
                if (style.isCitationKeyCiteMarkers()) {
                    text.add(style.formatCitationKeys(resolved.stream().map(Citation::getCitationKey).toList()).toString());
                } else {
                    List<CitationMarkerNumericEntry> numericEntries = new ArrayList<>(resolved);
                    text.add(style.getNumCitationMarker2(numericEntries).toString());
                }
            }
            group.getCitationsInLocalOrder().stream().filter(Citation::isUnresolved).forEach(citation -> text.add(RenderData.unresolved(citation.citationKey)));
            return OOText.fromString(text.toString());
        }
        return group.getCitationMarker().orElseThrow(() -> new RenderException("JStyle did not render a citation group"));
    }
}
