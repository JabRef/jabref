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
    void numericInTextPairsAuthorsWithSortedNumbersInStorageOrderIncludingRepeats() throws RenderException {
        RenderRequest request = request(List.of(group("normal", CitationType.AUTHORYEAR_PAR, "z", "a"),
                        group("narrative", CitationType.AUTHORYEAR_INTEXT, "z", "a", "z"), group("repeat", CitationType.AUTHORYEAR_PAR, "z")),
                List.of(entry("a", "Alpha"), entry("z", "Zebra").withField(StandardField.AUTHOR, "Doe, John")));
        RenderResult result = renderer(true, false).render(request);

        assertEquals(List.of("[1, 2]", "Doe [2], Smith [1], Doe [2]", "[2]"), markers(request, result));
        assertEquals(List.of("<p oo:ParaStyleName=\"Text body\"><span lang=\"zxx\">1. Alpha</span></p>",
                "<p oo:ParaStyleName=\"Text body\"><span lang=\"zxx\">2. Zebra</span></p>"), result.bibliography().stream().map(text -> text.toString()).toList());
        assertEquals(List.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.AUTHORYEAR_PAR),
                request.groupsInDocumentOrder().stream().map(group -> group.citationType).toList());
    }

    @Test
    void numericInTextKeepsInterleavedUnresolvedKeysWithoutConsumingNumbers() throws RenderException {
        RenderRequest request = request(List.of(group("normal", CitationType.AUTHORYEAR_PAR, "a"),
                        group("narrative", CitationType.AUTHORYEAR_INTEXT, "a", "missing", "b", "missing", "a"),
                        group("repeat", CitationType.AUTHORYEAR_PAR, "b")),
                List.of(entry("a", "A"), entry("b", "B").withField(StandardField.AUTHOR, "Doe, John")));
        RenderResult result = renderer(false, false).render(request);

        assertEquals(List.of("[1]", "Smith [1], Unresolved(missing), Doe [2], Unresolved(missing), Smith [1]", "[2]"), markers(request, result));
        assertEquals(List.of("missing"), result.unresolvedKeys());
    }

    @Test
    void numericInTextKeepsAuthorsOutsideSuperscriptMarkers() throws RenderException {
        CitationStyle base = numericStyle(true, false);
        String source = base.getSource().replace(
                "<layout prefix=\"[\" suffix=\"]\" delimiter=\", \"><text variable=\"citation-number\"/></layout>",
                "<layout delimiter=\", \"><text variable=\"citation-number\" vertical-align=\"sup\"/></layout>");
        CitationStyle superscript = new CitationStyle("superscript.csl", "superscript", "in-text", "Numeric superscript", "",
                true, true, true, false, source, false);
        RenderRequest request = request(List.of(group("normal", CitationType.AUTHORYEAR_PAR, "z", "a"),
                        group("narrative", CitationType.AUTHORYEAR_INTEXT, "z", "a"), group("repeat", CitationType.AUTHORYEAR_PAR, "z")),
                List.of(entry("a", "Alpha"), entry("z", "Zebra").withField(StandardField.AUTHOR, "Doe, John")));
        RenderResult result = new CslRenderer(superscript, entryTypesManager).render(request);

        assertEquals(List.of("<sup>1</sup>, <sup>2</sup>", "Doe <sup>2</sup>, Smith <sup>1</sup>", "<sup>2</sup>"), markers(request, result));
    }

    // [utest->req~openoffice.csl-in-text-citations~1]
    @ParameterizedTest
    @MethodSource("authorCases")
    void numericInTextUsesShortAuthorsThenEditorsThenTheKey(BibEntry entry, String expected) throws RenderException {
        RenderRequest request = request(List.of(group("narrative", CitationType.AUTHORYEAR_INTEXT, "key")), List.of(entry));

        assertEquals(List.of(expected + " [1]"), markers(request, renderer(false, false).render(request)));
    }

    @Test
    void numericInTextEscapesTheFallbackKey() throws RenderException {
        String key = "<key>&";
        RenderRequest request = request(List.of(group("narrative", CitationType.AUTHORYEAR_INTEXT, key)), List.of(new BibEntry().withCitationKey(key)));

        assertEquals(List.of("&lt;key&gt;&amp; [1]"), markers(request, renderer(false, false).render(request)));
    }

    @ParameterizedTest
    @EnumSource(value = StandardField.class, names = {"AUTHOR", "EDITOR"})
    void numericInTextResolvesInheritedAuthorsAndEditorsInTheirOwnLibrary(StandardField field) throws RenderException {
        BibEntry child = new BibEntry().withCitationKey("child").withField(StandardField.CROSSREF, "parent");
        BibEntry parent = new BibEntry().withCitationKey("parent").withField(field, "#authors#");
        BibDatabase library = new BibDatabase(List.of(child, parent));
        library.setStrings(List.of(new BibtexString("authors", "Jones, Alex and Smith, Jane")));
        BibDatabase other = new BibDatabase();
        other.setStrings(List.of(new BibtexString("authors", "Wrong, Author")));
        RenderRequest request = new RenderRequest(List.of(group("narrative", CitationType.AUTHORYEAR_INTEXT, "child")),
                List.of(other, library), new RenderOptions(BstCitationFormat.NUMERIC));

        assertEquals(List.of("Jones et al. [1]"), markers(request, renderer(false, false).render(request)));
        assertEquals(Optional.empty(), child.getField(field));
        assertEquals(Optional.of("#authors#"), parent.getField(field));
    }

    // [utest->req~openoffice.csl-in-text-citations~1]
    @ParameterizedTest
    @ValueSource(strings = {", ", " "})
    void authorYearInTextConvertsPlainFormsWithoutChangingStoredTypes(String delimiter) throws RenderException {
        CitationStyle style = authorYearStyle("""
                <layout prefix="(" suffix=")" delimiter="; ">
                  <names variable="author"><name form="short"/></names>
                  <date variable="issued" prefix="%s"><date-part name="year"/></date>
                </layout>
                """.formatted(delimiter));
        RenderRequest request = request(List.of(group("normal", CitationType.AUTHORYEAR_PAR, "a"), group("narrative", CitationType.AUTHORYEAR_INTEXT, "a"),
                group("repeat", CitationType.AUTHORYEAR_PAR, "a")), List.of(entry("a", "A")));

        assertEquals(List.of("(Smith" + delimiter + "2020)", "Smith (2020)", "(Smith" + delimiter + "2020)"),
                markers(request, new CslRenderer(style, entryTypesManager).render(request)));
        assertEquals(List.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.AUTHORYEAR_PAR),
                request.groupsInDocumentOrder().stream().map(group -> group.citationType).toList());
    }

    @Test
    void authorYearInTextConvertsAnExplicitNoDateTerm() throws RenderException {
        CitationStyle style = authorYearStyle("""
                <layout prefix="(" suffix=")" delimiter="; ">
                  <names variable="author"><name form="short"/></names>
                  <choose>
                    <if variable="issued"><date variable="issued" prefix=", "><date-part name="year"/></date></if>
                    <else><text term="no date" form="short" prefix=", "/></else>
                  </choose>
                </layout>
                """);
        BibEntry entry = new BibEntry().withCitationKey("a").withField(StandardField.AUTHOR, "Smith, Jane");
        RenderRequest request = request(List.of(group("normal", CitationType.AUTHORYEAR_PAR, "a"), group("narrative", CitationType.AUTHORYEAR_INTEXT, "a")), List.of(entry));

        assertEquals(List.of("(Smith, n.d.)", "Smith (n.d.)"), markers(request, new CslRenderer(style, entryTypesManager).render(request)));
    }

    @Test
    void authorYearInTextPreservesSquareBracketsAndTheYearSuffix() throws RenderException {
        CitationStyle style = authorYearStyle("""
                <layout prefix="[" suffix="]" delimiter="; ">
                  <names variable="author"><name form="short"/></names>
                  <date variable="issued" prefix=" "><date-part name="year"/></date><text value="a"/>
                </layout>
                """);
        RenderRequest request = request(List.of(group("normal", CitationType.AUTHORYEAR_PAR, "a"), group("narrative", CitationType.AUTHORYEAR_INTEXT, "a")), List.of(entry("a", "A")));

        assertEquals(List.of("[Smith 2020a]", "Smith [2020a]"), markers(request, new CslRenderer(style, entryTypesManager).render(request)));
    }

    @ParameterizedTest
    @MethodSource("markedUpAuthorYearLayouts")
    void authorYearInTextLeavesMarkupContainingFormsUnchanged(String layout, String expected) throws RenderException {
        RenderRequest request = request(List.of(group("normal", CitationType.AUTHORYEAR_PAR, "a"), group("narrative", CitationType.AUTHORYEAR_INTEXT, "a")), List.of(entry("a", "A")));

        assertEquals(List.of(expected, expected), markers(request, new CslRenderer(authorYearStyle(layout), entryTypesManager).render(request)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"(Smith, forthcoming)", "(Smith, 2020; Jones, 2021)", "(Smith, 2020, Jones, 2021)", "(Smith 2020 and Jones 2021)",
            "(Smith, 2020, 2021)", "(Smith, 2020, forthcoming)", "(Smith, n.d., Jones, n.d.)", "(Smith, forthcoming, Jones, 2021)", "(Smith, Jones, 2020)",
            "(Smith, 2020]", "[Smith, 2020)", "Smith, 2020", "(Smith, 2020"})
    void authorYearInTextLeavesUnknownMultiSourceAndUnpairedFormsUnchanged(String text) throws RenderException {
        CitationStyle style = authorYearStyle("<layout><text value=\"" + text + "\"/></layout>");
        RenderRequest request = request(List.of(group("narrative", CitationType.AUTHORYEAR_INTEXT, "a")), List.of(entry("a", "A")));

        assertEquals(List.of(text), markers(request, new CslRenderer(style, entryTypesManager).render(request)));
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
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.AUTHOR, " "), "key"),
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.AUTHOR, "Smith & Sons, Jane"), "Smith &amp; Sons"),
                Arguments.of(new BibEntry().withCitationKey("key").withField(StandardField.EDITOR, "Jones & Sons, Alex"), "Jones &amp; Sons"),
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

    private static Stream<Arguments> markedUpAuthorYearLayouts() {
        return Stream.of(
                Arguments.of("""
                        <layout prefix="(" suffix=")">
                          <names variable="author" font-style="italic"><name form="short"/></names>
                          <date variable="issued" prefix=", "><date-part name="year"/></date>
                        </layout>
                        """, "(<i>Smith</i>, 2020)"),
                Arguments.of("""
                        <layout prefix="(" suffix=")">
                          <names variable="author"><name form="short"/></names>
                          <date variable="issued" prefix=", " font-weight="bold"><date-part name="year"/></date>
                        </layout>
                        """, "(Smith, <b>2020</b>)"),
                Arguments.of("""
                        <layout prefix="(" suffix=")">
                          <names variable="author"><name form="short"/></names>
                          <date variable="issued" prefix=", "><date-part name="year"/></date>
                          <text value="a" vertical-align="sup"/>
                        </layout>
                        """, "(Smith, 2020<sup>a</sup>)"));
    }

    private static CitationStyle authorYearStyle() {
        return authorYearStyle("""
                <layout prefix="(" suffix=")" delimiter="; ">
                  <names variable="author"><name form="short"/></names>
                  <date variable="issued" prefix=", "><date-part name="year"/></date>
                </layout>
                """);
    }

    private static CitationStyle authorYearStyle(String layout) {
        String source = """
                <style xmlns="http://purl.org/net/xbiblio/csl" version="1.0" class="in-text">
                  <info><title>Partial author year</title><id>http://www.zotero.org/styles/partial-author-year</id></info>
                  <citation disambiguate-add-year-suffix="true">
                    %s
                  </citation>
                  <bibliography><layout><text variable="title"/></layout></bibliography>
                </style>
                """.formatted(layout);
        return new CitationStyle("partial-author-year.csl", "partial-author-year", "in-text", "Partial author year", "",
                false, true, false, false, source, false);
    }

    private CslRenderer renderer(boolean sorted, boolean collapse) {
        return new CslRenderer(numericStyle(sorted, collapse), entryTypesManager);
    }
}
