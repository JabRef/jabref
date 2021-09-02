package org.jabref.logic.exporter;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.xmlunit.builder.Input;
import org.xmlunit.builder.Input.Builder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class DocBook5ExporterTest {

    public BibDatabaseContext databaseContext;
    public Charset charset;
    public List<BibEntry> entries;

    private Path xmlFile;
    private Exporter exporter;

    @BeforeEach
    void setUp() throws URISyntaxException {
        xmlFile = Path.of(DocBook5ExporterTest.class.getResource("Docbook5ExportFormat.xml").toURI());

        List<TemplateExporter> customFormats = new ArrayList<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        BibEntryTypesManager entryTypesManager = mock(BibEntryTypesManager.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences, BibDatabaseMode.BIBTEX, entryTypesManager);

        exporter = exporterFactory.getExporterByName("docbook5").get();

        LocalDate myDate = LocalDate.of(2018, 1, 1);

        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        BibEntry entry = new BibEntry(StandardEntryType.Book);
        entry.setField(StandardField.TITLE, "my paper title");
        entry.setField(StandardField.AUTHOR, "Stefan Kolb and Tobias Diez");
        entry.setField(StandardField.ISBN, "1-2-34");
        entry.setCitationKey("mykey");
        entry.setDate(new org.jabref.model.entry.Date(myDate));
        entries = Collections.singletonList(entry);
    }

    @Test
    void testPerformExportForSingleEntry(@TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");

        exporter.export(databaseContext, path, charset, entries);

        Builder control = Input.from(Files.newInputStream(xmlFile));
        Builder test = Input.from(Files.newInputStream(path));

        assertThat(test, CompareMatcher.isSimilarTo(control)
                                       .normalizeWhitespace()
                                       .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }
}
