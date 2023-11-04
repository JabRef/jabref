package org.jabref.logic.exporter;

import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.SaveOrder;

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
        exporter = new TemplateExporter(
                "DocBook 5.1",
                "docbook5",
                "docbook5",
                null,
                StandardFileType.XML,
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS),
                SaveOrder.getDefaultSaveOrder());

        LocalDate myDate = LocalDate.of(2018, 1, 1);

        xmlFile = Path.of(DocBook5ExporterTest.class.getResource("Docbook5ExportFormat.xml").toURI());
        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "my paper title")
                .withField(StandardField.AUTHOR, "Stefan Kolb and Tobias Diez")
                .withField(StandardField.ISBN, "1-2-34")
                .withCitationKey("mykey")
                .withDate(new Date(myDate));
        entries = Collections.singletonList(entry);
    }

    @Test
    void testPerformExportForSingleEntry(@TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");

        exporter.export(databaseContext, path, entries);

        Builder control = Input.from(Files.newInputStream(xmlFile));
        Builder test = Input.from(Files.newInputStream(path));

        assertThat(test, CompareMatcher.isSimilarTo(control)
                                       .normalizeWhitespace()
                                       .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }
}
