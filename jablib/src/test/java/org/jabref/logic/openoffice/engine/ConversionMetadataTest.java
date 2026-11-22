package org.jabref.logic.openoffice.engine;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.engine.storage.UndoableDocumentProperties;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
    void indexReadsAndDecodesEachSidecarOnceForMultipleStrictLookupsAndRelinks() throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        ConversionMetadata first = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL first_uid", List.of("", ""), NATIVE);
        String secondName = NATIVE.replace("foreign-id", "second-id").replace("RNDnative1234", "RNDsecond1234");
        ConversionMetadata second = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL second_uid", List.of("", ""), secondName);
        String firstProperty = ConversionMetadata.PREFIX + "first_uid";
        String secondProperty = ConversionMetadata.PREFIX + "second_uid";
        String linked = NATIVE.replace("\"extra\":42", "\"extra\":42,\"linked\":true");
        String secondLinked = secondName.replace("\"extra\":42", "\"extra\":42,\"linked\":true");
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class);
             MockedStatic<ConversionMetadata> metadata = mockStatic(ConversionMetadata.class, CALLS_REAL_METHODS)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of("unrelated", firstProperty, secondProperty));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, firstProperty)).thenReturn(Optional.of(first.encode()));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, secondProperty)).thenReturn(Optional.of(second.encode()));
            ConversionMetadata.Index index = ConversionMetadata.readIndex(document);
            for (int lookup = 0; lookup < 3; lookup++) {
                assertEquals(Optional.of(first), index.forZotero(NATIVE));
                assertEquals(Optional.of(second), index.forZotero(secondName));
            }
            assertEquals(linked, index.prepareRelink(document, NATIVE, linked).orElseThrow().zoteroName());
            assertEquals(secondLinked, index.prepareRelink(document, secondName, secondLinked).orElseThrow().zoteroName());
            properties.verify(() -> UnoUserDefinedProperty.getListOfNames(document));
            properties.verify(() -> UnoUserDefinedProperty.getStringValue(document, firstProperty));
            properties.verify(() -> UnoUserDefinedProperty.getStringValue(document, secondProperty));
            properties.verify(() -> UnoUserDefinedProperty.getStringValue(document, "unrelated"), never());
            metadata.verify(() -> ConversionMetadata.decode(first.encode()));
            metadata.verify(() -> ConversionMetadata.decode(second.encode()));
            undoable.verify(() -> UndoableDocumentProperties.set(document, firstProperty, Optional.of(first.encode())), never());
        }
    }

    @Test
    void aNewActionReadsUpdatedSidecarsInsteadOfRetainingThePreviousIndex() throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        ConversionMetadata original = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL stable_uid", List.of("", ""), NATIVE);
        String changed = NATIVE.replace("\"extra\":42", "\"extra\":43");
        ConversionMetadata updated = new ConversionMetadata(original.canonicalName(), original.pageInfos(), changed);
        String property = ConversionMetadata.PREFIX + "stable_uid";
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(property));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, property)).thenReturn(Optional.of(original.encode()), Optional.of(updated.encode()));
            ConversionMetadata.Index firstAction = ConversionMetadata.readIndex(document);
            ConversionMetadata.Index secondAction = ConversionMetadata.readIndex(document);
            assertEquals(Optional.of(original), firstAction.forZotero(NATIVE));
            assertEquals(Optional.of(updated), secondAction.forZotero(changed));
            assertThrows(RenderException.class, () -> secondAction.forZotero(NATIVE));
            properties.verify(() -> UnoUserDefinedProperty.getListOfNames(document), times(2));
            properties.verify(() -> UnoUserDefinedProperty.getStringValue(document, property), times(2));
        }
    }

    @Test
    void freshRelinkLeavesAnUnrelatedStaleSnapshotUntouched() throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        ConversionMetadata fresh = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL fresh_uid", List.of("", ""), NATIVE);
        String unrelatedName = NATIVE.replace("foreign-id", "other-id").replace("RNDnative1234", "RNDsecond1234");
        ConversionMetadata unrelated = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL other_uid", List.of("", ""), unrelatedName);
        String staleCurrent = unrelatedName.replace("\"extra\":42", "\"extra\":43");
        String linked = NATIVE.replace("\"extra\":42", "\"extra\":42,\"linked\":true");
        String freshProperty = ConversionMetadata.PREFIX + "fresh_uid";
        String unrelatedProperty = ConversionMetadata.PREFIX + "other_uid";
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(unrelatedProperty, freshProperty));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, freshProperty)).thenReturn(Optional.of(fresh.encode()));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, unrelatedProperty)).thenReturn(Optional.of(unrelated.encode()));
            ConversionMetadata.Index index = ConversionMetadata.readIndex(document);
            ConversionMetadata prepared = index.prepareRelink(document, NATIVE, linked).orElseThrow();
            assertEquals(new ConversionMetadata(fresh.canonicalName(), fresh.pageInfos(), linked), prepared);
            assertEquals(Optional.empty(), index.prepareRelink(document, staleCurrent, staleCurrent));
            assertThrows(RenderException.class, () -> index.forZotero(staleCurrent));
            assertEquals(Optional.of(fresh), index.forZotero(NATIVE));
            undoable.verify(() -> UndoableDocumentProperties.preflight(document, Map.of(freshProperty, Optional.of(prepared.encode()))));
            undoable.verifyNoMoreInteractions();
        }
    }

    @Test
    void relinkCannotChangeTheStoredKeyLayoutOrReplaceTheOriginalSnapshot() throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        ConversionMetadata original = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL stable_uid", List.of("", ""), NATIVE);
        String property = ConversionMetadata.PREFIX + "stable_uid";
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(property));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, property)).thenReturn(Optional.of(original.encode()));
            ConversionMetadata.Index index = ConversionMetadata.readIndex(document);
            assertEquals(Optional.empty(), index.prepareRelink(document, NATIVE, NATIVE.replace("/beta", "/different")));
            assertEquals(Optional.of(original), index.forZotero(NATIVE));
            undoable.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"corrupt", "missing", "wrong-owner"})
    void indexRejectsInvalidRecordsStrictlyButNeverOverwritesThemDuringRelinking(String invalidRecord) throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        ConversionMetadata original = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL stable_uid", List.of("", ""), NATIVE);
        String property = ConversionMetadata.PREFIX + "wrong-owner";
        Optional<String> value = switch (invalidRecord) {
            case "corrupt" ->
                    Optional.of("not JSON");
            case "missing" ->
                    Optional.empty();
            default ->
                    Optional.of(original.encode());
        };
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(property));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, property)).thenReturn(value);
            ConversionMetadata.Index index = ConversionMetadata.readIndex(document);
            assertThrows(RenderException.class, () -> index.forZotero(NATIVE));
            assertEquals(Optional.empty(), index.prepareRelink(document, NATIVE, NATIVE));
            undoable.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"exact", "identity", "suffix"})
    void strictIndexRejectsAmbiguousIdentitiesOrSuffixesWhileRelinkingRequiresOneExactSnapshot(String collision) throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        ConversionMetadata first = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL first_uid", List.of("", ""), NATIVE);
        String secondName = switch (collision) {
            case "identity" ->
                    NATIVE.replace("RNDnative1234", "RNDsecond1234");
            case "suffix" ->
                    NATIVE.replace("foreign-id", "second-id");
            default ->
                    NATIVE;
        };
        ConversionMetadata second = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL second_uid", List.of("", ""), secondName);
        String firstProperty = ConversionMetadata.PREFIX + "first_uid";
        String secondProperty = ConversionMetadata.PREFIX + "second_uid";
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(firstProperty, secondProperty));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, firstProperty)).thenReturn(Optional.of(first.encode()));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, secondProperty)).thenReturn(Optional.of(second.encode()));
            ConversionMetadata.Index index = ConversionMetadata.readIndex(document);
            assertThrows(RenderException.class, () -> index.forZotero(NATIVE));
            if ("exact".equals(collision)) {
                assertEquals(Optional.empty(), index.prepareRelink(document, NATIVE, NATIVE));
                undoable.verifyNoInteractions();
            } else {
                assertEquals(Optional.of(first), index.prepareRelink(document, NATIVE, NATIVE));
                undoable.verify(() -> UndoableDocumentProperties.preflight(document, Map.of(firstProperty, Optional.of(first.encode()))));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"identity", "suffix"})
    void strictIndexRejectsStaleSnapshotsMatchedByEitherNativeIdentity(String match) throws Exception {
        XTextDocument document = mock(XTextDocument.class);
        ConversionMetadata original = new ConversionMetadata("JABREF_beta, JABREF_alpha NORMAL stable_uid", List.of("", ""), NATIVE);
        String current = "identity".equals(match) ? NATIVE.replace("RNDnative1234", "RNDsecond1234") : NATIVE.replace("foreign-id", "second-id");
        String property = ConversionMetadata.PREFIX + "stable_uid";
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(property));
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, property)).thenReturn(Optional.of(original.encode()));
            ConversionMetadata.Index index = ConversionMetadata.readIndex(document);
            assertThrows(RenderException.class, () -> index.forZotero(current));
            assertEquals(Optional.empty(), index.prepareRelink(document, current, current));
            undoable.verifyNoInteractions();
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

    @ParameterizedTest
    @ValueSource(strings = {"locator", "label", "prefix", "suffix", "author-only", "suppress-author", "mixed-suppression"})
        // [utest->req~openoffice.zotero-conversion-modifier-compatibility~1]
    void nativeModifiersSurviveMetadataRoundTripWithExistingGroupType(String modifier) throws Exception {
        JsonObject json = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(NATIVE)).getAsJsonObject();
        JsonObject first = json.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        if ("suppress-author".equals(modifier)) {
            json.getAsJsonArray("citationItems").forEach(item -> item.getAsJsonObject().addProperty("suppress-author", true));
        } else if ("mixed-suppression".equals(modifier)) {
            first.addProperty("suppress-author", true);
        } else if ("author-only".equals(modifier)) {
            first.addProperty(modifier, true);
        } else {
            first.addProperty(modifier, "value");
        }
        String name = ZoteroReferenceMark.PREFIX + json + " RNDnative1234";
        ZoteroReferenceMark mark = ZoteroReferenceMark.parse(name).orElseThrow();
        CitationType expectedType = "suppress-author".equals(modifier) || "mixed-suppression".equals(modifier)
                                    ? CitationType.AUTHORYEAR_INTEXT : CitationType.AUTHORYEAR_PAR;
        ConversionMetadata metadata = new ConversionMetadata(JabRefMarkCodec.encode(new JabRefMarkData(mark.citationKeys(), "stable_uid", expectedType)),
                List.of("", ""), name);

        ConversionMetadata restored = ConversionMetadata.decode(metadata.encode());

        assertEquals(metadata, restored);
        assertEquals(name, restored.zoteroName());
        assertEquals(expectedType, restored.group().citationType);
        assertEquals(expectedType, mark.citationType());
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
