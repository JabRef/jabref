package org.jabref.logic.openoffice.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.logic.JabRefException;
import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.backend.ReferenceMarkTextWriter;
import org.jabref.logic.openoffice.backend.WriterReferenceMark;
import org.jabref.logic.openoffice.backend.WriterReferenceMarks;
import org.jabref.logic.openoffice.engine.CitationEngine;
import org.jabref.logic.openoffice.engine.CitedEntryExporter;
import org.jabref.logic.openoffice.engine.JabRefMarkCodec;
import org.jabref.logic.openoffice.engine.JabRefMarkData;
import org.jabref.logic.openoffice.engine.storage.DocumentStyleStore;
import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.engine.storage.UndoableDocumentProperties;
import org.jabref.logic.openoffice.uno.NamedTextContentPlacement;
import org.jabref.logic.openoffice.uno.UnoRedlines;
import org.jabref.logic.openoffice.uno.UnoTextSection;
import org.jabref.logic.openoffice.uno.UnoUndo;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;
import org.jabref.logic.openoffice.uno.WriterOperationException;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.document.XUndoManager;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextTable;
import com.sun.star.util.XSearchDescriptor;
import com.sun.star.util.XSearchable;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.jabref.logic.openoffice.integration.HeadlessLibreOffice.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockStatic;

@NullMarked
@Tag("LibreOfficeTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("Localization.lang")
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class CitationFormatConversionWriterTest {
    @TempDir
    Path temporaryDirectory;

    private final HeadlessLibreOffice office = new HeadlessLibreOffice(Path.of("build/writer-integration/format-conversion"));
    private final BibDatabase database = new BibDatabase(List.of(
            new BibEntry().withCitationKey("alpha").withField(StandardField.TITLE, "Alpha title").withField(StandardField.AUTHOR, "Alpha, Alice").withField(StandardField.YEAR, "2020"),
            new BibEntry().withCitationKey("beta").withField(StandardField.TITLE, "Beta title").withField(StandardField.AUTHOR, "Beta, Bob").withField(StandardField.YEAR, "2021"),
            new BibEntry().withCitationKey("native").withField(StandardField.TITLE, "Native linked title").withField(StandardField.DOI, "10.123/native")
                          .withField(StandardField.AUTHOR, "Native, Nina").withField(StandardField.YEAR, "2020")));

    @BeforeAll
    void start(@TempDir Path profile) throws Exception {
        office.start(profile.resolve("profile"));
    }

    @AfterEach
    void saveArtifacts(TestInfo info) throws Exception {
        office.saveAndCloseDocuments(info.getTestMethod().orElseThrow().getName());
    }

    @AfterAll
    void stop() throws Exception {
        office.close();
    }

    // [utest->req~openoffice.next-citation-format-conversion~1]
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void onlyNextInsertionConvertsAndSingleUndoRedoRestoresTheWholeTransaction(boolean targetZotero) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", true);
            assertNewCitationIdentifierLengths(document, !targetZotero);
            if (!targetZotero) {
                String bibliography = bibliographyNames(document).getFirst();
                assertEquals(10, bibliography.substring(bibliography.lastIndexOf(" RND") + 4).length(), bibliography);
            }
            addSourceDetailsDiscardedByConversion(document, !targetZotero);
            preferences.setZoteroCompatibilityMode(targetZotero);
            Snapshot toggled = snapshot(document);
            assertTrue(success(engine.requiresFormatConversion(style())));
            assertEquals(toggled, snapshot(document));
            success(engine.exportCited(List.of(database)));
            assertEquals(toggled, snapshot(document));
            success(engine.refresh(List.of(database), style()));
            assertTrue(success(engine.requiresFormatConversion(style())));
            assertEquals(1, managedNames(document, !targetZotero).size());
            assertEquals(List.of(), managedNames(document, targetZotero));
        }
        // A new connection must detect the same mismatch without any preference-listener history.
        try (CitationEngine engine = engine(document, preferences)) {
            assertTrue(success(engine.requiresFormatConversion(style())));
            XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
            undo.clear();
            UnoRedlines.setRecordChanges(document, true);
            Snapshot before = snapshot(document);
            insert(engine, document, "beta", false);
            assertFalse(success(engine.requiresFormatConversion(style())));
            assertEquals(2, managedNames(document, targetZotero).size());
            assertNewCitationIdentifierLengths(document, targetZotero);
            assertEquals(List.of(), managedNames(document, !targetZotero));
            assertEquals(before.sections(), snapshot(document).sections());
            assertTrue(UnoRedlines.getRecordChanges(document));
            assertFalse(document.hasControllersLocked());
            assertEquals(Optional.of(new DocumentStyleStore.State(DocumentStyleStore.Family.CSL, style().getStyleId())), new DocumentStyleStore().read(document));
            if (targetZotero) {
                assertEquals(Optional.of(style().getStyleId()), ZoteroDocumentPreferences.findCitationStyle(document, List.of(style())).map(CitationStyle::getStyleId));
            } else {
                assertEquals(nativePreferenceProperties(before), nativePreferenceProperties(snapshot(document)));
            }
            assertNoConversionArchive(document);
            Snapshot after = snapshot(document);
            assertEquals(1, undo.getAllUndoActionTitles().length);
            undo.undo();
            assertEquals(before, snapshot(document));
            assertTrue(success(engine.requiresFormatConversion(style())));
            undo.redo();
            assertEquals(after, snapshot(document));
        }
    }

    @ParameterizedTest
    @EnumSource(CitationType.class)
    void canonicalStyleChangesPreserveMetadataButCrossFormatConversionsLoseIt(CitationType type) throws Exception {
        XTextDocument document = office.newDocument();
        MarkStore store = new MarkStore();
        String uid = ("stored" + type.ordinal() + "_").repeat(type.ordinal() % 2 == 0 ? 1 : 3);
        CitationGroup original = store.createCitationGroup(document, office.context(), List.of("beta", "alpha"),
                List.of(Optional.of(OOText.fromString("<b>p. 4</b>")), Optional.of(OOText.fromString("pp. 7–9"))),
                type, separatedEnd(document), false, false, uid);
        String originalName = original.getReferenceMarkNameForLinking().orElseThrow();
        Snapshot beforeRead = snapshot(document);
        store.readAll(document);
        assertEquals(beforeRead, snapshot(document));
        try (CitationEngine engine = engine(document, preferences(false))) {
            for (CitationStyle selected : List.of(style(), CSLStyleUtils.createCitationStyleFromFile(resource("superscript.csl").toString()).orElseThrow())) {
                assertFalse(success(engine.requiresFormatConversion(selected)));
                success(engine.refresh(List.of(database), selected));
                assertEquals(List.of(originalName), managedNames(document, false));
                CitationGroup unchanged = store.readCitationGroupFromDocumentOrThrow(document, originalName);
                assertEquals(uid, unchanged.groupId.citationGroupIdAsString());
                assertEquals(type, unchanged.citationType);
                assertEquals(List.of("beta", "alpha"), unchanged.citationsInStorageOrder.stream().map(citation -> citation.citationKey).toList());
                assertEquals(List.of(Optional.of("<b>p. 4</b>"), Optional.of("pp. 7–9")),
                        unchanged.citationsInStorageOrder.stream().map(citation -> citation.getPageInfo().map(Object::toString)).toList());
                assertEquals(Optional.of("<b>p. 4</b>"), UnoUserDefinedProperty.getStringValue(document, MarkStore.pageInfoProperty(uid, 0)));
                assertEquals(Optional.of("pp. 7–9"), UnoUserDefinedProperty.getStringValue(document, MarkStore.pageInfoProperty(uid, 1)));
                if (type == CitationType.INVISIBLE_CIT) {
                    assertEquals("\u200b\u200b", WriterReferenceMarks.getAnchor(document, originalName).orElseThrow().getString());
                } else if (type.isPartial()) {
                    assertEquals(type == CitationType.AUTHOR_ONLY ? "\u200bBeta; Alpha\u200b" : "\u200b2021; 2020\u200b",
                            WriterReferenceMarks.getAnchor(document, originalName).orElseThrow().getString());
                }
                assertNoConversionArchive(document);
            }
        }
        Map<String, String> originalSections = snapshot(document).sections();
        try (CitationEngine engine = engine(document, preferences(true))) {
            insertAndAssertUndoRedo(engine, document, "alpha");
        }
        assertEquals(List.of(), managedNames(document, false));
        assertNewCitationIdentifierLengths(document, true);
        assertEquals(2, managedNames(document, true).size());
        String nativeName = managedNames(document, true).stream()
                                                        .filter(name -> ZoteroReferenceMark.parse(name).orElseThrow().citationKeys().equals(List.of("beta", "alpha")))
                                                        .findFirst().orElseThrow();
        ZoteroReferenceMark nativeMark = ZoteroReferenceMark.parse(nativeName).orElseThrow();
        CitationType fallback = type == CitationType.AUTHORYEAR_INTEXT ? CitationType.AUTHORYEAR_INTEXT : CitationType.AUTHORYEAR_PAR;
        assertEquals(fallback, nativeMark.citationType());
        assertNotEquals(uid, nativeMark.uniqueId());
        JsonObject nativeJson = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(nativeName)).getAsJsonObject();
        assertNotEquals(uid, nativeJson.get("citationID").getAsString());
        for (var item : nativeJson.getAsJsonArray("citationItems")) {
            assertFalse(item.getAsJsonObject().has("locator"));
            assertFalse(item.getAsJsonObject().has("label"));
        }
        String nativeText = WriterReferenceMarks.getAnchor(document, nativeName).orElseThrow().getString();
        assertEquals(fallback == CitationType.AUTHORYEAR_INTEXT ? "Beta [2], Alpha [1]" : "[2, 1]", nativeText);
        assertEquals(Optional.empty(), UnoUserDefinedProperty.getStringValue(document, MarkStore.pageInfoProperty(uid, 0)));
        assertEquals(Optional.empty(), UnoUserDefinedProperty.getStringValue(document, MarkStore.pageInfoProperty(uid, 1)));
        assertNoConversionArchive(document);
        Map<String, String> nativeMarks = snapshot(document).marks();
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("types-in-zotero.odt"));
        assertEquals(nativeMarks, snapshot(reopened).marks());
        assertNoConversionArchive(reopened);
        try (CitationEngine engine = engine(reopened, preferences(true))) {
            assertFalse(success(engine.requiresFormatConversion(style())));
        }
        try (CitationEngine engine = engine(reopened, preferences(false))) {
            assertTrue(success(engine.requiresFormatConversion(style())));
            insertAndAssertUndoRedo(engine, reopened, "alpha");
        }
        assertEquals(List.of(), managedNames(reopened, true));
        assertEquals(3, managedNames(reopened, false).size());
        Map<String, String> canonicalMarks = snapshot(reopened).marks();
        XTextDocument canonical = office.saveAndReload(reopened, temporaryDirectory.resolve("types-back-in-canonical.odt"));
        assertEquals(canonicalMarks, snapshot(canonical).marks());
        String convertedName = canonicalNameForKeys(canonical, List.of("beta", "alpha"));
        CitationGroup converted = store.readCitationGroupFromDocumentOrThrow(canonical, convertedName);
        assertNotEquals(uid, converted.groupId.citationGroupIdAsString());
        assertEquals(fallback, converted.citationType);
        assertEquals(List.of("beta", "alpha"), converted.citationsInStorageOrder.stream().map(citation -> citation.citationKey).toList());
        assertEquals(List.of(Optional.empty(), Optional.empty()), converted.citationsInStorageOrder.stream().map(citation -> citation.getPageInfo()).toList());
        assertEquals("\u200b" + nativeText + "\u200b", WriterReferenceMarks.getAnchor(canonical, convertedName).orElseThrow().getString());
        assertEquals(List.of(), UnoUserDefinedProperty.getListOfNames(canonical).stream().filter(name -> name.startsWith("JabRef_page_info:")).toList());
        assertNoConversionArchive(canonical);
        assertEquals(originalSections, snapshot(canonical).sections());
        try (CitationEngine engine = engine(canonical, preferences(false))) {
            assertFalse(success(engine.requiresFormatConversion(style())));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 24})
    void nativeMetadataIsLostAndReverseConversionUsesCurrentLibraryAfterSaveReopen(int citationIdLength) throws Exception {
        XTextDocument document = office.newDocument();
        JsonObject expected = JsonParser.parseString(Files.readString(resource("zotero-native.json"))).getAsJsonObject();
        String citationId = "nativeId".repeat(citationIdLength / 8);
        expected.addProperty("citationID", citationId);
        expected.addProperty("foreign", "Discard citation metadata");
        JsonObject originalItem = expected.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        originalItem.addProperty("locator", "17");
        originalItem.addProperty("label", "page");
        originalItem.addProperty("prefix", "see ");
        originalItem.addProperty("suffix", " following");
        originalItem.addProperty("foreign", "Discard item metadata");
        String nativeName = ZoteroReferenceMark.PREFIX + expected + " RNDnative1234";
        XTextCursor end = separatedEnd(document);
        end.setString("[native]");
        WriterReferenceMarks.create(new NamedTextContentPlacement(document, nativeName, end, true));
        OpenOfficePreferences preferences = preferences(true);
        preferences.setCurrentStyle(style());
        try (CitationEngine engine = engine(document, preferences)) {
            Snapshot beforeRead = snapshot(document);
            assertFalse(success(engine.requiresFormatConversion(style())));
            success(engine.exportCited(List.of(database)));
            assertEquals(beforeRead, snapshot(document));
            preferences.setZoteroCompatibilityMode(false);
            assertTrue(success(engine.requiresFormatConversion(style())));
            insertAndAssertUndoRedo(engine, document, "alpha");
        }
        assertEquals(List.of(), managedNames(document, true));
        assertNewCitationIdentifierLengths(document, false);
        assertEquals(2, managedNames(document, false).size());
        String canonicalName = canonicalNameForKeys(document, List.of("native"));
        JabRefMarkData converted = JabRefMarkCodec.decode(canonicalName);
        assertEquals(CitationType.AUTHORYEAR_PAR, converted.citationType());
        assertNotEquals("RNDnative1234", converted.uniqueId());
        assertEquals(List.of(Optional.empty()), new MarkStore().readCitationGroupFromDocumentOrThrow(document, canonicalName)
                                                             .citationsInStorageOrder.stream().map(citation -> citation.getPageInfo()).toList());
        assertNoConversionArchive(document);
        Map<String, String> canonicalMarks = snapshot(document).marks();
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("native-in-canonical.odt"));
        assertEquals(canonicalMarks, snapshot(reopened).marks());
        assertNoConversionArchive(reopened);
        try (CitationEngine engine = engine(reopened, preferences(false))) {
            assertFalse(success(engine.requiresFormatConversion(style())));
        }
        BibDatabase currentLibrary = new BibDatabase(List.of(
                database.getEntryByCitationKey("alpha").orElseThrow(), database.getEntryByCitationKey("beta").orElseThrow(),
                new BibEntry(database.getEntryByCitationKey("native").orElseThrow()).withField(StandardField.TITLE, "Current library title")
                                                                                 .withField(StandardField.YEAR, "2026")));
        try (CitationEngine engine = engine(reopened, preferences(true))) {
            insertAndAssertUndoRedo(engine, reopened, "beta", currentLibrary);
            assertFalse(success(engine.requiresFormatConversion(style())));
        }
        assertEquals(List.of(), managedNames(reopened, false));
        assertEquals(3, managedNames(reopened, true).size());
        assertNewCitationIdentifierLengths(reopened, true);
        String synthesized = managedNames(reopened, true).stream()
                                                         .filter(name -> ZoteroReferenceMark.parse(name).orElseThrow().citationKeys().equals(List.of("native")))
                                                         .findFirst().orElseThrow();
        assertSynthesizedNativeMetadata(synthesized, expected, CitationType.AUTHORYEAR_PAR, List.of("native"));
        JsonObject itemData = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(synthesized)).getAsJsonObject()
                                        .getAsJsonArray("citationItems").get(0).getAsJsonObject().getAsJsonObject("itemData");
        assertEquals("Current library title", itemData.get("title").getAsString());
        assertEquals("2026", itemData.getAsJsonObject("issued").getAsJsonArray("date-parts").get(0).getAsJsonArray().get(0).getAsString());
        assertNoConversionArchive(reopened);
        Map<String, String> synthesizedMarks = snapshot(reopened).marks();
        XTextDocument nativeDocument = office.saveAndReload(reopened, temporaryDirectory.resolve("native-back-in-zotero.odt"));
        assertEquals(synthesizedMarks, snapshot(nativeDocument).marks());
        assertNoConversionArchive(nativeDocument);
        try (CitationEngine engine = engine(nativeDocument, preferences(true))) {
            assertFalse(success(engine.requiresFormatConversion(style())));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void invalidRenderingAndUnresolvedSourcesFailBeforeMutationOrUndo(boolean targetZotero) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            preferences.setZoteroCompatibilityMode(targetZotero);
            XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
            undo.clear();
            Snapshot before = snapshot(document);
            CitationStyle invalid = new CitationStyle("invalid.csl", "invalid", "in-text", "Invalid", "", true, true, false, false, "not XML", false);
            XTextCursor cursor = document.getText().createTextCursor();
            assertTrue(engine.insert(List.of(database.getEntryByCitationKey("beta").orElseThrow()), List.of(database), invalid,
                    CitationType.AUTHORYEAR_PAR, "", cursor, false).isError());
            assertEquals(before, snapshot(document));
            BibDatabase missing = new BibDatabase(List.of(database.getEntryByCitationKey("beta").orElseThrow()));
            assertTrue(engine.insert(missing.getEntries(), List.of(missing), style(), CitationType.AUTHORYEAR_PAR, "", cursor, false).isError());
            assertEquals(before, snapshot(document));

            assertTrue(engine.insert(missing.getEntries(), List.of(missing), style(), CitationType.AUTHORYEAR_PAR, "", cursor, false).isError());
            assertEquals(0, undo.getAllUndoActionTitles().length);
        }
    }

    @ParameterizedTest
    @CsvSource({"true, false, false", "false, false, false", "true, true, false", "false, true, false",
            "true, false, true", "false, false, true", "true, true, true", "false, true, true"})
    void pendingDeletionsInEitherFormatIncludingFootnotesBlockTheWholeConversion(boolean targetZotero, boolean footnote, boolean recording) throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("Before after");
        XText text = document.getText();
        if (footnote) {
            XTextCursor body = text.createTextCursor();
            body.goRight((short) 7, false);
            XTextContent note = query(XTextContent.class, query(XMultiServiceFactory.class, document).createInstance("com.sun.star.text.Footnote"));
            text.insertTextContent(body, note, false);
            text = query(XText.class, note);
        }
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            XTextCursor source = text.createTextCursor();
            success(engine.insert(List.of(database.getEntryByCitationKey("alpha").orElseThrow()), List.of(database), style(),
                    CitationType.AUTHORYEAR_PAR, "", source, false));
            String name = managedNames(document, !targetZotero).getFirst();
            UnoRedlines.setRecordChanges(document, true);
            XTextRange anchor = WriterReferenceMarks.getAnchor(document, name).orElseThrow();
            XTextCursor deletion = anchor.getText().createTextCursorByRange(anchor);
            deletion.collapseToStart();
            deletion.goRight((short) 1, false);
            deletion.goRight((short) 1, true);
            deletion.setString("");
            assertFalse(UnoRedlines.getDeletedRanges(document).isEmpty());
            UnoRedlines.setRecordChanges(document, recording);
            preferences.setZoteroCompatibilityMode(targetZotero);
            XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
            undo.clear();
            Snapshot before = snapshot(document);
            assertTrue(engine.insert(List.of(database.getEntryByCitationKey("beta").orElseThrow()), List.of(database), style(),
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), true).isError());
            assertEquals(before, snapshot(document));
            assertEquals(0, undo.getAllUndoActionTitles().length);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void uniformSourceConversionPreservesCitedSetWhileUnsupportedCidMarksStayUnmanaged(boolean targetZotero) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(!targetZotero);
        String legacy = "JABREF_legacyUnknown CID_7 ignored NORMAL";
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "beta", false);
            insert(engine, document, "native", false);
            XTextCursor end = separatedEnd(document);
            end.setString("Leave this unsupported CID citation alone");
            WriterReferenceMarks.create(new NamedTextContentPlacement(document, legacy, end, true));
            assertEquals(2, managedNames(document, !targetZotero).size());
            assertEquals(List.of(), managedNames(document, targetZotero));
            preferences.setZoteroCompatibilityMode(targetZotero);
            assertTrue(success(engine.requiresFormatConversion(style())));
            insert(engine, document, "alpha", false);
            assertEquals(3, managedNames(document, targetZotero).size());
            assertEquals(List.of(), managedNames(document, !targetZotero));
            CitedEntryExporter.ExportResult exported = success(engine.exportCited(List.of(database)));
            assertEquals(List.of("alpha", "beta", "native"), exported.database().getEntries().stream().map(entry -> entry.getCitationKey().orElseThrow()).sorted().toList());
            assertEquals(List.of(), exported.unresolvedKeys());
            assertEquals("Leave this unsupported CID citation alone", WriterReferenceMarks.getAnchor(document, legacy).orElseThrow().getString());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"native", "legacy"})
    void conversionDetectionUsesWritersFirstRecognizedNameWithoutAssumingDocumentOrder(String laterFormat) throws Exception {
        XTextDocument document = office.newDocument();
        String unrelated = "AAA_unrelated";
        String unsupported = "JABREF_aaa CID_7 ignored NORMAL";
        for (String name : List.of(unrelated, unsupported)) {
            XTextCursor cursor = separatedEnd(document);
            cursor.setString("Unmanaged text");
            WriterReferenceMarks.create(new NamedTextContentPlacement(document, name, cursor, true));
        }
        String canonical = new MarkStore().createCitationGroup(document, office.context(), List.of("beta"), List.of(Optional.empty()),
                CitationType.AUTHORYEAR_PAR, separatedEnd(document), false, false, "detection_uid").getReferenceMarkNameForLinking().orElseThrow();
        String later = "native".equals(laterFormat)
                       ? ZoteroReferenceMark.PREFIX + Files.readString(resource("zotero-native.json")).strip() + " RNDnative1234"
                       : "JR_cite_1_beta";
        XTextCursor cursor = separatedEnd(document);
        cursor.setString("Later different citation format");
        WriterReferenceMarks.create(new NamedTextContentPlacement(document, later, cursor, true));
        List<String> names = WriterReferenceMarks.getListOfNames(document);
        List<String> recognized = names.stream()
                                       .filter(name -> JabRefMarkCodec.isCanonicalMarkName(name) || "JR_cite_1_beta".equals(name)
                                               || ZoteroReferenceMark.isZoteroReferenceMarkName(name))
                                       .toList();
        assertEquals(Set.of(canonical, later), Set.copyOf(recognized));
        boolean nativeMode = !JabRefMarkCodec.isCanonicalMarkName(recognized.getFirst());
        XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
        undo.clear();
        OpenOfficePreferences preferences = preferences(nativeMode);
        try (CitationEngine engine = engine(document, preferences)) {
            Snapshot before = snapshot(document);
            assertFalse(success(engine.requiresFormatConversion(style())));
            assertEquals(before, snapshot(document));
            preferences.setZoteroCompatibilityMode(!nativeMode);
            assertTrue(success(engine.requiresFormatConversion(style())));
            assertEquals(before, snapshot(document));
            assertEquals(0, undo.getAllUndoActionTitles().length);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void superscriptFormattingSurvivesConversionRefreshAndSingleUndo(boolean targetZotero) throws Exception {
        XTextDocument document = office.newDocument();
        CitationStyle superscript = CSLStyleUtils.createCitationStyleFromFile(resource("superscript.csl").toString()).orElseThrow();
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            success(engine.insert(List.of(database.getEntryByCitationKey("alpha").orElseThrow()), List.of(database), superscript,
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), false));
            preferences.setZoteroCompatibilityMode(targetZotero);
            XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
            undo.clear();
            Snapshot before = snapshot(document);
            success(engine.insert(List.of(database.getEntryByCitationKey("beta").orElseThrow()), List.of(database), superscript,
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), false));
            for (String name : managedNames(document, targetZotero)) {
                assertEquals((short) 33, query(XPropertySet.class, WriterReferenceMarks.getAnchor(document, name).orElseThrow()
                                                                                       .getText().createTextCursorByRange(WriterReferenceMarks.getAnchor(document, name).orElseThrow())).getPropertyValue("CharEscapement"));
            }
            undo.undo();
            assertEquals(before, snapshot(document));
            undo.redo();
            success(engine.refresh(List.of(database), superscript));
            for (String name : managedNames(document, targetZotero)) {
                assertEquals((short) 33, query(XPropertySet.class, WriterReferenceMarks.getAnchor(document, name).orElseThrow()
                                                                                       .getText().createTextCursorByRange(WriterReferenceMarks.getAnchor(document, name).orElseThrow())).getPropertyValue("CharEscapement"));
            }
        }
    }

    @Test
    void externalNativeJsonEditsSurviveOrdinaryUpdatesButDoNotPreventLossyConversion() throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(false);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            preferences.setZoteroCompatibilityMode(true);
            insert(engine, document, "beta", false);
            String original = managedNames(document, true).stream().filter(name -> name.contains("http://www.jabref.org/alpha")).findFirst().orElseThrow();
            JsonObject json = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(original)).getAsJsonObject();
            json.addProperty("foreignEdit", true);
            JsonObject properties = new JsonObject();
            properties.addProperty("formattedCitation", "External Zotero formatting");
            json.add("properties", properties);
            String edited = ZoteroReferenceMark.PREFIX + json + " " + ZoteroReferenceMark.parse(original).orElseThrow().uniqueId();
            replaceNativeMarkName(document, original, edited);
            assertFalse(success(engine.requiresFormatConversion(style())));
            insert(engine, document, "native", false);
            assertTrue(managedNames(document, true).contains(edited));
            success(engine.refresh(List.of(database), style()));
            assertTrue(managedNames(document, true).contains(edited));
            assertEquals(json, JsonParser.parseString(ZoteroReferenceMark.getCSLJson(edited)));
            assertNoConversionArchive(document);
            preferences.setZoteroCompatibilityMode(false);
            assertTrue(success(engine.requiresFormatConversion(style())));
            insertAndAssertUndoRedo(engine, document, "beta");
            assertEquals(List.of(), managedNames(document, true));
            assertEquals(4, managedNames(document, false).size());
            assertFalse(success(engine.requiresFormatConversion(style())));
            assertNoConversionArchive(document);
            preferences.setZoteroCompatibilityMode(true);
            insertAndAssertUndoRedo(engine, document, "beta");
            String synthesized = managedNames(document, true).stream().filter(name -> name.contains("http://www.jabref.org/alpha")).findFirst().orElseThrow();
            JsonObject synthesizedJson = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(synthesized)).getAsJsonObject();
            assertFalse(synthesizedJson.has("foreignEdit"));
            assertFalse(synthesizedJson.has("properties"));
            assertNotEquals(json.get("citationID"), synthesizedJson.get("citationID"));
            assertNoConversionArchive(document);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void obsoleteConversionPropertiesAreIgnoredWithoutBeingCleaned(boolean targetZotero) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            String oldName = managedNames(document, !targetZotero).getFirst();
            String uid = targetZotero ? JabRefMarkCodec.decode(oldName).uniqueId() : ZoteroReferenceMark.parse(oldName).orElseThrow().uniqueId();
            String obsolete = "JabRef_citation_conversion:" + uid;
            UnoUserDefinedProperty.getPropertyContainer(document).orElseThrow().addProperty(obsolete,
                    (short) (PropertyAttribute.READONLY | PropertyAttribute.REMOVEABLE), 42);
            UnoUserDefinedProperty.setStringProperty(document, "JabRef_citation_conversion:unrelated", "not JSON");
            preferences.setZoteroCompatibilityMode(targetZotero);
            assertTrue(success(engine.requiresFormatConversion(style())));
            insertAndAssertUndoRedo(engine, document, "beta");
            assertEquals(List.of(), managedNames(document, !targetZotero));
            assertEquals(2, managedNames(document, targetZotero).size());
            assertEquals(42, snapshot(document).properties().get(obsolete));
            assertEquals("not JSON", snapshot(document).properties().get("JabRef_citation_conversion:unrelated"));
            assertEquals(Set.of(obsolete, "JabRef_citation_conversion:unrelated"), Set.copyOf(UnoUserDefinedProperty.getListOfNames(document).stream()
                                                                                                               .filter(name -> name.startsWith("JabRef_citation_conversion:")).toList()));
        }
    }

    private static void addSourceDetailsDiscardedByConversion(XTextDocument document, boolean nativeSource) throws Exception {
        String name = managedNames(document, nativeSource).getFirst();
        if (nativeSource) {
            JsonObject json = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(name)).getAsJsonObject();
            json.addProperty("foreign", "Original citation metadata");
            JsonObject properties = new JsonObject();
            properties.addProperty("plainCitation", "Original native text");
            properties.addProperty("custom", "Original custom property");
            json.add("properties", properties);
            JsonObject item = json.getAsJsonArray("citationItems").get(0).getAsJsonObject();
            item.addProperty("locator", "17");
            item.addProperty("label", "page");
            item.addProperty("id", "foreign-item-id");
            item.getAsJsonObject("itemData").addProperty("id", "foreign-item-id");
            item.getAsJsonObject("itemData").addProperty("archive-place", "Original archive");
            item.getAsJsonArray("uris").add("http://zotero.org/users/123/items/ORIGINAL");
            replaceNativeMarkName(document, name, ZoteroReferenceMark.PREFIX + json + " " + ZoteroReferenceMark.parse(name).orElseThrow().uniqueId());
        } else {
            UnoUserDefinedProperty.setStringProperty(document, MarkStore.pageInfoProperty(JabRefMarkCodec.decode(name).uniqueId(), 0), "<b>pp. 4–7</b>");
        }
    }

    private static void replaceNativeMarkName(XTextDocument document, String before, String after) throws Exception {
        XTextContent content = WriterReferenceMarks.getAsTextContent(document, before).orElseThrow();
        XTextCursor cursor = content.getAnchor().getText().createTextCursorByRange(content.getAnchor());
        String text = cursor.getString();
        content.getAnchor().getText().removeTextContent(content);
        cursor.setString(text);
        WriterReferenceMarks.create(new NamedTextContentPlacement(document, after, cursor, true));
    }

    private static String canonicalNameForKeys(XTextDocument document, List<String> keys) throws Exception {
        for (String name : managedNames(document, false)) {
            if (JabRefMarkCodec.decode(name).citationKeys().equals(keys)) {
                return name;
            }
        }
        return fail("No canonical citation for keys: " + keys);
    }

    private static void assertNoConversionArchive(XTextDocument document) throws Exception {
        assertEquals(List.of(), UnoUserDefinedProperty.getListOfNames(document).stream().filter(name -> name.startsWith("JabRef_citation_conversion:")).toList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void conversionPreservesBodyFootnoteAndTableCitationOrder(boolean targetZotero) throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("Before\nAfter");
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            XTextCursor end = document.getText().createTextCursor();
            end.gotoEnd(false);
            XTextContent note = query(XTextContent.class, query(XMultiServiceFactory.class, document).createInstance("com.sun.star.text.Footnote"));
            document.getText().insertTextContent(end, note, false);
            XText footnoteText = query(XText.class, note);
            success(engine.insert(List.of(database.getEntryByCitationKey("beta").orElseThrow()), List.of(database), style(),
                    CitationType.AUTHORYEAR_PAR, "", footnoteText.createTextCursor(), false));
            XTextTable table = query(XTextTable.class, query(XMultiServiceFactory.class, document).createInstance("com.sun.star.text.TextTable"));
            table.initialize(1, 2);
            end.gotoEnd(false);
            document.getText().insertTextContent(end, table, false);
            for (Map.Entry<String, String> cell : Map.of("A1", "native", "B1", "alpha").entrySet()) {
                XText text = query(XText.class, table.getCellByName(cell.getKey()));
                success(engine.insert(List.of(database.getEntryByCitationKey(cell.getValue()).orElseThrow()), List.of(database), style(),
                        CitationType.AUTHORYEAR_PAR, "", text.createTextCursor(), false));
            }
            preferences.setZoteroCompatibilityMode(targetZotero);
            insert(engine, document, "beta", false);
            Map<String, String> expected = Map.of("alpha", "[2]", "beta", "[1]", "native", "[3]");
            assertEquals(5, managedNames(document, targetZotero).size());
            for (String name : managedNames(document, targetZotero)) {
                String key = targetZotero ? ZoteroReferenceMark.parse(name).orElseThrow().citationKeys().getFirst()
                                          : JabRefMarkCodec.decode(name).citationKeys().getFirst();
                assertEquals(expected.get(key), WriterReferenceMarks.getAnchor(document, name).orElseThrow().getString().replace("\u200b", ""));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void protectedUserSectionsRejectInsertionBeforeAnyConversion(boolean targetZotero) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            XTextCursor end = separatedEnd(document);
            end.setString("Protected user content");
            UnoTextSection.create(new NamedTextContentPlacement(document, "Unrelated protected section", end, true));
            XTextSection section = UnoTextSection.getByName(document, "Unrelated protected section").orElseThrow();
            query(XPropertySet.class, section).setPropertyValue("IsProtected", true);
            preferences.setZoteroCompatibilityMode(targetZotero);
            XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
            undo.clear();
            Snapshot before = snapshot(document);
            XTextCursor inside = section.getAnchor().getText().createTextCursorByRange(section.getAnchor());
            inside.collapseToStart();
            inside.goRight((short) 1, false);
            assertTrue(engine.insert(List.of(database.getEntryByCitationKey("beta").orElseThrow()), List.of(database), style(),
                    CitationType.AUTHORYEAR_PAR, "", inside, false).isError());
            assertEquals(before, snapshot(document));
            assertEquals(0, undo.getAllUndoActionTitles().length);
        }
    }

    @ParameterizedTest
    @CsvSource({"true, false, true, true", "false, false, true, true", "true, true, true, true", "false, true, true, true",
            "true, false, false, true", "false, false, false, true", "true, true, false, true", "false, true, false, true",
            "true, true, true, false", "false, true, true, false"})
    void citationConversionOnlySynchronizesTheTargetBibliographyWhenRequested(boolean targetZotero, boolean synchronize,
                                                                              boolean targetExists, boolean hasBibliography) throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("Citation position\nCanonical bibliography\nFirst native bibliography\nSecond native bibliography\nUnrelated user content\n");
        if (targetZotero || targetExists) {
            createSection(document, "JR_bib", "Canonical bibliography");
        }
        if (!targetZotero || targetExists) {
            createSection(document, "ZOTERO_BIBL {\"uncited\":[\"opaque-id\"],\"unknown\":42} CSL_BIBLIOGRAPHY RNDfirstBib01", "First native bibliography");
            createSection(document, "ZOTERO_BIBL {\"omitted\":[\"native-id\"],\"custom\":[\"user metadata\"]} CSL_BIBLIOGRAPHY RNDsecondBib1", "Second native bibliography");
        }
        createSection(document, "Unrelated section", "Unrelated user content");
        UnoUserDefinedProperty.setStringProperty(document, "JabRef_bibliography_conversion", "Old bibliography metadata must stay untouched");
        UnoUserDefinedProperty.setStringProperty(document, "ZOTERO_BIBL_custom_state", "Native bibliography properties must stay untouched");
        CitationStyle selectedStyle = hasBibliography ? style() : citationOnlyStyle();
        assertEquals(hasBibliography, selectedStyle.hasBibliography());
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            preferences.setZoteroCompatibilityMode(targetZotero);
            XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
            undo.clear();
            Snapshot before = snapshot(document);
            success(engine.insert(List.of(database.getEntryByCitationKey("beta").orElseThrow()), List.of(database), selectedStyle,
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), synchronize));
            Snapshot after = snapshot(document);
            assertFalse(success(engine.requiresFormatConversion(selectedStyle)));
            assertEquals(bibliographyProperties(before), bibliographyProperties(after));
            boolean shouldSynchronize = synchronize && hasBibliography;
            if (!shouldSynchronize) {
                assertEquals(before.sections(), after.sections());
            } else {
                int targetCount = 0;
                for (Map.Entry<String, String> section : after.sections().entrySet()) {
                    boolean target = targetZotero ? section.getKey().startsWith("ZOTERO_BIBL ") : "JR_bib".equals(section.getKey());
                    if (target) {
                        targetCount++;
                        assertTrue(section.getValue().contains("Alpha title"));
                        assertTrue(section.getValue().contains("Beta title"));
                    } else {
                        assertEquals(before.sections().get(section.getKey()), section.getValue());
                    }
                }
                assertEquals(targetZotero && targetExists ? 2 : 1, targetCount);
                assertEquals(before.sections().size() + (targetExists ? 0 : 1), after.sections().size());
                for (String name : before.sections().keySet()) {
                    assertTrue(after.sections().containsKey(name));
                }
            }
            assertEquals(1, undo.getAllUndoActionTitles().length);
            undo.undo();
            assertEquals(before, snapshot(document));
            undo.redo();
            assertEquals(after, snapshot(document));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void citationConversionDoesNotCreateAnyBibliographyWithSynchronizationOff(boolean targetZotero) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            assertEquals(List.of(), bibliographyNames(document));
            preferences.setZoteroCompatibilityMode(targetZotero);
            insert(engine, document, "beta", false);
            assertEquals(List.of(), bibliographyNames(document));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void citationOnlyConversionLeavesPendingBibliographyDeletionsUntouchedWithSynchronizationOff(boolean targetZotero) throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("Citation position\nExisting bibliography content\n");
        String name = targetZotero ? "JR_bib" : "ZOTERO_BIBL {} CSL_BIBLIOGRAPHY RNDnativeBib1";
        createSection(document, name, "Existing bibliography content");
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            XTextRange anchor = UnoTextSection.getAnchor(document, name).orElseThrow();
            XTextCursor deletion = anchor.getText().createTextCursorByRange(anchor);
            deletion.collapseToStart();
            deletion.goRight((short) 1, false);
            deletion.goRight((short) 3, true);
            UnoRedlines.setRecordChanges(document, true);
            deletion.setString("");
            UnoRedlines.setRecordChanges(document, false);
            int pendingDeletions = UnoRedlines.getDeletedRanges(document).size();
            assertTrue(pendingDeletions > 0);
            Snapshot before = snapshot(document);
            preferences.setZoteroCompatibilityMode(targetZotero);
            insert(engine, document, "beta", false);
            assertEquals(before.sections(), snapshot(document).sections());
            assertEquals(pendingDeletions, UnoRedlines.getDeletedRanges(document).size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001", "2147483647", "2147483648", "9999999999999999999999999999999999999999"})
    void reverseConversionSynthesizesBoundedItemIdsInsteadOfRestoringForeignIds(String foreignId) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(false);
        try (CitationEngine engine = engine(document, preferences)) {
            JsonObject nativeJson = JsonParser.parseString(Files.readString(resource("zotero-native.json"))).getAsJsonObject();
            JsonObject nativeItem = nativeJson.getAsJsonArray("citationItems").get(0).getAsJsonObject();
            nativeItem.addProperty("id", foreignId);
            nativeItem.getAsJsonObject("itemData").addProperty("id", foreignId);
            XTextCursor end = separatedEnd(document);
            end.setString("[native]");
            WriterReferenceMarks.create(new NamedTextContentPlacement(document, ZoteroReferenceMark.PREFIX + nativeJson + " RNDnative1234", end, true));
            assertTrue(success(engine.requiresFormatConversion(style())));
            insert(engine, document, "alpha", false);
            assertEquals(List.of(), managedNames(document, true));
            assertEquals(2, managedNames(document, false).size());
            preferences.setZoteroCompatibilityMode(true);
            insert(engine, document, "beta", false);
            Set<String> allocated = new HashSet<>();
            for (String name : managedNames(document, true)) {
                JsonObject citation = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(name)).getAsJsonObject();
                JsonObject item = citation.getAsJsonArray("citationItems").get(0).getAsJsonObject();
                assertNotEquals("native-citation", citation.get("citationID").getAsString());
                assertNotEquals(foreignId, item.get("id").getAsString());
                assertEquals(item.get("id"), item.getAsJsonObject("itemData").get("id"));
                allocated.add(item.get("id").getAsString());
            }
            assertEquals(Set.of("1", "2", "3"), allocated);
            assertNoConversionArchive(document);
        }
    }

    private static void createSection(XTextDocument document, String name, String content) throws Exception {
        XSearchable searchable = query(XSearchable.class, document);
        XSearchDescriptor descriptor = searchable.createSearchDescriptor();
        descriptor.setSearchString(content);
        XTextRange range = query(XTextRange.class, searchable.findFirst(descriptor));
        UnoTextSection.create(new NamedTextContentPlacement(document, name, range, true));
        assertEquals(content, UnoTextSection.getAnchor(document, name).orElseThrow().getString());
    }

    private static Map<String, Object> bibliographyProperties(Snapshot snapshot) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Map.Entry<String, Object> property : snapshot.properties().entrySet()) {
            if (property.getKey().startsWith("JabRef_bibliography_") || property.getKey().startsWith("ZOTERO_BIBL")) {
                properties.put(property.getKey(), property.getValue());
            }
        }
        return properties;
    }

    private CitationStyle citationOnlyStyle() throws Exception {
        Path path = temporaryDirectory.resolve("citation-only.csl");
        Files.writeString(path, """
                <style xmlns="http://purl.org/net/xbiblio/csl" class="in-text" version="1.0">
                  <info>
                    <title>Writer citation-only test</title>
                    <id>http://www.zotero.org/styles/writer-citation-only-test</id>
                  </info>
                  <citation><layout prefix="[" suffix="]"><text variable="citation-number"/></layout></citation>
                </style>
                """);
        return CSLStyleUtils.createCitationStyleFromFile(path.toString()).orElseThrow();
    }

    // [utest->req~openoffice.zotero-conversion-modifier-compatibility~1]
    @Test
    void realNativeLocatorFixtureIsLostAcrossCanonicalSaveReopenAndReverseConversion() throws Exception {
        String name = ZoteroReferenceMark.PREFIX + Files.readString(resource("zotero-native-locator.json")).strip() + " RNDlocator001";
        assertNativeModifierLossAcrossConversion(name, CitationType.AUTHORYEAR_PAR, "[1]");
    }

    // [utest->req~openoffice.zotero-conversion-modifier-compatibility~1]
    @ParameterizedTest
    @CsvSource({"plain, AUTHORYEAR_PAR, [1]", "label, AUTHORYEAR_PAR, [1]", "prefix, AUTHORYEAR_PAR, [1]",
            "suffix, AUTHORYEAR_PAR, [1]", "author-only, AUTHORYEAR_PAR, [1]", "suppress-author, AUTHORYEAR_INTEXT, Native [1]",
            "mixed-suppression, AUTHORYEAR_INTEXT, 'Native [1], Native [1]'"})
    void nativeCitationModifiersKeepOnlyReaderGroupTypesAcrossLossyConversion(String modifier, CitationType expectedType, String expectedText) throws Exception {
        JsonObject json = JsonParser.parseString(Files.readString(resource("zotero-native.json"))).getAsJsonObject();
        JsonObject first = json.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        if ("mixed-suppression".equals(modifier)) {
            json.getAsJsonArray("citationItems").add(first.deepCopy());
            first.addProperty("suppress-author", true);
        } else if ("author-only".equals(modifier) || "suppress-author".equals(modifier)) {
            first.addProperty(modifier, true);
        } else if (!"plain".equals(modifier)) {
            first.addProperty(modifier, "label".equals(modifier) ? "page" : "native modifier");
        }
        assertNativeModifierLossAcrossConversion(ZoteroReferenceMark.PREFIX + json + " RNDmodifier01", expectedType, expectedText);
    }

    private void assertNativeModifierLossAcrossConversion(String nativeName, CitationType expectedType, String expectedText) throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("""
                Citation position
                Canonical bibliography
                Native bibliography
                """);
        createSection(document, "JR_bib", "Canonical bibliography");
        createSection(document, "ZOTERO_BIBL {} CSL_BIBLIOGRAPHY RNDnativeBib1", "Native bibliography");
        UnoUserDefinedProperty.setStringProperty(document, "JabRef_bibliography_conversion", "Keep bibliography metadata");
        UnoUserDefinedProperty.setStringProperty(document, "ZOTERO_BIBL_custom_state", "Keep native bibliography metadata");
        JsonObject expectedJson = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(nativeName)).getAsJsonObject();
        XTextCursor cursor = separatedEnd(document);
        cursor.setString(expectedJson.getAsJsonObject("properties").get("plainCitation").getAsString());
        WriterReferenceMarks.create(new NamedTextContentPlacement(document, nativeName, cursor, true));
        List<String> expectedKeys = expectedJson.getAsJsonArray("citationItems").asList().stream().map(_ -> "native").toList();
        Snapshot before = snapshot(document);
        try (CitationEngine engine = engine(document, preferences(false))) {
            assertTrue(success(engine.requiresFormatConversion(style())));
            insertAndAssertUndoRedo(engine, document, "alpha", database, separatedEnd(document));
            assertFalse(success(engine.requiresFormatConversion(style())));
        }
        assertEquals(List.of(), managedNames(document, true));
        assertEquals(2, managedNames(document, false).size());
        assertNoConversionArchive(document);
        String canonicalName = canonicalNameForKeys(document, expectedKeys);
        JabRefMarkData converted = JabRefMarkCodec.decode(canonicalName);
        assertEquals(expectedType, converted.citationType());
        assertEquals(expectedKeys, converted.citationKeys());
        assertEquals(expectedKeys.stream().map(_ -> Optional.empty()).toList(),
                new MarkStore().readCitationGroupFromDocumentOrThrow(document, canonicalName).citationsInStorageOrder.stream().map(citation -> citation.getPageInfo()).toList());
        String expectedCanonicalName = String.join(", ", expectedKeys.stream().map(key -> "JABREF_" + key).toList())
                + (expectedType == CitationType.AUTHORYEAR_INTEXT ? " IN_TEXT " : " NORMAL ") + converted.uniqueId();
        assertEquals(expectedCanonicalName, canonicalName);
        assertEquals("\u200b" + expectedText + "\u200b", WriterReferenceMarks.getAnchor(document, canonicalName).orElseThrow().getString());
        assertEquals(before.sections(), snapshot(document).sections());
        assertEquals(bibliographyProperties(before), bibliographyProperties(snapshot(document)));

        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("native-modifier-in-canonical.odt"));
        assertNoConversionArchive(reopened);
        assertEquals("\u200b" + expectedText + "\u200b", WriterReferenceMarks.getAnchor(reopened, canonicalName).orElseThrow().getString());
        try (CitationEngine engine = engine(reopened, preferences(true))) {
            assertTrue(success(engine.requiresFormatConversion(style())));
            insertAndAssertUndoRedo(engine, reopened, "beta", database, separatedEnd(reopened));
            assertFalse(success(engine.requiresFormatConversion(style())));
            assertEquals(List.of(), managedNames(reopened, false));
            assertEquals(3, managedNames(reopened, true).size());
            String synthesized = managedNames(reopened, true).stream()
                                                             .filter(name -> ZoteroReferenceMark.parse(name).orElseThrow().citationType() == expectedType
                                                                     && ZoteroReferenceMark.parse(name).orElseThrow().citationKeys().equals(expectedKeys))
                                                             .findFirst().orElseThrow();
            assertNotEquals(nativeName, synthesized);
            assertNotEquals(ZoteroReferenceMark.parse(nativeName).orElseThrow().uniqueId(), ZoteroReferenceMark.parse(synthesized).orElseThrow().uniqueId());
            assertSynthesizedNativeMetadata(synthesized, expectedJson, expectedType, expectedKeys);
            assertEquals(expectedText, WriterReferenceMarks.getAnchor(reopened, synthesized).orElseThrow().getString());
            assertNoConversionArchive(reopened);
            assertEquals(before.sections(), snapshot(reopened).sections());
            assertEquals(bibliographyProperties(before), bibliographyProperties(snapshot(reopened)));
            for (String operation : List.of("insert", "refresh")) {
                if ("insert".equals(operation)) {
                    insert(engine, reopened, "native", false);
                } else {
                    success(engine.refresh(List.of(database), style()));
                }
                assertFalse(success(engine.requiresFormatConversion(style())));
                assertEquals(4, managedNames(reopened, true).size());
                assertEquals(expectedText, WriterReferenceMarks.getAnchor(reopened, synthesized).orElseThrow().getString());
                assertTrue(managedNames(reopened, true).contains(synthesized));
                assertSynthesizedNativeMetadata(synthesized, expectedJson, expectedType, expectedKeys);
                assertNoConversionArchive(reopened);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"malformed", "readonly", "nonstring"})
    void canonicalConversionIgnoresUnusableNativePreferences(String kind) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(true);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            String originalPreferenceChunk = UnoUserDefinedProperty.getStringValue(document, "ZOTERO_PREF_1").orElseThrow();
            UnoUserDefinedProperty.removeIfExists(document, "ZOTERO_PREF_1");
            XPropertyContainer properties = UnoUserDefinedProperty.getPropertyContainer(document).orElseThrow();
            short attributes = PropertyAttribute.REMOVEABLE;
            if ("readonly".equals(kind)) {
                attributes |= PropertyAttribute.READONLY;
            }
            Object value = switch (kind) {
                case "nonstring" ->
                        42;
                case "readonly" ->
                        originalPreferenceChunk;
                default ->
                        "Not native preference XML";
            };
            properties.addProperty("ZOTERO_PREF_1", attributes, value);
            Map<String, Object> nativePreferencesBefore = nativePreferenceProperties(snapshot(document));
            preferences.setZoteroCompatibilityMode(false);
            try (MockedStatic<ZoteroDocumentPreferences> nativePreferences = mockStatic(ZoteroDocumentPreferences.class, invocation ->
                    fail("Canonical conversion must not access native preferences: " + invocation.getMethod().getName()))) {
                assertTrue(success(engine.requiresFormatConversion(style())));
                insertAndAssertUndoRedo(engine, document, "beta");
                assertFalse(success(engine.requiresFormatConversion(style())));
                nativePreferences.verifyNoInteractions();
            }
            assertEquals(List.of(), managedNames(document, true));
            assertEquals(2, managedNames(document, false).size());
            assertEquals(nativePreferencesBefore, nativePreferenceProperties(snapshot(document)));
            assertEquals(Optional.of(new DocumentStyleStore.State(DocumentStyleStore.Family.CSL, style().getStyleId())), new DocumentStyleStore().read(document));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"malformed", "readonly", "nonstring"})
    void nativeTargetStillValidatesNativePreferencesBeforeConversion(String kind) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(false);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            XPropertyContainer properties = UnoUserDefinedProperty.getPropertyContainer(document).orElseThrow();
            short attributes = PropertyAttribute.REMOVEABLE;
            if ("readonly".equals(kind)) {
                attributes |= PropertyAttribute.READONLY;
            }
            Object value = switch (kind) {
                case "nonstring" ->
                        42;
                case "readonly" ->
                        "<data data-version=\"3\"><style id=\"untouched-native-style\"/></data>";
                default ->
                        "Not native preference XML";
            };
            properties.addProperty("ZOTERO_PREF_1", attributes, value);
            preferences.setZoteroCompatibilityMode(true);
            assertTrue(success(engine.requiresFormatConversion(style())));
            assertRejectedWithoutMutation(engine, document);
        }
    }

    @ParameterizedTest
    @CsvSource({"numeric-style, true", "readonly-style, true", "nonremovable-page, true",
            "numeric-style, false", "readonly-style, false"})
    void propertyTypesWritabilityAndRemovabilityAreCheckedBeforeConversion(String kind, boolean targetZotero) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            String name = managedNames(document, !targetZotero).getFirst();
            String uid = targetZotero ? JabRefMarkCodec.decode(name).uniqueId() : ZoteroReferenceMark.parse(name).orElseThrow().uniqueId();
            String property = switch (kind) {
                case "nonremovable-page" ->
                        MarkStore.pageInfoProperty(uid, 0);
                default ->
                        DocumentStyleStore.PROPERTY_NAME;
            };
            UnoUserDefinedProperty.removeIfExists(document, property);
            XPropertyContainer container = UnoUserDefinedProperty.getPropertyContainer(document).orElseThrow();
            short attributes = "nonremovable-page".equals(kind) ? 0 : PropertyAttribute.REMOVEABLE;
            if ("readonly-style".equals(kind)) {
                attributes |= PropertyAttribute.READONLY;
            }
            container.addProperty(property, attributes, kind.startsWith("numeric") ? 42 : "original value");
            preferences.setZoteroCompatibilityMode(targetZotero);
            assertRejectedWithoutMutation(engine, document);
        }
    }

    @Test
    void conversionRequiresAnUnlockedUndoManagerBeforeWriting() throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(false);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            preferences.setZoteroCompatibilityMode(true);
            XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
            undo.clear();
            undo.lock();
            try {
                assertRejectedWithoutMutation(engine, document);
                assertTrue(undo.isLocked());
            } finally {
                undo.unlock();
            }
        }
    }

    // [utest->req~openoffice.next-citation-format-conversion~1]
    @ParameterizedTest
    @CsvSource({"true, markers", "false, markers", "true, properties", "false, properties"})
    void ordinaryWriteFailuresRollBackConversionAndInsertionWithoutUndoingEarlierUserActions(boolean targetZotero, String phase) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(!targetZotero);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", true);
            addSourceDetailsDiscardedByConversion(document, !targetZotero);
            preferences.setZoteroCompatibilityMode(targetZotero);
            UnoRedlines.setRecordChanges(document, true);
            Snapshot before = snapshot(document);
            XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
            List<String> previousUndoTitles = List.of(undo.getAllUndoActionTitles());
            AtomicBoolean renamed = new AtomicBoolean();
            try (MockedStatic<UndoableDocumentProperties> properties = mockStatic(UndoableDocumentProperties.class, invocation -> {
                if ("properties".equals(phase) && "set".equals(invocation.getMethod().getName())
                        && DocumentStyleStore.PROPERTY_NAME.equals(invocation.getArgument(1))) {
                    renamed.set(managedNames(document, !targetZotero).isEmpty() && managedNames(document, targetZotero).size() == 2);
                    throw new PropertyVetoException("Injected final property write failure");
                }
                return invocation.callRealMethod();
            });
                 MockedStatic<ReferenceMarkTextWriter> markers = mockStatic(ReferenceMarkTextWriter.class, invocation -> {
                     if ("markers".equals(phase) && "write".equals(invocation.getMethod().getName())) {
                         WriterReferenceMark range = invocation.getArgument(2);
                         if (range.getRangeName().contains("alpha")) {
                             renamed.set(!managedNames(document, targetZotero).isEmpty());
                             throw new WriterOperationException("Injected marker write failure after rename");
                         }
                     }
                     return invocation.callRealMethod();
                 })) {
                assertTrue(engine.insert(List.of(database.getEntryByCitationKey("beta").orElseThrow()), List.of(database), style(),
                        CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), false).isError());
            }
            assertTrue(renamed.get());
            assertEquals(before, snapshot(document));
            assertEquals(previousUndoTitles, List.of(undo.getAllUndoActionTitles()));
            assertFalse(undo.isRedoPossible());
            assertTrue(success(engine.requiresFormatConversion(style())));
        }
    }

    private JabRefException assertRejectedWithoutMutation(CitationEngine engine, XTextDocument document) throws Exception {
        XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
        List<String> beforeUndo = List.of(undo.getAllUndoActionTitles());
        Snapshot before = snapshot(document);
        OOResult<List<String>, JabRefException> result = engine.insert(List.of(database.getEntryByCitationKey("beta").orElseThrow()), List.of(database), style(),
                CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), false);
        assertTrue(result.isError());
        assertEquals(before, snapshot(document));
        assertEquals(beforeUndo, List.of(undo.getAllUndoActionTitles()));
        return result.getError();
    }

    private void insertAndAssertUndoRedo(CitationEngine engine, XTextDocument document, String key) throws Exception {
        insertAndAssertUndoRedo(engine, document, key, database);
    }

    private void insertAndAssertUndoRedo(CitationEngine engine, XTextDocument document, String key, BibDatabase sources) throws Exception {
        insertAndAssertUndoRedo(engine, document, key, sources, document.getText().createTextCursor());
    }

    private void insertAndAssertUndoRedo(CitationEngine engine, XTextDocument document, String key, BibDatabase sources, XTextCursor cursor) throws Exception {
        XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
        undo.clear();
        Snapshot before = snapshot(document);
        success(engine.insert(List.of(sources.getEntryByCitationKey(key).orElseThrow()), List.of(sources), style(),
                CitationType.AUTHORYEAR_PAR, "", cursor, false));
        Snapshot after = snapshot(document);
        assertEquals(1, undo.getAllUndoActionTitles().length);
        undo.undo();
        assertEquals(before, snapshot(document));
        undo.redo();
        assertEquals(after, snapshot(document));
    }

    private static void assertSynthesizedNativeMetadata(String name, JsonObject original, CitationType expectedType, List<String> expectedKeys) {
        ZoteroReferenceMark mark = ZoteroReferenceMark.parse(name).orElseThrow();
        assertEquals(expectedKeys, mark.citationKeys());
        assertEquals(expectedType, mark.citationType());
        JsonObject json = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(name)).getAsJsonObject();
        assertNotEquals(original.get("citationID"), json.get("citationID"));
        assertEquals(8, json.get("citationID").getAsString().length());
        assertEquals(Set.of("citationID", "citationItems", "schema"), json.keySet());
        for (int index = 0; index < expectedKeys.size(); index++) {
            JsonObject item = json.getAsJsonArray("citationItems").get(index).getAsJsonObject();
            JsonObject oldItem = original.getAsJsonArray("citationItems").get(index).getAsJsonObject();
            assertEquals(expectedType == CitationType.AUTHORYEAR_INTEXT ? Set.of("id", "uris", "itemData", "suppress-author") : Set.of("id", "uris", "itemData"), item.keySet());
            assertNotEquals(oldItem.get("id"), item.get("id"));
            assertEquals(item.get("id"), item.getAsJsonObject("itemData").get("id"));
            assertEquals(List.of("http://www.jabref.org/" + expectedKeys.get(index)), item.getAsJsonArray("uris").asList().stream().map(uri -> uri.getAsString()).toList());
            assertFalse(item.getAsJsonObject("itemData").has("archive-place"));
            assertEquals(expectedKeys.get(index), item.getAsJsonObject("itemData").get("citation-key").getAsString());
        }
    }

    private static void assertNewCitationIdentifierLengths(XTextDocument document, boolean zotero) throws Exception {
        List<String> names = managedNames(document, zotero);
        assertFalse(names.isEmpty());
        for (String name : names) {
            if (zotero) {
                JsonObject json = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(name)).getAsJsonObject();
                assertEquals(8, json.get("citationID").getAsString().length(), name);
                String suffix = ZoteroReferenceMark.parse(name).orElseThrow().uniqueId();
                assertEquals("RND", suffix.substring(0, 3));
                assertEquals(10, suffix.substring(3).length(), name);
            } else {
                assertEquals(10, JabRefMarkCodec.decode(name).uniqueId().length(), name);
            }
        }
    }

    private void insert(CitationEngine engine, XTextDocument document, String key, boolean bibliography) throws Exception {
        success(engine.insert(List.of(database.getEntryByCitationKey(key).orElseThrow()), List.of(database), style(),
                CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), bibliography));
    }

    private static XTextCursor separatedEnd(XTextDocument document) {
        XTextCursor cursor = document.getText().createTextCursor();
        cursor.gotoEnd(false);
        cursor.setString("\n");
        cursor.collapseToEnd();
        return cursor;
    }

    private CitationEngine engine(XTextDocument document, OpenOfficePreferences preferences) {
        return new CitationEngine(document, office.context(), preferences, new BibEntryTypesManager());
    }

    private static OpenOfficePreferences preferences(boolean zotero) {
        OpenOfficePreferences preferences = OpenOfficePreferences.getDefault();
        preferences.setZoteroCompatibilityMode(zotero);
        preferences.setCslBibliographyBodyFormat("Standard");
        return preferences;
    }

    private static CitationStyle style() throws Exception {
        return CSLStyleUtils.createCitationStyleFromFile(resource("zotero-numeric.csl").toString()).orElseThrow();
    }

    private static Path resource(String name) throws Exception {
        return Path.of(Optional.ofNullable(CitationFormatConversionWriterTest.class.getResource(name)).orElseThrow().toURI());
    }

    private static List<String> managedNames(XTextDocument document, boolean zotero) throws Exception {
        return WriterReferenceMarks.getListOfNames(document).stream().filter(zotero ? ZoteroReferenceMark::isZoteroReferenceMarkName : JabRefMarkCodec::isCanonicalMarkName).toList();
    }

    private static List<String> bibliographyNames(XTextDocument document) throws Exception {
        return Arrays.stream(UnoTextSection.getNameAccess(document).getElementNames()).filter(name -> "JR_bib".equals(name) || name.startsWith("ZOTERO_BIBL ")).toList();
    }

    private static Map<String, Object> nativePreferenceProperties(Snapshot snapshot) {
        Map<String, Object> properties = new LinkedHashMap<>();
        snapshot.properties().forEach((name, value) -> {
            if (name.startsWith("ZOTERO_PREF") || name.startsWith("CSL_PREF")) {
                properties.put(name, value);
            }
        });
        return properties;
    }

    private static Snapshot snapshot(XTextDocument document) throws Exception {
        Map<String, String> marks = new LinkedHashMap<>();
        Map<String, List<Object>> formatting = new LinkedHashMap<>();
        for (String name : WriterReferenceMarks.getListOfNames(document)) {
            XTextRange anchor = WriterReferenceMarks.getAnchor(document, name).orElseThrow();
            marks.put(name, anchor.getString());
            XPropertySet format = query(XPropertySet.class, anchor.getText().createTextCursorByRange(anchor));
            formatting.put(name, List.of(format.getPropertyValue("CharHeight"), format.getPropertyValue("CharWeight"),
                    format.getPropertyValue("CharPosture"), format.getPropertyValue("CharColor"),
                    format.getPropertyValue("CharEscapement"), format.getPropertyValue("CharEscapementHeight"), format.getPropertyValue("CharStyleName")));
        }
        Map<String, String> sections = new LinkedHashMap<>();
        for (String name : UnoTextSection.getNameAccess(document).getElementNames()) {
            sections.put(name, UnoTextSection.getAnchor(document, name).orElseThrow().getString());
        }
        XPropertySet values = query(XPropertySet.class, query(XDocumentPropertiesSupplier.class, document).getDocumentProperties().getUserDefinedProperties());
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String name : UnoUserDefinedProperty.getListOfNames(document)) {
            properties.put(name, values.getPropertyValue(name));
        }
        return new Snapshot(document.getText().getString(), marks, sections, properties, formatting, UnoRedlines.getRecordChanges(document), document.hasControllersLocked());
    }

    private static <T> T success(OOResult<T, JabRefException> result) {
        if (result.isError()) {
            fail("Citation operation failed", result.getError());
        }
        return result.get();
    }

    @NullMarked
    private record Snapshot(String body, Map<String, String> marks, Map<String, String> sections, Map<String, Object> properties,
                            Map<String, List<Object>> formatting, boolean recordingChanges, boolean controllersLocked) {
    }
}
