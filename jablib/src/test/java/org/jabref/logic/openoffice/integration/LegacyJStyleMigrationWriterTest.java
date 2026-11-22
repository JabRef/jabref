package org.jabref.logic.openoffice.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.logic.JabRefException;
import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.backend.ReferenceMarkTextWriter;
import org.jabref.logic.openoffice.backend.WriterReferenceMark;
import org.jabref.logic.openoffice.backend.WriterReferenceMarks;
import org.jabref.logic.openoffice.engine.BibliographySection;
import org.jabref.logic.openoffice.engine.CitationEngine;
import org.jabref.logic.openoffice.engine.CitationRenderer;
import org.jabref.logic.openoffice.engine.CslRenderer;
import org.jabref.logic.openoffice.engine.JStyleRenderer;
import org.jabref.logic.openoffice.engine.JabRefMarkCodec;
import org.jabref.logic.openoffice.engine.JabRefMarkData;
import org.jabref.logic.openoffice.engine.RenderRequest;
import org.jabref.logic.openoffice.engine.RenderResult;
import org.jabref.logic.openoffice.engine.RendererFactory;
import org.jabref.logic.openoffice.engine.storage.DocumentStyleStore;
import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.engine.storage.UndoableDocumentProperties;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.NamedTextContentPlacement;
import org.jabref.logic.openoffice.uno.OOTextWriter;
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

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.document.XUndoManager;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextSection;
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
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.jabref.logic.openoffice.integration.HeadlessLibreOffice.query;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@NullMarked
@Tag("LibreOfficeTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("Localization.lang")
@Timeout(value = 180, unit = TimeUnit.SECONDS)
// [utest->req~openoffice.legacy-jstyle-migration~1]
class LegacyJStyleMigrationWriterTest {
    private static final String CID = "JABREF_unknown CID_7 ignored NORMAL";
    private static final List<String> GROUP_KEYS = List.of("beta", "alpha", "beta");
    private static final List<CitationType> LEGACY_TYPES = List.of(
            CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.INVISIBLE_CIT,
            CitationType.AUTHORYEAR_NOPAR, CitationType.AUTHOR_ONLY, CitationType.YEAR_ONLY);

    @TempDir
    Path temporaryDirectory;

    private final HeadlessLibreOffice office = new HeadlessLibreOffice(Path.of("build/writer-integration/legacy-jstyle-migration"));
    private final BibDatabase database = new BibDatabase(List.of(
            new BibEntry().withCitationKey("alpha").withField(StandardField.TITLE, "Alpha title").withField(StandardField.AUTHOR, "Alpha, Alice").withField(StandardField.YEAR, "2020"),
            new BibEntry().withCitationKey("beta").withField(StandardField.TITLE, "Beta title").withField(StandardField.AUTHOR, "Beta, Bob").withField(StandardField.YEAR, "2021"),
            new BibEntry().withCitationKey("gamma").withField(StandardField.TITLE, "Gamma title").withField(StandardField.AUTHOR, "Gamma, Grace").withField(StandardField.YEAR, "2022"),
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

    @ParameterizedTest
    @CsvSource({"jstyle, false", "jstyle, true", "csl, false"})
    void nextInsertionMigratesAllTypesInOneRenderAndUndoTransactionAndPersistsIdentities(String family, boolean savedZoteroMode) throws Exception {
        XTextDocument document = office.newDocument();
        Map<String, CitationType> legacyTypes = new LinkedHashMap<>();
        for (int code = 1; code <= 6; code++) {
            String name = "JR_cite_" + code + "_beta,alpha,beta";
            legacyTypes.put(name, LEGACY_TYPES.get(code - 1));
            appendLegacy(document, name, code == 3 ? "" : "Old marker " + code, "p. " + code);
        }
        String repeated = "JR_cite42_1_beta,alpha,beta";
        legacyTypes.put(repeated, CitationType.AUTHORYEAR_PAR);
        appendMark(document, repeated, "Repeated group without page information");
        String canonical = createCanonical(document);
        appendLegacy(document, CID, "Unsupported CID text", "Untouched CID metadata");
        OpenOfficePreferences preferences = preferences(savedZoteroMode);
        OOStyle style = style(family);
        XTextCursor position = document.getText().createTextCursor();
        position.gotoEnd(false);
        XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
        undo.clear();
        UnoRedlines.setRecordChanges(document, true);
        Snapshot before = snapshot(document);
        Map<String, String> migratedNames = new LinkedHashMap<>();
        try (CitationEngine engine = engine(document, preferences)) {
            assertTrue(success(engine.requiresFormatConversion(style)));
            assertEquals(before, snapshot(document));
            List<RenderRequest> renders = new ArrayList<>();
            Map<String, OOText> renderedMarkers = new LinkedHashMap<>();
            CitationRenderer realRenderer = new RendererFactory(new BibEntryTypesManager(), preferences.getPandocPath()).create(style);
            try (MockedConstruction<JStyleRenderer> jstyleRenderers = mockConstruction(JStyleRenderer.class, (renderer, context) -> {
                assertSame(style, context.arguments().getFirst());
                observeRendering(renderer, realRenderer, document, before, renders, renderedMarkers);
            });
                 MockedConstruction<CslRenderer> cslRenderers = mockConstruction(CslRenderer.class, (renderer, context) -> {
                     assertSame(style, context.arguments().getFirst());
                     observeRendering(renderer, realRenderer, document, before, renders, renderedMarkers);
                 })) {
                assertEquals(List.of(), success(engine.insert(List.of(database.getEntryByCitationKey("gamma").orElseThrow()), List.of(database), style,
                        CitationType.AUTHORYEAR_PAR, "p. 90", position, true)));
            }
            assertEquals(1, renders.size(), "Render the complete migrated, canonical and pending citation set once");
            assertEquals(9, renders.getFirst().groupsInDocumentOrder().size());
            assertEquals(List.of(GROUP_KEYS, GROUP_KEYS, GROUP_KEYS, GROUP_KEYS, GROUP_KEYS, GROUP_KEYS, GROUP_KEYS, List.of("alpha"), List.of("gamma")),
                    renders.getFirst().groupsInDocumentOrder().stream()
                           .map(group -> group.citationsInStorageOrder.stream().map(citation -> citation.citationKey).toList()).toList());
            List<String> names = canonicalNames(document);
            assertEquals(9, names.size());
            assertEquals(9, names.stream().map(name -> decode(name).uniqueId()).distinct().count());
            XTextDocument expectedText = office.newDocument();
            for (String name : names) {
                expectedText.getText().setString("");
                OOText marker = Optional.ofNullable(renderedMarkers.get(decode(name).uniqueId())).orElseThrow();
                OOTextWriter.write(expectedText, expectedText.getText().createTextCursor(), marker);
                assertEquals("\u200b" + expectedText.getText().getString() + "\u200b", WriterReferenceMarks.getAnchor(document, name).orElseThrow().getString());
            }
            assertFalse(document.getText().getString().contains("Old marker"));
            assertFalse(document.getText().getString().contains("Repeated group without page information"));
            for (int index = 0; index < legacyTypes.size(); index++) {
                String oldName = new ArrayList<>(legacyTypes.keySet()).get(index);
                CitationGroup planned = renders.getFirst().groupsInDocumentOrder().get(index);
                String name = names.stream().filter(candidate -> decode(candidate).uniqueId().equals(planned.groupId.citationGroupIdAsString())).findFirst().orElseThrow();
                migratedNames.put(oldName, name);
                JabRefMarkData data = JabRefMarkCodec.decode(name);
                assertEquals(GROUP_KEYS, data.citationKeys());
                assertEquals(legacyTypes.get(oldName), data.citationType());
                assertFalse(data.uniqueId().isBlank());
                assertNotEquals("stable_current", data.uniqueId());
                assertNotEquals(oldName, data.uniqueId());
                assertNotEquals("42", data.uniqueId());
                List<Optional<String>> pages = List.of(Optional.empty(), Optional.empty(), index < 6 ? Optional.of("p. " + (index + 1)) : Optional.empty());
                assertGroup(document, name, GROUP_KEYS, data.citationType(), pages);
                assertFalse(WriterReferenceMarks.getListOfNames(document).contains(oldName));
                assertEquals(Optional.empty(), UnoUserDefinedProperty.getStringValue(document, oldName));
            }
            assertGroup(document, canonical, List.of("alpha"), CitationType.AUTHORYEAR_PAR, List.of(Optional.of("p. 88")));
            String pending = names.stream().filter(name -> decode(name).citationKeys().equals(List.of("gamma"))).findFirst().orElseThrow();
            assertGroup(document, pending, List.of("gamma"), CitationType.AUTHORYEAR_PAR, List.of(Optional.of("p. 90")));
            assertEquals("Unsupported CID text", WriterReferenceMarks.getAnchor(document, CID).orElseThrow().getString());
            assertEquals(Optional.of("Untouched CID metadata"), UnoUserDefinedProperty.getStringValue(document, CID));
            String bibliography = UnoTextSection.getAnchor(document, "JR_bib").orElseThrow().getString();
            for (String title : List.of("Alpha title", "Beta title", "Gamma title")) {
                assertTrue(bibliography.contains(title), bibliography);
                assertEquals(bibliography.indexOf(title), bibliography.lastIndexOf(title));
            }
            assertFalse(success(engine.requiresFormatConversion(style)));
            Snapshot after = snapshot(document);
            assertEquals(1, undo.getAllUndoActionTitles().length);
            undo.undo();
            assertEquals(before, snapshot(document));
            assertTrue(success(engine.requiresFormatConversion(style)));
            undo.redo();
            assertEquals(after, snapshot(document));
            assertFalse(success(engine.requiresFormatConversion(style)));
        }
        Snapshot migrated = snapshot(document);
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve(family + "-" + savedZoteroMode + "-migrated.odt"));
        assertEquals(migrated.marks(), snapshot(reopened).marks());
        assertEquals(migrated.properties(), snapshot(reopened).properties());
        try (CitationEngine engine = engine(reopened, preferences)) {
            assertFalse(success(engine.requiresFormatConversion(style)));
            success(engine.refresh(List.of(database), style));
            assertFalse(success(engine.requiresFormatConversion(style)));
            insert(engine, style, end(reopened), false);
        }
        for (String name : migratedNames.values()) {
            JabRefMarkData data = JabRefMarkCodec.decode(name);
            assertTrue(canonicalNames(reopened).contains(name));
            assertEquals(data.uniqueId(), new MarkStore().readCitationGroupFromDocumentOrThrow(reopened, name).groupId.citationGroupIdAsString());
            assertBoundaries(reopened, name, data.citationType());
        }
        assertTrue(canonicalNames(reopened).contains(canonical));
        assertEquals(10, canonicalNames(reopened).size());
    }

    @Test
    void migrationWithoutBibliographySyncDoesNotRequireBibliographyParagraphStyles() throws Exception {
        XTextDocument document = office.newDocument();
        String legacy = "JR_cite12_1_beta,alpha";
        appendLegacy(document, legacy, "Legacy citation", "p. 5");
        JStyle style = spy((JStyle) style("jstyle"));
        when(style.getReferenceParagraphFormat()).thenReturn("Missing bibliography paragraph style");
        when(style.getReferenceHeaderParagraphFormat()).thenReturn("Missing bibliography heading style");
        Map<String, String> sections = snapshot(document).sections();
        try (MockedConstruction<BibliographySection> bibliographies = mockConstruction(BibliographySection.class);
             CitationEngine engine = engine(document, preferences(false))) {
            insert(engine, style, end(document), false);

            assertFalse(WriterReferenceMarks.getListOfNames(document).contains(legacy));
            assertEquals(2, canonicalNames(document).size());
            assertEquals(sections, snapshot(document).sections());
            assertEquals(List.of(), bibliographies.constructed());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"jstyle", "csl"})
    void connectionDetectionExportAndRefreshNeverMigrateLegacyMarks(String family) throws Exception {
        XTextDocument document = office.newDocument();
        String legacy = "JR_cite7_1_beta,alpha";
        appendLegacy(document, legacy, "Original legacy text", "p. 12");
        appendLegacy(document, CID, "Original CID text", "Original CID property");
        Snapshot original = snapshot(document);
        OOStyle style = style(family);
        for (int connection = 0; connection < 2; connection++) {
            try (CitationEngine engine = engine(document, preferences(false))) {
                assertEquals(original.marks(), snapshot(document).marks());
                assertEquals(Optional.of("p. 12"), UnoUserDefinedProperty.getStringValue(document, legacy));
                Snapshot connected = snapshot(document);
                assertTrue(success(engine.requiresFormatConversion(style)));
                var exported = success(engine.exportCited(List.of(database)));
                assertEquals(List.of(), exported.database().getEntries());
                assertEquals(List.of(), exported.unresolvedKeys());
                assertEquals(connected, snapshot(document));
                success(engine.refresh(List.of(database), style));
                assertTrue(success(engine.requiresFormatConversion(style)));
                assertEquals(original.marks(), snapshot(document).marks());
                for (Map.Entry<String, Object> property : original.properties().entrySet()) {
                    assertEquals(property.getValue(), snapshot(document).properties().get(property.getKey()));
                }
            }
        }
    }

    @Test
    void nativeCslInsertionLeavesLegacyTextAndMetadataUntilNextCanonicalInsertion() throws Exception {
        XTextDocument document = office.newDocument();
        String legacy = "JR_cite0_2_beta,alpha";
        appendLegacy(document, legacy, "Legacy in-text citation", "pp. 7–9");
        appendLegacy(document, CID, "CID text", "CID metadata");
        OpenOfficePreferences preferences = preferences(true);
        OOStyle style = style("csl");
        try (CitationEngine engine = engine(document, preferences)) {
            assertFalse(success(engine.requiresFormatConversion(style)));
            insert(engine, style, end(document), false);
            assertEquals("Legacy in-text citation", WriterReferenceMarks.getAnchor(document, legacy).orElseThrow().getString());
            assertEquals(Optional.of("pp. 7–9"), UnoUserDefinedProperty.getStringValue(document, legacy));
            assertEquals(List.of(), canonicalNames(document));
            assertEquals(1, nativeNames(document).size());
            preferences.setZoteroCompatibilityMode(false);
            assertTrue(success(engine.requiresFormatConversion(style)));
            XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
            undo.clear();
            Snapshot before = snapshot(document);
            insert(engine, style, end(document), false);
            assertEquals(List.of(), nativeNames(document));
            assertFalse(WriterReferenceMarks.getListOfNames(document).contains(legacy));
            assertEquals(3, canonicalNames(document).size());
            String migrated = canonicalNames(document).stream().filter(name -> decode(name).citationKeys().equals(List.of("beta", "alpha"))).findFirst().orElseThrow();
            assertGroup(document, migrated, List.of("beta", "alpha"), CitationType.AUTHORYEAR_INTEXT, List.of(Optional.empty(), Optional.of("pp. 7–9")));
            assertEquals("CID text", WriterReferenceMarks.getAnchor(document, CID).orElseThrow().getString());
            assertEquals(Optional.of("CID metadata"), UnoUserDefinedProperty.getStringValue(document, CID));
            Snapshot after = snapshot(document);
            assertEquals(1, undo.getAllUndoActionTitles().length);
            undo.undo();
            assertEquals(before, snapshot(document));
            undo.redo();
            assertEquals(after, snapshot(document));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void jstyleMigrationLeavesNativeZoteroMarksAndPropertiesUntouched(boolean savedZoteroMode) throws Exception {
        XTextDocument document = office.newDocument();
        String nativeName = ZoteroReferenceMark.PREFIX + Files.readString(resource("zotero-native.json")).strip() + " RNDnative1234";
        appendMark(document, nativeName, "Native text must not be rendered by JStyle");
        UnoUserDefinedProperty.setStringProperty(document, "ZOTERO_PREF_1", "Native preferences must stay unchanged");
        appendLegacy(document, "JR_cite12_1_beta,alpha", "Old JStyle text", "p. 5");
        try (CitationEngine engine = engine(document, preferences(savedZoteroMode))) {
            OOStyle style = style("jstyle");
            assertTrue(success(engine.requiresFormatConversion(style)));
            insert(engine, style, end(document), false);
            assertFalse(success(engine.requiresFormatConversion(style)));
        }
        assertEquals(List.of(nativeName), nativeNames(document));
        assertEquals("Native text must not be rendered by JStyle", WriterReferenceMarks.getAnchor(document, nativeName).orElseThrow().getString());
        assertEquals(Optional.of("Native preferences must stay unchanged"), UnoUserDefinedProperty.getStringValue(document, "ZOTERO_PREF_1"));
        assertEquals(2, canonicalNames(document).size());
    }

    @ParameterizedTest
    @CsvSource({"jstyle, missing", "csl, missing", "jstyle, render", "csl, render",
            "jstyle, protected-source", "csl, protected-source", "jstyle, protected-cursor", "csl, protected-cursor"})
    void unresolvedKeysRenderFailuresAndProtectedRangesLeaveNoWritesOrUndo(String family, String failure) throws Exception {
        XTextDocument document = office.newDocument();
        String legacy = "JR_cite3_1_beta,alpha";
        appendLegacy(document, legacy, "Original legacy text", "p. 11");
        createCanonical(document);
        OOStyle style = style(family);
        XTextCursor cursor = end(document);
        if (failure.startsWith("protected")) {
            XTextRange range = "protected-source".equals(failure) ? WriterReferenceMarks.getAnchor(document, legacy).orElseThrow() : cursor;
            if ("protected-cursor".equals(failure)) {
                cursor.setString("Protected insertion position");
            }
            UnoTextSection.create(new NamedTextContentPlacement(document, "Protected user section", range, true));
            XTextSection section = UnoTextSection.getByName(document, "Protected user section").orElseThrow();
            query(XPropertySet.class, section).setPropertyValue("IsProtected", true);
            cursor = "protected-source".equals(failure) ? end(document) : section.getAnchor().getText().createTextCursorByRange(section.getAnchor());
            if ("protected-cursor".equals(failure)) {
                cursor.collapseToStart();
                cursor.goRight((short) 1, false);
            }
        }
        if ("render".equals(failure)) {
            style = failingStyle(family);
        }
        List<BibDatabase> sources = "missing".equals(failure)
                                    ? List.of(new BibDatabase(List.of(database.getEntryByCitationKey("alpha").orElseThrow(), database.getEntryByCitationKey("gamma").orElseThrow())))
                                    : List.of(database);
        XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
        undo.clear();
        UnoRedlines.setRecordChanges(document, true);
        try (CitationEngine engine = engine(document, preferences(false));
             MockedStatic<ReferenceMarkTextWriter> markers = mockStatic(ReferenceMarkTextWriter.class, invocation -> {
                 if ("write".equals(invocation.getMethod().getName())) {
                     return fail("Preflight rejection must not write citation text");
                 }
                 return invocation.callRealMethod();
             });
             MockedStatic<UndoableDocumentProperties> properties = mockStatic(UndoableDocumentProperties.class, invocation -> {
                 if ("set".equals(invocation.getMethod().getName())) {
                     return fail("Preflight rejection must not write document properties");
                 }
                 return invocation.callRealMethod();
             })) {
            assertTrue(success(engine.requiresFormatConversion(style)));
            Snapshot before = snapshot(document);
            assertTrue(engine.insert(List.of(database.getEntryByCitationKey("gamma").orElseThrow()), sources, style,
                    CitationType.AUTHORYEAR_PAR, "", cursor, true).isError());
            assertEquals(before, snapshot(document));
            assertEquals(0, undo.getAllUndoActionTitles().length);
            assertFalse(undo.isRedoPossible());
            assertTrue(success(engine.requiresFormatConversion(style)));
        }
    }

    @ParameterizedTest
    @CsvSource({"jstyle, markers", "csl, markers", "jstyle, properties", "csl, properties"})
    void writeFailuresRestoreLegacyPropertiesMarksAndEarlierUserUndoHistory(String family, String phase) throws Exception {
        XTextDocument document = office.newDocument();
        String legacy = "JR_cite9_1_beta,alpha";
        appendLegacy(document, legacy, "Original legacy text", "pp. 3–5");
        createCanonical(document);
        UnoUserDefinedProperty.setStringProperty(document, DocumentStyleStore.PROPERTY_NAME, "JSTYLE\noriginal style");
        UnoRedlines.setRecordChanges(document, true);
        Snapshot before = snapshot(document);
        XUndoManager undo = UnoUndo.getXUndoManager(document).orElseThrow();
        List<String> previousUndo = List.of(undo.getAllUndoActionTitles());
        assertFalse(previousUndo.isEmpty());
        AtomicBoolean wrote = new AtomicBoolean();
        try (CitationEngine engine = engine(document, preferences(false));
             MockedStatic<UndoableDocumentProperties> properties = mockStatic(UndoableDocumentProperties.class, invocation -> {
                 if ("properties".equals(phase) && "set".equals(invocation.getMethod().getName())
                         && DocumentStyleStore.PROPERTY_NAME.equals(invocation.getArgument(1))) {
                     wrote.set(!WriterReferenceMarks.getListOfNames(document).contains(legacy) && canonicalNames(document).size() == 3);
                     throw new PropertyVetoException("Injected migration property write failure");
                 }
                 return invocation.callRealMethod();
             });
             MockedStatic<ReferenceMarkTextWriter> markers = mockStatic(ReferenceMarkTextWriter.class, invocation -> {
                 if ("markers".equals(phase) && "write".equals(invocation.getMethod().getName())) {
                     WriterReferenceMark mark = invocation.getArgument(2);
                     if (JabRefMarkCodec.isCanonicalMarkName(mark.getRangeName()) && decode(mark.getRangeName()).citationKeys().equals(List.of("beta", "alpha"))) {
                         wrote.set(!WriterReferenceMarks.getListOfNames(document).contains(legacy));
                         throw new WriterOperationException("Injected migration marker write failure after rename");
                     }
                 }
                 return invocation.callRealMethod();
             })) {
            OOStyle style = style(family);
            assertTrue(success(engine.requiresFormatConversion(style)));
            assertTrue(engine.insert(List.of(database.getEntryByCitationKey("gamma").orElseThrow()), List.of(database), style,
                    CitationType.AUTHORYEAR_PAR, "", end(document), true).isError());
            assertTrue(wrote.get(), "The injected failure must occur after migration has started writing");
            assertEquals(before, snapshot(document));
            assertEquals(previousUndo, List.of(undo.getAllUndoActionTitles()));
            assertFalse(undo.isRedoPossible());
            assertTrue(success(engine.requiresFormatConversion(style)));
        }
    }

    private static void observeRendering(CitationRenderer observed, CitationRenderer realRenderer, XTextDocument document,
                                         Snapshot before, List<RenderRequest> renders, Map<String, OOText> renderedMarkers) throws Exception {
        when(observed.capabilities()).thenReturn(realRenderer.capabilities());
        when(observed.render(any())).thenAnswer(invocation -> {
            assertEquals(before, snapshot(document), "Rendering must precede all document writes");
            RenderRequest request = invocation.getArgument(0);
            renders.add(request);
            RenderResult result = realRenderer.render(request);
            result.markers().forEach((id, text) -> renderedMarkers.put(id.citationGroupIdAsString(), text));
            return result;
        });
    }

    private String createCanonical(XTextDocument document) throws Exception {
        return new MarkStore().createCitationGroup(document, office.context(), List.of("alpha"), List.of(Optional.of(OOText.fromString("p. 88"))),
                CitationType.AUTHORYEAR_PAR, separatedEnd(document), false, false, "stable_current").getReferenceMarkNameForLinking().orElseThrow();
    }

    private static void assertGroup(XTextDocument document, String name, List<String> keys, CitationType type, List<Optional<String>> pages) throws Exception {
        CitationGroup group = new MarkStore().readCitationGroupFromDocumentOrThrow(document, name);
        assertEquals(keys, group.citationsInStorageOrder.stream().map(citation -> citation.citationKey).toList());
        assertEquals(type, group.citationType);
        assertEquals(pages, group.citationsInStorageOrder.stream().map(citation -> citation.getPageInfo().map(Object::toString)).toList());
        assertEquals(JabRefMarkCodec.decode(name).uniqueId(), group.groupId.citationGroupIdAsString());
        assertBoundaries(document, name, type);
    }

    private static void assertBoundaries(XTextDocument document, String name, CitationType type) throws Exception {
        String text = WriterReferenceMarks.getAnchor(document, name).orElseThrow().getString();
        if (type == CitationType.INVISIBLE_CIT) {
            assertEquals("\u200b\u200b", text);
        } else {
            assertTrue(text.startsWith("\u200b"), name);
            assertTrue(text.endsWith("\u200b"), name);
            assertFalse(text.replace("\u200b", "").isBlank(), name);
            assertFalse(text.contains("Old marker"), name);
        }
    }

    private void insert(CitationEngine engine, OOStyle style, XTextCursor cursor, boolean bibliography) {
        assertEquals(List.of(), success(engine.insert(List.of(database.getEntryByCitationKey("gamma").orElseThrow()), List.of(database), style,
                CitationType.AUTHORYEAR_PAR, "", cursor, bibliography)));
    }

    private static void appendLegacy(XTextDocument document, String name, String text, String pageInfo) throws Exception {
        appendMark(document, name, text);
        UnoUserDefinedProperty.setStringProperty(document, name, pageInfo);
    }

    private static void appendMark(XTextDocument document, String name, String text) throws Exception {
        XTextCursor cursor = separatedEnd(document);
        cursor.setString(text);
        WriterReferenceMarks.create(new NamedTextContentPlacement(document, name, cursor, true));
    }

    private static XTextCursor end(XTextDocument document) {
        XTextCursor cursor = document.getText().createTextCursor();
        cursor.gotoEnd(false);
        return cursor;
    }

    private static XTextCursor separatedEnd(XTextDocument document) {
        XTextCursor cursor = end(document);
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
        preferences.setCslBibliographyHeaderFormat("Heading 2");
        return preferences;
    }

    private static OOStyle style(String family) throws Exception {
        return "csl".equals(family)
               ? CSLStyleUtils.createCitationStyleFromFile(resource("zotero-numeric.csl").toString()).orElseThrow()
               : new JStyle(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, LayoutFormatterPreferences.getDefault(), new JournalAbbreviationRepository());
    }

    private OOStyle failingStyle(String family) throws Exception {
        if ("csl".equals(family)) {
            return new CitationStyle("invalid.csl", "invalid", "in-text", "Invalid", "", true, true, false, false, "not XML", false);
        }
        Path path = temporaryDirectory.resolve("invalid.jstyle");
        Files.writeString(path, """
                NAME
                Invalid style without a default layout
                """);
        JStyle invalid = new JStyle(path, LayoutFormatterPreferences.getDefault(), new JournalAbbreviationRepository());
        assertFalse(invalid.isValid());
        return invalid;
    }

    private static Path resource(String name) throws Exception {
        return Path.of(Optional.ofNullable(LegacyJStyleMigrationWriterTest.class.getResource(name)).orElseThrow().toURI());
    }

    private static JabRefMarkData decode(String name) {
        return assertDoesNotThrow(() -> JabRefMarkCodec.decode(name));
    }

    private static List<String> canonicalNames(XTextDocument document) throws Exception {
        return WriterReferenceMarks.getListOfNames(document).stream().filter(JabRefMarkCodec::isCanonicalMarkName).toList();
    }

    private static List<String> nativeNames(XTextDocument document) throws Exception {
        return WriterReferenceMarks.getListOfNames(document).stream().filter(ZoteroReferenceMark::isZoteroReferenceMarkName).toList();
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
        Map<String, Object> protections = new LinkedHashMap<>();
        for (String name : UnoTextSection.getNameAccess(document).getElementNames()) {
            XTextSection section = UnoTextSection.getByName(document, name).orElseThrow();
            sections.put(name, section.getAnchor().getString());
            protections.put(name, query(XPropertySet.class, section).getPropertyValue("IsProtected"));
        }
        XPropertySet values = query(XPropertySet.class, query(XDocumentPropertiesSupplier.class, document).getDocumentProperties().getUserDefinedProperties());
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String name : UnoUserDefinedProperty.getListOfNames(document)) {
            properties.put(name, values.getPropertyValue(name));
        }
        return new Snapshot(document.getText().getString(), marks, properties, sections, protections, formatting,
                UnoRedlines.getRecordChanges(document), document.hasControllersLocked());
    }

    private static <T> T success(OOResult<T, JabRefException> result) {
        if (result.isError()) {
            fail("Citation operation failed", result.getError());
        }
        return result.get();
    }

    @NullMarked
    private record Snapshot(String body, Map<String, String> marks, Map<String, Object> properties, Map<String, String> sections,
                            Map<String, Object> protections, Map<String, List<Object>> formatting, boolean recordingChanges, boolean controllersLocked) {
    }
}
