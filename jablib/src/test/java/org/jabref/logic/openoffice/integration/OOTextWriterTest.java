package org.jabref.logic.openoffice.integration;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.JabRefException;
import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.engine.CitationEngine;
import org.jabref.logic.openoffice.engine.MarkCodec;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.OOTextWriter;
import org.jabref.logic.openoffice.uno.UnoTextSection;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.awt.FontSlant;
import com.sun.star.awt.FontWeight;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
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
import org.junit.jupiter.params.provider.ValueSource;

import static org.jabref.logic.openoffice.integration.HeadlessLibreOffice.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@NullMarked
@Tag("LibreOfficeTest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("Localization.lang")
@Timeout(value = 180, unit = TimeUnit.SECONDS)
class OOTextWriterTest {
    @TempDir
    Path temporaryDirectory;

    private int artifactIndex;
    private final HeadlessLibreOffice office = new HeadlessLibreOffice(
            Path.of(System.getProperty("jabref.writer.artifacts", "build/writer-integration")).resolve("entity-contract"));

    @BeforeAll
    void startWriter(@TempDir Path profileDirectory) throws Exception {
        office.start(profileDirectory.resolve("profile"));
    }

    @AfterEach
    void preserveDocuments(TestInfo info) throws com.sun.star.io.IOException {
        office.saveAndCloseDocuments(info.getTestMethod().orElseThrow().getName() + "-" + artifactIndex++);
    }

    @AfterAll
    void stopWriter() throws Exception {
        office.close();
    }

    @Test
    void taggedTextDecodesEntitiesWithoutReinterpretingEscapedTagsAsFormatting() throws Exception {
        XTextDocument document = office.newDocument();
        String expected = "Müller & <i>literal</i>";
        OOTextWriter.write(document, document.getText().createTextCursor(),
                OOText.fromString("<b>M&uuml;ller &amp; &lt;i&gt;literal&lt;/i&gt;</b>"));

        assertEquals(expected, document.getText().getString());
        assertCharacterFormatting(select(document, expected), FontWeight.BOLD, FontSlant.NONE);
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("tagged-entities.odt"));
        assertEquals(expected, reopened.getText().getString());
        assertCharacterFormatting(select(reopened, expected), FontWeight.BOLD, FontSlant.NONE);
    }

    @Test
    void trailingTextDecodesEntitiesExactlyOnceAfterTheLastFormattingTag() throws Exception {
        XTextDocument document = office.newDocument();
        String trailing = " Müller & <i>literal</i> &lt;b&gt;";
        OOTextWriter.write(document, document.getText().createTextCursor(),
                OOText.fromString("<b>Bold</b> M&uuml;ller &amp; &lt;i&gt;literal&lt;/i&gt; &amp;lt;b&amp;gt;"));

        assertEquals("Bold" + trailing, document.getText().getString());
        assertCharacterFormatting(select(document, "Bold"), FontWeight.BOLD, FontSlant.NONE);
        assertCharacterFormatting(select(document, trailing), FontWeight.NORMAL, FontSlant.NONE);
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("trailing-entities.odt"));
        assertEquals("Bold" + trailing, reopened.getText().getString());
        assertCharacterFormatting(select(reopened, trailing), FontWeight.NORMAL, FontSlant.NONE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"csl", "bst"})
    void bibliographyHeadingKeepsAmpersandsAndHtmlLookingTextLiteral(String family) throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences();
        String heading = "Sources & <b>literal</b> &amp;";
        preferences.setCslBibliographyTitle(heading);
        BibEntry entry = entry();
        BibDatabase database = new BibDatabase(List.of(entry));
        try (CitationEngine engine = new CitationEngine(document, office.context(), preferences, new BibEntryTypesManager())) {
            assertEquals(List.of(), success(engine.insert(List.of(entry), List.of(database), style(family),
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), true)));
            assertHeading(document, heading);
            assertEquals(List.of(), success(engine.refresh(List.of(database), style(family))));
            assertHeading(document, heading);
        }
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve(family + "-literal-heading.odt"));
        assertHeading(reopened, heading);
    }

    @Test
    void bstAuthorYearCitationWritesAccentedNamesAndAmpersandsAsPlainText() throws Exception {
        XTextDocument document = office.newDocument();
        OpenOfficePreferences preferences = preferences();
        preferences.setBstCitationFormat(BstCitationFormat.AUTHOR_YEAR);
        BibEntry entry = entry();
        BibDatabase database = new BibDatabase(List.of(entry));
        String expected = "(Müller & Söhne, 2020)";
        try (CitationEngine engine = new CitationEngine(document, office.context(), preferences, new BibEntryTypesManager())) {
            assertEquals(List.of(), success(engine.insert(List.of(entry), List.of(database), style("bst"),
                    CitationType.AUTHORYEAR_PAR, "", document.getText().createTextCursor(), true)));
            assertCanonicalCitation(document, expected);
            assertEquals(List.of(), success(engine.refresh(List.of(database), style("bst"))));
            assertCanonicalCitation(document, expected);
        }
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("bst-author-entities.odt"));
        assertCanonicalCitation(reopened, expected);
    }

    @Test
    void cslNumericInTextPrefixWritesAccentedNamesAndAmpersandsAsPlainText() throws Exception {
        XTextDocument document = office.newDocument();
        BibEntry entry = entry();
        BibDatabase database = new BibDatabase(List.of(entry));
        String expected = "Müller & Söhne [1]";
        try (CitationEngine engine = new CitationEngine(document, office.context(), preferences(), new BibEntryTypesManager())) {
            assertEquals(List.of(), success(engine.insert(List.of(entry), List.of(database), style("csl"),
                    CitationType.AUTHORYEAR_INTEXT, "", document.getText().createTextCursor(), true)));
            assertCanonicalCitation(document, expected);
            assertEquals(List.of(), success(engine.refresh(List.of(database), style("csl"))));
            assertCanonicalCitation(document, expected);
        }
        XTextDocument reopened = office.saveAndReload(document, temporaryDirectory.resolve("csl-prefix-entities.odt"));
        assertCanonicalCitation(reopened, expected);
    }

    private static OpenOfficePreferences preferences() {
        OpenOfficePreferences preferences = OpenOfficePreferences.getDefault();
        preferences.setCslBibliographyHeaderFormat("Standard");
        preferences.setCslBibliographyBodyFormat("Standard");
        preferences.setPandocPath(Optional.ofNullable(System.getenv("PANDOC_EXECUTABLE")).filter(value -> !value.isBlank()).orElse("pandoc"));
        return preferences;
    }

    private static BibEntry entry() {
        return new BibEntry().withCitationKey("mueller").withField(StandardField.AUTHOR, "Müller & Söhne, Anna")
                             .withField(StandardField.YEAR, "2020").withField(StandardField.TITLE, "Entity contract entry");
    }

    private static OOStyle style(String family) throws Exception {
        return "bst".equals(family) ? BstStyle.loadExternal(resource("golden.bst"))
                                    : CSLStyleUtils.createCitationStyleFromFile(resource("zotero-numeric.csl").toString()).orElseThrow();
    }

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(Optional.ofNullable(OOTextWriterTest.class.getResource(name)).orElseThrow().toURI());
    }

    private static void assertHeading(XTextDocument document, String heading) throws Exception {
        XTextRange bibliography = UnoTextSection.getAnchor(document, "JR_bib").orElseThrow();
        int start = bibliography.getString().indexOf(heading);
        assertTrue(start >= 0, () -> "Missing literal bibliography heading in " + bibliography.getString());
        assertEquals(start, bibliography.getString().lastIndexOf(heading));
        assertCharacterFormatting(select(document, heading), FontWeight.NORMAL, FontSlant.NONE);
    }

    private static void assertCanonicalCitation(XTextDocument document, String expected) throws Exception {
        XNameAccess marks = query(XReferenceMarksSupplier.class, document).getReferenceMarks();
        List<String> names = Arrays.stream(marks.getElementNames()).filter(MarkCodec::isCanonicalMarkName).toList();
        assertEquals(1, names.size());
        XTextRange anchor = query(XTextContent.class, marks.getByName(names.getFirst())).getAnchor();
        assertEquals("\u200b" + expected + "\u200b", anchor.getString());
        assertCharacterFormatting(select(document, expected), FontWeight.NORMAL, FontSlant.NONE);
    }

    private static XTextCursor select(XTextDocument document, String text) throws Exception {
        // Section anchors can expose a paragraph boundary that is not a character at their start.
        XSearchable search = query(XSearchable.class, document);
        XSearchDescriptor descriptor = search.createSearchDescriptor();
        descriptor.setSearchString(text);
        descriptor.setPropertyValue("SearchRegularExpression", false);
        descriptor.setPropertyValue("SearchCaseSensitive", true);
        XIndexAccess matches = search.findAll(descriptor);
        assertEquals(1, matches.getCount(), "Expected exactly one literal occurrence of " + text);
        XTextRange range = query(XTextRange.class, matches.getByIndex(0));
        XTextCursor cursor = range.getText().createTextCursorByRange(range);
        assertEquals(text, cursor.getString());
        return cursor;
    }

    private static void assertCharacterFormatting(XTextCursor cursor, float weight, FontSlant posture) throws Exception {
        XPropertySet properties = query(XPropertySet.class, cursor);
        assertEquals(weight, properties.getPropertyValue("CharWeight"));
        assertEquals(posture, properties.getPropertyValue("CharPosture"));
    }

    private static <T> T success(OOResult<T, JabRefException> result) {
        if (result.isError()) {
            fail("Entity-contract engine operation failed", result.getError());
        }
        return result.get();
    }
}
