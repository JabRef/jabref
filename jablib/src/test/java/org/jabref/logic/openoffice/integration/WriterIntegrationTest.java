package org.jabref.logic.openoffice.integration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.JabRefException;
import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.engine.CitationEngine;
import org.jabref.logic.openoffice.engine.CitedEntryExporter;
import org.jabref.logic.openoffice.engine.JabRefMarkData;
import org.jabref.logic.openoffice.engine.MarkCodec;
import org.jabref.logic.openoffice.engine.MissingCitationLabelException;
import org.jabref.logic.openoffice.engine.MissingWriterStyleException;
import org.jabref.logic.openoffice.engine.PandocRenderException;
import org.jabref.logic.openoffice.engine.RenderException;
import org.jabref.logic.openoffice.engine.StyleCapabilities;
import org.jabref.logic.openoffice.engine.UpdatePipeline;
import org.jabref.logic.openoffice.engine.storage.DocumentStateStore;
import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.frontend.DocumentCitations;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.UnoRedlines;
import org.jabref.logic.openoffice.uno.UnoTextSection;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.document.XRedlinesSupplier;
import com.sun.star.text.XFootnotesSupplier;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextTable;
import com.sun.star.text.XTextTablesSupplier;
import com.sun.star.uno.UnoRuntime;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.logic.openoffice.integration.HeadlessLibreOffice.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@NullMarked
@Tag("LibreOfficeTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("Localization.lang")
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class WriterIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriterIntegrationTest.class);
    private static final String ALPHA = "JABREF_alpha NORMAL goldenAlpha";
    private static final String BETA = "JABREF_beta NORMAL goldenBeta";
    private static final String DELTA = "JABREF_delta EMPTY goldenEmpty";
    private static final String GAMMA = "JABREF_gamma NORMAL goldenGamma";
    private static final String BOUNDARY = "\u200b";
    private static final Map<String, String> CANONICAL_MARKS = Map.of(
            ALPHA, bounded("[1]"), BETA, bounded("[2]"), GAMMA, bounded("[3]"), DELTA, bounded(""));

    @TempDir
    Path temporaryDirectory;

    private int artifactIndex;
    private final HeadlessLibreOffice office = new HeadlessLibreOffice(
            Path.of(System.getProperty("jabref.writer.artifacts", "build/writer-integration")).resolve("engine-acceptance"));

    @BeforeAll
    void startWriter(@TempDir Path profileDirectory) throws Exception {
        office.start(profileDirectory.resolve("profile"));
    }

    @AfterEach
    void preserveDocuments(TestInfo testInfo) throws com.sun.star.io.IOException {
        office.saveAndCloseDocuments(testInfo.getTestMethod().orElseThrow().getName() + "-" + artifactIndex++);
    }

    @AfterAll
    void stopWriter() throws Exception {
        office.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"jstyle", "csl", "bst", "csl-empty"})
    void legacyGoldenMarksSurviveWriterSaveAndReopenWithoutBeingMigrated(String family) throws Exception {
        XTextDocument document = office.load(resource(family + ".fodt"));
        Map<String, String> expected = legacyMarks(family);
        assertEquals(expected, marks(document));
        if ("csl-empty".equals(family)) {
            assertEquals("Before after.", document.getText().getString());
        } else {
            assertTextContainers(document, "csl".equals(family) ? "2" : "[2]", "csl".equals(family) ? "3" : "[3]");
        }
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve(family + ".odt"));
        assertEquals(expected, marks(reopened));
        if ("csl-empty".equals(family)) {
            assertEquals("Before after.", reopened.getText().getString());
        } else {
            assertTextContainers(reopened, "csl".equals(family) ? "2" : "[2]", "csl".equals(family) ? "3" : "[3]");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"jstyle", "csl", "bst"})
    void canonicalGoldenRefreshNumbersFootnotesTablesAndEmptyCitationsAcrossSaveReopen(String family) throws Exception {
        XTextDocument document = office.load(resource("canonical.fodt"));
        assertEquals(CANONICAL_MARKS, marks(document));
        OOStyle style = style(family);
        try (CitationEngine engine = engine(document)) {
            assertEquals(List.of(), success(engine.refresh(List.of(database()), style)));
            assertGoldenNumbers(document, family);
            assertBibliographyOrder(document, List.of("Alpha title", "Beta title", "Gamma title", "Delta title"));
            assertEquals(List.of("alpha", "beta", "gamma", "delta"), exportedKeys(success(engine.exportCited(List.of(database())))));
        }
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve(family + "-canonical.odt"));
        assertGoldenNumbers(reopened, family);
        assertBibliographyOrder(reopened, List.of("Alpha title", "Beta title", "Gamma title", "Delta title"));
    }

    // [utest->req~openoffice.cross-family-citations~1]
    @Test
    void jstyleCslBstJstyleRoundTripPreservesEveryMarkTypeAndPageInfo() throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("Citations: .\nBibliography below.");
        XTextCursor cursor = document.getText().createTextCursor();
        cursor.goRight((short) 11, false);
        BibDatabase database = database();
        JStyle jstyle = authorYearStyle();
        List<String> keys = List.of("alpha", "beta", "gamma", "delta", "epsilon", "zeta");
        List<CitationType> types = List.of(CitationType.values());
        try (CitationEngine engine = engine(document)) {
            for (int index = 0; index < types.size(); index++) {
                assertEquals(List.of(), success(engine.insert(List.of(entry(database, keys.get(index))), List.of(database), jstyle,
                        types.get(index), "p. " + (index + 1), cursor, false)));
                cursor.setString(" | ");
                cursor.collapseToEnd();
            }
            assertEquals(List.of(), success(engine.refresh(List.of(database), jstyle)));
        }
        Map<String, JabRefMarkData> identities = identities(document);
        assertEquals(types, groups(document).stream().map(group -> group.citationType).toList());
        assertEquals(keys, groups(document).stream().flatMap(group -> group.citationsInStorageOrder.stream())
                                           .map(citation -> citation.getCitationKey()).toList());
        Map<String, Object> pageInfos = pageProperties(document);
        assertEquals(6, pageInfos.size());
        for (int index = 0; index < keys.size(); index++) {
            JabRefMarkData identity = identityForKey(identities, keys.get(index));
            assertEquals("p. " + (index + 1), pageInfos.get(MarkStore.pageInfoProperty(identity.uniqueId(), 0)));
        }
        Map<String, String> originalText = textsByKey(document);
        assertEquals(Map.of("alpha", "[Alpha 2020; p. 1]", "beta", "Beta [2021; p. 2]", "gamma", "",
                "delta", "Delta 2023; p. 4", "epsilon", "Epsilon", "zeta", "2025; p. 6"), originalText);

        List<OOStyle> styles = List.of(style("csl"), style("bst"), jstyle);
        List<DocumentStateStore.Family> families = List.of(DocumentStateStore.Family.CSL, DocumentStateStore.Family.BST, DocumentStateStore.Family.JSTYLE);
        for (int index = 0; index < styles.size(); index++) {
            try (CitationEngine engine = engine(document)) {
                assertEquals(List.of(), success(engine.refresh(List.of(database), styles.get(index))));
                assertEquals(identities, identities(document));
                assertEquals(pageInfos, pageProperties(document));
                assertEquals("", textsByKey(document).get("gamma"));
                assertEquals(keys, exportedKeys(success(engine.exportCited(List.of(database)))));
                assertEquals(families.get(index), new DocumentStateStore().read(document).orElseThrow().family());
                if (index < 2) {
                    assertFalse(textsByKey(document).values().stream().anyMatch(text -> text.contains("p. ")));
                }
            }
            document = office.saveAndReload(document, temporaryDirectory.resolve("roundtrip-" + index + ".odt"));
            assertEquals(identities, identities(document));
            assertEquals(pageInfos, pageProperties(document));
        }
        assertEquals(originalText, textsByKey(document));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jstyle", "csl", "bst"})
    void insertionRenumbersExistingMarkersAndBibliographyInDocumentOrder(String family) throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("Citations: .\nBibliography below.");
        XTextCursor position = document.getText().createTextCursor();
        position.goRight((short) 11, false);
        BibDatabase database = database();
        OOStyle style = style(family);
        try (CitationEngine engine = engine(document)) {
            assertEquals(List.of(), success(engine.insert(List.of(entry(database, "beta")), List.of(database), style,
                    CitationType.AUTHORYEAR_PAR, "", position, true)));
            assertEquals(List.of(), success(engine.insert(List.of(entry(database, "alpha")), List.of(database), style,
                    CitationType.AUTHORYEAR_PAR, "", position, true)));
            XTextCursor beginning = document.getText().createTextCursor();
            assertEquals(List.of(), success(engine.insert(List.of(entry(database, "gamma")), List.of(database), style,
                    CitationType.AUTHORYEAR_PAR, "", beginning, true)));
            assertEquals(Map.of("gamma", number(family, 1), "beta", number(family, 2), "alpha", number(family, 3)), textsByKey(document));
            assertBibliographyOrder(document, List.of("Gamma title", "Beta title", "Alpha title"));
            assertEquals(List.of("gamma", "beta", "alpha"), exportedKeys(success(engine.exportCited(List.of(database)))));
        }
    }

    @Test
    void repeatedEngineSuperscriptRefreshKeepsHeightAndNormalSurroundingTextAfterReconnect() throws Exception {
        XTextDocument document = office.load(resource("canonical.fodt"));
        Map<String, JabRefMarkData> identities = identities(document);
        Map<String, Object> initialHeights = new LinkedHashMap<>();
        for (String mark : List.of(ALPHA, BETA, GAMMA)) {
            initialHeights.put(mark, query(XPropertySet.class, innerCursor(document, mark)).getPropertyValue("CharHeight"));
        }
        LOGGER.info("Initial citation CharHeight before rendering: {}", initialHeights);
        for (int refresh = 0; refresh < 5; refresh++) {
            // Reconnecting deliberately defeats the writer's in-memory unchanged-text cache.
            try (CitationEngine engine = engine(document)) {
                assertEquals(List.of(), success(engine.refresh(List.of(database()), style("csl"))));
            }
            assertGoldenNumbers(document, "csl");
            for (String mark : List.of(ALPHA, BETA, GAMMA)) {
                XPropertySet properties = query(XPropertySet.class, innerCursor(document, mark));
                assertEquals((byte) 58, properties.getPropertyValue("CharEscapementHeight"), mark);
                assertEquals((short) 33, properties.getPropertyValue("CharEscapement"), mark);
                assertEquals(initialHeights.get(mark), properties.getPropertyValue("CharHeight"), mark);
            }
            XTextCursor surrounding = document.getText().createTextCursor();
            surrounding.goRight((short) 6, true);
            assertEquals("Before", surrounding.getString());
            assertEquals((short) 0, query(XPropertySet.class, surrounding).getPropertyValue("CharEscapement"));
            assertEquals(identities, identities(document));
            document = office.saveAndReload(document, temporaryDirectory.resolve("superscript-" + refresh + ".odt"));
        }
        for (String mark : List.of(ALPHA, BETA, GAMMA)) {
            XPropertySet properties = query(XPropertySet.class, innerCursor(document, mark));
            assertEquals((byte) 58, properties.getPropertyValue("CharEscapementHeight"), mark);
            assertEquals((short) 33, properties.getPropertyValue("CharEscapement"), mark);
            assertEquals(initialHeights.get(mark), properties.getPropertyValue("CharHeight"), mark);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"refresh", "insert"})
    void missingPandocLeavesTextMarksFormattingPropertiesAndBibliographyUnchanged(String operation) throws Exception {
        XTextDocument document = office.load(resource("canonical.fodt"));
        BibDatabase database = database();
        try (CitationEngine engine = engine(document)) {
            assertEquals(List.of(), success(engine.refresh(List.of(database), style("csl"))));
        }
        DocumentSnapshot before = snapshot(document);
        OpenOfficePreferences preferences = preferences();
        String pandocPath = temporaryDirectory.resolve("missing-pandoc-executable").toString();
        preferences.setPandocPath(pandocPath);
        try (CitationEngine engine = new CitationEngine(document, office.context(), preferences, new BibEntryTypesManager())) {
            OOResult<List<String>, JabRefException> result = "refresh".equals(operation)
                                                             ? engine.refresh(List.of(database), style("bst"))
                                                             : engine.insert(List.of(entry(database, "epsilon")), List.of(database), style("bst"),
                    CitationType.AUTHORYEAR_PAR, "p. 99", document.getText().createTextCursor(), true);
            assertTrue(result.isError(), "A nonexistent Pandoc executable must fail the operation");
            assertInstanceOf(PandocRenderException.class, result.getError().getCause());
            assertEquals(Localization.lang("Could not run Pandoc. Install Pandoc and set its path in Preferences > OpenOffice/LibreOffice.")
                    + "\n" + Localization.lang("Pandoc path: %0", pandocPath), result.getError().getLocalizedMessage());
        }
        assertEquals(before, snapshot(document));
    }

    @ParameterizedTest
    @CsvSource({"refresh, paragraph", "insert, paragraph", "refresh, header", "insert, header", "refresh, character", "insert, character"})
    void missingWriterStyleRetainsLocalizedCorrectiveActionWithoutDocumentMutation(String operation, String family) throws Exception {
        XTextDocument document = office.load(resource("canonical.fodt"));
        String missingStyle = "Required research " + family + " style";
        Path path = temporaryDirectory.resolve("missing-style.jstyle");
        Files.writeString(path, """
                NAME
                Missing Writer style regression
                PROPERTIES
                ReferenceParagraphFormat="%s"
                ReferenceHeaderParagraphFormat="%s"
                CITATION
                FormatCitations="true"
                CitationCharacterFormat="%s"
                LAYOUT
                default=\\title
                """.formatted("paragraph".equals(family) ? missingStyle : "Standard",
                "header".equals(family) ? missingStyle : "Heading 2", missingStyle));
        JStyle style = new JStyle(path, LayoutFormatterPreferences.getDefault(), new JournalAbbreviationRepository());
        BibDatabase database = database();
        DocumentSnapshot before = snapshot(document);

        try (CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> result = "refresh".equals(operation)
                                                             ? engine.refresh(List.of(database), style)
                                                             : engine.insert(List.of(entry(database, "epsilon")), List.of(database), style,
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), true);

            assertTrue(result.isError());
            assertInstanceOf(MissingWriterStyleException.class, result.getError().getCause());
            String expected = "character".equals(family)
                              ? Localization.lang("You must create the following character style in LibreOffice: %0", missingStyle)
                              : Localization.lang("You must create the following paragraph style in LibreOffice: %0", missingStyle);
            assertEquals(expected, result.getError().getLocalizedMessage());
        }
        assertEquals(before, snapshot(document));
    }

    @ParameterizedTest
    @ValueSource(strings = {"refresh", "insert"})
    void missingBstLabelRetainsLocalizedFormatGuidanceWithoutDocumentMutation(String operation) throws Exception {
        XTextDocument document = office.load(resource("canonical.fodt"));
        BibDatabase database = database();
        DocumentSnapshot before = snapshot(document);
        OpenOfficePreferences preferences = preferences();
        preferences.setBstCitationFormat(BstCitationFormat.STYLE_DEFINED);
        try (CitationEngine engine = new CitationEngine(document, office.context(), preferences, new BibEntryTypesManager())) {
            OOResult<List<String>, JabRefException> result = "refresh".equals(operation)
                                                             ? engine.refresh(List.of(database), style("bst"))
                                                             : engine.insert(List.of(entry(database, "epsilon")), List.of(database), style("bst"),
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), true);

            assertTrue(result.isError());
            assertInstanceOf(MissingCitationLabelException.class, result.getError().getCause());
            assertEquals(Localization.lang("The selected BST style does not define any citation format for style-defined citations.\nPlease choose either numeric or author-year."),
                    result.getError().getLocalizedMessage());
        }
        assertEquals(before, snapshot(document));
    }

    @Test
    void unexpectedRenderDiagnosticIsNotShownAsUserGuidance() throws Exception {
        XTextDocument document = office.load(resource("canonical.fodt"));
        DocumentSnapshot before = snapshot(document);
        try (MockedConstruction<UpdatePipeline> pipelines = mockConstruction(UpdatePipeline.class, (pipeline, _) ->
                when(pipeline.render(any(), anyList(), anyList(), anyBoolean())).thenThrow(new RenderException("Internal diagnostic, not a user message")));
             CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> result = engine.refresh(List.of(database()), style("csl"));

            assertTrue(result.isError());
            assertInstanceOf(RenderException.class, result.getError().getCause());
            assertEquals(Localization.lang("Could not update bibliography"), result.getError().getLocalizedMessage());
            assertEquals(1, pipelines.constructed().size());
        }
        assertEquals(before, snapshot(document));
    }

    // [utest->req~openoffice.legacy-marks-unmanaged~1]
    @ParameterizedTest
    @ValueSource(strings = {"jstyle", "csl", "bst", "csl-empty"})
    void engineDoesNotReadExportRewriteOrMigrateLegacyMarks(String family) throws Exception {
        XTextDocument document = office.load(resource(family + ".fodt"));
        Map<String, String> legacy = marks(document);
        Map<String, Object> legacyProperties = properties(document);
        assertEquals(0, new DocumentCitations(document).citationGroups.numberOfCitationGroups());
        try (CitationEngine engine = engine(document)) {
            CitedEntryExporter.ExportResult exported = success(engine.exportCited(List.of(database())));
            assertEquals(List.of(), exportedKeys(exported));
            assertEquals(List.of(), exported.unresolvedKeys());
            assertEquals(List.of(), success(engine.refresh(List.of(database()), style("csl"))));
        }
        assertEquals(legacy, marks(document));
        for (Map.Entry<String, Object> property : legacyProperties.entrySet()) {
            assertEquals(property.getValue(), properties(document).get(property.getKey()));
        }
        assertEquals(0, new DocumentCitations(document).citationGroups.numberOfCitationGroups());
    }

    @Test
    void engineMergeSeparateKeepsEachPageInfoAndKeyOrderThroughRefreshAndReopen() throws Exception {
        XTextDocument document = office.newDocument();
        BibDatabase database = database();
        JStyle style = authorYearStyle();
        XTextCursor cursor = document.getText().createTextCursor();
        try (CitationEngine engine = engine(document)) {
            assertEquals(List.of(), success(engine.insert(List.of(entry(database, "beta")), List.of(database), style,
                    CitationType.AUTHORYEAR_PAR, "p. 5", cursor, false)));
            assertEquals(List.of(), success(engine.insert(List.of(entry(database, "alpha")), List.of(database), style,
                    CitationType.AUTHORYEAR_PAR, "p. 7", cursor, false)));
            assertEquals(List.of(), success(engine.merge(List.of(database), style)));
            List<CitationGroup> merged = groups(document);
            assertEquals(1, merged.size());
            assertEquals(List.of("beta", "alpha"), merged.getFirst().citationsInStorageOrder.stream().map(citation -> citation.getCitationKey()).toList());
            assertEquals(List.of(Optional.of("p. 5"), Optional.of("p. 7")), pageInfos(merged));
            assertEquals(List.of(), success(engine.separate(List.of(database), style)));
            assertEquals(List.of("beta", "alpha"), groups(document).stream().flatMap(group -> group.citationsInStorageOrder.stream()).map(citation -> citation.getCitationKey()).toList());
            assertEquals(List.of(Optional.of("p. 5"), Optional.of("p. 7")), pageInfos(groups(document)));
            assertEquals(List.of(), success(engine.refresh(List.of(database), style)));
        }
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("merged-separated.odt"));
        assertEquals(List.of(Optional.of("p. 5"), Optional.of("p. 7")), pageInfos(groups(reopened)));
        assertEquals(2, pageProperties(reopened).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"csl", "bst"})
    void capabilitiesRejectUnsupportedGroupEditsWithoutChangingTheDocument(String family) throws Exception {
        XTextDocument document = office.load(resource("canonical.fodt"));
        DocumentSnapshot before = snapshot(document);
        try (CitationEngine engine = engine(document)) {
            StyleCapabilities capabilities = engine.capabilities(style(family));
            assertFalse(capabilities.supportsGroupEditing());
            assertFalse(capabilities.supportsPageInfo());
            assertTrue(capabilities.hasBibliography());
            assertTrue(engine.capabilities(authorYearStyle()).supportsGroupEditing());
            assertTrue(engine.capabilities(authorYearStyle()).supportsPageInfo());
            assertTrue(engine.merge(List.of(database()), style(family)).isError());
            assertEquals(before, snapshot(document));
            assertTrue(engine.separate(List.of(database()), style(family)).isError());
            assertEquals(before, snapshot(document));
        }
    }

    @Test
    void engineRefreshPreservesUserRedlinesAndRestoresChangeRecordingAndControllerLocks() throws Exception {
        XTextDocument document = office.load(resource("canonical.fodt"));
        UnoRedlines.setRecordChanges(document, true);
        XTextCursor cursor = document.getText().createTextCursor();
        cursor.gotoEnd(false);
        cursor.setString("User insertion");
        int count = redlineCount(document);
        assertTrue(count > 0);
        try (CitationEngine engine = engine(document)) {
            assertEquals(List.of(), success(engine.refresh(List.of(database()), style("csl"))));
        }
        assertGoldenNumbers(document, "csl");
        assertTrue(UnoRedlines.getRecordChanges(document));
        assertFalse(document.hasControllersLocked());
        assertEquals(count, redlineCount(document));
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("engine-tracked.odt"));
        assertTrue(UnoRedlines.getRecordChanges(reopened));
        assertEquals(count, redlineCount(reopened));
    }

    @ParameterizedTest
    @ValueSource(strings = {"insert", "refresh"})
    void pendingDeletionWithRecordingDisabledRejectsCanonicalEditsWithoutMutation(String operation) throws Exception {
        XTextDocument document = office.load(resource("canonical.fodt"));
        UnoRedlines.setRecordChanges(document, true);
        XTextRange anchor = markRange(document, ALPHA);
        XTextCursor deletion = anchor.getText().createTextCursorByRange(anchor);
        deletion.collapseToStart();
        deletion.goRight((short) 2, false);
        deletion.goRight((short) 1, true);
        deletion.setString("");
        assertTrue(marks(document).containsKey(ALPHA));
        UnoRedlines.setRecordChanges(document, false);
        List<XTextRange> pending = UnoRedlines.getDeletedRanges(document);
        assertEquals(1, pending.size());
        assertTrue(UnoRuntime.areSame(document.getText(), pending.getFirst().getText()),
                "Deletion checks must use document anchors, not the detached deleted-text container");
        DocumentSnapshot before = snapshot(document);
        BibDatabase database = database();

        try (CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> result = "refresh".equals(operation)
                                                             ? engine.refresh(List.of(database), style("csl"))
                                                             : engine.insert(List.of(entry(database, "epsilon")), List.of(database), style("csl"),
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), true);
            assertTrue(result.isError(), "Pending citation deletions must block edits even when recording is disabled");
            assertEquals(Localization.lang("Recording and/or Recorded changes"), result.getError().getMessage());
        }

        assertEquals(before, snapshot(document));
        assertFalse(UnoRedlines.getRecordChanges(document));
        assertFalse(document.hasControllersLocked());
    }

    @Test
    void suspendedTrackChangesRestoresRecordingAfterAnExceptionalAction() throws Exception {
        XTextDocument document = office.load(resource("canonical.fodt"));
        UnoRedlines.setRecordChanges(document, true);
        assertThrows(com.sun.star.lang.WrappedTargetException.class,
                () -> UnoRedlines.withRecordChangesSuspended(document, () -> {
                    assertFalse(UnoRedlines.getRecordChanges(document));
                    throw new com.sun.star.lang.WrappedTargetException();
                }));
        assertTrue(UnoRedlines.getRecordChanges(document));
        assertEquals(CANONICAL_MARKS, marks(document));
        assertEquals(0, redlineCount(document));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jstyle", "csl", "bst"})
    void unavailableLibraryUsesPlaceholdersAndRestoringItRecoversTheSameCanonicalMarks(String family) throws Exception {
        XTextDocument document = office.load(resource("canonical.fodt"));
        Map<String, JabRefMarkData> identities = identities(document);
        try (CitationEngine engine = engine(document)) {
            assertEquals(List.of("alpha", "beta", "gamma", "delta"), success(engine.refresh(List.of(), style(family))));
            for (String key : List.of("alpha", "beta", "gamma")) {
                String expected = "Unresolved(" + key + ")";
                assertEquals("bst".equals(family) ? "[" + expected + "]" : expected, textsByKey(document).get(key));
            }
            assertEquals("", textsByKey(document).get("delta"));
            CitedEntryExporter.ExportResult exported = success(engine.exportCited(List.of()));
            assertEquals(List.of("alpha", "beta", "gamma", "delta"), exported.unresolvedKeys());
            assertEquals(List.of(), exportedKeys(exported));
            assertEquals(List.of(), success(engine.refresh(List.of(database()), style(family))));
        }
        assertEquals(identities, identities(document));
        assertGoldenNumbers(document, family);
    }

    @Test
    void insertionReportsExistingCitationKeysMissingFromTheSelectedLibrary() throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("Existing citation: .\nNew citation: .\nBibliography below.\n");
        BibEntry oldEntry = new BibEntry().withCitationKey("old").withField(StandardField.TITLE, "Original library title");
        BibEntry newEntry = new BibEntry().withCitationKey("new").withField(StandardField.TITLE, "Second library title");
        OOStyle selectedStyle = CSLStyleUtils.createCitationStyleFromFile(resource("zotero-numeric.csl").toString()).orElseThrow();
        try (CitationEngine engine = engine(document)) {
            XTextCursor firstPosition = document.getText().createTextCursor();
            firstPosition.goRight((short) "Existing citation: ".length(), false);
            assertEquals(List.of(), success(engine.insert(List.of(oldEntry), List.of(new BibDatabase(List.of(oldEntry))),
                    selectedStyle, CitationType.AUTHORYEAR_PAR, "", firstPosition, true)));
            XTextCursor nextPosition = document.getText().createTextCursor();
            nextPosition.goRight((short) (document.getText().getString().indexOf("New citation: ") + "New citation: ".length()), false);

            assertEquals(List.of("old"), success(engine.insert(List.of(newEntry), List.of(new BibDatabase(List.of(newEntry))),
                    selectedStyle, CitationType.AUTHORYEAR_PAR, "", nextPosition, true)));

            assertEquals(Map.of("old", "Unresolved(old)", "new", "[1]"), textsByKey(document));
            assertEquals(List.of("References", "1. Second library title"), UnoTextSection.getAnchor(document, "JR_bib").orElseThrow().getString().strip().lines().toList());
        }
    }

    private CitationEngine engine(XTextDocument document) {
        return new CitationEngine(document, office.context(), preferences(), new BibEntryTypesManager());
    }

    private static OpenOfficePreferences preferences() {
        OpenOfficePreferences preferences = OpenOfficePreferences.getDefault();
        preferences.setCslBibliographyBodyFormat("Standard");
        preferences.setCslBibliographyHeaderFormat("Heading 2");
        preferences.setPandocPath(Optional.ofNullable(System.getenv("PANDOC_EXECUTABLE")).filter(value -> !value.isBlank()).orElse("pandoc"));
        return preferences;
    }

    private static OOStyle style(String family) throws IOException, URISyntaxException {
        return switch (family) {
            case "csl" ->
                    CSLStyleUtils.createCitationStyleFromFile(resource("superscript.csl").toString()).orElseThrow();
            case "bst" ->
                    BstStyle.loadExternal(resource("golden.bst"));
            default ->
                    new JStyle(JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, LayoutFormatterPreferences.getDefault(), new JournalAbbreviationRepository());
        };
    }

    private static JStyle authorYearStyle() throws IOException {
        return new JStyle(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, LayoutFormatterPreferences.getDefault(), new JournalAbbreviationRepository());
    }

    private static BibDatabase database() {
        List<BibEntry> entries = new ArrayList<>();
        List<String> keys = List.of("alpha", "beta", "gamma", "delta", "epsilon", "zeta");
        for (int index = 0; index < keys.size(); index++) {
            String key = keys.get(index);
            String name = Character.toUpperCase(key.charAt(0)) + key.substring(1);
            entries.add(new BibEntry().withCitationKey(key).withField(StandardField.TITLE, name + " title")
                                      .withField(StandardField.AUTHOR, name + ", Alice").withField(StandardField.YEAR, Integer.toString(2020 + index)));
        }
        return new BibDatabase(entries);
    }

    private static BibEntry entry(BibDatabase database, String key) {
        return database.getEntryByCitationKey(key).orElseThrow();
    }

    private static <T> T success(OOResult<T, JabRefException> result) {
        if (result.isError()) {
            fail("CitationEngine operation failed", result.getError());
        }
        return result.get();
    }

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(Optional.ofNullable(WriterIntegrationTest.class.getResource(name)).orElseThrow().toURI());
    }

    private static Map<String, String> marks(XTextDocument document) throws com.sun.star.uno.Exception {
        XNameAccess access = query(XReferenceMarksSupplier.class, document).getReferenceMarks();
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : Arrays.stream(access.getElementNames()).sorted().toList()) {
            result.put(name, query(XTextContent.class, access.getByName(name)).getAnchor().getString());
        }
        return result;
    }

    private static Map<String, JabRefMarkData> identities(XTextDocument document) throws Exception {
        Map<String, JabRefMarkData> result = new LinkedHashMap<>();
        for (String name : marks(document).keySet()) {
            assertTrue(MarkCodec.isCanonicalMarkName(name), name);
            result.put(name, MarkCodec.decode(name));
        }
        return result;
    }

    private static JabRefMarkData identityForKey(Map<String, JabRefMarkData> identities, String key) {
        return identities.values().stream().filter(identity -> identity.citationKeys().equals(List.of(key))).findFirst().orElseThrow();
    }

    private static Map<String, String> textsByKey(XTextDocument document) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> mark : marks(document).entrySet()) {
            JabRefMarkData data = MarkCodec.decode(mark.getKey());
            assertEquals(1, data.citationKeys().size());
            String raw = mark.getValue();
            assertTrue(raw.length() >= 2, mark.getKey());
            String visible = raw.substring(1, raw.length() - 1);
            assertEquals(bounded(visible), raw, "Canonical boundaries must remain intact: " + mark.getKey());
            result.put(data.citationKeys().getFirst(), visible);
        }
        return result;
    }

    private static List<CitationGroup> groups(XTextDocument document) throws Exception {
        DocumentCitations documentCitations = new DocumentCitations(document);
        documentCitations.imposeGlobalOrder(document);
        return documentCitations.citationGroups.getCitationGroupsInGlobalOrder();
    }

    private static List<Optional<String>> pageInfos(List<CitationGroup> groups) {
        return groups.stream().flatMap(group -> group.citationsInStorageOrder.stream())
                     .map(citation -> citation.getPageInfo().map(text -> text.toString())).toList();
    }

    private static Map<String, Object> properties(XTextDocument document) throws com.sun.star.uno.Exception {
        XPropertySet values = query(XPropertySet.class, query(XDocumentPropertiesSupplier.class, document).getDocumentProperties().getUserDefinedProperties());
        Map<String, Object> result = new LinkedHashMap<>();
        for (String name : UnoUserDefinedProperty.getListOfNames(document).stream().sorted().toList()) {
            result.put(name, values.getPropertyValue(name));
        }
        return result;
    }

    private static Map<String, Object> pageProperties(XTextDocument document) throws com.sun.star.uno.Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        properties(document).forEach((name, value) -> {
            if (name.startsWith(MarkStore.PAGE_INFO_PREFIX)) {
                result.put(name, value);
            }
        });
        return result;
    }

    private static XTextRange markRange(XTextDocument document, String name) throws com.sun.star.uno.Exception {
        return query(XTextContent.class, query(XReferenceMarksSupplier.class, document).getReferenceMarks().getByName(name)).getAnchor();
    }

    private static XTextCursor innerCursor(XTextDocument document, String name) throws com.sun.star.uno.Exception {
        XTextRange range = markRange(document, name);
        XTextCursor end = range.getText().createTextCursorByRange(range.getEnd());
        end.goLeft((short) 1, false);
        XTextCursor inner = range.getText().createTextCursorByRange(range.getStart());
        inner.goRight((short) 1, false);
        inner.gotoRange(end, true);
        return inner;
    }

    private static String bounded(String visible) {
        return BOUNDARY + visible + BOUNDARY;
    }

    private static String number(String family, int number) {
        return "csl".equals(family) ? Integer.toString(number) : "[" + number + "]";
    }

    private static void assertGoldenNumbers(XTextDocument document, String family) throws Exception {
        assertEquals(Set.of(ALPHA, BETA, GAMMA, DELTA), marks(document).keySet());
        assertEquals(Map.of("alpha", number(family, 1), "beta", number(family, 2), "gamma", number(family, 3), "delta", ""), textsByKey(document));
        assertTextContainers(document, bounded(number(family, 2)), bounded(number(family, 3)));
    }

    private static void assertTextContainers(XTextDocument document, String footnoteMarker, String tableMarker) throws com.sun.star.uno.Exception {
        XIndexAccess footnotes = query(XFootnotesSupplier.class, document).getFootnotes();
        assertEquals(1, footnotes.getCount());
        assertEquals("Footnote " + footnoteMarker + ".", query(XText.class, footnotes.getByIndex(0)).getString());
        XNameAccess tables = query(XTextTablesSupplier.class, document).getTextTables();
        assertEquals(List.of("Citations"), List.of(tables.getElementNames()));
        XTextTable table = query(XTextTable.class, tables.getByName("Citations"));
        assertEquals("Table " + tableMarker + ".", query(XText.class, table.getCellByName("A1")).getString());
    }

    private static void assertBibliographyOrder(XTextDocument document, List<String> titles) throws Exception {
        String bibliography = UnoTextSection.getAnchor(document, "JR_bib").orElseThrow().getString();
        int previous = -1;
        for (String title : titles) {
            int position = bibliography.indexOf(title);
            assertTrue(position > previous, () -> "Missing or misordered bibliography title " + title + " in " + bibliography);
            assertEquals(position, bibliography.lastIndexOf(title), "Duplicate bibliography entry: " + title);
            previous = position;
        }
    }

    private static List<String> exportedKeys(CitedEntryExporter.ExportResult result) {
        return result.database().getEntries().stream().map(entry -> entry.getCitationKey().orElseThrow()).toList();
    }

    private static int redlineCount(XTextDocument document) throws com.sun.star.uno.Exception {
        XEnumeration redlines = query(XRedlinesSupplier.class, document).getRedlines().createEnumeration();
        int count = 0;
        while (redlines.hasMoreElements()) {
            redlines.nextElement();
            count++;
        }
        return count;
    }

    private static DocumentSnapshot snapshot(XTextDocument document) throws Exception {
        List<String> notes = new ArrayList<>();
        XIndexAccess footnotes = query(XFootnotesSupplier.class, document).getFootnotes();
        for (int index = 0; index < footnotes.getCount(); index++) {
            notes.add(query(XText.class, footnotes.getByIndex(index)).getString());
        }
        Map<String, String> cells = new LinkedHashMap<>();
        XNameAccess tables = query(XTextTablesSupplier.class, document).getTextTables();
        for (String tableName : tables.getElementNames()) {
            XTextTable table = query(XTextTable.class, tables.getByName(tableName));
            for (String cell : table.getCellNames()) {
                cells.put(tableName + ":" + cell, query(XText.class, table.getCellByName(cell)).getString());
            }
        }
        Map<String, List<Object>> formatting = new LinkedHashMap<>();
        for (String mark : marks(document).keySet()) {
            XPropertySet values = query(XPropertySet.class, innerCursor(document, mark));
            formatting.put(mark, List.of(values.getPropertyValue("CharHeight"), values.getPropertyValue("CharEscapement"), values.getPropertyValue("CharEscapementHeight")));
        }
        return new DocumentSnapshot(document.getText().getString(), marks(document), properties(document), notes, cells, formatting,
                redlineCount(document), UnoRedlines.getRecordChanges(document), document.hasControllersLocked());
    }

    private static Map<String, String> legacyMarks(String family) {
        return switch (family) {
            case "jstyle" ->
                    Map.of("JR_cite_1_alpha", "[1]", "JR_cite_1_beta", "[2]", "JR_cite_1_gamma", "[3]", "JR_cite_3_delta", "");
            case "csl-empty" ->
                    Map.of("JABREF_delta CID_1 goldenEmpty EMPTY", "");
            case "csl" ->
                    Map.of("JABREF_alpha CID_1 goldenAlpha NORMAL", "1", "JABREF_beta CID_2 goldenBeta NORMAL", "2", "JABREF_gamma CID_3 goldenGamma NORMAL", "3");
            default ->
                    Map.of("JABREF_alpha CID_1 goldenAlpha NORMAL", "[1]", "JABREF_beta CID_2 goldenBeta NORMAL", "[2]", "JABREF_gamma CID_3 goldenGamma NORMAL", "[3]");
        };
    }

    @NullMarked
    private record DocumentSnapshot(String body, Map<String, String> marks, Map<String, Object> properties,
                                    List<String> footnotes, Map<String, String> cells, Map<String, List<Object>> formatting,
                                    int redlines, boolean recording, boolean controllersLocked) {
    }
}
