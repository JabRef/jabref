package org.jabref.logic.openoffice.engine.storage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.JabRefException;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.backend.ReferenceMarkTextWriter;
import org.jabref.logic.openoffice.backend.WriterReferenceMark;
import org.jabref.logic.openoffice.backend.WriterReferenceMark.BoundaryPolicy;
import org.jabref.logic.openoffice.backend.WriterReferenceMarks;
import org.jabref.logic.openoffice.engine.CitationEngine;
import org.jabref.logic.openoffice.engine.CitationGroupEditor;
import org.jabref.logic.openoffice.engine.CitationRenderer;
import org.jabref.logic.openoffice.engine.DocumentSession;
import org.jabref.logic.openoffice.engine.JStyleRenderer;
import org.jabref.logic.openoffice.engine.MarkCodec;
import org.jabref.logic.openoffice.engine.RenderException;
import org.jabref.logic.openoffice.engine.RenderOptions;
import org.jabref.logic.openoffice.engine.RenderRequest;
import org.jabref.logic.openoffice.engine.RenderResult;
import org.jabref.logic.openoffice.engine.RendererFactory;
import org.jabref.logic.openoffice.engine.StyleCapabilities;
import org.jabref.logic.openoffice.frontend.DocumentCitations;
import org.jabref.logic.openoffice.frontend.DocumentOrder;
import org.jabref.logic.openoffice.frontend.RangeForOverlapCheck;
import org.jabref.logic.openoffice.rangesort.FunctionalTextViewCursor;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.uno.DocumentAnnotation;
import org.jabref.logic.openoffice.uno.UnoRedlines;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.awt.FontWeight;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XUndoManager;
import com.sun.star.document.XUndoManagerSupplier;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import static org.jabref.logic.openoffice.engine.storage.HeadlessLibreOffice.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@Tag("LibreOfficeTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("Localization.lang")
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class CanonicalStorageWriterTest {

    @TempDir
    Path temporaryDirectory;

    private final HeadlessLibreOffice office = new HeadlessLibreOffice(
            Path.of(System.getProperty("jabref.writer.artifacts", "build/writer-integration")).resolve("canonical-storage"));

    @BeforeAll
    void startWriter(@TempDir Path profile) throws Exception {
        office.start(profile.resolve("profile"));
    }

    @AfterEach
    void saveDocuments(TestInfo info) throws com.sun.star.io.IOException {
        office.saveAndCloseDocuments(info.getTestMethod().orElseThrow().getName());
    }

    @AfterAll
    void stopWriter() throws Exception {
        office.close();
    }

    @Test
    void pageInfosFollowStorageIndexesAfterSortingAndSaveReopen() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        CitationGroup group = create(documentCitations, document, List.of("beta", "alpha", "beta"),
                List.of(info("p. 5"), info("p. 7"), info("p. 9")), CitationType.AUTHORYEAR_PAR);
        String name = group.getReferenceMarkNameForLinking().orElseThrow();
        BibDatabase database = new BibDatabase(List.of(
                new BibEntry().withCitationKey("alpha").withField(StandardField.TITLE, "Alpha"),
                new BibEntry().withCitationKey("beta").withField(StandardField.TITLE, "Beta")));
        documentCitations.citationGroups.lookupCitations(List.of(database));
        documentCitations.citationGroups.imposeLocalOrder(Comparator.comparing(entry -> entry.getField(StandardField.TITLE).orElse("")));
        documentCitations.markStore.savePageInfos(document, group);
        MarkWriter writer = new MarkWriter(document, office.context());
        writer.write(documentCitations.markStore, group, OOText.fromString("first"));
        writer.write(documentCitations.markStore, group, OOText.fromString("second"));

        assertEquals(List.of("beta", "alpha", "beta"), MarkCodec.decode(name).citationKeys());
        assertEquals(List.of(name), WriterReferenceMarks.getListOfNames(document));
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("canonical-page-info.odt"));
        CitationGroup restored = new DocumentCitations(reopened).citationGroups.getCitationGroupsUnordered().getFirst();
        assertEquals(List.of("beta", "alpha", "beta"), keys(restored));
        assertEquals(List.of(info("p. 5"), info("p. 7"), info("p. 9")), pageInfos(restored));
        assertEquals("second", visible(WriterReferenceMarks.getAnchor(reopened, name).orElseThrow().getString()));
    }

    @Test
    void canonicalReadIgnoresLegacyJstyleAndCidMarks() throws Exception {
        XTextDocument document = office.newDocument();
        XTextCursor cursor = document.getText().createTextCursor();
        cursor.setString("legacy");
        WriterReferenceMarks.create(new DocumentAnnotation(document, "JR_cite_1_legacy", cursor, true));
        cursor.gotoEnd(false);
        cursor.setString("cid");
        WriterReferenceMarks.create(new DocumentAnnotation(document, "JABREF_legacy CID_1 old NORMAL", cursor, true));
        DocumentCitations documentCitations = new DocumentCitations(document);
        assertEquals(0, documentCitations.citationGroups.numberOfCitationGroups());
        create(documentCitations, document, List.of("canonical"), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR);

        assertEquals(List.of("canonical"), new DocumentCitations(document).citationGroups.getCitationGroupsUnordered().stream()
                                                                                         .flatMap(group -> keys(group).stream()).toList());
        assertEquals(3, WriterReferenceMarks.getListOfNames(document).size());
    }

    @Test
    void writerComparesFormattedMarkupAndKeepsOneCharacterAndInvisibleMarks() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        CitationGroup visible = create(documentCitations, document, List.of("visible"), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR);
        CitationGroup invisible = create(documentCitations, document, List.of("hidden"), List.of(Optional.empty()), CitationType.INVISIBLE_CIT);
        List<String> names = WriterReferenceMarks.getListOfNames(document);
        MarkWriter writer = new MarkWriter(document, office.context());
        assertTrue(writer.write(documentCitations.markStore, visible, OOText.fromString("<i>A</i>")));
        assertTrue(writer.write(documentCitations.markStore, visible, OOText.fromString("<b>A</b>")));
        assertFalse(writer.write(documentCitations.markStore, visible, OOText.fromString("<b>A</b>")));
        RenderResult rendered = new JStyleRenderer(numericStyle()).render(new RenderRequest(List.of(invisible), List.of(),
                new RenderOptions(BstCitationFormat.NUMERIC, false, "", "", "", false)));
        OOText invisibleMarker = Optional.ofNullable(rendered.markers().get(invisible.groupId)).orElseThrow();
        assertEquals(OOText.fromString(""), invisibleMarker);
        writer.write(documentCitations.markStore, invisible, invisibleMarker);
        assertEquals(invisible.getReferenceMarkNameForLinking().orElseThrow(), documentCitations.markStore.referenceMark(invisible).getRangeName());

        assertEquals("A", visible(documentCitations.getMarkRange(document, visible).orElseThrow().getString()));
        assertEquals("", visible(documentCitations.getMarkRange(document, invisible).orElseThrow().getString()));
        assertEquals(names, WriterReferenceMarks.getListOfNames(document));
        assertEquals(List.of(), UnoUserDefinedProperty.getListOfNames(document));
    }

    @Test
    void repeatedSuperscriptRefreshDoesNotShrinkFontOrRenameMarks() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        CitationGroup group = create(documentCitations, document, List.of("alpha"), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR);
        MarkWriter writer = new MarkWriter(document, office.context());
        writer.write(documentCitations.markStore, group, OOText.fromString("<sup>1</sup>"));
        XPropertySet initial = query(XPropertySet.class, inner(documentCitations, document, group));
        Object height = initial.getPropertyValue("CharHeight");
        List<String> names = WriterReferenceMarks.getListOfNames(document);
        for (int number = 2; number <= 6; number++) {
            writer.write(documentCitations.markStore, group, OOText.fromString("<sup>" + number + "</sup>"));
            XPropertySet properties = query(XPropertySet.class, inner(documentCitations, document, group));
            assertEquals((byte) 58, properties.getPropertyValue("CharEscapementHeight"));
            assertEquals(height, properties.getPropertyValue("CharHeight"));
        }
        assertEquals(names, WriterReferenceMarks.getListOfNames(document));
    }

    @ParameterizedTest
    @EnumSource(BoundaryPolicy.class)
    void formattedRangeWriterPreservesFontHeightAndExplicitBoundaryPolicy(BoundaryPolicy policy) throws Exception {
        XTextDocument document = office.newDocument();
        String name = "uninterpreted name";
        WriterReferenceMark range = new WriterReferenceMarks().create(document, office.context(), name,
                document.getText().createTextCursor(), false, false, policy);
        Object height = query(XPropertySet.class, range.getRawCursor(document).orElseThrow()).getPropertyValue("CharHeight");
        for (String content : List.of("1", "12", "2", "23")) {
            for (String tag : List.of("sup", "sub")) {
                ReferenceMarkTextWriter.write(document, office.context(), range, OOText.fromString("<" + tag + ">" + content + "</" + tag + ">"));

                XTextCursor full = range.getRawCursor(document).orElseThrow();
                String expected = policy == BoundaryPolicy.PERSISTENT ? "\u200b" + content + "\u200b"
                                                                      : content.length() == 1 ? "\u200b" + content : content;
                assertEquals(expected, full.getString());
                XPropertySet properties = query(XPropertySet.class, full);
                assertEquals(height, properties.getPropertyValue("CharHeight"));
                assertEquals("sup".equals(tag) ? (short) 33 : (short) -10, properties.getPropertyValue("CharEscapement"));
                assertEquals((byte) 58, properties.getPropertyValue("CharEscapementHeight"));
                assertEquals(false, properties.getPropertyValue("CharAutoEscapement"));
            }
        }
        assertEquals(List.of(name), WriterReferenceMarks.getListOfNames(document));
        assertEquals(List.of(), UnoUserDefinedProperty.getListOfNames(document));
    }

    @Test
    void mergeAndSeparatePreserveEachPageInfoAndOriginalKeyOrder() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        CitationGroup beta = create(documentCitations, document, List.of("beta"), List.of(info("p. 5")), CitationType.AUTHORYEAR_PAR);
        CitationGroup alpha = create(documentCitations, document, List.of("alpha"), List.of(info("p. 7")), CitationType.AUTHORYEAR_PAR);
        MarkWriter writer = new MarkWriter(document, office.context());
        writer.write(documentCitations.markStore, beta, OOText.fromString("[1]"));
        writer.write(documentCitations.markStore, alpha, OOText.fromString("[2]"));
        List<String> originalProperties = UnoUserDefinedProperty.getListOfNames(document);
        CitationGroupEditor editor = new CitationGroupEditor(document, office.context(), writer);
        DocumentSession session = new DocumentSession(document);

        assertEquals(true, session.write("Merge citations", () -> editor.merge(documentCitations)).get());
        CitationGroup merged = documentCitations.citationGroups.getCitationGroupsUnordered().getFirst();
        assertEquals(List.of("beta", "alpha"), keys(merged));
        assertEquals(List.of(info("p. 5"), info("p. 7")), pageInfos(merged));
        assertFalse(originalProperties.stream().anyMatch(property -> UnoUserDefinedProperty.getListOfNames(document).contains(property)));
        assertEquals(true, session.write("Separate citations", () -> editor.separate(documentCitations)).get());
        List<CitationGroup> separated = new DocumentOrder().withinTextContainers(document, documentCitations.citationGroups, documentCitations.markStore);
        assertEquals(List.of("beta", "alpha"), separated.stream().flatMap(group -> keys(group).stream()).toList());
        assertEquals(List.of(info("p. 5"), info("p. 7")), separated.stream().flatMap(group -> pageInfos(group).stream()).toList());
    }

    @Test
    void mergeDoesNotCrossParagraphsOrMergeNonNormalCitationTypes() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        create(documentCitations, document, List.of("first"), List.of(info("p. 1")), CitationType.AUTHORYEAR_PAR);
        XTextCursor paragraph = document.getText().createTextCursor();
        paragraph.gotoEnd(false);
        paragraph.setString("\n");
        create(documentCitations, document, List.of("second"), List.of(info("p. 2")), CitationType.AUTHORYEAR_PAR);
        CitationGroup partial = create(documentCitations, document, List.of("third", "fourth"),
                List.of(info("p. 3"), info("p. 4")), CitationType.AUTHOR_ONLY);
        List<String> names = WriterReferenceMarks.getListOfNames(document);

        CitationGroupEditor editor = new CitationGroupEditor(document, office.context(), new MarkWriter(document, office.context()));
        DocumentSession session = new DocumentSession(document);
        assertEquals(false, session.write("Merge citations", () -> editor.merge(documentCitations)).get());
        assertEquals(false, session.write("Separate citations", () -> editor.separate(documentCitations)).get());
        assertEquals(names, WriterReferenceMarks.getListOfNames(document));
        assertEquals(List.of(info("p. 3"), info("p. 4")), pageInfos(partial));
    }

    @Test
    void groupEditsRestoreTrackChangesWithoutCreatingRedlines() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        create(documentCitations, document, List.of("first"), List.of(info("p. 1")), CitationType.AUTHORYEAR_PAR);
        create(documentCitations, document, List.of("second"), List.of(info("p. 2")), CitationType.AUTHORYEAR_PAR);
        documentCitations.imposeGlobalOrder(document);
        UnoRedlines.setRecordChanges(document, true);
        CitationGroupEditor editor = new CitationGroupEditor(document, office.context(), new MarkWriter(document, office.context()));
        DocumentSession session = new DocumentSession(document);

        assertEquals(true, session.write("Merge citations", () -> editor.merge(documentCitations)).get());
        assertTrue(UnoRedlines.getRecordChanges(document));
        assertFalse(document.hasControllersLocked());
        assertEquals(true, session.write("Separate citations", () -> editor.separate(documentCitations)).get());
        assertTrue(UnoRedlines.getRecordChanges(document));
        assertFalse(document.hasControllersLocked());
        assertEquals(List.of(), UnoRedlines.getDeletedRanges(document));
    }

    @Test
    void documentOrderMapsFootnoteCitationsAndProtectsTheirBodyAnchor() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        create(documentCitations, document, List.of("body"), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR);
        XTextContent footnote = query(XTextContent.class, query(XMultiServiceFactory.class, document).createInstance("com.sun.star.text.Footnote"));
        XTextCursor bodyPosition = document.getText().createTextCursor();
        bodyPosition.gotoEnd(false);
        document.getText().insertTextContent(bodyPosition, footnote, false);
        XText note = query(XText.class, footnote);
        XTextCursor notePosition = note.createTextCursor();
        documentCitations.createCitationGroup(document, office.context(), List.of("note-first"), List.of(info("p. 5")),
                CitationType.AUTHORYEAR_PAR, notePosition, false, true);
        notePosition.gotoEnd(false);
        documentCitations.createCitationGroup(document, office.context(), List.of("note-second"), List.of(info("p. 7")),
                CitationType.AUTHORYEAR_PAR, notePosition, false, true);
        create(documentCitations, document, List.of("after"), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR);
        DocumentOrder order = new DocumentOrder();

        assertEquals(List.of("body", "note-first", "note-second", "after"),
                order.inDocumentOrder(document, documentCitations.citationGroups, documentCitations.markStore, Optional.empty()).stream()
                     .flatMap(group -> keys(group).stream()).toList());
        RangeForOverlapCheck<CitationGroupId> protectedAnchor = new RangeForOverlapCheck<>(footnote.getAnchor(), new CitationGroupId("selection"), RangeForOverlapCheck.CURSOR_MARK_KIND, "footnote anchor");
        assertTrue(documentCitations.checkRangeOverlapsWithCursor(document, List.of(protectedAnchor), false).isError());
    }

    @Test
    void documentStateRoundTripsWithoutTouchingZoteroPreferencesAndOrphanCleanupIsScoped() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        CitationGroup group = create(documentCitations, document, List.of("alpha"), List.of(info("p. 5")), CitationType.AUTHORYEAR_PAR);
        UnoUserDefinedProperty.setStringProperty(document, "ZOTERO_PREF", "untouched");
        UnoUserDefinedProperty.setStringProperty(document, MarkStore.pageInfoProperty("orphan", 0), "p. 99");
        DocumentStateStore state = new DocumentStateStore();
        DocumentStateStore.State expected = new DocumentStateStore.State(DocumentStateStore.Family.CSL, "nature.csl");
        state.store(document, expected);
        documentCitations.markStore.removeOrphanPageInfos(document);

        assertEquals(Optional.of(expected), state.read(document));
        assertEquals(Optional.of("untouched"), UnoUserDefinedProperty.getStringValue(document, "ZOTERO_PREF"));
        assertEquals(Optional.empty(), UnoUserDefinedProperty.getStringValue(document, MarkStore.pageInfoProperty("orphan", 0)));
        assertEquals(Optional.of("p. 5"), UnoUserDefinedProperty.getStringValue(document,
                MarkStore.pageInfoProperty(documentCitations.markStore.getMarkData(group).orElseThrow().uniqueId(), 0)));
    }

    @Test
    void benchmarkThreeHundredCanonicalInsertionsAndVisualOrdering() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        MarkWriter writer = new MarkWriter(document, office.context());
        List<String> expected = new ArrayList<>();
        long start = System.nanoTime();
        writer.beginUpdate();
        try {
            for (int index = 0; index < 300; index++) {
                String key = "entry" + index;
                expected.add(key);
                CitationGroup group = create(documentCitations, document, List.of(key), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR);
                writer.write(documentCitations.markStore, group, OOText.fromString("[" + (index + 1) + "]"));
            }
            long inserted = System.nanoTime();
            OOResult<FunctionalTextViewCursor, String> cursor = FunctionalTextViewCursor.get(document);
            assertFalse(cursor.isError());
            List<CitationGroup> sorted = new DocumentOrder().inDocumentOrder(document, documentCitations.citationGroups, documentCitations.markStore, Optional.of(cursor.get()));
            long ordered = System.nanoTime();
            assertEquals(expected, sorted.stream().flatMap(group -> keys(group).stream()).toList());
            documentCitations.citationGroups.setGlobalOrder(sorted.stream().map(group -> group.groupId).toList());
            Map<CitationGroupId, OOText> markers = new LinkedHashMap<>();
            for (int index = 0; index < sorted.size(); index++) {
                markers.put(sorted.get(index).groupId, OOText.fromString("[" + (index + 1) + "]"));
            }
            assertEquals(0, writer.apply(documentCitations.markStore, sorted, markers));
            System.out.printf("[phase3-storage-benchmark] citations=300 insertionMs=%d visualOrderMs=%d%n",
                    TimeUnit.NANOSECONDS.toMillis(inserted - start), TimeUnit.NANOSECONDS.toMillis(ordered - inserted));
        } finally {
            writer.endUpdate();
            writer.close();
        }
    }

    @Test
    void undoStyleChangeInvalidatesCachedMarkupBeforeRefreshingTheSameStyle() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        CitationGroup group = create(documentCitations, document, List.of("alpha"), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR);
        XUndoManager undo = undoManager(document);
        try (MarkWriter writer = new MarkWriter(document, office.context())) {
            writeInContext(writer, documentCitations, group, undo, "Style A", OOText.fromString("<i>marker</i>"));
            undo.clear();
            writeInContext(writer, documentCitations, group, undo, "Style B", OOText.fromString("<b>marker</b>"));
            assertEquals("marker", inner(documentCitations, document, group).getString());
            undo.undo();
            assertEquals("marker", inner(documentCitations, document, group).getString());
            assertEquals(FontWeight.NORMAL, query(XPropertySet.class, inner(documentCitations, document, group)).getPropertyValue("CharWeight"));

            assertTrue(writeInContext(writer, documentCitations, group, undo, "Refresh B", OOText.fromString("<b>marker</b>")));
            assertEquals("marker", inner(documentCitations, document, group).getString());
            assertEquals(FontWeight.BOLD, query(XPropertySet.class, inner(documentCitations, document, group)).getPropertyValue("CharWeight"));
            undo.undo();
            assertEquals(FontWeight.NORMAL, query(XPropertySet.class, inner(documentCitations, document, group)).getPropertyValue("CharWeight"));
            undo.redo();
            assertEquals("marker", inner(documentCitations, document, group).getString());
            assertEquals(FontWeight.BOLD, query(XPropertySet.class, inner(documentCitations, document, group)).getPropertyValue("CharWeight"));
            assertTrue(writer.write(documentCitations.markStore, group, OOText.fromString("<b>marker</b>")));
            assertFalse(writer.write(documentCitations.markStore, group, OOText.fromString("<b>marker</b>")));
        }
    }

    @Test
    void manualTextAndFormattingEditsInvalidateTheFormattedBaseline() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        CitationGroup group = create(documentCitations, document, List.of("alpha"), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR);
        OOText marker = OOText.fromString("<b>stable</b>");
        try (MarkWriter writer = new MarkWriter(document, office.context())) {
            assertTrue(writer.write(documentCitations.markStore, group, marker));
            assertFalse(writer.write(documentCitations.markStore, group, marker));
            inner(documentCitations, document, group).setString("manual edit");
            assertTrue(writer.write(documentCitations.markStore, group, marker));
            assertEquals("stable", inner(documentCitations, document, group).getString());
            XPropertySet properties = query(XPropertySet.class, inner(documentCitations, document, group));
            properties.setPropertyValue("CharWeight", FontWeight.NORMAL);
            assertEquals(FontWeight.NORMAL, properties.getPropertyValue("CharWeight"));

            assertTrue(writer.write(documentCitations.markStore, group, marker));
            assertEquals("stable", inner(documentCitations, document, group).getString());
            assertEquals(FontWeight.BOLD, query(XPropertySet.class, inner(documentCitations, document, group)).getPropertyValue("CharWeight"));
            assertFalse(writer.write(documentCitations.markStore, group, marker));
        }
    }

    @Test
    void fullUpdateScopeRetainsAllBaselinesAcrossOtherOwnedDocumentChanges() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        CitationGroup first = create(documentCitations, document, List.of("alpha"), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR);
        CitationGroup second = create(documentCitations, document, List.of("beta"), List.of(Optional.empty()), CitationType.AUTHORYEAR_PAR);
        List<CitationGroup> groups = List.of(first, second);
        Map<CitationGroupId, OOText> markers = Map.of(first.groupId, OOText.fromString("first"), second.groupId, OOText.fromString("second"));
        try (MarkWriter writer = new MarkWriter(document, office.context())) {
            writer.beginUpdate();
            try {
                assertEquals(2, writer.apply(documentCitations.markStore, groups, markers));
                XTextCursor end = document.getText().createTextCursor();
                end.gotoEnd(false);
                end.setString("Owned bibliography update");
                assertEquals(0, writer.apply(documentCitations.markStore, groups, markers));
            } finally {
                writer.endUpdate();
            }
            assertEquals(0, writer.apply(documentCitations.markStore, groups, markers));
            XTextCursor end = document.getText().createTextCursor();
            end.gotoEnd(false);
            end.setString("External edit");
            assertEquals(2, writer.apply(documentCitations.markStore, groups, markers));
        }
    }

    @Test
    void pageInfoChangesAndRemovalsAreUndoableWithoutAddingActionsDuringReplay() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        CitationGroup group = create(documentCitations, document, List.of("alpha"), List.of(info("p. 5")), CitationType.AUTHORYEAR_PAR);
        String property = MarkStore.pageInfoProperty(group.groupId.citationGroupIdAsString(), 0);
        XUndoManager undo = undoManager(document);
        undo.clear();
        group.citationsInStorageOrder.getFirst().setPageInfo(info("p. 7"));
        undo.enterUndoContext("Change citation page info");
        try {
            documentCitations.markStore.savePageInfos(document, group);
        } finally {
            undo.leaveUndoContext();
        }
        documentCitations.markStore.savePageInfos(document, group);
        assertEquals(1, undo.getAllUndoActionTitles().length);
        undo.undo();
        assertEquals(Optional.of("p. 5"), UnoUserDefinedProperty.getStringValue(document, property));
        undo.redo();
        assertEquals(Optional.of("p. 7"), UnoUserDefinedProperty.getStringValue(document, property));
        assertEquals(1, undo.getAllUndoActionTitles().length);

        group.citationsInStorageOrder.getFirst().setPageInfo(Optional.empty());
        documentCitations.markStore.savePageInfos(document, group);
        assertEquals(Optional.empty(), UnoUserDefinedProperty.getStringValue(document, property));
        undo.undo();
        assertEquals(Optional.of("p. 7"), UnoUserDefinedProperty.getStringValue(document, property));
        undo.redo();
        assertEquals(Optional.empty(), UnoUserDefinedProperty.getStringValue(document, property));
        assertEquals(2, undo.getAllUndoActionTitles().length);
    }

    @Test
    void undoRefreshThenUndoUserDeletionRestoresTheCitationUidAndPageInfo() throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("Before ");
        DocumentCitations documentCitations = new DocumentCitations(document);
        CitationGroup group = create(documentCitations, document, List.of("alpha"), List.of(info("p. 5")), CitationType.AUTHORYEAR_PAR);
        String name = group.getReferenceMarkNameForLinking().orElseThrow();
        String property = MarkStore.pageInfoProperty(group.groupId.citationGroupIdAsString(), 0);
        try (MarkWriter writer = new MarkWriter(document, office.context())) {
            writer.write(documentCitations.markStore, group, OOText.fromString("[1]"));
        }
        XUndoManager undo = undoManager(document);
        undo.clear();
        undo.enterUndoContext("User deletes citation");
        try {
            XTextRange range = WriterReferenceMarks.getAnchor(document, name).orElseThrow();
            XTextCursor end = range.getText().createTextCursorByRange(range.getEnd());
            end.goRight((short) 1, false);
            XTextCursor deletion = range.getText().createTextCursorByRange(range.getStart());
            deletion.goLeft((short) 1, false);
            deletion.gotoRange(end, true);
            deletion.setString("");
        } finally {
            undo.leaveUndoContext();
        }
        assertEquals(List.of(), WriterReferenceMarks.getListOfNames(document));
        assertEquals(Optional.of("p. 5"), UnoUserDefinedProperty.getStringValue(document, property));
        try (CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> refreshed = engine.refresh(List.of(database()), numericStyle());
            assertFalse(refreshed.isError(), () -> refreshed.getError().toString());
        }
        assertEquals(Optional.empty(), UnoUserDefinedProperty.getStringValue(document, property));
        assertEquals(2, undo.getAllUndoActionTitles().length);
        undo.undo();
        assertEquals(Optional.of("p. 5"), UnoUserDefinedProperty.getStringValue(document, property));
        undo.undo();
        assertEquals(List.of(name), WriterReferenceMarks.getListOfNames(document));
        assertEquals(List.of(info("p. 5")), pageInfos(new DocumentCitations(document).citationGroups.getCitationGroupsUnordered().getFirst()));
        undo.redo();
        undo.redo();
        assertEquals(List.of(), WriterReferenceMarks.getListOfNames(document));
        assertEquals(Optional.empty(), UnoUserDefinedProperty.getStringValue(document, property));
    }

    @Test
    void propertyDeltasUndoMergeAndSeparateInsideTheCallersSingleContext() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        create(documentCitations, document, List.of("beta"), List.of(info("p. 5")), CitationType.AUTHORYEAR_PAR);
        create(documentCitations, document, List.of("alpha"), List.of(info("p. 7")), CitationType.AUTHORYEAR_PAR);
        Set<String> originalNames = Set.copyOf(WriterReferenceMarks.getListOfNames(document));
        Map<String, String> originalProperties = pageProperties(document);
        XUndoManager undo = undoManager(document);
        undo.clear();
        DocumentSession session = new DocumentSession(document);
        try (MarkWriter writer = new MarkWriter(document, office.context())) {
            CitationGroupEditor editor = new CitationGroupEditor(document, office.context(), writer);
            writer.beginUpdate();
            try {
                assertEquals(true, session.write("Merge citations", () -> editor.merge(documentCitations)).get());
            } finally {
                writer.endUpdate();
            }
            assertEquals(1, undo.getAllUndoActionTitles().length);
            List<String> mergedNames = WriterReferenceMarks.getListOfNames(document);
            Map<String, String> mergedProperties = pageProperties(document);
            undo.undo();
            assertEquals(originalNames, Set.copyOf(WriterReferenceMarks.getListOfNames(document)));
            assertEquals(originalProperties, pageProperties(document));
            undo.redo();
            assertEquals(mergedNames, WriterReferenceMarks.getListOfNames(document));
            assertEquals(mergedProperties, pageProperties(document));

            undo.clear();
            DocumentCitations merged = new DocumentCitations(document);
            writer.beginUpdate();
            try {
                assertEquals(true, session.write("Separate citations", () -> editor.separate(merged)).get());
            } finally {
                writer.endUpdate();
            }
            assertEquals(1, undo.getAllUndoActionTitles().length);
            Map<String, String> separatedProperties = pageProperties(document);
            undo.undo();
            assertEquals(mergedNames, WriterReferenceMarks.getListOfNames(document));
            assertEquals(mergedProperties, pageProperties(document));
            undo.redo();
            assertEquals(2, new DocumentCitations(document).citationGroups.numberOfCitationGroups());
            assertEquals(separatedProperties, pageProperties(document));
        }
    }

    // [utest->req~openoffice.single-step-group-undo~1]
    @Test
    void undoMergeRestoresOriginalUidsAndTheirPageInfoProperties() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        create(documentCitations, document, List.of("beta"), List.of(info("p. 5")), CitationType.AUTHORYEAR_PAR);
        create(documentCitations, document, List.of("alpha"), List.of(info("p. 7")), CitationType.AUTHORYEAR_PAR);
        Set<String> originalNames = Set.copyOf(WriterReferenceMarks.getListOfNames(document));
        Map<String, String> originalProperties = pageProperties(document);
        XUndoManager undo = undoManager(document);
        undo.clear();
        try (CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> merged = engine.merge(List.of(database()), numericStyle());
            assertFalse(merged.isError(), () -> merged.getError().toString());
        }
        assertEquals(1, new DocumentCitations(document).citationGroups.numberOfCitationGroups());
        assertEquals(1, undo.getAllUndoActionTitles().length);
        Set<String> mergedNames = Set.copyOf(WriterReferenceMarks.getListOfNames(document));
        Map<String, String> mergedProperties = pageProperties(document);
        assertEquals(2, mergedProperties.size());
        assertTrue(originalProperties.keySet().stream().noneMatch(mergedProperties::containsKey));
        undo.undo();
        assertEquals(originalNames, Set.copyOf(WriterReferenceMarks.getListOfNames(document)));
        assertEquals(originalProperties, pageProperties(document));
        undo.redo();
        assertEquals(mergedNames, Set.copyOf(WriterReferenceMarks.getListOfNames(document)));
        assertEquals(mergedProperties, pageProperties(document));
    }

    // [utest->req~openoffice.single-step-group-undo~1]
    @Test
    void undoSeparateRestoresTheMergedUidAndEveryPageInfoSlot() throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        create(documentCitations, document, List.of("beta", "alpha"), List.of(info("p. 5"), info("p. 7")), CitationType.AUTHORYEAR_PAR);
        List<String> mergedNames = WriterReferenceMarks.getListOfNames(document);
        Map<String, String> mergedProperties = pageProperties(document);
        XUndoManager undo = undoManager(document);
        undo.clear();
        try (CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> separated = engine.separate(List.of(database()), numericStyle());
            assertFalse(separated.isError(), () -> separated.getError().toString());
        }
        assertEquals(2, new DocumentCitations(document).citationGroups.numberOfCitationGroups());
        assertEquals(1, undo.getAllUndoActionTitles().length);
        Map<String, String> separatedProperties = pageProperties(document);
        assertEquals(2, separatedProperties.size());
        undo.undo();
        assertEquals(mergedNames, WriterReferenceMarks.getListOfNames(document));
        assertEquals(mergedProperties, pageProperties(document));
        undo.redo();
        assertEquals(2, new DocumentCitations(document).citationGroups.numberOfCitationGroups());
        assertEquals(separatedProperties, pageProperties(document));
    }

    // [utest->req~openoffice.single-step-group-undo~1]
    @ParameterizedTest
    @ValueSource(strings = {"merge", "separate"})
    void transformedGroupRenderingFailureLeavesMarksPropertiesTextAndUndoUntouched(String operation) throws Exception {
        XTextDocument document = office.newDocument();
        DocumentCitations documentCitations = new DocumentCitations(document);
        boolean merging = "merge".equals(operation);
        if (merging) {
            create(documentCitations, document, List.of("beta"), List.of(info("p. 5")), CitationType.AUTHORYEAR_PAR);
            create(documentCitations, document, List.of("alpha"), List.of(info("p. 7")), CitationType.AUTHORYEAR_PAR);
        } else {
            create(documentCitations, document, List.of("beta", "alpha"), List.of(info("p. 5"), info("p. 7")), CitationType.AUTHORYEAR_PAR);
        }
        Set<String> names = Set.copyOf(WriterReferenceMarks.getListOfNames(document));
        Map<String, String> properties = pageProperties(document);
        String text = document.getText().getString();
        XUndoManager undo = undoManager(document);
        undo.clear();
        int proposedCount = merging ? 1 : 2;
        CitationRenderer renderer = mock(CitationRenderer.class);
        when(renderer.capabilities()).thenReturn(new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR), true, true, true));
        when(renderer.render(any())).thenAnswer(invocation -> {
            RenderRequest request = invocation.getArgument(0);
            if (request.groupsInDocumentOrder().size() == proposedCount) {
                throw new RenderException("Cannot render the proposed group transformation");
            }
            Map<CitationGroupId, OOText> markers = new LinkedHashMap<>();
            request.groupsInDocumentOrder().forEach(group -> markers.put(group.groupId, OOText.fromString("preflight")));
            return new RenderResult(markers, List.of(), List.of());
        });
        try (MockedConstruction<RendererFactory> factories = mockConstruction(RendererFactory.class,
                (factory, _) -> when(factory.create(any())).thenReturn(renderer));
             CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> result = merging ? engine.merge(List.of(database()), numericStyle()) : engine.separate(List.of(database()), numericStyle());
            assertTrue(result.isError());
        }

        assertEquals(names, Set.copyOf(WriterReferenceMarks.getListOfNames(document)));
        assertEquals(properties, pageProperties(document));
        assertEquals(text, document.getText().getString());
        assertEquals(0, undo.getAllUndoActionTitles().length);
        ArgumentCaptor<RenderRequest> request = ArgumentCaptor.forClass(RenderRequest.class);
        verify(renderer).render(request.capture());
        assertEquals(proposedCount, request.getValue().groupsInDocumentOrder().size());
        assertTrue(request.getValue().options().includeBibliography());
    }

    private boolean writeInContext(MarkWriter writer, DocumentCitations documentCitations, CitationGroup group, XUndoManager undo,
                                   String title, OOText marker) throws Exception {
        writer.beginUpdate();
        try {
            undo.enterUndoContext(title);
            try {
                return writer.write(documentCitations.markStore, group, marker);
            } finally {
                undo.leaveUndoContext();
            }
        } finally {
            writer.endUpdate();
        }
    }

    private CitationEngine engine(XTextDocument document) {
        return new CitationEngine(document, office.context(), OpenOfficePreferences.getDefault(), new BibEntryTypesManager());
    }

    private static JStyle numericStyle() throws Exception {
        return new JStyle(JStyleLoader.DEFAULT_NUMERICAL_STYLE_PATH, LayoutFormatterPreferences.getDefault(), new JournalAbbreviationRepository());
    }

    private static BibDatabase database() {
        return new BibDatabase(List.of(new BibEntry().withCitationKey("alpha").withField(StandardField.TITLE, "Alpha"),
                new BibEntry().withCitationKey("beta").withField(StandardField.TITLE, "Beta")));
    }

    private static XUndoManager undoManager(XTextDocument document) {
        return query(XUndoManagerSupplier.class, document).getUndoManager();
    }

    private static Map<String, String> pageProperties(XTextDocument document) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : UnoUserDefinedProperty.getListOfNames(document)) {
            if (name.startsWith(MarkStore.PAGE_INFO_PREFIX)) {
                result.put(name, UnoUserDefinedProperty.getStringValue(document, name).orElseThrow());
            }
        }
        return result;
    }

    private CitationGroup create(DocumentCitations documentCitations, XTextDocument document, List<String> keys, List<Optional<OOText>> infos, CitationType type) throws Exception {
        XTextCursor position = document.getText().createTextCursor();
        position.gotoEnd(false);
        return documentCitations.createCitationGroup(document, office.context(), keys, infos, type, position, false, true);
    }

    private static XTextCursor inner(DocumentCitations documentCitations, XTextDocument document, CitationGroup group) throws Exception {
        XTextCursor full = documentCitations.markStore.getRawCursorForCitationGroup(group, document).orElseThrow();
        XTextCursor start = full.getText().createTextCursorByRange(full.getStart());
        start.goRight((short) 1, false);
        XTextCursor end = full.getText().createTextCursorByRange(full.getEnd());
        end.goLeft((short) 1, false);
        start.gotoRange(end, true);
        return start;
    }

    private static Optional<OOText> info(String text) {
        return Optional.of(OOText.fromString(text));
    }

    private static List<String> keys(CitationGroup group) {
        return group.citationsInStorageOrder.stream().map(Citation::getCitationKey).toList();
    }

    private static List<Optional<OOText>> pageInfos(CitationGroup group) {
        return group.citationsInStorageOrder.stream().map(Citation::getPageInfo).toList();
    }

    private static String visible(String text) {
        return text.replace("\u200b", "");
    }
}
