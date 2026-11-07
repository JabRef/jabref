package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;

import static org.jabref.logic.openoffice.engine.RendererTestData.entry;
import static org.jabref.logic.openoffice.engine.RendererTestData.group;
import static org.jabref.logic.openoffice.engine.RendererTestData.markers;
import static org.jabref.logic.openoffice.engine.RendererTestData.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@NullMarked
@ResourceLock("Localization.lang")
class JStyleRendererTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void citationKeyModeEscapesResolvedAndMixedKeysWithStyleMarkup(boolean numberEntries) throws IOException, RenderException {
        RenderRequest request = request(List.of(
                        group("resolved", CitationType.AUTHORYEAR_PAR, "<b>", "&amp;", "研究🔬"),
                        group("mixed", CitationType.AUTHORYEAR_PAR, "<b>", "&amp;", "研究🔬", "<b>missing&amp;"),
                        group("unresolved", CitationType.AUTHORYEAR_PAR, "<b>missing&amp;")),
                List.of(entry("<b>", "Zebra"), entry("&amp;", "Beta"), entry("研究🔬", "Alpha")));

        RenderResult result = new JStyleRenderer(citationKeyStyle(numberEntries)).render(request);

        assertEquals(List.of("<i>{研究🔬,&amp;amp;,&lt;b&gt;}</i>",
                "<i>{研究🔬,&amp;amp;,&lt;b&gt;}</i>; Unresolved(&lt;b&gt;missing&amp;amp;)",
                "Unresolved(&lt;b&gt;missing&amp;amp;)"), markers(request, result));
        assertEquals(List.of("<b>missing&amp;"), result.unresolvedKeys());
        assertEquals("<p oo:ParaStyleName=\"Standard\">Alpha</p>", result.bibliography().get(2).toString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void citationKeyModeFallsBackForUnsupportedTypesAndKeepsInvisibleSourcesInBibliography(boolean numberEntries) throws IOException, RenderException {
        List<CitationGroup> groups = Arrays.stream(CitationType.values()).map(type -> {
            CitationGroup group = group(type.name(), type, type.name());
            group.citationsInStorageOrder.getFirst().setPageInfo(Optional.of(OOText.fromString("p. 5")));
            return group;
        }).toList();
        List<BibEntry> entries = Arrays.stream(CitationType.values()).map(type -> entry(type.name(), type.name())).toList();
        RenderRequest request = request(groups, entries);
        JStyleRenderer renderer = new JStyleRenderer(citationKeyStyle(numberEntries));

        RenderResult result = renderer.render(request);

        assertEquals(Set.of(CitationType.AUTHORYEAR_PAR, CitationType.INVISIBLE_CIT), renderer.capabilities().supportedTypes());
        assertEquals(List.of("<i>{AUTHORYEAR_PAR}</i>", "<i>{AUTHORYEAR_INTEXT}</i>", "", "<i>{AUTHORYEAR_NOPAR}</i>", "<i>{AUTHOR_ONLY}</i>", "<i>{YEAR_ONLY}</i>"),
                markers(request, result));
        assertEquals(List.of("<p oo:ParaStyleName=\"Standard\">AUTHOR_ONLY</p>",
                "<p oo:ParaStyleName=\"Standard\">AUTHORYEAR_INTEXT</p>",
                "<p oo:ParaStyleName=\"Standard\">AUTHORYEAR_NOPAR</p>",
                "<p oo:ParaStyleName=\"Standard\">AUTHORYEAR_PAR</p>",
                "<p oo:ParaStyleName=\"Standard\">INVISIBLE_CIT</p>",
                "<p oo:ParaStyleName=\"Standard\">YEAR_ONLY</p>"), result.bibliography().stream().skip(1).map(text -> text.toString()).toList());
        assertEquals(List.of(Optional.of(OOText.fromString("p. 5"))),
                groups.getFirst().citationsInStorageOrder.stream().map(citation -> citation.getPageInfo()).toList());
        assertEquals(Optional.empty(), groups.getFirst().getCitationMarker());
    }

    @Test
    void rendersAllSixTypesIndependently() throws IOException, RenderException {
        List<CitationGroup> groups = Arrays.stream(CitationType.values()).map(type -> {
            CitationGroup group = group(type.name(), type, "a");
            group.citationsInStorageOrder.getFirst().setPageInfo(Optional.of(OOText.fromString("p. 5")));
            return group;
        }).toList();
        RenderRequest request = request(groups, List.of(entry("a", "A")));

        assertEquals(List.of("[Smith 2020; p. 5]", "Smith [2020; p. 5]", "", "Smith 2020; p. 5", "Smith", "2020; p. 5"),
                markers(request, new JStyleRenderer(style(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH)).render(request)));
    }

    @Test
    void doesNotMutateGroupsOrTemporaryBibliographyFieldsInLibraries() throws IOException, RenderException {
        BibEntry entry = entry("a", "A").withField(new UnknownField("uniq"), "existing");
        CitationGroup group = group("g", CitationType.AUTHORYEAR_PAR, "a");
        RenderRequest request = request(List.of(group), List.of(entry));
        new JStyleRenderer(style(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH)).render(request);

        assertEquals(List.of(Optional.of("existing"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                List.of(entry.getField(new UnknownField("uniq")), group.getIndexInGlobalOrder(), group.getCitationMarker(),
                        group.citationsInStorageOrder.getFirst().getNumber(), group.citationsInStorageOrder.getFirst().getLookupResult()));
    }

    @Test
    void uniqueLettersAreRenderedInBothMarkersAndBibliographyBeforeWriting() throws IOException, RenderException {
        RenderRequest request = request(List.of(group("a", CitationType.AUTHORYEAR_PAR, "a"), group("b", CitationType.AUTHORYEAR_PAR, "b")),
                List.of(entry("a", "Zebra"), entry("b", "Alpha")));
        RenderResult result = new JStyleRenderer(style(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH)).render(request);

        assertEquals(List.of("[Smith 2020a]", "[Smith 2020b]"), markers(request, result));
        assertTrue(result.bibliography().get(1).toString().contains("<b>2020b</b>"));
        assertTrue(result.bibliography().get(2).toString().contains("<b>2020a</b>"));
    }

    @Test
    void numericUnresolvedCitationsUseSharedPlaceholderAndConsumeNoNumber() throws IOException, RenderException {
        RenderRequest request = request(List.of(group("mixed", CitationType.AUTHORYEAR_PAR, "a", "missing"), group("b", CitationType.AUTHORYEAR_PAR, "b")),
                List.of(entry("a", "A"), entry("b", "B")));
        RenderResult result = new JStyleRenderer(style(JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH)).render(request);

        assertEquals(List.of(List.of("[1]; Unresolved(missing)", "[2]"), List.of("missing")), List.of(markers(request, result), result.unresolvedKeys()));
    }

    @Test
    void keepsPageInfoOnEachCitationWhenLocalOrderChanges() throws IOException, RenderException {
        CitationGroup group = group("g", CitationType.AUTHORYEAR_PAR, "z", "a");
        group.citationsInStorageOrder.getFirst().setPageInfo(Optional.of(OOText.fromString("p. 5")));
        group.citationsInStorageOrder.getLast().setPageInfo(Optional.of(OOText.fromString("p. 7")));
        RenderRequest request = request(List.of(group), List.of(entry("z", "Zebra"), entry("a", "Alpha")));
        RenderResult result = new JStyleRenderer(style(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH)).render(request);

        assertEquals(List.of(Optional.of(OOText.fromString("p. 5")), Optional.of(OOText.fromString("p. 7"))),
                group.citationsInStorageOrder.stream().map(citation -> citation.getPageInfo()).toList());
        assertTrue(result.markers().get(group.groupId).toString().contains("p. 7"));
        assertTrue(result.markers().get(group.groupId).toString().contains("p. 5"));
    }

    @Test
    void preparesPageReferenceMarkupAndSignalsPostWriteRefresh() throws IOException, RenderException {
        RenderRequest original = request(List.of(group("g", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry("a", "A")));
        RenderRequest request = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(BstCitationFormat.NUMERIC, true, "", "", "", true));
        RenderResult result = new JStyleRenderer(style(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH)).render(request);

        assertTrue(result.refreshCitedOnPages());
        assertTrue(result.bibliography().getLast().toString().contains("<oo:referenceToPageNumberOfReferenceMark target=\"mark-g\">"));
    }

    @Test
    void doesNotRenderBibliographyOrRequestCrossrefRefreshWhenExcluded() throws IOException, RenderException {
        RenderRequest original = request(List.of(group("g", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry("a", "A")));
        RenderRequest request = new RenderRequest(original.groupsInDocumentOrder(), original.databases(),
                new RenderOptions(BstCitationFormat.NUMERIC, false, "", "", "", true));
        RenderResult result = new JStyleRenderer(style(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH)).render(request);

        assertEquals(new RenderResult(result.markers(), List.of(), List.of(), false), result);
    }

    private JStyle citationKeyStyle(boolean numberEntries) throws IOException {
        Path path = tempDir.resolve("keys.jstyle");
        Files.writeString(path, """
                NAME
                Citation keys
                PROPERTIES
                IsNumberEntries=%s
                CITATION
                BibTeXKeyCitations=true
                CitationGroupMarkupBefore="<i>{"
                CitationGroupMarkupAfter="}</i>"
                LAYOUT
                default=\\title
                """.formatted(numberEntries));
        return new JStyle(path, mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS), mock(JournalAbbreviationRepository.class));
    }

    private static JStyle style(String path) throws IOException {
        return new JStyle(path, mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS), mock(JournalAbbreviationRepository.class));
    }
}
