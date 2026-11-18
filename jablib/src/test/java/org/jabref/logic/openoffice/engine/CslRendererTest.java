package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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
        CitationGroup group = group("unsupported", CitationType.AUTHORYEAR_NOPAR, "a");
        group.citationsInStorageOrder.getFirst().setPageInfo(Optional.of(OOText.fromString("p. 5")));
        RenderRequest request = request(List.of(group), List.of(entry("a", "A")));
        RenderResult result = renderer(false, false).render(request);

        assertEquals(List.of(List.of("[1]"), CitationType.AUTHORYEAR_NOPAR, Optional.of(OOText.fromString("p. 5")), Optional.empty()),
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
    void canonicalCapabilitiesIncludeOnlyTheDefinedPartialModes() {
        assertEquals(new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.INVISIBLE_CIT,
                        CitationType.AUTHOR_ONLY, CitationType.YEAR_ONLY), true, false, false),
                renderer(false, false).capabilities());
    }

    // [utest->req~openoffice.csl-partial-citations~1]
    @ParameterizedTest
    @MethodSource("partialStyles")
    void partialModesUseFixedJabRefFormattingRegardlessOfStyle(CitationStyle style) throws RenderException {
        RenderRequest request = request(List.of(group("author", CitationType.AUTHOR_ONLY, "a"), group("year", CitationType.YEAR_ONLY, "a")),
                List.of(entry("a", "A").withField(StandardField.AUTHOR, "Smith, Jane and Jones, Alex")));

        assertEquals(List.of("Smith et al.", "2020"), markers(request, new CslRenderer(style, entryTypesManager).render(request)));
    }

    @ParameterizedTest
    @MethodSource("authorCases")
    void authorOnlyUsesShortAuthorsThenEditorsThenTheKey(BibEntry entry, String expected) throws RenderException {
        RenderRequest request = request(List.of(group("author", CitationType.AUTHOR_ONLY, "key")), List.of(entry));

        assertEquals(List.of(expected), markers(request, renderer(false, false).render(request)));
    }

    @ParameterizedTest
    @MethodSource("yearCases")
    void yearOnlyUsesResolvedYearThenDateThenTheKey(BibEntry entry, String expected) throws RenderException {
        RenderRequest request = request(List.of(group("year", CitationType.YEAR_ONLY, "key")), List.of(entry));

        assertEquals(List.of(expected), markers(request, renderer(false, false).render(request)));
    }

    @ParameterizedTest
    @EnumSource(value = CitationType.class, names = {"AUTHOR_ONLY", "YEAR_ONLY"})
    void partialGroupsPreserveOrderRepeatsAndUnresolvedKeys(CitationType type) throws RenderException {
        RenderRequest request = request(List.of(group("mixed", type, "a", "missing", "b", "a"), group("repeat", type, "b", "missing")),
                List.of(entry("a", "A"), entry("b", "B").withField(StandardField.AUTHOR, "Jones, Alex").withField(StandardField.YEAR, "2021")));
        RenderResult result = renderer(false, false).render(request);
        List<String> expected = type == CitationType.AUTHOR_ONLY
                                ? List.of("Smith; Unresolved(missing); Jones; Smith", "Jones; Unresolved(missing)")
                                : List.of("2020; Unresolved(missing); 2021; 2020", "2021; Unresolved(missing)");

        assertEquals(List.of(expected, List.of("missing")), List.of(markers(request, result), result.unresolvedKeys()));
    }

    @ParameterizedTest
    @EnumSource(value = CitationType.class, names = {"AUTHOR_ONLY", "YEAR_ONLY"})
    void partialModesDoNotMutateStoredTypesPageInfoNumbersOrMarkers(CitationType type) throws RenderException {
        CitationGroup group = group("partial", type, "a");
        group.citationsInStorageOrder.getFirst().setPageInfo(Optional.of(OOText.fromString("p. 5")));
        group.setCitationMarker(Optional.of(OOText.fromString("previous")));
        RenderRequest request = request(List.of(group), List.of(entry("a", "A")));
        RenderResult result = renderer(false, false).render(request);

        assertEquals(List.of(List.of(type == CitationType.AUTHOR_ONLY ? "Smith" : "2020"), type, Optional.of(OOText.fromString("p. 5")),
                        Optional.empty(), Optional.of(OOText.fromString("previous"))),
                List.of(markers(request, result), group.citationType, group.citationsInStorageOrder.getFirst().getPageInfo(),
                        group.citationsInStorageOrder.getFirst().getNumber(), group.getCitationMarker()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void partialOnlyKeysParticipateInTheSameNumberingAndBibliography(boolean sorted) throws RenderException {
        List<BibEntry> entries = List.of(entry("a", "Zebra"), entry("b", "Middle"), entry("c", "Alpha"));
        RenderRequest request = request(List.of(group("author", CitationType.AUTHOR_ONLY, "a"), group("year", CitationType.YEAR_ONLY, "b"),
                group("normal", CitationType.AUTHORYEAR_PAR, "c")), entries);
        RenderRequest normal = request(List.of(group("a", CitationType.AUTHORYEAR_PAR, "a"), group("b", CitationType.AUTHORYEAR_PAR, "b"),
                group("c", CitationType.AUTHORYEAR_PAR, "c")), entries);
        CslRenderer renderer = renderer(sorted, false);
        RenderResult result = renderer.render(request);

        assertEquals(List.of(List.of("Smith", "2020", sorted ? "[1]" : "[3]"), renderer.render(normal).bibliography()),
                List.of(markers(request, result), result.bibliography()));
    }

    @Test
    void partialFieldsResolveCrossrefsAndStringsInTheirOwnLibrary() throws RenderException {
        BibEntry child = new BibEntry().withCitationKey("child").withField(StandardField.CROSSREF, "parent");
        BibEntry parent = new BibEntry().withCitationKey("parent").withField(StandardField.AUTHOR, "#authors#").withField(StandardField.YEAR, "#year#");
        BibEntry dated = new BibEntry().withCitationKey("dated").withField(StandardField.DATE, "#date#");
        BibEntry edited = new BibEntry().withCitationKey("edited").withField(StandardField.EDITOR, "#authors#");
        BibDatabase library = new BibDatabase(List.of(child, parent, dated, edited));
        library.setStrings(List.of(new BibtexString("authors", "Jones, Alex and Smith, Jane"), new BibtexString("year", "2022b"),
                new BibtexString("date", "2023-04-05")));
        BibDatabase other = new BibDatabase();
        other.setStrings(List.of(new BibtexString("authors", "Wrong, Author"), new BibtexString("year", "1900"), new BibtexString("date", "1900-01-01")));
        RenderRequest request = new RenderRequest(List.of(group("authors", CitationType.AUTHOR_ONLY, "child", "edited"),
                group("years", CitationType.YEAR_ONLY, "child", "dated")), List.of(other, library), new RenderOptions(BstCitationFormat.NUMERIC));
        RenderResult result = renderer(false, false).render(request);

        assertEquals(List.of(List.of("Jones et al.; Jones et al.", "2022b; 2023"), Optional.of("#authors#"), Optional.of("#year#"), Optional.empty(), Optional.of("#date#")),
                List.of(markers(request, result), parent.getField(StandardField.AUTHOR), parent.getField(StandardField.YEAR),
                        child.getField(StandardField.AUTHOR), dated.getField(StandardField.DATE)));
    }

    @Test
    void sameAuthorAndYearDoNotInventDisambiguationSuffixes() throws RenderException {
        RenderRequest request = request(List.of(group("authors", CitationType.AUTHOR_ONLY, "a", "b"), group("years", CitationType.YEAR_ONLY, "a", "b")),
                List.of(entry("a", "A"), entry("b", "B")));

        assertEquals(List.of("Smith; Smith", "2020; 2020"), markers(request, new CslRenderer(authorYearStyle(), entryTypesManager).render(request)));
    }

    @Test
    void nativeRenderingApproximatesCanonicalOnlyModesWithNormalCitations() throws RenderException {
        RenderRequest request = request(List.of(group("author", CitationType.AUTHOR_ONLY, "a"), group("year", CitationType.YEAR_ONLY, "b"),
                        group("nopar", CitationType.AUTHORYEAR_NOPAR, "a"), group("narrative", CitationType.AUTHORYEAR_INTEXT, "b"),
                        group("invisible", CitationType.INVISIBLE_CIT, "c"), group("normal", CitationType.AUTHORYEAR_PAR, "c")),
                List.of(entry("a", "A"), entry("b", "B"), entry("c", "C")));

        assertEquals(List.of("[1]", "[2]", "[1]", "Smith [2]", "[3]", "[3]"), markers(request, renderer(false, false).renderNative(request)));
    }

    @ParameterizedTest
    @MethodSource("partialStyles")
    void nativeRenderingMatchesCanonicalRenderingForSupportedTypes(CitationStyle style) throws RenderException {
        RenderRequest request = request(List.of(group("normal", CitationType.AUTHORYEAR_PAR, "a"), group("narrative", CitationType.AUTHORYEAR_INTEXT, "a"),
                group("other", CitationType.AUTHORYEAR_PAR, "b")), List.of(entry("a", "A"), entry("b", "B")));
        CslRenderer renderer = new CslRenderer(style, entryTypesManager);

        assertEquals(renderer.render(request), renderer.renderNative(request));
    }

    @Test
    void invalidStyleFailsWithCheckedRenderError() {
        CitationStyle invalid = new CitationStyle("invalid.csl", "invalid", "in-text", "Invalid", "", true, true, false, false, "not XML", false);
        RenderRequest request = request(List.of(group("g", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry("a", "A")));

        assertThrows(RenderException.class, () -> new CslRenderer(invalid, entryTypesManager).render(request));
    }

    private static Stream<Arguments> authorCases() {
        return Stream.of(
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.AUTHOR, "Smith, Jane")
                                           .withField(StandardField.EDITOR, "Jones, Alex"), "Smith"),
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.AUTHOR, "Smith, Jane and Jones, Alex"), "Smith et al."),
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.AUTHOR, "Smith, Jane and others"), "Smith et al."),
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.EDITOR, "Jones, Alex and Smith, Jane"), "Jones et al."),
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.AUTHOR, " ").withField(StandardField.EDITOR, "Jones, Alex"), "Jones"),
                Arguments.of(new BibEntry().withCitationKey("key"), "key"));
    }

    private static Stream<Arguments> yearCases() {
        return Stream.of(
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.YEAR, "2020").withField(StandardField.DATE, "2021-04-05"), "2020"),
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.YEAR, "2020b"), "2020b"),
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.DATE, "2021-04-05"), "2021"),
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.YEAR, " ").withField(StandardField.DATE, "2021-04-05"), "2021"),
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.DATE, "2019/2021"), "2019"),
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.DATE, "not a date"), "key"),
                Arguments.of(new BibEntry().withCitationKey("key"), "key"));
    }

    private static Stream<CitationStyle> partialStyles() {
        CitationStyle numeric = numericStyle(false, false);
        CitationStyle alpha = new CitationStyle("alpha.csl", "alpha", "in-text",
                "DIN 1505-2 (alphanumeric, Deutsch) - standard superseded by ISO-690", "", false, true, false, false, numeric.getSource(), false);
        return Stream.of(numeric, alpha, authorYearStyle());
    }

    private static CitationStyle authorYearStyle() {
        String source = """
                <style xmlns="http://purl.org/net/xbiblio/csl" version="1.0" class="in-text">
                  <info><title>Partial author year</title><id>http://www.zotero.org/styles/partial-author-year</id></info>
                  <citation disambiguate-add-year-suffix="true">
                    <layout prefix="(" suffix=")" delimiter="; ">
                      <names variable="author"><name form="short"/></names>
                      <date variable="issued" prefix=", "><date-part name="year"/></date>
                    </layout>
                  </citation>
                  <bibliography><layout><text variable="title"/></layout></bibliography>
                </style>
                """;
        return new CitationStyle("partial-author-year.csl", "partial-author-year", "in-text", "Partial author year", "",
                false, true, false, false, source, false);
    }

    private CslRenderer renderer(boolean sorted, boolean collapse) {
        return new CslRenderer(numericStyle(sorted, collapse), entryTypesManager);
    }
}
