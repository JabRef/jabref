package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.jabref.logic.openoffice.engine.RendererTestData.entry;
import static org.jabref.logic.openoffice.engine.RendererTestData.group;
import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
@ResourceLock("Localization.lang")
class CslRendererMarkupParityTest {

    @ParameterizedTest
    @MethodSource("markupCases")
    void freshRenderingPreservesFormattingAndNeverPatchesOldText(String previousText, List<Integer> numbers, String layout, String expected) throws RenderException {
        int maximum = numbers.stream().mapToInt(Integer::intValue).max().orElse(1);
        List<CitationGroup> groups = new ArrayList<>();
        List<BibEntry> entries = new ArrayList<>();
        for (int number = 1; number <= maximum; number++) {
            groups.add(group("prefix" + number, CitationType.AUTHORYEAR_PAR, "key" + number));
            BibEntry entry = entry("key" + number, "Title " + number);
            if (!numbers.isEmpty() && number == numbers.getFirst()) {
                entry = entry.withField(StandardField.VOLUME, "1");
            }
            entries.add(entry);
        }
        List<String> keys = numbers.isEmpty() ? List.of("key1") : numbers.stream().map(number -> "key" + number).toList();
        CitationGroup target = group("target", CitationType.AUTHORYEAR_PAR, keys.toArray(String[]::new));
        target.setCitationMarker(Optional.of(OOText.fromString(previousText)));
        groups.add(target);
        String source = """
                <style xmlns="http://purl.org/net/xbiblio/csl" version="1.0" class="in-text">
                  <info><title>Markup parity</title><id>http://www.zotero.org/styles/markup-parity</id></info>
                  <citation collapse="citation-number">%s</citation>
                </style>
                """.formatted(layout);
        CitationStyle style = new CitationStyle("markup.csl", "markup", "in-text", "Markup parity", "", true, false, false, false, source, false);
        RenderRequest request = new RenderRequest(groups, List.of(new BibDatabase(entries)), new RenderOptions(BstCitationFormat.NUMERIC));
        RenderResult result = new CslRenderer(style, new BibEntryTypesManager()).render(request);

        assertEquals(OOFormat.setLocaleNone(OOText.fromString(expected)), result.markers().get(target.groupId));
        assertEquals(Optional.of(OOText.fromString(previousText)), target.getCitationMarker());
    }

    private static Stream<Arguments> markupCases() {
        return Stream.of(
                Arguments.of("<span lang=\"zxx\">(<i>1</i>)</span>", List.of(3),
                        "<layout prefix=\"(\" suffix=\")\"><text variable=\"citation-number\" font-style=\"italic\"/></layout>", "(<i>3</i>)"),
                Arguments.of("<span lang=\"zxx\"><sup>1</sup>, <sup>2</sup></span>", List.of(4, 7),
                        "<layout delimiter=\", \"><text variable=\"citation-number\" vertical-align=\"sup\"/></layout>", "<sup>4</sup>, <sup>7</sup>"),
                Arguments.of("<span lang=\"zxx\">[<b>1</b>]</span>", List.of(5),
                        "<layout prefix=\"[\" suffix=\"]\"><text variable=\"citation-number\" font-weight=\"bold\"/></layout>", "[<b>5</b>]"),
                Arguments.of("<span lang=\"zxx\"><sub>1</sub></span>", List.of(9),
                        "<layout><text variable=\"citation-number\" vertical-align=\"sub\"/></layout>", "<sub>9</sub>"),
                Arguments.of("<span oo:CharStyleName=\"Citation 1\"><i>1</i></span>", List.of(8),
                        "<layout><text variable=\"citation-number\" font-style=\"italic\"/></layout>", "<i>8</i>"),
                Arguments.of("<span lang=\"zxx\">(<i><sup>1</sup></i>; <b>2</b>)</span>", List.of(6, 10),
                        "<layout prefix=\"(\" suffix=\")\" delimiter=\"; \"><choose><if variable=\"volume\"><text variable=\"citation-number\" font-style=\"italic\" vertical-align=\"sup\"/></if><else><text variable=\"citation-number\" font-weight=\"bold\"/></else></choose></layout>",
                        "(<i><sup>6</sup></i>; <b>10</b>)"),
                Arguments.of("<span oo:CharStyleName=\"Citation 12\"><sup>1,2,3</sup></span>", List.of(10, 20, 30),
                        "<layout delimiter=\",\" vertical-align=\"sup\"><text variable=\"citation-number\"/></layout>", "<sup>10,20,30</sup>"),
                Arguments.of("[1, 2, 3]", List.of(8, 9, 10), "<layout prefix=\"[\" suffix=\"]\" delimiter=\", \"><text variable=\"citation-number\"/></layout>", "[8, 9, 10]"),
                Arguments.of("[1]", List.of(4), "<layout prefix=\"[\" suffix=\"]\"><text variable=\"citation-number\"/></layout>", "[4]"),
                Arguments.of("Doe (2020) [1]", List.of(), "<layout prefix=\"Doe (2020) [\" suffix=\"]\"><text variable=\"citation-number\"/></layout>", "Doe (2020) [1]"),
                Arguments.of("<sup>1–3</sup>", List.of(4, 5, 6), "<layout delimiter=\",\" vertical-align=\"sup\"><text variable=\"citation-number\"/></layout>", "<sup>4,5,6</sup>"),
                Arguments.of("<sup>1–3</sup>", List.of(4, 7, 9), "<layout delimiter=\",\" vertical-align=\"sup\"><text variable=\"citation-number\"/></layout>", "<sup>4,7,9</sup>"));
    }
}
