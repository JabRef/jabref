package org.jabref.logic.openoffice.integration;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.JabRefException;
import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.ZoteroCitationLinker;
import org.jabref.logic.openoffice.ZoteroCitationLinker.LinkPlan;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.backend.WriterReferenceMark;
import org.jabref.logic.openoffice.backend.WriterReferenceMark.BoundaryPolicy;
import org.jabref.logic.openoffice.backend.WriterReferenceMarks;
import org.jabref.logic.openoffice.bst.BstStyle;
import org.jabref.logic.openoffice.engine.CitationEngine;
import org.jabref.logic.openoffice.engine.CitedEntryExporter;
import org.jabref.logic.openoffice.engine.JabRefMarkCodec;
import org.jabref.logic.openoffice.engine.RendererFactory;
import org.jabref.logic.openoffice.engine.ZoteroCitationStrategy.Reason;
import org.jabref.logic.openoffice.engine.ZoteroCitationStrategy.StrategyException;
import org.jabref.logic.openoffice.jstyle.JStyle;
import org.jabref.logic.openoffice.jstyle.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.NamedTextContentPlacement;
import org.jabref.logic.openoffice.uno.UnoRedlines;
import org.jabref.logic.openoffice.uno.UnoTextSection;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.star.awt.FontSlant;
import com.sun.star.awt.FontWeight;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameAccess;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XFootnotesSupplier;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.jabref.logic.openoffice.integration.HeadlessLibreOffice.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@NullMarked
@Tag("LibreOfficeTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("Localization.lang")
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class ZoteroWriterTest {
    private static final Pattern BIBLIOGRAPHY_NAME = Pattern.compile(
            Pattern.quote("ZOTERO_BIBL {\"uncited\":[],\"omitted\":[],\"custom\":[]} CSL_BIBLIOGRAPHY RND") + "[a-z][a-z0-9]{9}");
    private static final String NATIVE_URI = "http://zotero.org/users/123/items/NATIVE123";
    private static final String CANONICAL = "JABREF_ignored NORMAL stableIgnored";
    private static final String LEGACY_CID = "JABREF_ignored CID_7 old NORMAL";
    private static final String LEGACY_JSTYLE = "JR_cite_1_ignored";

    @TempDir
    Path temporaryDirectory;

    private int artifactIndex;
    private final HeadlessLibreOffice office = new HeadlessLibreOffice(
            Path.of(System.getProperty("jabref.writer.artifacts", "build/writer-integration")).resolve("zotero-acceptance"));

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
    @CsvSource({"TEMPORARY, ''", "TEMPORARY, x", "TEMPORARY, xy", "TEMPORARY, \u200b\u200b",
            "TEMPORARY, \u200bx", "TEMPORARY, native", "PERSISTENT, \u200b\u200b", "PERSISTENT, \u200bold\u200b"})
    void explicitBoundaryPoliciesPreserveNativeShortMarksAcrossRepeatedFillAndReopen(BoundaryPolicy policy, String initial) throws Exception {
        XTextDocument document = office.newDocument();
        String name = nativeMarkName();
        document.getText().setString("before " + initial + " after");
        XTextCursor position = document.getText().createTextCursor();
        position.goRight((short) 7, false);
        position.goRight((short) initial.length(), true);
        WriterReferenceMarks.create(new NamedTextContentPlacement(document, name, position, true));
        WriterReferenceMarks marks = new WriterReferenceMarks();
        for (String rendered : List.of("", "X", "updated", "")) {
            WriterReferenceMark mark = marks.find(document, name, policy).orElseThrow();
            mark.getFillCursor(document).setString(rendered);
            mark.cleanFillCursor(document);
            String expected = policy == BoundaryPolicy.PERSISTENT || rendered.isEmpty()
                              ? "\u200b" + rendered + "\u200b"
                              : rendered.length() == 1 ? "\u200b" + rendered : rendered;
            assertEquals(expected, mark.getRawCursor(document).orElseThrow().getString());
            assertEquals("before " + expected + " after", document.getText().getString());
            assertEquals(List.of(name), marks.names(document));
        }
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("native-boundary.odt"));
        WriterReferenceMark mark = marks.find(reopened, name, policy).orElseThrow();
        assertEquals("\u200b\u200b", mark.getRawCursor(reopened).orElseThrow().getString());
        mark.getFillCursor(reopened).setString("again");
        mark.cleanFillCursor(reopened);
        String expected = policy == BoundaryPolicy.PERSISTENT ? "\u200bagain\u200b" : "again";
        assertEquals("before " + expected + " after", reopened.getText().getString());
        assertEquals(List.of(name), marks.names(reopened));
    }

    @Test
    void nativeLinksArePlannedWithoutChangingDocumentAndReuseTheirUriOnInsertion() throws Exception {
        XTextDocument document = office.newDocument();
        String originalName = nativeMarkName();
        appendMark(document, originalName, "[native]");
        String originalBody = document.getText().getString();
        BibEntry entry = nativeEntry("native");
        BibDatabase database = new BibDatabase(List.of(entry));
        BibEntryTypesManager entryTypes = new BibEntryTypesManager();
        LinkPlan plan = ZoteroCitationLinker.planLinks(List.of(originalName), new BibDatabaseContext(database), entryTypes);
        assertEquals(1, plan.updates().size());
        assertEquals(originalBody, document.getText().getString());
        String linkedName = plan.updatedName(originalName);
        assertNativeMetadata(linkedName, List.of(NATIVE_URI, "http://www.jabref.org/native"));
        assertEquals(Map.of(originalName, "[native]"), zoteroMarks(document));
        assertEquals(List.of(), ZoteroCitationLinker.planLinks(List.of(linkedName), new BibDatabaseContext(database), entryTypes).updates());

        try (CitationEngine engine = engine(document)) {
            assertEquals(List.of(), success(engine.refresh(List.of(database), style())));
            assertEquals(Map.of(linkedName, "[1]"), zoteroMarks(document));
            assertEquals(List.of(), success(engine.insert(List.of(entry), List.of(database), style(), CitationType.AUTHORYEAR_PAR,
                    "", document.getText().createTextCursor(), false)));
            assertEquals(2, zoteroMarks(document).size());
            assertTrue(zoteroMarks(document).containsKey(linkedName));
            for (String name : zoteroMarks(document).keySet()) {
                assertEquals(Map.of("native", NATIVE_URI), ZoteroReferenceMark.extractZoteroUriByCitationKey(name));
                assertEquals("[1]", zoteroMarks(document).get(name));
            }
            assertEquals(2, zoteroMarks(document).keySet().stream()
                                                 .map(name -> ZoteroReferenceMark.parse(name).orElseThrow().uniqueId()).distinct().count());
            assertEquals(List.of("native"), exportedKeys(success(engine.exportCited(List.of(database)))));
        }
        Map<String, String> expected = zoteroMarks(document);
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("native-linked.odt"));
        try (CitationEngine engine = engine(reopened)) {
            assertEquals(List.of(), success(engine.refresh(List.of(database), style())));
            assertEquals(expected, zoteroMarks(reopened));
            assertEquals(List.of("native"), exportedKeys(success(engine.exportCited(List.of(database)))));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"refresh", "insert"})
    void nativeLocatorsRemainInMetadataButUseOrdinaryRenderingOnRefreshAndInsertion(String action) throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("New citation: .\n");
        String name = ZoteroReferenceMark.PREFIX + Files.readString(resource("zotero-native-locator.json")).strip() + " RNDlocator001";
        appendMark(document, name, "[1, p. 17]");
        Path styleFile = temporaryDirectory.resolve("locator-aware.csl");
        Files.writeString(styleFile, Files.readString(resource("zotero-numeric.csl"))
                                          .replace("<text variable=\"citation-number\"/>", "<text variable=\"citation-number\"/><text variable=\"locator\" prefix=\", p. \"/>"));
        CitationStyle selectedStyle = CSLStyleUtils.createCitationStyleFromFile(styleFile.toString()).orElseThrow();
        BibEntry entry = nativeEntry("native");
        List<BibDatabase> databases = List.of(new BibDatabase(List.of(entry)));
        try (CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> result = "refresh".equals(action)
                                                             ? engine.refresh(databases, selectedStyle)
                                                             : engine.insert(List.of(entry), databases, selectedStyle, CitationType.AUTHORYEAR_PAR,
                    "", document.getText().createTextCursor(), false);
            assertEquals(List.of(), success(result));
            assertLinkedNativeCitation(document, name, "[1]");
            Map<String, String> citations = zoteroMarks(document);
            assertEquals("insert".equals(action) ? 2 : 1, citations.size());
            assertEquals(Set.of("[1]"), Set.copyOf(citations.values()));
        }
        Map<String, String> expected = zoteroMarks(document);
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("native-locator-" + action + ".odt"));
        assertEquals(expected, zoteroMarks(reopened));
        try (CitationEngine engine = engine(reopened)) {
            assertEquals(List.of(), success(engine.refresh(databases, selectedStyle)));
            assertEquals(expected, zoteroMarks(reopened));
            assertLinkedNativeCitation(reopened, name, "[1]");
        }
    }

    @ParameterizedTest
    @MethodSource("nativeModifierUpdates")
        // [utest->req~openoffice.zotero-modifier-compatibility~1]
    void ordinaryNativeModifiersAllowUpdatesAndPreserveMetadata(String operation, String location, String field, String expectedText) throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("New citation: .\n");
        String ordinaryName = nativeMarkName().replace("native-citation", "supported-citation");
        appendMark(document, ordinaryName, "[supported]");
        JsonObject citation = json(nativeMarkName());
        JsonObject item = citation.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        citation.addProperty("foreign", "keep citation metadata");
        item.addProperty("foreign", "keep item metadata");
        if ("author-only".equals(field) || "suppress-author".equals(field)) {
            item.addProperty(field, true);
        } else if ("mixed-suppression".equals(field)) {
            citation.getAsJsonArray("citationItems").add(item.deepCopy());
            item.addProperty("suppress-author", true);
        } else {
            JsonObject target = switch (location) {
                case "properties" ->
                        citation.getAsJsonObject("properties");
                case "item" ->
                        item;
                default ->
                        citation;
            };
            target.addProperty(field, "17");
        }
        String name = ZoteroReferenceMark.PREFIX + citation + " RNDmodifier01";
        appendMark(document, name, "[native, p. 17]");
        XTextRange anchor = WriterReferenceMarks.getAnchor(document, name).orElseThrow();
        XPropertySet format = query(XPropertySet.class, anchor.getText().createTextCursorByRange(anchor));
        format.setPropertyValue("CharHeight", 18.0f);
        format.setPropertyValue("CharWeight", FontWeight.BOLD);
        format.setPropertyValue("CharPosture", FontSlant.ITALIC);
        format.setPropertyValue("CharColor", 0x345678);
        XTextCursor bibliography = document.getText().createTextCursor();
        bibliography.gotoEnd(false);
        bibliography.setString("\n");
        bibliography.collapseToEnd();
        bibliography.setString("Existing native bibliography");
        UnoTextSection.create(new NamedTextContentPlacement(document, "ZOTERO_BIBL unchanged", bibliography, true));
        String obsoleteProperty = "JabRef_citation_conversion:unrelated";
        UnoUserDefinedProperty.setStringProperty(document, obsoleteProperty, "not JSON");
        UnoUserDefinedProperty.setStringProperty(document, "JabRef_document_style", "CSL\nuntouched");
        UnoRedlines.setRecordChanges(document, true);
        BibEntry entry = nativeEntry("native");
        List<BibDatabase> databases = List.of(new BibDatabase(List.of(entry)));
        try (CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> result = "refresh".equals(operation)
                                                             ? engine.refresh(databases, style())
                                                             : engine.insert(List.of(entry), databases, style(), CitationType.AUTHORYEAR_PAR,
                    "", document.getText().createTextCursor(), true);
            assertEquals(List.of(), success(result));
            String linkedOrdinaryName = assertLinkedNativeCitation(document, ordinaryName, "[1]");
            String linkedModifierName = assertLinkedNativeCitation(document, name, expectedText);
            Map<String, String> citations = zoteroMarks(document);
            assertEquals("insert".equals(operation) ? 3 : 2, citations.size());
            Map<String, String> inserted = new LinkedHashMap<>(citations);
            inserted.remove(linkedOrdinaryName);
            inserted.remove(linkedModifierName);
            assertEquals("insert".equals(operation) ? List.of("[1]") : List.of(), List.copyOf(inserted.values()));
            assertEquals(List.of("ZOTERO_BIBL unchanged"), zoteroBibliographies(document));
            String bibliographyText = UnoTextSection.getAnchor(document, "ZOTERO_BIBL unchanged").orElseThrow().getString();
            assertEquals(List.of("1. Native linked title"), bibliographyText.lines().map(String::strip).filter(line -> !line.isEmpty()).toList());
            assertEquals(Optional.of("not JSON"), UnoUserDefinedProperty.getStringValue(document, obsoleteProperty));
            assertEquals(List.of(obsoleteProperty), UnoUserDefinedProperty.getListOfNames(document).stream().filter(property -> property.startsWith("JabRef_citation_conversion:")).toList());
            assertEquals(Optional.of("CSL\nuntouched"), UnoUserDefinedProperty.getStringValue(document, "JabRef_document_style"));
            assertTrue(UnoRedlines.getRecordChanges(document));
            DocumentSnapshot beforeExport = snapshot(document);
            CitedEntryExporter.ExportResult exported = success(engine.exportCited(databases));
            assertEquals(List.of("native"), exportedKeys(exported));
            assertEquals(List.of(), exported.unresolvedKeys());
            assertEquals(beforeExport, snapshot(document));
        }
        Map<String, String> expected = zoteroMarks(document);
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("native-modifier-" + operation + "-" + location + "-" + field + ".odt"));
        assertEquals(expected, zoteroMarks(reopened));
        assertEquals(Optional.of("not JSON"), UnoUserDefinedProperty.getStringValue(reopened, obsoleteProperty));
        assertEquals(List.of(obsoleteProperty), UnoUserDefinedProperty.getListOfNames(reopened).stream().filter(property -> property.startsWith("JabRef_citation_conversion:")).toList());
        try (CitationEngine engine = engine(reopened)) {
            assertEquals(List.of(), success(engine.refresh(databases, style())));
            assertEquals(expected, zoteroMarks(reopened));
            assertLinkedNativeCitation(reopened, ordinaryName, "[1]");
            assertLinkedNativeCitation(reopened, name, expectedText);
            assertEquals(Optional.of("not JSON"), UnoUserDefinedProperty.getStringValue(reopened, obsoleteProperty));
            assertEquals(List.of(obsoleteProperty), UnoUserDefinedProperty.getListOfNames(reopened).stream().filter(property -> property.startsWith("JabRef_citation_conversion:")).toList());
        }
    }

    private static Stream<Arguments> nativeModifierUpdates() {
        Stream.Builder<Arguments> cases = Stream.builder();
        for (String operation : List.of("refresh", "insert")) {
            for (String field : List.of("locator", "label", "prefix", "suffix")) {
                for (String location : List.of("citation", "properties", "item")) {
                    cases.add(Arguments.of(operation, location, field, "[1]"));
                }
            }
            cases.add(Arguments.of(operation, "item", "author-only", "[1]"));
            cases.add(Arguments.of(operation, "item", "suppress-author", "Native [1]"));
            cases.add(Arguments.of(operation, "item", "mixed-suppression", "Native [1], Native [1]"));
        }
        return cases.build();
    }

    @Test
    void ambiguousNativeDuplicatesRemainUnlinkedAndExportAsUnresolved() throws Exception {
        XTextDocument document = office.newDocument();
        String name = nativeMarkName();
        appendMark(document, name, "[native]");
        BibDatabase database = new BibDatabase(List.of(nativeEntry("first"), nativeEntry("second")));
        assertEquals(List.of(), ZoteroCitationLinker.planLinks(List.of(name), new BibDatabaseContext(database), new BibEntryTypesManager()).updates());
        assertEquals(Map.of(name, "[native]"), zoteroMarks(document));
        try (CitationEngine engine = engine(document)) {
            assertEquals(List.of("Zotero-600"), success(engine.refresh(List.of(database), style())));
            assertEquals(Map.of(name, "Unresolved(Zotero-600)"), zoteroMarks(document));
            assertNativeMetadata(name, List.of(NATIVE_URI));
            CitedEntryExporter.ExportResult exported = success(engine.exportCited(List.of(database)));
            assertEquals(List.of(), exportedKeys(exported));
            assertEquals(List.of("Zotero-600"), exported.unresolvedKeys());
        }
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("native-ambiguous.odt"));
        assertEquals(Map.of(name, "Unresolved(Zotero-600)"), zoteroMarks(reopened));
    }

    @Test
    void normalAndInTextCitationsRemainIndependentAcrossRefreshExportAndSaveReopen() throws Exception {
        XTextDocument document = office.newDocument();
        document.getText().setString("Citations: .\nBibliography below.");
        XTextCursor cursor = document.getText().createTextCursor();
        cursor.goRight((short) 11, false);
        BibDatabase database = database();
        List<String> keys = List.of("alpha", "beta", "gamma");
        List<CitationType> types = List.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.AUTHORYEAR_PAR);
        try (CitationEngine engine = engine(document)) {
            for (int index = 0; index < keys.size(); index++) {
                assertEquals(List.of(), success(engine.insert(List.of(database.getEntryByCitationKey(keys.get(index)).orElseThrow()),
                        List.of(database), style(), types.get(index), "", cursor, false)));
                cursor.setString(" | ");
                cursor.collapseToEnd();
            }
            assertEquals(Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT), engine.capabilities(style()).supportedTypes());
            assertEquals(List.of(), success(engine.refresh(List.of(database), style())));
            assertMixedCitations(document);
            assertBibliography(document);
            assertEquals(keys, exportedKeys(success(engine.exportCited(List.of(database)))));
            DocumentSnapshot before = snapshot(document);
            assertReason(Reason.UNSUPPORTED_TYPE, engine.insert(List.of(database.getEntryByCitationKey("ignored").orElseThrow()),
                    List.of(database), style(), CitationType.INVISIBLE_CIT, "", cursor, true));
            assertEquals(before, snapshot(document));
        }
        Map<String, String> expected = zoteroMarks(document);
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("mixed-zotero.odt"));
        assertEquals(expected, zoteroMarks(reopened));
        for (int refresh = 0; refresh < 3; refresh++) {
            try (CitationEngine engine = engine(reopened)) {
                assertEquals(List.of(), success(engine.refresh(List.of(database), style())));
                assertEquals(expected, zoteroMarks(reopened));
                assertMixedCitations(reopened);
                assertBibliography(reopened);
                CitedEntryExporter.ExportResult exported = success(engine.exportCited(List.of(database)));
                assertEquals(keys, exportedKeys(exported));
                assertEquals(List.of(), exported.unresolvedKeys());
                assertEquals(Optional.of(style().getStyleId()), ZoteroDocumentPreferences.findCitationStyle(reopened, List.of(style())).map(CitationStyle::getStyleId));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"refresh, ''", "refresh, \u200b", "refresh, \u200b\u200b", "insert, ''", "insert, \u200b", "insert, \u200b\u200b"})
    void blankOrdinaryNativeCitationsRenderAsNormalAcrossRefreshInsertionAndSaveReopen(String action, String initialText) throws Exception {
        XTextDocument document = office.newDocument();
        JsonObject citation = json(nativeMarkName());
        citation.getAsJsonObject("properties").addProperty("plainCitation", initialText);
        String name = ZoteroReferenceMark.PREFIX + citation + " RNDnative1234";
        appendMark(document, name, initialText);
        assertEquals(CitationType.AUTHORYEAR_PAR, ZoteroReferenceMark.parse(name).orElseThrow().citationType());
        BibEntry entry = nativeEntry("native");
        List<BibDatabase> databases = List.of(new BibDatabase(List.of(entry)));
        try (CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> result = "refresh".equals(action)
                                                             ? engine.refresh(databases, style())
                                                             : engine.insert(List.of(entry), databases, style(), CitationType.AUTHORYEAR_PAR,
                    "", document.getText().createTextCursor(), false);
            assertEquals(List.of(), success(result));
            assertLinkedNativeCitation(document, name, "[1]");
            assertEquals("insert".equals(action) ? 2 : 1, zoteroMarks(document).size());
            for (Map.Entry<String, String> mark : zoteroMarks(document).entrySet()) {
                assertEquals(CitationType.AUTHORYEAR_PAR, ZoteroReferenceMark.parse(mark.getKey()).orElseThrow().citationType());
                assertEquals("[1]", mark.getValue());
            }
            assertEquals(List.of("native"), exportedKeys(success(engine.exportCited(databases))));
        }
        Map<String, String> expected = zoteroMarks(document);
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("blank-native-" + action + ".odt"));
        assertEquals(expected, zoteroMarks(reopened));
        try (CitationEngine engine = engine(reopened)) {
            assertEquals(List.of(), success(engine.refresh(databases, style())));
            assertEquals(expected, zoteroMarks(reopened));
            assertEquals(List.of("native"), exportedKeys(success(engine.exportCited(databases))));
        }
    }

    @Test
    void nextZoteroInsertionConvertsCanonicalButStillIgnoresUnsupportedCidMarks() throws Exception {
        XTextDocument document = office.newDocument();
        appendMark(document, CANONICAL, "\u200b[canonical]\u200b");
        appendMark(document, LEGACY_CID, "[old CID]");
        XTextCursor cursor = document.getText().createTextCursor();
        cursor.gotoEnd(false);
        cursor.setString("\n");
        cursor.collapseToEnd();
        cursor.setString("Existing JabRef bibliography");
        UnoTextSection.create(new NamedTextContentPlacement(document, "JR_bib", cursor, true));
        Map<String, String> ignored = marks(document);
        UnoUserDefinedProperty.setStringProperty(document, "JabRef_document_style", "JSTYLE\nuntouched");
        UnoUserDefinedProperty.setStringProperty(document, "JabRef_page_info:stableIgnored:0", "p. 99");
        BibDatabase database = database();
        try (CitationEngine engine = engine(document)) {
            assertTrue(success(engine.requiresFormatConversion(style())));
            assertEquals(List.of(), success(engine.insert(List.of(database.getEntryByCitationKey("alpha").orElseThrow()), List.of(database),
                    style(), CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), true)));
            assertEquals(List.of(), success(engine.refresh(List.of(database), style())));
            assertEquals(List.of("alpha", "ignored"), exportedKeys(success(engine.exportCited(List.of(database)))).stream().sorted().toList());
        }
        ignored.remove(CANONICAL);
        assertEquals(ignored, marks(document).entrySet().stream().filter(entry -> !ZoteroReferenceMark.isZoteroReferenceMarkName(entry.getKey()))
                                             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        assertEquals("Existing JabRef bibliography", UnoTextSection.getAnchor(document, "JR_bib").orElseThrow().getString());
        assertEquals(1, zoteroBibliographies(document).size());
        assertEquals(Optional.empty(), UnoUserDefinedProperty.getStringValue(document, "JabRef_page_info:stableIgnored:0"));
        assertEquals(2, zoteroMarks(document).size());
        String converted = zoteroMarks(document).keySet().stream().filter(name -> ZoteroReferenceMark.parse(name).orElseThrow().citationKeys().equals(List.of("ignored"))).findFirst().orElseThrow();
        assertEquals(CitationType.AUTHORYEAR_PAR, ZoteroReferenceMark.parse(converted).orElseThrow().citationType());
        assertEquals(8, json(converted).get("citationID").getAsString().length());
        assertFalse(json(converted).has("properties"));
        assertFalse(json(converted).getAsJsonArray("citationItems").get(0).getAsJsonObject().has("locator"));
        assertNoConversionArchive(document);
        Map<String, String> chosenFormat = zoteroMarks(document);
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("zotero-isolation.odt"));
        assertEquals(chosenFormat, zoteroMarks(reopened));
        assertNoConversionArchive(reopened);
        assertEquals(Optional.empty(), UnoUserDefinedProperty.getStringValue(reopened, "JabRef_page_info:stableIgnored:0"));
        try (CitationEngine engine = engine(reopened)) {
            assertFalse(success(engine.requiresFormatConversion(style())));
            assertEquals(List.of(), success(engine.refresh(List.of(database), style())));
            assertEquals(List.of("alpha", "ignored"), exportedKeys(success(engine.exportCited(List.of(database)))).stream().sorted().toList());
        }
        assertEquals(ignored, marks(reopened).entrySet().stream().filter(entry -> !ZoteroReferenceMark.isZoteroReferenceMarkName(entry.getKey()))
                                             .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @ParameterizedTest
    @CsvSource({"0000000001, 2", "2147483646, 2147483647", "2147483648, 1", "9999999999999999999999999999999999999999, 1"})
    void nativeInsertionAllocatesBoundedIdsWithoutChangingForeignIds(String foreignId, String expectedId) throws Exception {
        XTextDocument document = office.newDocument();
        JsonObject citation = json(nativeMarkName());
        JsonObject item = citation.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        item.addProperty("id", foreignId);
        item.getAsJsonObject("itemData").addProperty("id", foreignId);
        appendMark(document, ZoteroReferenceMark.PREFIX + citation + " RNDnative1234", "[native]");
        BibDatabase selected = database();
        selected.insertEntry(nativeEntry("native"));
        try (CitationEngine engine = engine(document)) {
            success(engine.insert(List.of(selected.getEntryByCitationKey("alpha").orElseThrow()), List.of(selected), style(),
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), false));
        }
        for (String name : zoteroMarks(document).keySet()) {
            JsonObject renderedItem = json(name).getAsJsonArray("citationItems").get(0).getAsJsonObject();
            String key = ZoteroReferenceMark.parse(name).orElseThrow().citationKeys().getFirst();
            String expected = "native".equals(key) ? foreignId : expectedId;
            assertEquals(expected, renderedItem.get("id").getAsString());
            assertEquals(expected, renderedItem.getAsJsonObject("itemData").get("id").getAsString());
        }
    }

    @Test
    void exhaustedNativeIdsRejectInsertionWithoutWrappingOrChangingTheDocument() throws Exception {
        XTextDocument document = office.newDocument();
        JsonObject citation = json(nativeMarkName());
        JsonObject item = citation.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        item.addProperty("id", "2147483647");
        item.getAsJsonObject("itemData").addProperty("id", "2147483647");
        appendMark(document, ZoteroReferenceMark.PREFIX + citation + " RNDnative1234", "[native]");
        DocumentSnapshot before = snapshot(document);
        try (CitationEngine engine = engine(document)) {
            assertReason(Reason.INVALID_ENTRY, engine.insert(List.of(database().getEntryByCitationKey("alpha").orElseThrow()), List.of(database()), style(),
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), false));
        }
        assertEquals(before, snapshot(document));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void refreshAloneStillLeavesUniformSourceMarksAndJabRefStateUntouched(boolean canonicalSource) throws Exception {
        XTextDocument document = office.newDocument();
        appendMark(document, canonicalSource ? CANONICAL : LEGACY_JSTYLE, canonicalSource ? "\u200b[canonical]\u200b" : "[old JStyle]");
        appendMark(document, LEGACY_CID, "[old CID]");
        XTextCursor cursor = document.getText().createTextCursor();
        cursor.gotoEnd(false);
        cursor.setString("\n");
        cursor.collapseToEnd();
        cursor.setString("Existing JabRef bibliography");
        UnoTextSection.create(new NamedTextContentPlacement(document, "JR_bib", cursor, true));
        UnoUserDefinedProperty.setStringProperty(document, "JabRef_document_style", "JSTYLE\nuntouched");
        UnoUserDefinedProperty.setStringProperty(document, "JabRef_page_info:stableIgnored:0", "p. 99");
        Map<String, String> ignored = marks(document);
        try (CitationEngine engine = engine(document)) {
            assertEquals(canonicalSource, success(engine.requiresFormatConversion(style())));
            success(engine.refresh(List.of(database()), style()));
            assertEquals(List.of(), exportedKeys(success(engine.exportCited(List.of(database())))));
            assertEquals(canonicalSource, success(engine.requiresFormatConversion(style())));
        }
        assertIgnoredContent(document, ignored);
    }

    @ParameterizedTest
    @ValueSource(strings = {"jstyle", "bst"})
    void nonCslStylesUseCanonicalStorageAndPreserveNativeCitationsAndTheSavedMode(String family) throws Exception {
        XTextDocument document = office.newDocument();
        appendMark(document, nativeMarkName(), "[native]");
        Map<String, String> nativeMarks = zoteroMarks(document);
        OOStyle selectedStyle = "bst".equals(family) ? BstStyle.loadExternal(resource("title-only.bst"))
                                                     : new JStyle(JStyleLoader.DEFAULT_AUTHORYEAR_STYLE_PATH, LayoutFormatterPreferences.getDefault(), new JournalAbbreviationRepository());
        OpenOfficePreferences preferences = OpenOfficePreferences.getDefault();
        preferences.setZoteroCompatibilityMode(true);
        preferences.setCurrentStyle(selectedStyle);
        Optional.ofNullable(System.getenv("PANDOC_EXECUTABLE")).ifPresent(preferences::setPandocPath);
        BibEntryTypesManager entryTypes = new BibEntryTypesManager();
        BibDatabase database = database();
        try (CitationEngine engine = new CitationEngine(document, office.context(), preferences, entryTypes)) {
            assertFalse(success(engine.requiresFormatConversion(selectedStyle)));
            assertEquals(new RendererFactory(entryTypes, preferences.getPandocPath()).create(selectedStyle).capabilities(),
                    engine.capabilities(selectedStyle));
            assertEquals(List.of(), success(engine.refresh(List.of(database), selectedStyle)));
            assertEquals(nativeMarks, zoteroMarks(document));
            assertEquals(List.of(), success(engine.insert(List.of(database.getEntryByCitationKey("alpha").orElseThrow()), List.of(database), selectedStyle,
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), true)));

            List<String> canonicalMarks = marks(document).keySet().stream().filter(JabRefMarkCodec::isCanonicalMarkName).toList();
            assertEquals(1, canonicalMarks.size());
            assertEquals(List.of("alpha"), JabRefMarkCodec.decode(canonicalMarks.getFirst()).citationKeys());
            assertEquals(List.of("alpha"), exportedKeys(success(engine.exportCited(List.of(database)))));
            assertEquals(nativeMarks, zoteroMarks(document));
            assertTrue(preferences.getZoteroCompatibilityMode());
        }
    }

    @ParameterizedTest
    @EnumSource(value = CitationType.class, names = {"AUTHOR_ONLY", "YEAR_ONLY", "AUTHORYEAR_NOPAR", "INVISIBLE_CIT"})
    void unsupportedCitationTypesAreRejectedWithoutChangingExistingMarks(CitationType type) throws Exception {
        XTextDocument document = office.newDocument();
        appendMark(document, nativeMarkName(), "[native]");
        DocumentSnapshot before = snapshot(document);
        try (CitationEngine engine = engine(document)) {
            assertReason(Reason.UNSUPPORTED_TYPE, engine.insert(List.of(nativeEntry("native")), List.of(database()), style(), type,
                    "", document.getText().createTextCursor(), true));
        }
        assertEquals(before, snapshot(document));
    }

    @Test
    void pageInfoIsRejectedWithoutChangingTheRealDocument() throws Exception {
        XTextDocument document = office.newDocument();
        appendMark(document, nativeMarkName(), "[native]");
        DocumentSnapshot before = snapshot(document);
        try (CitationEngine engine = engine(document)) {
            assertReason(Reason.PAGE_INFO_NOT_SUPPORTED, engine.insert(List.of(nativeEntry("native")), List.of(database()), style(),
                    CitationType.AUTHORYEAR_PAR, "p. 5", document.getText().createTextCursor(), true));
            assertFalse(engine.capabilities(style()).supportsGroupEditing());
            assertFalse(engine.capabilities(style()).supportsPageInfo());
            assertTrue(engine.merge(List.of(database()), style()).isError());
            assertTrue(engine.separate(List.of(database()), style()).isError());
        }
        assertEquals(before, snapshot(document));
    }

    @ParameterizedTest
    @ValueSource(strings = {"insert", "refresh"})
    void invalidCslDoesNotApplyNativeLinksOrChangeTextFormattingOrProperties(String operation) throws Exception {
        XTextDocument document = office.newDocument();
        String name = nativeMarkName();
        appendMark(document, name, "[native citation]");
        XTextRange anchor = WriterReferenceMarks.getAnchor(document, name).orElseThrow();
        XPropertySet format = query(XPropertySet.class, anchor.getText().createTextCursorByRange(anchor));
        format.setPropertyValue("CharHeight", 18.0f);
        format.setPropertyValue("CharWeight", FontWeight.BOLD);
        format.setPropertyValue("CharPosture", FontSlant.ITALIC);
        format.setPropertyValue("CharColor", 0x345678);
        XTextContent original = query(XTextContent.class, query(XReferenceMarksSupplier.class, document).getReferenceMarks().getByName(name));
        BibEntry entry = nativeEntry("native");
        BibDatabase database = new BibDatabase(List.of(entry));
        assertEquals(1, ZoteroCitationLinker.planLinks(List.of(name), new BibDatabaseContext(database), new BibEntryTypesManager()).updates().size());
        DocumentSnapshot before = snapshot(document);
        CitationStyle invalid = new CitationStyle("invalid.csl", "invalid", "in-text", "Invalid", "", true, true, false, false, "not XML", false);
        try (CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> result = "refresh".equals(operation) ? engine.refresh(List.of(database), invalid)
                                                                                         : engine.insert(List.of(entry), List.of(database), invalid, CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), true);
            assertReason(Reason.DOCUMENT_FAILURE, result);
        }

        assertEquals(before, snapshot(document));
        assertTrue(UnoRuntime.areSame(original, query(XTextContent.class,
                query(XReferenceMarksSupplier.class, document).getReferenceMarks().getByName(name))));
        assertNativeMetadata(name, List.of(NATIVE_URI));
    }

    @ParameterizedTest
    @ValueSource(strings = {"zotero", "canonical", "legacy", "zotero-bibliography", "jabref-bibliography"})
    void insertionInsideManagedOrUnmanagedProtectedContentIsRejected(String target) throws Exception {
        XTextDocument document = office.newDocument();
        XTextRange anchor;
        if (target.endsWith("bibliography")) {
            String name = "jabref-bibliography".equals(target) ? "JR_bib" : "ZOTERO_BIBL {} CSL_BIBLIOGRAPHY RNDprotected1";
            anchor = createBibliography(document, name);
        } else {
            String name = switch (target) {
                case "canonical" ->
                        CANONICAL;
                case "legacy" ->
                        LEGACY_CID;
                default ->
                        nativeMarkName();
            };
            appendMark(document, name, "Protected citation");
            anchor = WriterReferenceMarks.getAnchor(document, name).orElseThrow();
        }
        XTextCursor cursor = inside(anchor);
        DocumentSnapshot before = snapshot(document);
        BibEntry entry = nativeEntry("native");
        try (CitationEngine engine = engine(document)) {
            assertReason(Reason.PROTECTED_RANGE, engine.insert(List.of(entry), List.of(new BibDatabase(List.of(entry))), style(),
                    CitationType.AUTHORYEAR_PAR, "", cursor, true));
        }

        assertEquals(before, snapshot(document));
    }

    @ParameterizedTest
    @CsvSource({"insert, false", "refresh, false", "insert, true", "refresh, true"})
    void pendingCitationDeletionsAreRejectedEvenAfterRecordingIsDisabled(String operation, boolean recording) throws Exception {
        XTextDocument document = office.newDocument();
        String name = nativeMarkName();
        appendMark(document, name, "[native citation]");
        UnoRedlines.setRecordChanges(document, true);
        XTextCursor deletion = inside(WriterReferenceMarks.getAnchor(document, name).orElseThrow());
        deletion.goRight((short) 2, true);
        deletion.setString("");
        assertFalse(UnoRedlines.getDeletedRanges(document).isEmpty());
        assertTrue(zoteroMarks(document).containsKey(name));
        UnoRedlines.setRecordChanges(document, recording);
        DocumentSnapshot before = snapshot(document);
        BibEntry entry = nativeEntry("native");
        BibDatabase database = new BibDatabase(List.of(entry));
        try (CitationEngine engine = engine(document)) {
            OOResult<List<String>, JabRefException> result = "refresh".equals(operation) ? engine.refresh(List.of(database), style())
                                                                                         : engine.insert(List.of(entry), List.of(database), style(), CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), true);
            assertReason(Reason.PENDING_DELETIONS, result);
        }

        assertEquals(before, snapshot(document));
        assertEquals(recording, UnoRedlines.getRecordChanges(document));
    }

    @Test
    void pendingBibliographyDeletionIsRejectedWithRecordingOff() throws Exception {
        XTextDocument document = office.newDocument();
        XTextRange anchor = createBibliography(document, "ZOTERO_BIBL {} CSL_BIBLIOGRAPHY RNDprotected1");
        UnoRedlines.setRecordChanges(document, true);
        XTextCursor deletion = inside(anchor);
        deletion.goRight((short) 2, true);
        deletion.setString("");
        assertFalse(UnoRedlines.getDeletedRanges(document).isEmpty());
        UnoRedlines.setRecordChanges(document, false);

        DocumentSnapshot before = snapshot(document);
        try (CitationEngine engine = engine(document)) {
            assertReason(Reason.PENDING_DELETIONS, engine.refresh(List.of(database()), style()));
        }

        assertEquals(before, snapshot(document));
        assertFalse(UnoRedlines.getRecordChanges(document));
    }

    @Test
    void insertionOverFootnoteAnchorCannotDestroyItsZoteroCitation() throws Exception {
        XTextDocument document = office.newDocument();
        XTextContent footnote = footnoteWithNativeCitation(document);
        XTextRange anchor = footnote.getAnchor();
        XTextCursor selection = anchor.getText().createTextCursorByRange(anchor);
        DocumentSnapshot before = snapshot(document);
        BibEntry entry = nativeEntry("native");
        try (CitationEngine engine = engine(document)) {
            assertReason(Reason.PROTECTED_RANGE, engine.insert(List.of(entry), List.of(new BibDatabase(List.of(entry))), style(),
                    CitationType.AUTHORYEAR_PAR, "", selection, true));
        }

        assertEquals(before, snapshot(document));
        assertEquals(1, query(XFootnotesSupplier.class, document).getFootnotes().getCount());
    }

    @Test
    void refreshRejectsBodyCitationOverFootnoteAnchorHoldingAnotherCitation() throws Exception {
        XTextDocument document = office.newDocument();
        XTextContent footnote = footnoteWithNativeCitation(document);
        String bodyName = nativeMarkName().replace("native-citation", "body-citation");
        WriterReferenceMarks.create(new NamedTextContentPlacement(document, bodyName, footnote.getAnchor(), true));
        DocumentSnapshot before = snapshot(document);
        try (CitationEngine engine = engine(document)) {
            assertReason(Reason.OVERLAPPING_RANGES, engine.refresh(List.of(new BibDatabase(List.of(nativeEntry("native")))), style()));
        }

        assertEquals(before, snapshot(document));
        assertEquals(1, query(XFootnotesSupplier.class, document).getFootnotes().getCount());
    }

    @Test
    void successfulNativeLinkUsesFullyRenderedSuperscriptWithoutShrinkingOnRefresh() throws Exception {
        XTextDocument document = office.newDocument();
        String original = nativeMarkName();
        appendMark(document, original, "[native]");
        XTextRange anchor = WriterReferenceMarks.getAnchor(document, original).orElseThrow();
        query(XPropertySet.class, anchor.getText().createTextCursorByRange(anchor)).setPropertyValue("CharHeight", 18.0f);
        CitationStyle superscript = CSLStyleUtils.createCitationStyleFromFile(resource("superscript.csl").toString()).orElseThrow();
        BibDatabase database = new BibDatabase(List.of(nativeEntry("native")));
        String linked = ZoteroCitationLinker.planLinks(List.of(original), new BibDatabaseContext(database), new BibEntryTypesManager()).updatedName(original);
        try (CitationEngine engine = engine(document)) {
            for (int refresh = 0; refresh < 3; refresh++) {
                assertEquals(List.of(), success(engine.refresh(List.of(database), superscript)));
                XTextRange range = WriterReferenceMarks.getAnchor(document, linked).orElseThrow();
                XTextCursor inner = inside(range);
                inner.goRight((short) 1, true);
                XPropertySet format = query(XPropertySet.class, inner);
                assertEquals((short) 33, format.getPropertyValue("CharEscapement"));
                assertEquals((byte) 58, format.getPropertyValue("CharEscapementHeight"));
                assertEquals(18.0f, format.getPropertyValue("CharHeight"));
            }
        }
        assertNativeMetadata(linked, List.of(NATIVE_URI, "http://www.jabref.org/native"));
    }

    private static XTextCursor inside(XTextRange range) {
        XTextCursor cursor = range.getText().createTextCursorByRange(range);
        cursor.collapseToStart();
        cursor.goRight((short) 1, false);
        return cursor;
    }

    private static XTextRange createBibliography(XTextDocument document, String name) throws Exception {
        document.getText().setString("Protected bibliography");
        XTextCursor cursor = document.getText().createTextCursor();
        cursor.gotoEnd(true);
        UnoTextSection.create(new NamedTextContentPlacement(document, name, cursor, true));
        return UnoTextSection.getAnchor(document, name).orElseThrow();
    }

    private XTextContent footnoteWithNativeCitation(XTextDocument document) throws Exception {
        document.getText().setString("Before after");
        XTextCursor body = document.getText().createTextCursor();
        body.goRight((short) 7, false);
        XTextContent footnote = query(XTextContent.class, query(XMultiServiceFactory.class, document).createInstance("com.sun.star.text.Footnote"));
        document.getText().insertTextContent(body, footnote, false);
        XText text = query(XText.class, footnote);
        XTextCursor cursor = text.createTextCursor();
        cursor.setString("[native]");
        WriterReferenceMarks.create(new NamedTextContentPlacement(document, nativeMarkName(), cursor, true));
        return footnote;
    }

    private CitationEngine engine(XTextDocument document) {
        OpenOfficePreferences preferences = OpenOfficePreferences.getDefault();
        preferences.setZoteroCompatibilityMode(true);
        preferences.setCslBibliographyBodyFormat("Standard");
        return new CitationEngine(document, office.context(), preferences, new BibEntryTypesManager());
    }

    private static CitationStyle style() throws URISyntaxException {
        return CSLStyleUtils.createCitationStyleFromFile(resource("zotero-numeric.csl").toString()).orElseThrow();
    }

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(Optional.ofNullable(ZoteroWriterTest.class.getResource(name)).orElseThrow().toURI());
    }

    private static String nativeMarkName() throws Exception {
        return ZoteroReferenceMark.PREFIX + Files.readString(resource("zotero-native.json")).strip() + " RNDnative1234";
    }

    private static BibEntry nativeEntry(String key) {
        return new BibEntry(StandardEntryType.Article).withCitationKey(key)
                                                      .withField(StandardField.DOI, "10.123/native").withField(StandardField.TITLE, "Native linked title")
                                                      .withField(StandardField.AUTHOR, "Native, Nina").withField(StandardField.YEAR, "2020");
    }

    private static BibDatabase database() {
        return new BibDatabase(List.of(
                new BibEntry().withCitationKey("alpha").withField(StandardField.TITLE, "Alpha title").withField(StandardField.AUTHOR, "Alpha, Alice").withField(StandardField.YEAR, "2020"),
                new BibEntry().withCitationKey("beta").withField(StandardField.TITLE, "Beta title").withField(StandardField.AUTHOR, "Beta, Bob").withField(StandardField.YEAR, "2021"),
                new BibEntry().withCitationKey("gamma").withField(StandardField.TITLE, "Gamma title").withField(StandardField.AUTHOR, "Gamma, Grace").withField(StandardField.YEAR, "2022"),
                new BibEntry().withCitationKey("ignored").withField(StandardField.TITLE, "Ignored title")));
    }

    private static void appendMark(XTextDocument document, String name, String content) throws Exception {
        XTextCursor cursor = document.getText().createTextCursor();
        cursor.gotoEnd(false);
        cursor.setString("\n");
        cursor.collapseToEnd();
        cursor.setString(content);
        WriterReferenceMarks.create(new NamedTextContentPlacement(document, name, cursor, true));
    }

    private static Map<String, String> marks(XTextDocument document) throws Exception {
        XNameAccess access = query(XReferenceMarksSupplier.class, document).getReferenceMarks();
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : Arrays.stream(access.getElementNames()).sorted().toList()) {
            result.put(name, query(XTextContent.class, access.getByName(name)).getAnchor().getString());
        }
        return result;
    }

    private static Map<String, String> zoteroMarks(XTextDocument document) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        marks(document).forEach((name, text) -> {
            if (ZoteroReferenceMark.isZoteroReferenceMarkName(name)) {
                result.put(name, text);
            }
        });
        return result;
    }

    private static JsonObject json(String mark) {
        return JsonParser.parseString(ZoteroReferenceMark.getCSLJson(mark)).getAsJsonObject();
    }

    private static List<String> uris(JsonObject item) {
        return item.getAsJsonArray("uris").asList().stream().map(uri -> uri.getAsString()).toList();
    }

    private static String assertLinkedNativeCitation(XTextDocument document, String originalName, String expectedText) throws Exception {
        JsonObject expected = json(originalName);
        for (JsonElement element : expected.getAsJsonArray("citationItems")) {
            element.getAsJsonObject().getAsJsonArray("uris").add("http://www.jabref.org/native");
        }
        Map<String, String> citations = zoteroMarks(document);
        String citationId = expected.get("citationID").getAsString();
        String linkedName = citations.keySet().stream()
                                     .filter(name -> citationId.equals(json(name).get("citationID").getAsString())).findFirst().orElseThrow();
        assertEquals(expected, json(linkedName));
        assertEquals(ZoteroReferenceMark.parse(originalName).orElseThrow().uniqueId(), ZoteroReferenceMark.parse(linkedName).orElseThrow().uniqueId());
        assertEquals(expectedText, citations.get(linkedName));
        return linkedName;
    }

    private static void assertNativeMetadata(String name, List<String> expectedUris) {
        JsonObject citation = json(name);
        assertEquals("native-citation", citation.get("citationID").getAsString());
        assertEquals("keep native metadata", citation.getAsJsonObject("properties").get("custom").getAsString());
        assertEquals("https://github.com/citation-style-language/schema/raw/master/csl-citation.json", citation.get("schema").getAsString());
        JsonObject item = citation.getAsJsonArray("citationItems").get(0).getAsJsonObject();
        assertEquals(expectedUris, uris(item));
        assertEquals("Original archive", item.getAsJsonObject("itemData").get("archive-place").getAsString());
    }

    private static void assertMixedCitations(XTextDocument document) throws Exception {
        Map<String, String> texts = new LinkedHashMap<>();
        for (Map.Entry<String, String> mark : zoteroMarks(document).entrySet()) {
            ZoteroReferenceMark parsed = ZoteroReferenceMark.parse(mark.getKey()).orElseThrow();
            assertEquals(1, parsed.citationKeys().size());
            String key = parsed.citationKeys().getFirst();
            texts.put(key, mark.getValue());
            JsonObject item = json(mark.getKey()).getAsJsonArray("citationItems").get(0).getAsJsonObject();
            assertEquals(List.of("http://www.jabref.org/" + key), uris(item));
            if ("beta".equals(key)) {
                assertTrue(item.get("suppress-author").getAsBoolean());
                assertEquals(CitationType.AUTHORYEAR_INTEXT, parsed.citationType());
            } else {
                assertFalse(item.has("suppress-author"));
                assertEquals(CitationType.AUTHORYEAR_PAR, parsed.citationType());
            }
        }
        assertEquals(Map.of("alpha", "[1]", "beta", "Beta [2]", "gamma", "[3]"), texts);
    }

    private static void assertBibliography(XTextDocument document) throws Exception {
        List<String> sections = zoteroBibliographies(document);
        assertEquals(1, sections.size());
        assertTrue(BIBLIOGRAPHY_NAME.matcher(sections.getFirst()).matches());
        String text = UnoTextSection.getAnchor(document, sections.getFirst()).orElseThrow().getString();
        int previous = -1;
        for (String entry : List.of("1. Alpha title", "2. Beta title", "3. Gamma title")) {
            int index = text.indexOf(entry);
            assertTrue(index > previous, () -> "Missing or misordered Zotero bibliography entry: " + entry + " in " + text);
            assertEquals(index, text.lastIndexOf(entry));
            previous = index;
        }
    }

    private static List<String> zoteroBibliographies(XTextDocument document) throws Exception {
        return Arrays.stream(UnoTextSection.getNameAccess(document).getElementNames()).filter(name -> name.startsWith("ZOTERO_BIBL ")).toList();
    }

    private static void assertIgnoredContent(XTextDocument document, Map<String, String> ignored) throws Exception {
        Map<String, String> remaining = new LinkedHashMap<>();
        marks(document).forEach((name, content) -> {
            if (!ZoteroReferenceMark.isZoteroReferenceMarkName(name)) {
                remaining.put(name, content);
            }
        });
        assertEquals(ignored, remaining);
        assertEquals("Existing JabRef bibliography", UnoTextSection.getAnchor(document, "JR_bib").orElseThrow().getString());
        assertEquals(Optional.of("JSTYLE\nuntouched"), UnoUserDefinedProperty.getStringValue(document, "JabRef_document_style"));
        assertEquals(Optional.of("p. 99"), UnoUserDefinedProperty.getStringValue(document, "JabRef_page_info:stableIgnored:0"));
        assertEquals(1, zoteroBibliographies(document).size());
    }

    private static <T> T success(OOResult<T, JabRefException> result) {
        if (result.isError()) {
            fail("Zotero engine operation failed", result.getError());
        }
        return result.get();
    }

    private static void assertReason(Reason expected, OOResult<?, JabRefException> result) {
        assertTrue(result.isError());
        assertEquals(expected, assertInstanceOf(StrategyException.class, result.getError()).reason());
    }

    private static List<String> exportedKeys(CitedEntryExporter.ExportResult result) {
        return result.database().getEntries().stream().map(entry -> entry.getCitationKey().orElseThrow()).toList();
    }

    private static List<Object> formatOf(XTextRange range) throws Exception {
        XTextCursor cursor = range.getText().createTextCursorByRange(range);
        XPropertySet properties = query(XPropertySet.class, cursor);
        return List.of(properties.getPropertyValue("CharHeight"), properties.getPropertyValue("CharWeight"),
                properties.getPropertyValue("CharPosture"), properties.getPropertyValue("CharColor"),
                properties.getPropertyValue("CharEscapement"), properties.getPropertyValue("CharEscapementHeight"), properties.getPropertyValue("CharStyleName"));
    }

    private static void assertNoConversionArchive(XTextDocument document) throws Exception {
        assertEquals(List.of(), UnoUserDefinedProperty.getListOfNames(document).stream().filter(name -> name.startsWith("JabRef_citation_conversion:")).toList());
    }

    private static DocumentSnapshot snapshot(XTextDocument document) throws Exception {
        Map<String, String> sections = new LinkedHashMap<>();
        for (String name : UnoTextSection.getNameAccess(document).getElementNames()) {
            sections.put(name, UnoTextSection.getAnchor(document, name).orElseThrow().getString());
        }
        XPropertySet values = query(XPropertySet.class, query(XDocumentPropertiesSupplier.class, document).getDocumentProperties().getUserDefinedProperties());
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String name : UnoUserDefinedProperty.getListOfNames(document)) {
            properties.put(name, values.getPropertyValue(name));
        }
        Map<String, List<Object>> formatting = new LinkedHashMap<>();
        for (String name : marks(document).keySet()) {
            formatting.put(name, formatOf(WriterReferenceMarks.getAnchor(document, name).orElseThrow()));
        }
        return new DocumentSnapshot(document.getText().getString(), marks(document), sections, properties, formatting, document.hasControllersLocked());
    }

    @NullMarked
    private record DocumentSnapshot(String body, Map<String, String> marks, Map<String, String> sections,
                                    Map<String, Object> properties, Map<String, List<Object>> formatting, boolean controllersLocked) {
    }
}
