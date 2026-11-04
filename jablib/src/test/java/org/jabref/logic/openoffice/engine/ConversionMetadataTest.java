package org.jabref.logic.openoffice.engine;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.engine.storage.UndoableDocumentProperties;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

@NullMarked
@ResourceLock("Localization.lang")
class ConversionMetadataTest {
    private static final String NATIVE = ZoteroReferenceMark.PREFIX + """
            {"citationID":"foreign-id","properties":{"custom":"unchanged"},
             "citationItems":[{"id":"opaque-id","uris":["http://www.jabref.org/beta"],"locator":"4-7","label":"page","extra":42},
                              {"id":"123456789012345678901234567890","uris":["http://www.jabref.org/alpha"]}]}
            """.strip() + " RNDnative1234";

    @ParameterizedTest
    @EnumSource(CitationType.class)
    void roundTripRetainsEveryStoredTypeUidKeyOrderPagesAndOpaqueNativeJson(CitationType type) throws Exception {
        Citation beta = new Citation("beta");
        beta.setPageInfo(Optional.of(OOText.fromString("<b>pp. 4–7</b>")));
        Citation alpha = new Citation("alpha");
        alpha.setPageInfo(Optional.of(OOText.fromString("p. 9")));
        CitationGroup original = new CitationGroup(new CitationGroupId("stable_uid"), type, List.of(beta, alpha), Optional.empty());
        ConversionMetadata captured = ConversionMetadata.capture(original, NATIVE);
        ConversionMetadata decoded = ConversionMetadata.decode(captured.encode());
        CitationGroup restored = decoded.group();
        assertEquals(captured, decoded);
        assertEquals(original.groupId.citationGroupIdAsString(), restored.groupId.citationGroupIdAsString());
        assertEquals(type, restored.citationType);
        assertEquals(List.of("beta", "alpha"), restored.citationsInStorageOrder.stream().map(citation -> citation.citationKey).toList());
        assertEquals(List.of(Optional.of("<b>pp. 4–7</b>"), Optional.of("p. 9")),
                restored.citationsInStorageOrder.stream().map(citation -> citation.getPageInfo().map(text -> text.toString())).toList());
        assertEquals(NATIVE, decoded.zoteroName());
        assertEquals(JsonParser.parseString(ZoteroReferenceMark.getCSLJson(NATIVE)), JsonParser.parseString(ZoteroReferenceMark.getCSLJson(decoded.zoteroName())));
        assertFalse(decoded.encode().contains("rendered"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "{}", "[]", "{\"version\":2}",
            "{\"version\":1,\"canonicalName\":\"JABREF_alpha NORMAL uid\",\"pageInfos\":[],\"zoteroName\":\"invalid\"}"})
    void invalidSidecarsAreCheckedFailures(String value) {
        assertThrows(RenderException.class, () -> ConversionMetadata.decode(value));
    }

    @Test
    void plannedLinkedMarkNameUpdatesKeepTheSameUndoableSidecarAndCanonicalState() throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        ConversionMetadata original = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL stable_uid", List.of("p. 7", "p. 9"), NATIVE);
        String linked = NATIVE.replace("\"custom\":\"unchanged\"", "\"custom\":\"unchanged\",\"linked\":true");
        ConversionMetadata expected = new ConversionMetadata(original.canonicalName(), original.pageInfos(), linked);
        String property = ConversionMetadata.PREFIX + "stable_uid";
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(property));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, property)).thenReturn(Optional.of(original.encode()));
            ConversionMetadata prepared = ConversionMetadata.prepareRelink(document, NATIVE, linked).orElseThrow();
            assertEquals(expected, prepared);
            undoable.verify(() -> UndoableDocumentProperties.preflight(document, Map.of(property, Optional.of(expected.encode()))));
            undoable.verify(() -> UndoableDocumentProperties.set(document, property, Optional.of(expected.encode())), never());
            prepared.store(document);
            undoable.verify(() -> UndoableDocumentProperties.set(document, property, Optional.of(expected.encode())));
        }
    }

    @Test
    void staleNativeSnapshotsAreNotRebasedByUriLinkPlanning() throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        ConversionMetadata original = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL stable_uid", List.of("p. 7", "p. 9"), NATIVE);
        String current = NATIVE.replace("\"custom\":\"unchanged\"", "\"custom\":\"unchanged\",\"formattedCitation\":\"external update\"");
        String linked = current.replace("\"extra\":42", "\"extra\":42,\"linked\":true");
        String property = ConversionMetadata.PREFIX + "stable_uid";
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(property));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, property)).thenReturn(Optional.of(original.encode()));
            assertEquals(Optional.empty(), ConversionMetadata.prepareRelink(document, current, linked));
            assertThrows(RenderException.class, () -> ConversionMetadata.forZotero(document, current));
            undoable.verifyNoInteractions();
        }
    }

    @Test
    void corruptUnrelatedSnapshotsDoNotBlockFreshNativeLinkPlanningButStillBlockConversion() throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        ConversionMetadata original = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL stable_uid", List.of("p. 7", "p. 9"), NATIVE);
        String linked = NATIVE.replace("\"extra\":42", "\"extra\":42,\"linked\":true");
        String property = ConversionMetadata.PREFIX + "stable_uid";
        String corrupt = ConversionMetadata.PREFIX + "unrelated";
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(corrupt, property));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, corrupt)).thenReturn(Optional.of("not JSON"));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, property)).thenReturn(Optional.of(original.encode()));
            ConversionMetadata prepared = ConversionMetadata.prepareRelink(document, NATIVE, linked).orElseThrow();
            assertEquals(original.canonicalName(), prepared.canonicalName());
            assertEquals(linked, prepared.zoteroName());
            undoable.verify(() -> UndoableDocumentProperties.preflight(document, Map.of(property, Optional.of(prepared.encode()))));
            assertThrows(RenderException.class, () -> ConversionMetadata.forZotero(document, NATIVE));
        }
    }

    @Test
    void nativeLinkMetadataReadFailuresRemainTypedBeforeAnyWrites() throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        String property = ConversionMetadata.PREFIX + "stable_uid";
        WrappedTargetException cause = new WrappedTargetException("Cannot read document property");
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(property));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, property)).thenThrow(cause);
            assertSame(cause, assertThrows(WrappedTargetException.class, () -> ConversionMetadata.prepareRelink(document, NATIVE, NATIVE)));
            undoable.verifyNoInteractions();
        }
    }

    @Test
    void freshNativeLinkMetadataIsPreflightedBeforeItCanBeApplied() throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        ConversionMetadata original = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL stable_uid", List.of("p. 7", "p. 9"), NATIVE);
        String property = ConversionMetadata.PREFIX + "stable_uid";
        PropertyVetoException cause = new PropertyVetoException("Read-only sidecar");
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(property));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, property)).thenReturn(Optional.of(original.encode()));
            undoable.when(() -> UndoableDocumentProperties.preflight(document, Map.of(property, Optional.of(original.encode())))).thenThrow(cause);
            assertSame(cause, assertThrows(PropertyVetoException.class, () -> ConversionMetadata.prepareRelink(document, NATIVE, NATIVE)));
            undoable.verify(() -> UndoableDocumentProperties.set(document, property, Optional.of(original.encode())), never());
        }
    }

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

    @Test
    void mismatchDetectionInspectsAllNamesAndNeverRecognizesLegacyFormats() {
        List<String> names = List.of("JR_cite_1_alpha", "JABREF_alpha CID_7 old NORMAL", "unrelated", NATIVE, "JABREF_beta NORMAL stable");
        assertTrue(CitationFormatConverter.required(names, true));
        assertTrue(CitationFormatConverter.required(names, false));
        assertFalse(CitationFormatConverter.required(names.subList(0, 3), true));
        assertFalse(CitationFormatConverter.required(names.subList(0, 3), false));
        assertFalse(CitationFormatConverter.required(List.of(NATIVE), true));
        assertFalse(CitationFormatConverter.required(List.of("JABREF_beta NORMAL stable"), false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"locator", "label", "prefix", "suffix"})
    void nonemptyNativeCitationModifiersAreRejected(String field) {
        String name = ZoteroReferenceMark.PREFIX + "{\"citationItems\":[{\"" + field + "\":\"value\"}]}";
        assertThrows(RenderException.class, () -> ConversionMetadata.requireSupportedModifiers(name));
    }

    @Test
    void conversionRetainsItsOwnModifierGuidanceAndTheValidationCause() {
        RenderException error = assertThrows(RenderException.class, () -> ConversionMetadata.requireSupportedModifiers(NATIVE));

        assertEquals(Optional.of(Localization.lang("Cannot convert Zotero citations with locators, labels, prefixes, suffixes, author-only items, or mixed author suppression. Remove these modifiers in Zotero and try again.")), error.localizedMessage());
        assertEquals("Unsupported native Zotero citation modifiers", assertInstanceOf(RenderException.class, error.getCause()).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "null", "not JSON", "{\"citationItems\":null}",
            "{\"citationItems\":{}}", "{\"citationItems\":[null]}", "{\"citationItems\":[{\"suppress-author\":{}}]}"})
    void malformedNativeModifiersRetainInvalidMetadataGuidance(String json) {
        RenderException error = assertThrows(RenderException.class,
                () -> ConversionMetadata.requireSupportedModifiers(ZoteroReferenceMark.PREFIX + json));

        assertEquals(CitationFormatConverter.invalidMetadata().localizedMessage(), error.localizedMessage());
        assertInstanceOf(RenderException.class, error.getCause());
    }

    @Test
    void uniformSuppressionIsSupportedButMixedSuppressionIsRejected() {
        for (String flags : List.of("", "\"suppress-author\":true,")) {
            String item = "{" + flags + "\"id\":\"opaque\",\"unknown-metadata\":42}";
            String name = ZoteroReferenceMark.PREFIX + "{\"citationItems\":[" + item + "," + item + "]}";
            assertDoesNotThrow(() -> ConversionMetadata.requireSupportedModifiers(name));
        }
        String mixed = ZoteroReferenceMark.PREFIX + "{\"citationItems\":[{\"suppress-author\":true},{\"suppress-author\":false}]}";
        assertThrows(RenderException.class, () -> ConversionMetadata.requireSupportedModifiers(mixed));
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
