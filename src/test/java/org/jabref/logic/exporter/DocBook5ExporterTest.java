package org.jabref.logic.exporter;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;

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
    private Exporter exportFormat;

    @BeforeEach
    void setUp() throws URISyntaxException {
        xmlFile = Paths.get(DocBook5ExporterTest.class.getResource("Docbook5ExportFormat.xml").toURI());

        List<TemplateExporter> customFormats = new ArrayList<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences);

        exportFormat = exporterFactory.getExporterByName("docbook5").get();

        LocalDate myDate = LocalDate.of(2018, 1, 1);

        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        BibEntry entry = new BibEntry(BibtexEntryTypes.BOOK);
        entry.setField(FieldName.TITLE, "my paper title");
        entry.setField(FieldName.AUTHOR, "Stefan Kolb and Tobias Diez");
        entry.setField(FieldName.ISBN, "1-2-34");
        entry.setCiteKey("mykey");
        entry.setDate(new org.jabref.model.entry.Date(myDate));
        entries = Arrays.asList(entry);
    }

    @Test
    void testPerformExportForSingleEntry(@TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");

        exportFormat.export(databaseContext, path, charset, entries);

        Builder control = Input.from(Files.newInputStream(xmlFile));
        Builder test = Input.from(Files.newInputStream(path));

        assertThat(test, CompareMatcher.isSimilarTo(control)
                                       .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }
}
