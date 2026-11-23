package org.jabref.logic.openoffice.engine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.model.openoffice.style.CitationType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@NullMarked
@ResourceLock("Localization.lang")
class CitationFormatConverterTest {
    private static final String NATIVE = ZoteroReferenceMark.PREFIX + """
            {"citationID":"foreign-id","properties":{"custom":"unchanged"},
             "citationItems":[{"id":"opaque-id","uris":["http://www.jabref.org/beta"],"locator":"4-7","label":"page","extra":42},
                              {"id":"123456789012345678901234567890","uris":["http://www.jabref.org/alpha"]}]}
            """.strip() + " RNDnative1234";

    @Test
    void changingNativeCitationTypePreservesUnknownJsonAndOpaqueItemIds() {
        JsonObject expected = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(NATIVE)).getAsJsonObject();
        for (JsonElement item : expected.getAsJsonArray("citationItems")) {
            item.getAsJsonObject().addProperty("suppress-author", true);
        }
        String changed = ZoteroReferenceMark.updateCitationType(NATIVE, CitationType.AUTHORYEAR_INTEXT);
        assertEquals(expected, JsonParser.parseString(ZoteroReferenceMark.getCSLJson(changed)));
        assertEquals("RNDnative1234", ZoteroReferenceMark.parse(changed).orElseThrow().uniqueId());
    }

    @Test
    void preparedItemIdsAvoidNativeIdsWithoutInterpretingForeignOpaqueIdentifiers() throws RenderException {
        Set<String> used = new HashSet<>(Set.of("1", "3", "opaque-id", "123456789012345678901234567890"));
        assertEquals(4, CitationFormatConverter.reserveItemIds(used, 1, 2));
        assertEquals(Set.of("1", "3", "4", "5", "opaque-id", "123456789012345678901234567890"), used);
        assertEquals(6, CitationFormatConverter.reserveItemIds(used, 6, 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cslConversionCases")
    void cslConversionDependsOnlyOnTheFirstRecognizedCitation(String description, List<String> names,
                                                              boolean nativeConversionRequired, boolean canonicalConversionRequired) {
        OOStyle style = CSLStyleLoader.getDefaultStyle();

        assertEquals(nativeConversionRequired, CitationFormatConverter.required(names, style, true));
        assertEquals(canonicalConversionRequired, CitationFormatConverter.required(names, style, false));
    }

    private static Stream<Arguments> cslConversionCases() {
        String canonical = "JABREF_beta NORMAL stable";
        String legacy = "JR_cite_1_alpha";
        return Stream.of(
                Arguments.of("empty document", List.of(), false, false),
                Arguments.of("unrelated and unsupported CID names", List.of("unrelated", "JABREF_alpha CID_7 old NORMAL"), false, false),
                Arguments.of("skip unrelated and CID names before canonical", List.of("unrelated", "JABREF_alpha CID_7 old NORMAL", canonical), true, false),
                Arguments.of("skip unrelated and CID names before native", List.of("unrelated", "JABREF_alpha CID_7 old NORMAL", NATIVE), false, true),
                Arguments.of("skip unrelated and CID names before JR", List.of("unrelated", "JABREF_alpha CID_7 old NORMAL", legacy), false, true),
                Arguments.of("canonical before native", List.of(canonical, NATIVE), true, false),
                Arguments.of("canonical before JR", List.of(canonical, legacy), true, false),
                Arguments.of("native before canonical", List.of(NATIVE, canonical), false, true),
                Arguments.of("native before JR", List.of(NATIVE, legacy), false, true),
                Arguments.of("JR before canonical", List.of(legacy, canonical), false, true),
                Arguments.of("JR before native", List.of(legacy, NATIVE), false, true));
    }

    @ParameterizedTest
    @MethodSource("nonCslConversionCases")
    void nonCslConversionOnlyMigratesAFirstRecognizedJrCitationRegardlessOfSavedMode(OOStyle style, boolean zoteroMode,
                                                                                     List<String> names, boolean expected) {
        assertEquals(expected, CitationFormatConverter.required(names, style, zoteroMode));
    }

    private static Stream<Arguments> nonCslConversionCases() {
        String canonical = "JABREF_beta NORMAL stable";
        String legacy = "JR_cite_1_alpha";
        return Stream.of(mock(JStyle.class), mock(BstStyle.class))
                     .flatMap(style -> Stream.of(false, true).flatMap(zoteroMode -> Stream.of(
                             Arguments.of(style, zoteroMode, List.of(), false),
                             Arguments.of(style, zoteroMode, List.of("unrelated", "JABREF_alpha CID_7 old NORMAL"), false),
                             Arguments.of(style, zoteroMode, List.of("unrelated", "JABREF_alpha CID_7 old NORMAL", legacy), true),
                             Arguments.of(style, zoteroMode, List.of(canonical), false),
                             Arguments.of(style, zoteroMode, List.of(NATIVE), false),
                             Arguments.of(style, zoteroMode, List.of(canonical, legacy), false),
                             Arguments.of(style, zoteroMode, List.of(NATIVE, legacy), false),
                             Arguments.of(style, zoteroMode, List.of(legacy, canonical, NATIVE), true))));
    }

    @Test
    void leadingZeroNativeIdsReserveTheirBoundedNumericValuesWithoutChangingTheOriginals() throws RenderException {
        Set<String> used = new HashSet<>(Set.of("0001", "0000000002", "2147483648", "opaque-id"));
        assertEquals(3, CitationFormatConverter.reserveItemIds(used, 1, 2));
        assertEquals(Set.of("0001", "0000000002", "2147483648", "opaque-id", "3", "4"), used);
    }

    @Test
    void allocationCanUseTheLastBoundedIdButNeverWraps() throws RenderException {
        Set<String> used = new HashSet<>();
        assertEquals(Integer.MAX_VALUE - 1, CitationFormatConverter.reserveItemIds(used, Integer.MAX_VALUE - 1, 2));
        assertEquals(Set.of("2147483646", "2147483647"), used);
        assertThrows(RenderException.class, () -> CitationFormatConverter.reserveItemIds(used, Integer.MAX_VALUE - 1, 1));
        assertEquals(Set.of("2147483646", "2147483647"), used);
    }

    @Test
    void overflowingBlocksAreRejectedBeforeReservingAnyIds() {
        Set<String> used = new HashSet<>();
        assertThrows(RenderException.class, () -> CitationFormatConverter.reserveItemIds(used, Integer.MAX_VALUE, 2));
        assertThrows(RenderException.class, () -> CitationFormatConverter.reserveItemIds(used, -1, 1));
        assertEquals(Set.of(), used);
    }
}
