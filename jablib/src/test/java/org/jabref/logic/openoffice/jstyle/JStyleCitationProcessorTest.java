package org.jabref.logic.openoffice.jstyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@NullMarked
class JStyleCitationProcessorTest {

    @TempDir
    Path tempDir;

    private final LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final JournalAbbreviationRepository abbreviationRepository = mock(JournalAbbreviationRepository.class);

    @Test
    void citationKeyMarkersEscapeLiteralKeysButKeepStyleMarkup() throws IOException {
        CitationGroups groups = groupsInOrder(group("keys", CitationType.AUTHORYEAR_PAR,
                new Citation("<b>"), new Citation("&amp;"), new Citation("研究🔬")));
        List<BibEntry> entries = List.of(
                entry("<b>", "Zebra, Zoe", "2020", "Markup key"),
                entry("&amp;", "Beta, Bob", "2020", "Entity key"),
                entry("研究🔬", "Alpha, Alice", "2020", "Unicode key"));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(entries)), citationKeyStyle(false));

        assertEquals(List.of("<i>{研究🔬,&amp;amp;,&lt;b&gt;}</i>"), markers(groups));
    }

    @Test
    void citationKeyMarkersUseOnlyCommaSeparatorsWithoutCustomWrappers() throws IOException {
        JStyle style = citationKeyStyle(false);
        style.getCitProperties().put("CitationGroupMarkupBefore", "");
        style.getCitProperties().put("CitationGroupMarkupAfter", "");
        CitationGroups groups = groupsInOrder(group("keys", CitationType.AUTHORYEAR_PAR, new Citation("alpha"), new Citation("beta")));
        List<BibEntry> entries = List.of(entry("alpha", "Alpha, Alice", "2020", "A"), entry("beta", "Beta, Bob", "2020", "B"));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(entries)), style);

        assertEquals(List.of("alpha,beta"), markers(groups));
    }

    @Test
    void citationKeyMarkersKeepChronologicalLocalOrderAndPageInfoWithoutChangingStorageOrder() throws IOException {
        Citation later = new Citation("later");
        Citation earlier = new Citation("earlier");
        OOText pageInfo = OOText.fromString("p. 5");
        later.setPageInfo(Optional.of(pageInfo));
        CitationGroup merged = group("merged", CitationType.AUTHORYEAR_PAR, later, earlier);
        CitationGroups groups = groupsInOrder(merged);
        List<BibEntry> entries = List.of(
                entry("later", "Alpha, Alice", "2024", "Later work"),
                entry("earlier", "Zebra, Zoe", "2020", "Earlier work"));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(entries)), citationKeyStyle(false));

        assertEquals(List.of("<i>{earlier,later}</i>"), markers(groups));
        assertEquals(List.of("later", "earlier"), merged.citationsInStorageOrder.stream().map(Citation::getCitationKey).toList());
        assertEquals(List.of(Optional.of(pageInfo), Optional.empty()), merged.citationsInStorageOrder.stream().map(Citation::getPageInfo).toList());
        assertEquals(List.of("later", "earlier"), bibliographyKeys(groups));
    }

    @Test
    void invisibleCitationKeyGroupsContributeToBibliographyWithoutVisibleMarkers() throws IOException {
        CitationGroups groups = groupsInOrder(
                group("hidden", CitationType.INVISIBLE_CIT, new Citation("hidden")),
                group("visible", CitationType.AUTHORYEAR_PAR, new Citation("visible")));
        List<BibEntry> entries = List.of(
                entry("hidden", "Alpha, Alice", "2020", "Hidden work"),
                entry("visible", "Zebra, Zoe", "2024", "Visible work"));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(entries)), citationKeyStyle(false));

        assertEquals(List.of("", "<i>{visible}</i>"), markers(groups));
        assertEquals(List.of("hidden", "visible"), bibliographyKeys(groups));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void citationKeyModeProducesUnnumberedBibliographyEvenWhenNumberingIsRequested(boolean numberEntries) throws IOException {
        JStyle style = citationKeyStyle(numberEntries);
        CitationGroups groups = groupsInOrder(group("keys", CitationType.AUTHORYEAR_PAR, new Citation("doe")));
        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(List.of(entry("doe", "Doe, Jane", "2003", "A work")))), style);

        assertEquals(List.of("<i>{doe}</i>"), markers(groups));
        assertEquals(List.of(Optional.empty()), groups.getBibliography().orElseThrow().values().stream().map(reference -> reference.getNumber()).toList());
        assertEquals("<p oo:ParaStyleName=\"Standard\">A work</p>",
                JStyleBibliographyFormatter.formatBibliographyBody(groups, groups.getBibliography().orElseThrow(), style, false).toString());
    }

    @Test
    void unresolvedBibliographyKeysRemainLiteral() throws IOException {
        JStyle style = citationKeyStyle(false);
        CitationGroups groups = groupsInOrder(group("missing", CitationType.AUTHORYEAR_PAR, new Citation("<b>&amp;研究")));
        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(), style);

        assertEquals("Unresolved(&lt;b&gt;&amp;amp;研究)",
                JStyleBibliographyFormatter.formatBibliographyEntryBody(groups.getBibliography().orElseThrow().values().getFirst(), style).toString());
    }

    @Test
    void numericMarkersUseGlobalOrderAndReuseNumbersAcrossGroups() throws IOException {
        BibEntry zebra = entry("zebra", "Zebra, Zoe", "2024", "First cited");
        BibEntry alpha = entry("alpha", "Alpha, Alice", "2020", "Second cited");
        CitationGroup first = group("first", CitationType.AUTHORYEAR_PAR, new Citation("zebra"));
        CitationGroup second = group("second", CitationType.AUTHORYEAR_PAR, new Citation("alpha"));
        CitationGroup repeated = group("repeated", CitationType.AUTHORYEAR_PAR, new Citation("zebra"));
        CitationGroups groups = groupsInOrder(second, repeated, first);
        groups.setGlobalOrder(List.of(first.groupId, second.groupId, repeated.groupId));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(List.of(alpha, zebra))), numericStyle());

        assertEquals(List.of("[1]", "[2]", "[1]"), markers(groups));
        assertEquals(List.of("zebra", "alpha"), bibliographyKeys(groups));
    }

    @Test
    void numericMarkersNumberNewSourcesInChronologicalLocalOrderWithoutChangingStorageOrder() throws IOException {
        BibEntry later = entry("later", "Alpha, Alice", "2024", "Later work");
        BibEntry earlier = entry("earlier", "Zebra, Zoe", "2020", "Earlier work");
        Citation laterCitation = new Citation("later");
        Citation earlierCitation = new Citation("earlier");
        CitationGroup merged = group("merged", CitationType.AUTHORYEAR_PAR, laterCitation, earlierCitation);
        CitationGroups groups = groupsInOrder(merged, group("repeated", CitationType.AUTHORYEAR_PAR, new Citation("later")));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(List.of(later, earlier))), numericStyle());

        assertEquals(List.of("[1; 2]", "[2]"), markers(groups));
        assertEquals(List.of(Optional.of(2), Optional.of(1)), merged.citationsInStorageOrder.stream().map(Citation::getNumber).toList());
        assertEquals(List.of("earlier", "later"), bibliographyKeys(groups));
    }

    @Test
    void unresolvedNumericCitationsTakeNoNumberAndAreReportedOnce() throws IOException {
        BibEntry resolved = entry("resolved", "Doe, Jane", "2003", "Resolved work");
        CitationGroup unresolved = group("unresolved", CitationType.AUTHORYEAR_PAR, new Citation("missing"));
        CitationGroup mixed = group("mixed", CitationType.AUTHORYEAR_PAR, new Citation("resolved"), new Citation("missing"));
        CitationGroups groups = groupsInOrder(unresolved, mixed);

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(List.of(resolved))), numericStyle());

        assertEquals(List.of("[??missing]", "[??missing; 1]"), markers(groups));
        assertEquals(List.of("missing"), groups.getUnresolvedKeys());
        assertEquals(List.of(Optional.empty(), Optional.of(1)), groups.getBibliography().orElseThrow().values().stream().map(reference -> reference.getNumber()).toList());
    }

    @Test
    void pageInfoStaysWithItsCitationWhenPresentationOrderChanges() throws IOException {
        BibEntry later = entry("later", "Alpha, Alice", "2024", "Later work");
        BibEntry earlier = entry("earlier", "Zebra, Zoe", "2020", "Earlier work");
        Citation laterCitation = new Citation("later");
        Citation earlierCitation = new Citation("earlier");
        OOText pageInfo = OOText.fromString("p. 5");
        earlierCitation.setPageInfo(Optional.of(pageInfo));
        CitationGroups groups = groupsInOrder(group("merged", CitationType.AUTHORYEAR_PAR, laterCitation, earlierCitation));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(List.of(later, earlier))), numericStyle());

        assertEquals(List.of("[1; p. 5; 2]"), markers(groups));
        assertEquals(List.of(Optional.empty(), Optional.of(pageInfo)), Stream.of(laterCitation, earlierCitation).map(Citation::getPageInfo).toList());
    }

    @Test
        // [utest->req~openoffice.narrative-citations~1]
    void numericNormalAndNarrativeGroupsShareNumbersWithoutCompressingAuthors() throws IOException {
        CitationGroups groups = groupsInOrder(
                group("normal", CitationType.AUTHORYEAR_PAR, new Citation("smith")),
                group("narrative", CitationType.AUTHORYEAR_INTEXT, new Citation("smith"), new Citation("doe"), new Citation("brown")),
                group("normalMerged", CitationType.AUTHORYEAR_PAR, new Citation("smith"), new Citation("doe"), new Citation("brown")));
        List<BibEntry> entries = List.of(
                entry("smith", "Smith, Jane", "2020", "A"),
                entry("doe", "Doe, John", "2021", "B"),
                entry("brown", "Brown, Bob", "2022", "C"));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(entries)), numericStyle());

        assertEquals(List.of("[1]", "Smith [1]; Doe [2]; Brown [3]", "[1-3]"), markers(groups));
        assertEquals(List.of("smith", "doe", "brown"), bibliographyKeys(groups));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void numericNarrativeOmitsAuthorSeparatorAndMarkupWhenAuthorAndEditorAreMissing(boolean authorMarkup) throws IOException {
        JStyle style = numericStyle();
        style.getCitProperties().put("InTextYearSeparator", " :: ");
        style.getCitProperties().put("AuthorsPartMarkupBefore", authorMarkup ? "<b>" : "");
        style.getCitProperties().put("AuthorsPartMarkupAfter", authorMarkup ? "</b>" : "");
        BibEntry anonymous = new BibEntry().withCitationKey("anonymous").withField(StandardField.TITLE, "Anonymous work");
        CitationGroups groups = groupsInOrder(
                group("normal", CitationType.AUTHORYEAR_PAR, new Citation("anonymous")),
                group("narrative", CitationType.AUTHORYEAR_INTEXT, new Citation("anonymous")));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(List.of(anonymous))), style);

        assertEquals(List.of("[1]", "[1]"), markers(groups));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void numericNarrativeKeepsLocalAuthorOrderAndBibliographyNumberAssociation(boolean sortByPosition) throws IOException {
        JStyle style = numericStyle();
        style.getProperties().put("IsSortByPosition", sortByPosition);
        CitationGroup narrative = group("narrative", CitationType.AUTHORYEAR_INTEXT, new Citation("later"), new Citation("earlier"));
        CitationGroups groups = groupsInOrder(group("first", CitationType.AUTHORYEAR_PAR, new Citation("earlier")), narrative);
        List<BibEntry> entries = List.of(
                entry("later", "Alpha, Alice", "2024", "Later"),
                entry("earlier", "Zebra, Zoe", "2020", "Earlier"));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(entries)), style);

        assertEquals(sortByPosition ? List.of("[1]", "Zebra [1]; Alpha [2]") : List.of("[2]", "Zebra [2]; Alpha [1]"), markers(groups));
        assertEquals(sortByPosition ? List.of(Optional.of(2), Optional.of(1)) : List.of(Optional.of(1), Optional.of(2)),
                narrative.citationsInStorageOrder.stream().map(Citation::getNumber).toList());
        assertEquals(sortByPosition ? List.of("earlier", "later") : List.of("later", "earlier"), bibliographyKeys(groups));
        assertEquals(CitationType.AUTHORYEAR_INTEXT, narrative.citationType);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 2})
    void numericNarrativeKeepsEachPageSlotWithItsAuthorAndNumber(int pageSlot) throws IOException {
        Citation later = new Citation("later");
        Citation earlier = new Citation("earlier");
        if (pageSlot == 0 || pageSlot == 2) {
            later.setPageInfo(Optional.of(OOText.fromString("<i>p. 9</i>")));
        }
        if (pageSlot == 1 || pageSlot == 2) {
            earlier.setPageInfo(Optional.of(OOText.fromString("p. 3")));
        }
        CitationGroups groups = groupsInOrder(group("narrative", CitationType.AUTHORYEAR_INTEXT, later, earlier));
        List<BibEntry> entries = List.of(
                entry("later", "Alpha, Alice", "2024", "Later"),
                entry("earlier", "Zebra, Zoe", "2020", "Earlier"));
        JStyle style = numericStyle();
        style.getCitProperties().put("BracketBefore", "<sup>[");
        style.getCitProperties().put("BracketAfter", "]</sup>");
        style.getCitProperties().put("PageInfoSeparator", ": ");

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(entries)), style);

        String earlierPage = pageSlot == 1 || pageSlot == 2 ? ": p. 3" : "";
        String laterPage = pageSlot == 0 || pageSlot == 2 ? ": <i>p. 9</i>" : "";
        assertEquals(List.of("Zebra <sup>[1" + earlierPage + "]</sup>; Alpha <sup>[2" + laterPage + "]</sup>"), markers(groups));
    }

    @Test
    void numericNarrativeUsesResolvedAuthorFieldsAndEditorFallback() throws IOException {
        BibEntry parent = entry("parent", "{Research and Development}", "2020", "Parent");
        BibEntry child = new BibEntry().withCitationKey("child").withField(StandardField.CROSSREF, "parent");
        BibEntry edited = new BibEntry().withCitationKey("edited").withField(StandardField.EDITOR, "von Neumann, John and Doe, Jane")
                                        .withField(StandardField.YEAR, "2021");
        CitationGroups groups = groupsInOrder(group("narrative", CitationType.AUTHORYEAR_INTEXT, new Citation("child"), new Citation("edited")));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(List.of(parent, child, edited))), numericStyle());

        assertEquals(List.of("Research and Development [1]; von Neumann & Doe [2]"), markers(groups));
    }

    @Test
    void numericNarrativeReusesCustomAuthorRulesOnFirstAndRepeatedAppearances() throws IOException {
        JStyle style = numericStyle();
        style.getCitProperties().put("AuthorField", "editor/author");
        style.getCitProperties().put("MaxAuthors", 2);
        style.getCitProperties().put("MaxAuthorsFirst", -1);
        style.getCitProperties().put("AuthorSeparator", " / ");
        style.getCitProperties().put("AuthorLastSeparator", " & ");
        style.getCitProperties().put("AuthorLastSeparatorInText", " plus ");
        style.getCitProperties().put("OxfordComma", ",");
        style.getCitProperties().put("EtAlString", " et al.");
        style.getCitProperties().put("ItalicEtAl", true);
        style.getCitProperties().put("AuthorsPartMarkupBefore", "<b>");
        style.getCitProperties().put("AuthorsPartMarkupAfter", "</b>");
        style.getCitProperties().put("AuthorNamesListMarkupBefore", "<i>");
        style.getCitProperties().put("AuthorNamesListMarkupAfter", "</i>");
        style.getCitProperties().put("AuthorNameMarkupBefore", "<u>");
        style.getCitProperties().put("AuthorNameMarkupAfter", "</u>");
        style.getCitProperties().put("InTextYearSeparator", " :: ");
        style.getCitProperties().put("CitationSeparator", " | ");
        style.getCitProperties().put("CitationGroupMarkupBefore", "<sup>");
        style.getCitProperties().put("CitationGroupMarkupAfter", "</sup>");
        BibEntry edited = entry("edited", "Ignored, Author", "2020", "Edited")
                .withField(StandardField.EDITOR, "Alpha, Alice and Beta, Bob and Gamma, Grace");
        CitationGroups groups = groupsInOrder(
                group("first", CitationType.AUTHORYEAR_INTEXT, new Citation("edited"), new Citation("pair")),
                group("repeat", CitationType.AUTHORYEAR_INTEXT, new Citation("edited")));
        BibEntry pair = entry("pair", "Delta, Dan and Epsilon, Eve", "2021", "Pair");

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(List.of(edited, pair))), style);

        String abbreviated = "<b><i><u>Alpha</u></i><i> et al.</i></b> :: [1]";
        assertEquals(List.of(
                "<sup><b><i><u>Alpha</u> / <u>Beta</u>, plus <u>Gamma</u></i></b> :: [1] | <b><i><u>Delta</u> plus <u>Epsilon</u></i></b> :: [2]</sup>",
                "<sup>" + abbreviated + "</sup>"), markers(groups));
    }

    @Test
    void numericNarrativeKeepsUnresolvedKeysLiteralWithoutConsumingNumbers() throws IOException {
        JStyle style = numericStyle();
        style.getCitProperties().put("CitationSeparator", " | ");
        style.getCitProperties().put("CitationGroupMarkupBefore", "<i>");
        style.getCitProperties().put("CitationGroupMarkupAfter", "</i>");
        CitationGroups groups = groupsInOrder(
                group("missing", CitationType.AUTHORYEAR_INTEXT, new Citation("<b>&amp;")),
                group("mixed", CitationType.AUTHORYEAR_INTEXT, new Citation("smith"), new Citation("<b>&amp;")),
                group("normal", CitationType.AUTHORYEAR_PAR, new Citation("smith")));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(List.of(entry("smith", "Smith, Jane", "2020", "A")))), style);

        assertEquals(List.of("<i>Unresolved(&lt;b&gt;&amp;amp;)</i>", "<i>Unresolved(&lt;b&gt;&amp;amp;) | Smith [1]</i>", "[1]"), markers(groups));
        assertEquals(List.of("<b>&amp;"), groups.getUnresolvedKeys());
    }

    @Test
    void authorYearLettersFollowFirstAppearanceRatherThanBibliographySortOrder() throws IOException {
        BibEntry first = entry("first", "Doe, Jane", "2003", "Zebra title");
        BibEntry second = entry("second", "Doe, Jane", "2003", "Alpha title");
        CitationGroups groups = groupsInOrder(
                group("first", CitationType.AUTHORYEAR_PAR, new Citation("first")),
                group("second", CitationType.AUTHORYEAR_PAR, new Citation("second")),
                group("repeated", CitationType.AUTHORYEAR_PAR, new Citation("first")));

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(List.of(second, first))), authorYearStyle());

        assertEquals(List.of("[Doe 2003a]", "[Doe 2003b]", "[Doe 2003a]"), markers(groups));
        assertEquals(List.of("second", "first"), bibliographyKeys(groups));
    }

    @Test
    void addingConflictingAuthorYearCitationChangesPreviouslyRenderedLabel() throws IOException {
        BibEntry first = entry("first", "Doe, Jane", "2003", "First work");
        BibEntry second = entry("second", "Doe, Jane", "2003", "Second work");
        BibDatabase database = new BibDatabase(List.of(first, second));
        JStyle style = authorYearStyle();
        CitationGroups before = groupsInOrder(group("first", CitationType.AUTHORYEAR_PAR, new Citation("first")));
        CitationGroups after = groupsInOrder(
                group("first", CitationType.AUTHORYEAR_PAR, new Citation("first")),
                group("second", CitationType.AUTHORYEAR_PAR, new Citation("second")));

        JStyleCitationProcessor.produceCitationMarkers(before, List.of(database), style);
        JStyleCitationProcessor.produceCitationMarkers(after, List.of(database), style);

        assertEquals(List.of(List.of("[Doe 2003]"), List.of("[Doe 2003a]", "[Doe 2003b]")), List.of(markers(before), markers(after)));
    }

    @Test
    void authorYearCitationTypesAndPageInfoRemainIndependentAcrossGroups() throws IOException {
        BibEntry entry = entry("doe", "Doe, Jane", "2003", "A work");
        List<CitationType> types = List.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.AUTHORYEAR_NOPAR, CitationType.AUTHOR_ONLY, CitationType.YEAR_ONLY);
        CitationGroup[] citationGroups = types.stream().map(type -> {
            Citation citation = new Citation("doe");
            citation.setPageInfo(Optional.of(OOText.fromString("p. 5")));
            return group(type.name(), type, citation);
        }).toArray(CitationGroup[]::new);
        CitationGroups groups = groupsInOrder(citationGroups);

        JStyleCitationProcessor.produceCitationMarkers(groups, List.of(new BibDatabase(List.of(entry))), authorYearStyle());

        assertEquals(List.of("[Doe 2003; p. 5]", "Doe [2003; p. 5]", "Doe 2003; p. 5", "Doe", "2003; p. 5"), markers(groups));
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
        return new JStyle(path, layoutPreferences, abbreviationRepository);
    }

    private JStyle numericStyle() throws IOException {
        return new JStyle(JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, layoutPreferences, abbreviationRepository);
    }

    private JStyle authorYearStyle() throws IOException {
        return new JStyle(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, layoutPreferences, abbreviationRepository);
    }

    private static BibEntry entry(String key, String author, String year, String title) {
        return new BibEntry()
                .withCitationKey(key)
                .withField(StandardField.AUTHOR, author)
                .withField(StandardField.YEAR, year)
                .withField(StandardField.TITLE, title);
    }

    private static CitationGroup group(String id, CitationType type, Citation... citations) {
        return new CitationGroup(new CitationGroupId(id), type, List.of(citations), Optional.empty());
    }

    private static CitationGroups groupsInOrder(CitationGroup... groups) {
        Map<CitationGroupId, CitationGroup> byId = new LinkedHashMap<>();
        for (CitationGroup group : groups) {
            byId.put(group.groupId, group);
        }
        CitationGroups result = new CitationGroups(byId);
        result.setGlobalOrder(Stream.of(groups).map(group -> group.groupId).toList());
        return result;
    }

    private static List<String> markers(CitationGroups groups) {
        return groups.getCitationGroupsInGlobalOrder().stream()
                     .map(group -> group.getCitationMarker().orElseThrow().toString())
                     .toList();
    }

    private static List<String> bibliographyKeys(CitationGroups groups) {
        return groups.getBibliography().orElseThrow().values().stream()
                     .map(reference -> reference.citationKey)
                     .toList();
    }
}
