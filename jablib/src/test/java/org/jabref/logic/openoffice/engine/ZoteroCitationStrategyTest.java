package org.jabref.logic.openoffice.engine;

import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.JabRefException;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.ZoteroCitationLinker;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.backend.WriterReferenceMark;
import org.jabref.logic.openoffice.backend.WriterReferenceMark.BoundaryPolicy;
import org.jabref.logic.openoffice.backend.WriterReferenceMarks;
import org.jabref.logic.openoffice.engine.ZoteroCitationStrategy.Reason;
import org.jabref.logic.openoffice.engine.ZoteroCitationStrategy.StrategyException;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.OOTextWriter;
import org.jabref.logic.openoffice.uno.UnoDispatch;
import org.jabref.logic.openoffice.uno.UnoRedlines;
import org.jabref.logic.openoffice.uno.UnoUndo;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.document.XRedlinesSupplier;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.jabref.logic.openoffice.engine.RendererTestData.entry;
import static org.jabref.logic.openoffice.engine.RendererTestData.numericStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@NullMarked
@ResourceLock("Localization.lang")
class ZoteroCitationStrategyTest {

    private static final String ZOTERO = ZoteroReferenceMark.PREFIX + """
            {"citationID":"citation1","citationItems":[{"id":"1",\
            "uris":["http://zotero.org/users/1/items/ITEM1","http://www.jabref.org/a"],\
            "itemData":{"id":"1","citation-key":"a","type":"article","title":"A"}}],\
            "properties":{"noteIndex":3,"custom":"keep metadata"}} RNDabcdefghij""";
    private static final String LEGACY_CID = "JABREF_legacy CID_7 uid NORMAL";
    private static final String LEGACY_JSTYLE = "JR_cite0_1_legacy";
    private static final String CANONICAL = "JABREF_canonical NORMAL stableuid";

    private final XTextDocument document = mock(XTextDocument.class, withSettings().extraInterfaces(XTextSectionsSupplier.class, XRedlinesSupplier.class));
    private final XComponentContext context = mock(XComponentContext.class);
    private final OpenOfficePreferences preferences = mock(OpenOfficePreferences.class);
    private final BibEntryTypesManager entryTypes = new BibEntryTypesManager();
    private final WriterReferenceMarks ranges = mock(WriterReferenceMarks.class);
    private final XText text = mock(XText.class, withSettings().extraInterfaces(XTextRangeCompare.class));
    private final XTextCursor cursor = mock(XTextCursor.class);
    private final WriterReferenceMark range = mock(WriterReferenceMark.class);
    private final Map<XTextRange, int[]> positions = new IdentityHashMap<>();
    private final XEnumeration redlines = mock(XEnumeration.class);

    @BeforeEach
    void prepareDocument() throws Exception {
        when(preferences.getCslBibliographyBodyFormat()).thenReturn("Text body");
        when(ranges.names(document)).thenReturn(List.of());
        when(text.createTextCursorByRange(any(XTextRange.class))).thenReturn(cursor);
        when(cursor.getText()).thenReturn(text);
        when(cursor.getStart()).thenReturn(cursor);
        when(cursor.getEnd()).thenReturn(cursor);
        positions.put(cursor, new int[] {10, 10});
        XTextRangeCompare compare = (XTextRangeCompare) text;
        when(compare.compareRegionStarts(any(XTextRange.class), any(XTextRange.class))).thenAnswer(invocation ->
                (short) Integer.compare(coordinate(invocation.getArgument(1), 0), coordinate(invocation.getArgument(0), 0)));
        when(compare.compareRegionEnds(any(XTextRange.class), any(XTextRange.class))).thenAnswer(invocation ->
                (short) Integer.compare(coordinate(invocation.getArgument(1), 1), coordinate(invocation.getArgument(0), 1)));
        XNameAccess sections = mock(XNameAccess.class);
        when(((XTextSectionsSupplier) document).getTextSections()).thenReturn(sections);
        when(sections.getElementNames()).thenReturn(new String[0]);
        XEnumerationAccess changes = mock(XEnumerationAccess.class);
        when(((XRedlinesSupplier) document).getRedlines()).thenReturn(changes);
        when(changes.createEnumeration()).thenReturn(redlines);
        WriterReferenceMark foreign = mock(WriterReferenceMark.class);
        XTextRange foreignAnchor = span(30, 35);
        when(foreign.getMarkRange(document)).thenReturn(Optional.of(foreignAnchor));
        when(ranges.find(document, LEGACY_CID, BoundaryPolicy.TEMPORARY)).thenReturn(Optional.of(foreign));
        when(cursor.getString()).thenReturn("[1]");
        XTextRange anchor = span(1, 4);
        when(range.getMarkRange(document)).thenReturn(Optional.of(anchor));
        when(range.getRawCursor(document)).thenReturn(Optional.of(cursor));
        when(range.getFillCursor(document)).thenReturn(cursor);
        when(ranges.find(document, ZOTERO, BoundaryPolicy.TEMPORARY)).thenReturn(Optional.of(range));
        when(ranges.create(eq(document), eq(context), anyString(), eq(cursor), anyBoolean(), anyBoolean(), eq(BoundaryPolicy.TEMPORARY))).thenReturn(range);
    }

    @ParameterizedTest
    @MethodSource("nonCslStyles")
    void nonCslStylesReturnLocalizedTypedErrorBeforeDocumentAccess(OOStyle style) {
        OOResult<List<String>, JabRefException> result = strategy().refresh(List.of(), style);
        StrategyException error = assertInstanceOf(StrategyException.class, result.getError());

        assertEquals(Reason.UNSUPPORTED_STYLE, error.reason());
        assertEquals(Localization.lang("Cannot generate citation based on selected citation style."), error.getLocalizedMessage());
        verifyNoInteractions(document, context, ranges, preferences);
    }

    private static Stream<OOStyle> nonCslStyles() {
        return Stream.of(mock(JStyle.class), mock(BstStyle.class));
    }

    @ParameterizedTest
    @EnumSource(value = CitationType.class, names = {"AUTHOR_ONLY", "YEAR_ONLY", "AUTHORYEAR_NOPAR"})
    void unsupportedTypesAreNotSilentlyReinterpreted(CitationType type) {
        OOResult<List<String>, JabRefException> result = strategy().insert(List.of(entry("a", "A")), List.of(), style(), type, "", cursor, false);

        assertEquals(Reason.UNSUPPORTED_TYPE, assertInstanceOf(StrategyException.class, result.getError()).reason());
        verifyNoInteractions(document, context, ranges);
    }

    @Test
    void pageInfoReturnsTypedErrorBeforeAnyWrites() {
        OOResult<List<String>, JabRefException> result = strategy().insert(List.of(entry("a", "A")), List.of(), style(), CitationType.AUTHORYEAR_PAR, "p. 5", cursor, false);

        assertEquals(Reason.PAGE_INFO_NOT_SUPPORTED, assertInstanceOf(StrategyException.class, result.getError()).reason());
        verifyNoInteractions(document, ranges);
    }

    @Test
    void missingKeysAreRejectedWithoutEntryIdFallback() {
        OOResult<List<String>, JabRefException> result = strategy().insert(List.of(new BibEntry()), List.of(), style(), CitationType.AUTHORYEAR_PAR, "", cursor, false);

        assertEquals(Reason.INVALID_ENTRY, assertInstanceOf(StrategyException.class, result.getError()).reason());
        verifyNoInteractions(document, ranges);
    }

    @Test
    void exportReadsOnlyZoteroMarksAndDoesNotModifyDocument() throws Exception {
        when(ranges.names(document)).thenReturn(List.of(LEGACY_CID, LEGACY_JSTYLE, CANONICAL, ZOTERO));
        BibDatabase database = new BibDatabase(List.of(entry("a", "A"), entry("legacy", "Old"), entry("canonical", "Canonical")));
        CitedEntryExporter.ExportResult result = strategy().exportCited(List.of(database)).get();

        assertEquals(List.of("a"), result.database().getEntries().stream().map(entry -> entry.getCitationKey().orElseThrow()).toList());
        assertEquals(List.of(), result.unresolvedKeys());
        verify(ranges, never()).find(document, LEGACY_CID, BoundaryPolicy.TEMPORARY);
        verify(ranges, never()).find(document, LEGACY_JSTYLE, BoundaryPolicy.TEMPORARY);
        verify(ranges, never()).find(document, CANONICAL, BoundaryPolicy.TEMPORARY);
        verifyNoInteractions(document, preferences, context);
    }

    @ParameterizedTest
    @MethodSource("unsupportedNativeUpdates")
        // [utest->req~openoffice.zotero-modifier-safe-update~1]
    void unsupportedModifiersRejectNativeUpdatesBeforeLinkingRenderingOrWrites(String operation, String name) throws Exception {
        when(ranges.names(document)).thenReturn(List.of(ZOTERO, name));
        when(ranges.find(document, name, BoundaryPolicy.TEMPORARY)).thenReturn(Optional.of(range));
        BibEntry entry = entry("a", "A");
        List<BibDatabase> databases = List.of(new BibDatabase(List.of(entry)));
        try (MockedStatic<ZoteroCitationLinker> linker = mockStatic(ZoteroCitationLinker.class);
             MockedStatic<ConversionMetadata> metadata = mockStatic(ConversionMetadata.class);
             MockedStatic<OOTextWriter> writer = mockStatic(OOTextWriter.class);
             MockedStatic<ZoteroDocumentPreferences> documentPreferences = mockStatic(ZoteroDocumentPreferences.class)) {
            OOResult<List<String>, JabRefException> result = "refresh".equals(operation)
                                                             ? strategy().refresh(databases, style())
                                                             : strategy().insert(List.of(entry), databases, style(), CitationType.AUTHORYEAR_PAR, "", cursor, true);
            StrategyException error = assertInstanceOf(StrategyException.class, result.getError());
            assertEquals(Reason.UNSUPPORTED_MODIFIERS, error.reason());
            assertEquals(Localization.lang("Cannot update Zotero citations with unsupported modifiers. No citations were changed. Update them in Zotero instead."), error.getLocalizedMessage());
            assertEquals("Unsupported native Zotero citation modifiers", assertInstanceOf(RenderException.class, error.getCause()).getMessage());
            linker.verifyNoInteractions();
            metadata.verifyNoInteractions();
            writer.verifyNoInteractions();
            documentPreferences.verifyNoInteractions();
            verifyNoInteractions(document, context, preferences, range);
            verify(ranges, never()).find(any(), anyString(), any(BoundaryPolicy.class));
            verify(ranges, never()).create(any(), any(), anyString(), any(), anyBoolean(), anyBoolean(), any(BoundaryPolicy.class));
            verify(cursor, never()).setString(anyString());
            verify(text, never()).removeTextContent(any());
        }
    }

    @ParameterizedTest
    @MethodSource("unsupportedNativeUpdates")
        // [utest->req~openoffice.zotero-modifier-safe-update~1]
    void engineRejectsUnsupportedNativeModifiersBeforeOpeningAWriteSession(String operation, String name) throws Exception {
        when(preferences.getZoteroCompatibilityMode()).thenReturn(true);
        BibEntry entry = entry("a", "A");
        List<BibDatabase> databases = List.of(new BibDatabase(List.of(entry)));
        try (MockedStatic<WriterReferenceMarks> marks = mockStatic(WriterReferenceMarks.class);
             MockedStatic<UnoUndo> undo = mockStatic(UnoUndo.class);
             MockedStatic<UnoRedlines> recording = mockStatic(UnoRedlines.class)) {
            marks.when(() -> WriterReferenceMarks.getListOfNames(document)).thenReturn(List.of(LEGACY_CID, ZOTERO, name));
            try (CitationEngine engine = new CitationEngine(document, context, preferences, entryTypes)) {
                undo.clearInvocations();
                OOResult<List<String>, JabRefException> result = "refresh".equals(operation)
                                                                 ? engine.refresh(databases, style())
                                                                 : engine.insert(List.of(entry), databases, style(), CitationType.AUTHORYEAR_PAR, "", cursor, true);

                StrategyException error = assertInstanceOf(StrategyException.class, result.getError());
                assertEquals(Reason.UNSUPPORTED_MODIFIERS, error.reason());
                assertEquals(Localization.lang("Cannot update Zotero citations with unsupported modifiers. No citations were changed. Update them in Zotero instead."), error.getLocalizedMessage());
                assertEquals("Unsupported native Zotero citation modifiers", assertInstanceOf(RenderException.class, error.getCause()).getMessage());
                undo.verifyNoInteractions();
                recording.verifyNoInteractions();
                verify(document, never()).lockControllers();
                verify(document, never()).unlockControllers();
                verify(document, never()).getText();
                verifyNoInteractions(context);
                verify(cursor, never()).setString(anyString());
            }
        }
    }

    @Test
    void malformedSuppressionWithLocatorCannotBeReadByTheTolerantParser() {
        assertEquals(Optional.empty(), ZoteroReferenceMark.parse(malformedSuppressionWithLocatorName()));
    }

    @ParameterizedTest
    @MethodSource("unsupportedNativeNames")
    void exportWithUnsupportedModifiersStillReadsKeysWithoutDocumentWrites(String name) throws Exception {
        when(ranges.names(document)).thenReturn(List.of(name));
        when(ranges.find(document, name, BoundaryPolicy.TEMPORARY)).thenReturn(Optional.of(range));
        CitedEntryExporter.ExportResult result = strategy().exportCited(List.of(new BibDatabase(List.of(entry("a", "A"))))).get();

        assertEquals(List.of("a"), result.database().getEntries().stream().map(entry -> entry.getCitationKey().orElseThrow()).toList());
        assertEquals(List.of(), result.unresolvedKeys());
        verifyNoInteractions(document, context, preferences);
        verify(ranges, never()).create(any(), any(), anyString(), any(), anyBoolean(), anyBoolean(), any(BoundaryPolicy.class));
        verify(cursor, never()).setString(anyString());
    }

    private static Stream<Arguments> unsupportedNativeUpdates() {
        return Stream.concat(unsupportedNativeNames(), Stream.of(malformedSuppressionWithLocatorName()))
                     .flatMap(name -> Stream.of(Arguments.of("refresh", name), Arguments.of("insert", name)));
    }

    private static String malformedSuppressionWithLocatorName() {
        JsonObject citation = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(ZOTERO)).getAsJsonObject();
        JsonObject item = citation.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        item.addProperty("locator", "17");
        item.add("suppress-author", new JsonObject());
        return ZoteroReferenceMark.PREFIX + citation + " RNDabcdefghij";
    }

    private static Stream<String> unsupportedNativeNames() {
        Stream.Builder<String> names = Stream.builder();
        for (String field : List.of("locator", "label", "prefix", "suffix")) {
            for (String location : List.of("citation", "properties", "item")) {
                JsonObject citation = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(ZOTERO)).getAsJsonObject();
                JsonObject target = switch (location) {
                    case "properties" ->
                            citation.getAsJsonObject("properties");
                    case "item" ->
                            citation.getAsJsonArray("citationItems").get(0).getAsJsonObject();
                    default ->
                            citation;
                };
                target.addProperty(field, "value");
                names.add(ZoteroReferenceMark.PREFIX + citation + " RNDabcdefghij");
            }
        }
        JsonObject authorOnly = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(ZOTERO)).getAsJsonObject();
        authorOnly.getAsJsonArray("citationItems").get(0).getAsJsonObject().addProperty("author-only", true);
        names.add(ZoteroReferenceMark.PREFIX + authorOnly + " RNDabcdefghij");
        JsonObject mixed = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(ZOTERO)).getAsJsonObject();
        JsonObject item = mixed.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        mixed.getAsJsonArray("citationItems").add(item.deepCopy());
        item.addProperty("suppress-author", true);
        names.add(ZoteroReferenceMark.PREFIX + mixed + " RNDabcdefghij");
        return names.build();
    }

    @Test
    void exportRereadsMarksInsteadOfRetainingEarlierActionState() throws Exception {
        when(ranges.names(document)).thenReturn(List.of(ZOTERO), List.of(LEGACY_CID));
        ZoteroCitationStrategy strategy = strategy();
        BibDatabase database = new BibDatabase(List.of(entry("a", "A")));
        assertEquals(1, strategy.exportCited(List.of(database)).get().database().getEntryCount());

        assertEquals(List.of(), strategy.exportCited(List.of(database)).get().database().getEntries());
    }

    @Test
    void refreshUsesExplicitStyleAndPreservesMarkIdentityAndMetadata() throws Exception {
        when(ranges.names(document)).thenReturn(List.of(ZOTERO, LEGACY_CID));
        CitationStyle style = style();
        try (MockedStatic<ZoteroCitationLinker> linker = mockStatic(ZoteroCitationLinker.class, CALLS_REAL_METHODS);
             MockedStatic<ZoteroDocumentPreferences> documentPreferences = mockStatic(ZoteroDocumentPreferences.class);
             MockedStatic<OOTextWriter> writer = mockStatic(OOTextWriter.class);
             MockedStatic<UnoDispatch> dispatch = mockStatic(UnoDispatch.class)) {
            documentPreferences.when(() -> ZoteroDocumentPreferences.writeCitationStyle(document, style)).thenReturn(true);
            OOResult<List<String>, JabRefException> result = strategy().refresh(List.of(new BibDatabase(List.of(entry("a", "A")))), style);

            assertEquals(List.of(), result.get());
            writer.verify(() -> OOTextWriter.write(document, cursor, OOText.fromString("<span lang=\"zxx\">[1]</span>")));
            linker.verify(() -> ZoteroCitationLinker.planLinks(eq(List.of(ZOTERO)), any(BibDatabaseContext.class), eq(entryTypes)));
            documentPreferences.verify(() -> ZoteroDocumentPreferences.writeCitationStyle(document, style));
            verify(ranges, never()).create(any(), any(), anyString(), any(), anyBoolean(), anyBoolean(), any(BoundaryPolicy.class));
            verify(preferences, never()).getCurrentStyle();
            verify(preferences, never()).zoteroCompatibilityModeProperty();
            verify(ranges).find(document, ZOTERO, BoundaryPolicy.TEMPORARY);
        }
    }

    @Test
    void insertionReusesTheLinkedZoteroUriWithoutMigratingCidMarks() throws Exception {
        when(ranges.names(document)).thenReturn(List.of(ZOTERO, LEGACY_CID));
        CitationStyle style = style();
        try (MockedStatic<ZoteroCitationLinker> linker = mockStatic(ZoteroCitationLinker.class, CALLS_REAL_METHODS);
             MockedStatic<ZoteroDocumentPreferences> documentPreferences = mockStatic(ZoteroDocumentPreferences.class);
             MockedStatic<OOTextWriter> writer = mockStatic(OOTextWriter.class);
             MockedStatic<UnoDispatch> dispatch = mockStatic(UnoDispatch.class)) {
            documentPreferences.when(() -> ZoteroDocumentPreferences.writeCitationStyle(document, style)).thenReturn(true);
            BibEntry entry = entry("a", "A");
            OOResult<List<String>, JabRefException> result = strategy().insert(List.of(entry), List.of(new BibDatabase(List.of(entry))), style, CitationType.AUTHORYEAR_PAR, "", cursor, false);
            assertEquals(List.of(), result.get());
            ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
            verify(ranges).create(eq(document), eq(context), name.capture(), eq(cursor), eq(false), eq(false), eq(BoundaryPolicy.TEMPORARY));

            assertEquals("http://zotero.org/users/1/items/ITEM1", ZoteroReferenceMark.extractZoteroUriByCitationKey(name.getValue()).get("a"));
            assertEquals(List.of("a"), ZoteroReferenceMark.parse(name.getValue()).orElseThrow().citationKeys());
            verify(ranges).find(document, LEGACY_CID, BoundaryPolicy.TEMPORARY);
        }
    }

    @Test
    void emptyZoteroCitationStaysEmptyAfterFreshStrategyAndRead() throws Exception {
        when(ranges.names(document)).thenReturn(List.of(ZOTERO));
        when(cursor.getString()).thenReturn("\u200b\u200b");
        CitationStyle style = style();
        try (MockedStatic<ZoteroCitationLinker> linker = mockStatic(ZoteroCitationLinker.class, CALLS_REAL_METHODS);
             MockedStatic<ZoteroDocumentPreferences> documentPreferences = mockStatic(ZoteroDocumentPreferences.class);
             MockedStatic<OOTextWriter> writer = mockStatic(OOTextWriter.class);
             MockedStatic<UnoDispatch> dispatch = mockStatic(UnoDispatch.class)) {
            documentPreferences.when(() -> ZoteroDocumentPreferences.writeCitationStyle(document, style)).thenReturn(true);
            BibDatabase database = new BibDatabase(List.of(entry("a", "A")));
            assertEquals(List.of(), strategy().refresh(List.of(database), style).get());
            assertEquals(List.of(), strategy().refresh(List.of(database), style).get());

            writer.verify(() -> OOTextWriter.write(document, cursor, OOText.fromString("")), times(2));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"insert", "refresh"})
    void nativeLinkMetadataReadFailureIsReportedBeforeAnyWrites(String operation) throws Exception {
        String nativeName = ZOTERO.replace(",\"http://www.jabref.org/a\"", "")
                                  .replace("\"title\":\"A\"", "\"title\":\"A\",\"DOI\":\"10.123/native\"");
        when(ranges.names(document)).thenReturn(List.of(nativeName));
        when(ranges.find(document, nativeName, BoundaryPolicy.TEMPORARY)).thenReturn(Optional.of(range));
        BibEntry entry = entry("a", "A").withField(StandardField.DOI, "10.123/native");
        BibDatabase database = new BibDatabase(List.of(entry));
        try (MockedStatic<ConversionMetadata> metadata = mockStatic(ConversionMetadata.class);
             MockedStatic<OOTextWriter> writer = mockStatic(OOTextWriter.class);
             MockedStatic<ZoteroDocumentPreferences> documentPreferences = mockStatic(ZoteroDocumentPreferences.class)) {
            metadata.when(() -> ConversionMetadata.prepareRelink(eq(document), eq(nativeName), anyString()))
                    .thenThrow(new WrappedTargetException("Cannot read citation conversion properties"));
            OOResult<List<String>, JabRefException> result = "refresh".equals(operation) ? strategy().refresh(List.of(database), style())
                                                                                         : strategy().insert(List.of(entry), List.of(database), style(), CitationType.AUTHORYEAR_PAR, "", cursor, false);
            assertEquals(Reason.DOCUMENT_FAILURE, assertInstanceOf(StrategyException.class, result.getError()).reason());
            writer.verifyNoInteractions();
            documentPreferences.verifyNoInteractions();
            verify(ranges, never()).create(any(), any(), anyString(), any(), anyBoolean(), anyBoolean(), any(BoundaryPolicy.class));
            verify(text, never()).removeTextContent(any());
        }
    }

    @Test
    void linkableNativeMarkIsNotRenamedWhenRenderingFails() throws Exception {
        String nativeName = ZOTERO.replace(",\"http://www.jabref.org/a\"", "")
                                  .replace("\"title\":\"A\"", "\"title\":\"A\",\"DOI\":\"10.123/native\"");
        when(ranges.names(document)).thenReturn(List.of(nativeName));
        when(ranges.find(document, nativeName, BoundaryPolicy.TEMPORARY)).thenReturn(Optional.of(range));
        BibDatabase database = new BibDatabase(List.of(entry("a", "A").withField(StandardField.DOI, "10.123/native")));
        CitationStyle invalid = new CitationStyle("invalid.csl", "invalid", "in-text", "Invalid", "", true, false, false, false, "not XML", false);
        try (MockedStatic<OOTextWriter> writer = mockStatic(OOTextWriter.class);
             MockedStatic<ZoteroDocumentPreferences> documentPreferences = mockStatic(ZoteroDocumentPreferences.class)) {
            OOResult<List<String>, JabRefException> result = strategy().refresh(List.of(database), invalid);

            assertEquals(Reason.DOCUMENT_FAILURE, assertInstanceOf(StrategyException.class, result.getError()).reason());
            writer.verifyNoInteractions();
            documentPreferences.verifyNoInteractions();
            verify(ranges, never()).create(any(), any(), anyString(), any(), anyBoolean(), anyBoolean(), any(BoundaryPolicy.class));
            verify(text, never()).removeTextContent(any());
        }
    }

    @Test
    void pendingDeletionsAreCheckedWithoutConsultingRecordingFlag() throws Exception {
        when(ranges.names(document)).thenReturn(List.of(ZOTERO));
        XTextRange start = point(2);
        XTextRange end = point(3);
        XTextCursor deletion = mock(XTextCursor.class);
        positions.put(deletion, new int[] {2, 3});
        when(deletion.getText()).thenReturn(text);
        when(deletion.getStart()).thenReturn(start);
        when(deletion.getEnd()).thenReturn(end);
        when(text.createTextCursorByRange(start)).thenReturn(deletion);
        XPropertySet change = mock(XPropertySet.class);
        when(change.getPropertyValue("RedlineType")).thenReturn("Delete");
        when(change.getPropertyValue("RedlineStart")).thenReturn(start);
        when(change.getPropertyValue("RedlineEnd")).thenReturn(end);
        when(redlines.hasMoreElements()).thenReturn(true, false);
        when(redlines.nextElement()).thenReturn(change);
        try (MockedStatic<UnoRedlines> recording = mockStatic(UnoRedlines.class, CALLS_REAL_METHODS);
             MockedStatic<ZoteroCitationLinker> linker = mockStatic(ZoteroCitationLinker.class, CALLS_REAL_METHODS);
             MockedStatic<OOTextWriter> writer = mockStatic(OOTextWriter.class)) {
            OOResult<List<String>, JabRefException> result = strategy().refresh(List.of(new BibDatabase(List.of(entry("a", "A")))), style());

            assertEquals(Reason.PENDING_DELETIONS, assertInstanceOf(StrategyException.class, result.getError()).reason());
            recording.verify(() -> UnoRedlines.getRecordChanges(document), never());
            linker.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void insertionProtectsOwnAndUnmanagedMarkAnchorsBeforePlanning(boolean foreign) throws Exception {
        when(ranges.names(document)).thenReturn(foreign ? List.of(LEGACY_CID) : List.of(ZOTERO));
        positions.put(cursor, foreign ? new int[] {32, 32} : new int[] {2, 2});
        try (MockedStatic<ZoteroCitationLinker> linker = mockStatic(ZoteroCitationLinker.class, CALLS_REAL_METHODS);
             MockedStatic<OOTextWriter> writer = mockStatic(OOTextWriter.class)) {
            OOResult<List<String>, JabRefException> result = strategy().insert(List.of(entry("a", "A")), List.of(), style(), CitationType.AUTHORYEAR_PAR, "", cursor, false);

            assertEquals(Reason.PROTECTED_RANGE, assertInstanceOf(StrategyException.class, result.getError()).reason());
            linker.verifyNoInteractions();
            writer.verifyNoInteractions();
        }
    }

    @Test
    void nestedZoteroRangesAreRejectedBeforeLinkPlanning() throws Exception {
        String nestedName = ZOTERO.replace("citation1", "citation2");
        WriterReferenceMark nested = mock(WriterReferenceMark.class);
        XTextRange nestedAnchor = span(2, 3);
        when(nested.getMarkRange(document)).thenReturn(Optional.of(nestedAnchor));
        when(nested.getRawCursor(document)).thenReturn(Optional.of(cursor));
        when(ranges.find(document, nestedName, BoundaryPolicy.TEMPORARY)).thenReturn(Optional.of(nested));
        when(ranges.names(document)).thenReturn(List.of(ZOTERO, nestedName));
        try (MockedStatic<ZoteroCitationLinker> linker = mockStatic(ZoteroCitationLinker.class, CALLS_REAL_METHODS)) {
            OOResult<List<String>, JabRefException> result = strategy().refresh(List.of(new BibDatabase(List.of(entry("a", "A")))), style());

            assertEquals(Reason.OVERLAPPING_RANGES, assertInstanceOf(StrategyException.class, result.getError()).reason());
            linker.verifyNoInteractions();
        }
    }

    @Test
    void documentReadFailureIsReturnedAsCheckedLocalizedError() throws Exception {
        NoDocumentException cause = new NoDocumentException("Disconnected");
        when(ranges.names(document)).thenThrow(cause);
        OOResult<CitedEntryExporter.ExportResult, JabRefException> result = strategy().exportCited(List.of());
        StrategyException error = assertInstanceOf(StrategyException.class, result.getError());

        assertEquals(Reason.DOCUMENT_FAILURE, error.reason());
        assertSame(cause, error.getCause());
        assertEquals(Localization.lang("Problem collecting citations"), error.getLocalizedMessage());
    }

    @Test
    void closeIsIdempotentAndPreventsDocumentAccess() {
        ZoteroCitationStrategy strategy = strategy();
        strategy.close();
        strategy.close();

        assertEquals(Reason.CLOSED, assertInstanceOf(StrategyException.class, strategy.exportCited(List.of()).getError()).reason());
        verifyNoInteractions(document, ranges);
    }

    @Test
    void styleImplementationsAreFinal() {
        assertEquals(List.of(true, true, true), Stream.of(CitationStyle.class, JStyle.class, BstStyle.class).map(type -> Modifier.isFinal(type.getModifiers())).toList());
    }

    private int coordinate(XTextRange range, int endpoint) {
        return Optional.ofNullable(positions.get(range)).orElseThrow()[endpoint];
    }

    private XTextRange point(int position) {
        XTextRange range = mock(XTextRange.class);
        positions.put(range, new int[] {position, position});
        when(range.getText()).thenReturn(text);
        when(range.getStart()).thenReturn(range);
        when(range.getEnd()).thenReturn(range);
        return range;
    }

    private XTextRange span(int start, int end) {
        XTextRange range = mock(XTextRange.class);
        positions.put(range, new int[] {start, end});
        when(range.getText()).thenReturn(text);
        XTextRange first = point(start);
        XTextRange last = point(end);
        when(range.getStart()).thenReturn(first);
        when(range.getEnd()).thenReturn(last);
        return range;
    }

    private ZoteroCitationStrategy strategy() {
        return new ZoteroCitationStrategy(document, context, preferences, entryTypes, ranges);
    }

    private static CitationStyle style() {
        CitationStyle base = numericStyle(false, false);
        return new CitationStyle("zotero-test.csl", "http://www.zotero.org/styles/test", "in-text", "Zotero test", "", true, false, false, false, base.getSource(), false);
    }
}
