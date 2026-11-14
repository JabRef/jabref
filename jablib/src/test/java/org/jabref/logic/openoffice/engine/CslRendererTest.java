package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.jabref.logic.openoffice.engine.RendererTestData.entry;
import static org.jabref.logic.openoffice.engine.RendererTestData.group;
import static org.jabref.logic.openoffice.engine.RendererTestData.markers;
import static org.jabref.logic.openoffice.engine.RendererTestData.numericStyle;
import static org.jabref.logic.openoffice.engine.RendererTestData.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
@ResourceLock("Localization.lang")
class CslRendererTest {

    private final BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();

    @Test
    void assignsDocumentNumbersForAStyleRequestingCollapsedRanges() throws RenderException {
        RenderRequest request = request(List.of(group("first", CitationType.AUTHORYEAR_PAR, "a"), group("second", CitationType.AUTHORYEAR_PAR, "b"),
                        group("range", CitationType.AUTHORYEAR_PAR, "c", "d", "e")),
                List.of(entry("a", "A"), entry("b", "B"), entry("c", "C"), entry("d", "D"), entry("e", "E")));

        // citeproc-java 3.5.2 does not implement collapse; the cluster must still use the real document numbers.
        assertEquals(List.of("[1]", "[2]", "[3, 4, 5]"), markers(request, renderer(false, true).render(request)));
    }

    @Test
    void reusesNumbersInNonconsecutiveClusters() throws RenderException {
        RenderRequest request = request(List.of(group("all", CitationType.AUTHORYEAR_PAR, "a", "b", "c", "d", "e"),
                        group("gaps", CitationType.AUTHORYEAR_PAR, "a", "c", "e")),
                List.of(entry("a", "A"), entry("b", "B"), entry("c", "C"), entry("d", "D"), entry("e", "E")));

        assertEquals(List.of("[1, 2, 3, 4, 5]", "[1, 3, 5]"), markers(request, renderer(false, true).render(request)));
    }

    @Test
    void citationAndBibliographyNumbersFollowTheSameSortedSet() throws RenderException {
        RenderRequest request = request(List.of(group("z", CitationType.AUTHORYEAR_PAR, "z"), group("a", CitationType.AUTHORYEAR_PAR, "a")),
                List.of(entry("z", "Zebra"), entry("a", "Alpha")));
        RenderResult result = renderer(true, false).render(request);

        assertEquals(List.of(List.of("[2]", "[1]"), List.of("<p oo:ParaStyleName=\"Text body\"><span lang=\"zxx\">1. Alpha</span></p>",
                        "<p oo:ParaStyleName=\"Text body\"><span lang=\"zxx\">2. Zebra</span></p>")),
                List.of(markers(request, result), result.bibliography().stream().map(text -> text.toString()).toList()));
    }

    @Test
    void perGroupTypesDoNotRewriteOtherGroups() throws RenderException {
        RenderRequest request = request(List.of(group("normal", CitationType.AUTHORYEAR_PAR, "a"), group("narrative", CitationType.AUTHORYEAR_INTEXT, "a"),
                group("empty", CitationType.INVISIBLE_CIT, "a")), List.of(entry("a", "A")));

        assertEquals(List.of("[1]", "Smith [1]", ""), markers(request, renderer(false, false).render(request)));
    }

    @Test
    void invisibleCitationsParticipateInSetWideNumbering() throws RenderException {
        RenderRequest request = request(List.of(group("empty", CitationType.INVISIBLE_CIT, "a"), group("normal", CitationType.AUTHORYEAR_PAR, "b")),
                List.of(entry("a", "A"), entry("b", "B")));

        assertEquals(List.of("", "[2]"), markers(request, renderer(false, false).render(request)));
    }

    @Test
    void unresolvedCitationsRemainVisibleWithoutConsumingNumbers() throws RenderException {
        RenderRequest request = request(List.of(group("mixed", CitationType.AUTHORYEAR_PAR, "a", "missing", "b"),
                group("repeat", CitationType.AUTHORYEAR_PAR, "missing", "b")), List.of(entry("a", "A"), entry("b", "B")));
        RenderResult result = renderer(false, false).render(request);

        assertEquals(List.of(List.of("[1]; Unresolved(missing); [2]", "Unresolved(missing); [2]"), List.of("missing")),
                List.of(markers(request, result), result.unresolvedKeys()));
    }

    @Test
    void unsupportedTypesAreApproximatedWithoutMutatingStoredTypesOrPageInfo() throws RenderException {
        CitationGroup group = group("partial", CitationType.AUTHOR_ONLY, "a");
        group.citationsInStorageOrder.getFirst().setPageInfo(Optional.of(OOText.fromString("p. 5")));
        RenderRequest request = request(List.of(group), List.of(entry("a", "A")));
        RenderResult result = renderer(false, false).render(request);

        assertEquals(List.of(List.of("[1]"), CitationType.AUTHOR_ONLY, Optional.of(OOText.fromString("p. 5")), Optional.empty()),
                List.of(markers(request, result), group.citationType, group.citationsInStorageOrder.getFirst().getPageInfo(), group.citationsInStorageOrder.getFirst().getNumber()));
    }

    @Test
    void skippingBibliographyDoesNotChangeSortedNumbers() throws RenderException {
        RenderRequest original = request(List.of(group("z", CitationType.AUTHORYEAR_PAR, "z"), group("a", CitationType.AUTHORYEAR_PAR, "a")),
                List.of(entry("z", "Zebra"), entry("a", "Alpha")));
        RenderRequest request = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(BstCitationFormat.NUMERIC, false, "References", "Heading 1", "Text body", false));
        RenderResult result = renderer(true, false).render(request);

        assertEquals(List.of(List.of("[2]", "[1]"), List.of()), List.of(markers(request, result), result.bibliography()));
    }

    @Test
    void preservesTheAlphanumericHelper() throws RenderException {
        CitationStyle base = numericStyle(false, false);
        CitationStyle alpha = new CitationStyle("alpha.csl", "alpha", "in-text",
                "DIN 1505-2 (alphanumeric, Deutsch) - standard superseded by ISO-690", "", false, true, false, false, base.getSource(), false);
        BibEntry entry = entry("a", "A");
        RenderRequest request = request(List.of(group("normal", CitationType.AUTHORYEAR_PAR, "a"), group("narrative", CitationType.AUTHORYEAR_INTEXT, "a")), List.of(entry));

        assertEquals(List.of("[Sm20]", "Smith [Sm20]"), markers(request, new CslRenderer(alpha, entryTypesManager).render(request)));
    }

    @Test
    void invalidStyleFailsWithCheckedRenderError() {
        CitationStyle invalid = new CitationStyle("invalid.csl", "invalid", "in-text", "Invalid", "", true, true, false, false, "not XML", false);
        RenderRequest request = request(List.of(group("g", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry("a", "A")));

        assertThrows(RenderException.class, () -> new CslRenderer(invalid, entryTypesManager).render(request));
    }

    private CslRenderer renderer(boolean sorted, boolean collapse) {
        return new CslRenderer(numericStyle(sorted, collapse), entryTypesManager);
    }
}
