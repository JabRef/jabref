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
import org.jabref.logic.openoffice.ZoteroCitationLinker;
import org.jabref.logic.openoffice.ZoteroCitationLinker.LinkPlan;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.backend.ReferenceMarkTextWriter;
import org.jabref.logic.openoffice.backend.WriterReferenceMark;
import org.jabref.logic.openoffice.backend.WriterReferenceMarks;
import org.jabref.logic.openoffice.engine.CitationEngine;
import org.jabref.logic.openoffice.engine.CitedEntryExporter;
import org.jabref.logic.openoffice.engine.MarkCodec;
import org.jabref.logic.openoffice.engine.storage.DocumentStateStore;
import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.engine.storage.UndoableDocumentProperties;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.DocumentAnnotation;
import org.jabref.logic.openoffice.uno.UnoRedlines;
import org.jabref.logic.openoffice.uno.UnoTextSection;
import org.jabref.logic.openoffice.uno.UnoUndo;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;

import com.google.gson.JsonArray;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.jabref.logic.openoffice.integration.HeadlessLibreOffice.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            assertEquals(List.of(), managedNames(document, !targetZotero));
            assertEquals(before.sections(), snapshot(document).sections());
            assertTrue(UnoRedlines.getRecordChanges(document));
            assertFalse(document.hasControllersLocked());
            assertEquals(Optional.of(style().getStyleId()), ZoteroDocumentPreferences.findCitationStyle(document, List.of(style())).map(CitationStyle::getStyleId));
            Snapshot after = snapshot(document);
            assertEquals(1, undo.getAllUndoActionTitles().length);
            undo.undo();
            assertEquals(before, snapshot(document));
            assertTrue(success(engine.requiresFormatConversion(style())));
            undo.redo();
            assertEquals(after, snapshot(document));
        }
    }

    @Test
    void allCanonicalTypesGroupUidKeyOrderAndPageSlotsSurviveBothConversionsAndSaveReopen() throws Exception {
        XTextDocument document = office.newDocument();
        MarkStore store = new MarkStore();
        Map<String, List<Optional<String>>> expected = new LinkedHashMap<>();
        for (CitationType type : CitationType.values()) {
            XTextCursor end = separatedEnd(document);
            CitationGroup group = store.createCitationGroup(document, office.context(), List.of("beta", "alpha"),
                    List.of(Optional.of(OOText.fromString("<b>p. 4</b>")), Optional.of(OOText.fromString("pp. 7–9"))),
                    type, end, false, false, "stable_" + type.name());
            expected.put(group.getReferenceMarkNameForLinking().orElseThrow(), List.of(Optional.of("<b>p. 4</b>"), Optional.of("pp. 7–9")));
        }
        try (CitationEngine engine = engine(document, preferences(true))) {
            insert(engine, document, "alpha", false);
        }
        assertEquals(List.of(), bibliographyNames(document));
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("types-in-zotero.odt"));
        try (CitationEngine engine = engine(reopened, preferences(false))) {
            assertTrue(success(engine.requiresFormatConversion(style())));
            insert(engine, reopened, "beta", false);
        }
        MarkStore restored = new MarkStore();
        for (Map.Entry<String, List<Optional<String>>> entry : expected.entrySet()) {
            CitationGroup group = restored.readCitationGroupFromDocumentOrThrow(reopened, entry.getKey());
            assertEquals(List.of("beta", "alpha"), group.citationsInStorageOrder.stream().map(citation -> citation.citationKey).toList());
            assertEquals(entry.getValue(), group.citationsInStorageOrder.stream().map(citation -> citation.getPageInfo().map(text -> text.toString())).toList());
        }
        assertEquals(List.of(), bibliographyNames(reopened));
    }

    @Test
    void nativeZoteroJsonUrisAndUnknownPropertiesSurviveCanonicalSaveReopenAndBack() throws Exception {
        XTextDocument document = office.newDocument();
        String nativeName = ZoteroReferenceMark.PREFIX + Files.readString(resource("zotero-native.json")).strip() + " RNDnative1234";
        XTextCursor end = separatedEnd(document);
        end.setString("[native]");
        WriterReferenceMarks.create(new DocumentAnnotation(document, nativeName, end, true));
        try (CitationEngine engine = engine(document, preferences(false))) {
            insert(engine, document, "alpha", false);
        }
        assertEquals(List.of(), managedNames(document, true));
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("native-in-canonical.odt"));
        try (CitationEngine engine = engine(reopened, preferences(true))) {
            insert(engine, reopened, "beta", false);
        }
        String restored = managedNames(reopened, true).stream().filter(name -> name.contains("native-citation")).findFirst().orElseThrow();
        JsonObject expected = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(nativeName)).getAsJsonObject();
        expected.getAsJsonArray("citationItems").get(0).getAsJsonObject().getAsJsonArray("uris").add("http://www.jabref.org/native");
        assertEquals(expected, JsonParser.parseString(ZoteroReferenceMark.getCSLJson(restored)));
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
    void mixedFormatsMergeCitedSetWhileLegacyMarksStayUnmanaged(boolean targetZotero) throws Exception {
        XTextDocument document = office.newDocument();
        new MarkStore().createCitationGroup(document, office.context(), List.of("beta"), List.of(Optional.empty()),
                CitationType.AUTHORYEAR_PAR, separatedEnd(document), false, false, "mixed_uid");
        String nativeName = ZoteroReferenceMark.PREFIX + Files.readString(resource("zotero-native.json")).strip() + " RNDnative1234";
        XTextCursor end = separatedEnd(document);
        end.setString("[native]");
        WriterReferenceMarks.create(new DocumentAnnotation(document, nativeName, end, true));
        String legacy = "JR_cite_1_legacyUnknown";
        end = separatedEnd(document);
        end.setString("Leave this legacy citation alone");
        WriterReferenceMarks.create(new DocumentAnnotation(document, legacy, end, true));
        try (CitationEngine engine = engine(document, preferences(targetZotero))) {
            assertTrue(success(engine.requiresFormatConversion(style())));
            insert(engine, document, "alpha", false);
            assertEquals(3, managedNames(document, targetZotero).size());
            assertEquals(List.of(), managedNames(document, !targetZotero));
            CitedEntryExporter.ExportResult exported = success(engine.exportCited(List.of(database)));
            assertEquals(List.of("alpha", "beta", "native"), exported.database().getEntries().stream().map(entry -> entry.getCitationKey().orElseThrow()).sorted().toList());
            assertEquals(List.of(), exported.unresolvedKeys());
            assertEquals("Leave this legacy citation alone", WriterReferenceMarks.getAnchor(document, legacy).orElseThrow().getString());
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
    void externalEditsToConvertedZoteroJsonAreRejectedWithoutDiscardingSidecarState() throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(false);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            preferences.setZoteroCompatibilityMode(true);
            insert(engine, document, "beta", false);
            String converted = managedNames(document, true).stream().filter(name -> name.contains("http://www.jabref.org/alpha")).findFirst().orElseThrow();
            XTextContent content = WriterReferenceMarks.getAsTextContent(document, converted).orElseThrow();
            XTextCursor cursor = content.getAnchor().getText().createTextCursorByRange(content.getAnchor());
            String text = cursor.getString();
            content.getAnchor().getText().removeTextContent(content);
            cursor.setString(text);
            WriterReferenceMarks.create(new DocumentAnnotation(document, converted.replace("\"citationItems\":", "\"foreignEdit\":true,\"citationItems\":"), cursor, true));
            preferences.setZoteroCompatibilityMode(false);
            Snapshot before = snapshot(document);
            XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
            undo.clear();
            assertTrue(engine.insert(List.of(database.getEntryByCitationKey("beta").orElseThrow()), List.of(database), style(),
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), false).isError());
            assertEquals(before, snapshot(document));
            assertEquals(0, undo.getAllUndoActionTitles().length);
        }
    }

    @Test
    void nativeInsertAndRefreshIgnoreStaleSnapshotsAfterExternalFormattedCitationUpdate() throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(false);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            preferences.setZoteroCompatibilityMode(true);
            insert(engine, document, "beta", false);
            Map<String, String> snapshots = conversionSnapshots(document);
            String original = managedNames(document, true).stream().filter(name -> name.contains("http://www.jabref.org/alpha")).findFirst().orElseThrow();
            JsonObject json = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(original)).getAsJsonObject();
            JsonObject properties = json.has("properties") ? json.getAsJsonObject("properties") : new JsonObject();
            properties.addProperty("formattedCitation", "External Zotero formatting");
            json.add("properties", properties);
            String edited = ZoteroReferenceMark.PREFIX + json + " " + ZoteroReferenceMark.parse(original).orElseThrow().uniqueId();
            replaceNativeMarkName(document, original, edited);

            assertFalse(success(engine.requiresFormatConversion(style())));
            insert(engine, document, "native", false);
            assertEquals(snapshots, conversionSnapshots(document));
            assertTrue(managedNames(document, true).contains(edited));
            success(engine.refresh(List.of(database), style()));
            assertFalse(success(engine.requiresFormatConversion(style())));
            assertEquals(snapshots, conversionSnapshots(document));
            assertTrue(managedNames(document, true).contains(edited));
            assertEquals("External Zotero formatting", JsonParser.parseString(ZoteroReferenceMark.getCSLJson(edited)).getAsJsonObject()
                                                                 .getAsJsonObject("properties").get("formattedCitation").getAsString());

            preferences.setZoteroCompatibilityMode(false);
            JabRefException failure = assertRejectedWithoutMutation(engine, document);
            assertTrue(Optional.ofNullable(failure.getLocalizedMessage()).orElseThrow().contains("stored conversion information"));
            assertEquals(snapshots, conversionSnapshots(document));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"insert", "refresh"})
    void nativeUriLinkingLeavesStaleAndCorruptSnapshotsUnchanged(String operation) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(false);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "native", false);
            preferences.setZoteroCompatibilityMode(true);
            insert(engine, document, "alpha", false);
            String original = managedNames(document, true).stream().filter(name -> name.contains("http://www.jabref.org/native")).findFirst().orElseThrow();
            JsonObject json = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(original)).getAsJsonObject();
            JsonArray uris = new JsonArray();
            uris.add("http://zotero.org/users/123/items/NATIVE123");
            json.getAsJsonArray("citationItems").get(0).getAsJsonObject().add("uris", uris);
            JsonObject properties = json.has("properties") ? json.getAsJsonObject("properties") : new JsonObject();
            properties.addProperty("formattedCitation", "External Zotero formatting");
            json.add("properties", properties);
            String unlinked = ZoteroReferenceMark.PREFIX + json + " " + ZoteroReferenceMark.parse(original).orElseThrow().uniqueId();
            replaceNativeMarkName(document, original, unlinked);
            UnoUserDefinedProperty.setStringProperty(document, "JabRef_citation_conversion:unrelated", "not JSON");
            Map<String, String> snapshots = conversionSnapshots(document);
            LinkPlan links = ZoteroCitationLinker.planLinks(managedNames(document, true), new BibDatabaseContext(database), new BibEntryTypesManager());
            assertEquals(1, links.updates().size());
            assertFalse(success(engine.requiresFormatConversion(style())));

            Snapshot before = snapshot(document);
            CitationStyle invalid = new CitationStyle("invalid.csl", "invalid", "in-text", "Invalid", "", true, true, false, false, "not XML", false);
            OOResult<List<String>, JabRefException> failed = "refresh".equals(operation) ? engine.refresh(List.of(database), invalid)
                                                                                         : engine.insert(List.of(database.getEntryByCitationKey("beta").orElseThrow()), List.of(database), invalid,
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), false);
            assertTrue(failed.isError());
            assertEquals(before, snapshot(document));
            assertEquals(snapshots, conversionSnapshots(document));

            if ("refresh".equals(operation)) {
                success(engine.refresh(List.of(database), style()));
            } else {
                insert(engine, document, "beta", false);
            }
            String linked = links.updatedName(unlinked);
            assertTrue(managedNames(document, true).contains(linked));
            assertFalse(managedNames(document, true).contains(unlinked));
            assertEquals(snapshots, conversionSnapshots(document));
            assertFalse(success(engine.requiresFormatConversion(style())));
            JsonObject linkedJson = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(linked)).getAsJsonObject();
            assertEquals("External Zotero formatting", linkedJson.getAsJsonObject("properties").get("formattedCitation").getAsString());
            assertEquals(List.of("http://zotero.org/users/123/items/NATIVE123", "http://www.jabref.org/native"),
                    linkedJson.getAsJsonArray("citationItems").get(0).getAsJsonObject().getAsJsonArray("uris").asList().stream().map(uri -> uri.getAsString()).toList());

            preferences.setZoteroCompatibilityMode(false);
            assertRejectedWithoutMutation(engine, document);
            assertEquals(snapshots, conversionSnapshots(document));
        }
    }

    private static void replaceNativeMarkName(XTextDocument document, String before, String after) throws Exception {
        XTextContent content = WriterReferenceMarks.getAsTextContent(document, before).orElseThrow();
        XTextCursor cursor = content.getAnchor().getText().createTextCursorByRange(content.getAnchor());
        String text = cursor.getString();
        content.getAnchor().getText().removeTextContent(content);
        cursor.setString(text);
        WriterReferenceMarks.create(new DocumentAnnotation(document, after, cursor, true));
    }

    private static Map<String, String> conversionSnapshots(XTextDocument document) throws Exception {
        Map<String, String> snapshots = new LinkedHashMap<>();
        for (String property : UnoUserDefinedProperty.getListOfNames(document)) {
            if (property.startsWith("JabRef_citation_conversion:")) {
                snapshots.put(property, UnoUserDefinedProperty.getStringValue(document, property).orElseThrow());
            }
        }
        return snapshots;
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
                                          : MarkCodec.decode(name).citationKeys().getFirst();
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
            UnoTextSection.create(new DocumentAnnotation(document, "Unrelated protected section", end, true));
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
    void conversionPreservesForeignIdsAndAvoidsNumericCollisions(String foreignId) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(false);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            JsonObject nativeJson = JsonParser.parseString(Files.readString(resource("zotero-native.json"))).getAsJsonObject();
            JsonObject nativeItem = nativeJson.getAsJsonArray("citationItems").get(0).getAsJsonObject();
            nativeItem.addProperty("id", foreignId);
            nativeItem.getAsJsonObject("itemData").addProperty("id", foreignId);
            XTextCursor end = separatedEnd(document);
            end.setString("[native]");
            WriterReferenceMarks.create(new DocumentAnnotation(document, ZoteroReferenceMark.PREFIX + nativeJson + " RNDnative1234", end, true));
            preferences.setZoteroCompatibilityMode(true);
            insert(engine, document, "beta", false);
            Set<String> allocated = new HashSet<>();
            for (String name : managedNames(document, true)) {
                JsonObject citation = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(name)).getAsJsonObject();
                JsonObject item = citation.getAsJsonArray("citationItems").get(0).getAsJsonObject();
                if ("native-citation".equals(citation.get("citationID").getAsString())) {
                    assertEquals(foreignId, item.get("id").getAsString());
                    assertEquals(foreignId, item.getAsJsonObject("itemData").get("id").getAsString());
                } else {
                    allocated.add(item.get("id").getAsString());
                }
            }
            assertEquals("0001".equals(foreignId) ? Set.of("2", "3") : Set.of("1", "2"), allocated);
        }
    }

    private static void createSection(XTextDocument document, String name, String content) throws Exception {
        XSearchable searchable = query(XSearchable.class, document);
        XSearchDescriptor descriptor = searchable.createSearchDescriptor();
        descriptor.setSearchString(content);
        XTextRange range = query(XTextRange.class, searchable.findFirst(descriptor));
        UnoTextSection.create(new DocumentAnnotation(document, name, range, true));
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

    @Test
    void realNativeLocatorFixtureRetainsItsVisibleTextAndJsonWhenConversionIsRejected() throws Exception {
        XTextDocument document = office.newDocument();
        String name = ZoteroReferenceMark.PREFIX + Files.readString(resource("zotero-native-locator.json")).strip() + " RNDlocator001";
        XTextCursor cursor = separatedEnd(document);
        cursor.setString("[Native, 2020, p. 17]");
        WriterReferenceMarks.create(new DocumentAnnotation(document, name, cursor, true));
        try (CitationEngine engine = engine(document, preferences(false))) {
            JabRefException failure = assertRejectedWithoutMutation(engine, document);
            assertTrue(Optional.ofNullable(failure.getLocalizedMessage()).orElseThrow().contains("locators"));
            assertEquals("[Native, 2020, p. 17]", WriterReferenceMarks.getAnchor(document, name).orElseThrow().getString());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"label", "prefix", "suffix", "mixed-suppression", "author-only"})
    void unsupportedNativeCitationModifiersRejectConversionBeforeMutation(String modifier) throws Exception {
        XTextDocument document = office.newDocument();
        JsonObject json = JsonParser.parseString(Files.readString(resource("zotero-native.json"))).getAsJsonObject();
        JsonObject first = json.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        if ("mixed-suppression".equals(modifier)) {
            first.addProperty("suppress-author", true);
            JsonObject second = first.deepCopy();
            second.remove("suppress-author");
            json.getAsJsonArray("citationItems").add(second);
        } else if ("author-only".equals(modifier)) {
            first.addProperty(modifier, true);
        } else {
            first.addProperty(modifier, "native modifier");
        }
        XTextCursor cursor = separatedEnd(document);
        cursor.setString("Visible native content must not change");
        WriterReferenceMarks.create(new DocumentAnnotation(document, ZoteroReferenceMark.PREFIX + json + " RNDmodifier01", cursor, true));
        try (CitationEngine engine = engine(document, preferences(false))) {
            JabRefException failure = assertRejectedWithoutMutation(engine, document);
            assertTrue(Optional.ofNullable(failure.getLocalizedMessage()).orElseThrow().contains("Remove these modifiers in Zotero"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"numeric-style", "readonly-style", "nonremovable-page", "numeric-metadata"})
    void propertyTypesWritabilityAndRemovabilityAreCheckedBeforeConversion(String kind) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences(false);
        try (CitationEngine engine = engine(document, preferences)) {
            insert(engine, document, "alpha", false);
            String uid = MarkCodec.decode(managedNames(document, false).getFirst()).uniqueId();
            String property = switch (kind) {
                case "nonremovable-page" ->
                        MarkStore.pageInfoProperty(uid, 0);
                case "numeric-metadata" ->
                        "JabRef_citation_conversion:" + uid;
                default ->
                        DocumentStateStore.PROPERTY_NAME;
            };
            UnoUserDefinedProperty.removeIfExists(document, property);
            XPropertyContainer container = UnoUserDefinedProperty.getPropertyContainer(document).orElseThrow();
            short attributes = "nonremovable-page".equals(kind) ? 0 : PropertyAttribute.REMOVEABLE;
            if ("readonly-style".equals(kind)) {
                attributes |= PropertyAttribute.READONLY;
            }
            container.addProperty(property, attributes, kind.startsWith("numeric") ? 42 : "original value");
            preferences.setZoteroCompatibilityMode(true);
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
            preferences.setZoteroCompatibilityMode(targetZotero);
            UnoRedlines.setRecordChanges(document, true);
            Snapshot before = snapshot(document);
            XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
            List<String> previousUndoTitles = List.of(undo.getAllUndoActionTitles());
            AtomicBoolean renamed = new AtomicBoolean();
            try (MockedStatic<UndoableDocumentProperties> properties = mockStatic(UndoableDocumentProperties.class, invocation -> {
                if ("properties".equals(phase) && "set".equals(invocation.getMethod().getName())
                        && DocumentStateStore.PROPERTY_NAME.equals(invocation.getArgument(1))) {
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
                             throw new CreationException("Injected marker write failure after rename");
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
        return WriterReferenceMarks.getListOfNames(document).stream().filter(zotero ? ZoteroReferenceMark::isZoteroReferenceMarkName : MarkCodec::isCanonicalMarkName).toList();
    }

    private static List<String> bibliographyNames(XTextDocument document) throws Exception {
        return Arrays.stream(UnoTextSection.getNameAccess(document).getElementNames()).filter(name -> "JR_bib".equals(name) || name.startsWith("ZOTERO_BIBL ")).toList();
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
