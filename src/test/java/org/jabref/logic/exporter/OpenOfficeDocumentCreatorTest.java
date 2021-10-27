package org.jabref.logic.exporter;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class OpenOfficeDocumentCreatorTest {
    private static final String OPEN_OFFICE_EXPORTER_ID = "oocalc";

    public BibDatabaseContext databaseContext;
    public Charset charset;
    public List<BibEntry> entries;

    private Path xmlFile;
    private Exporter exporter;

    @BeforeEach
    void setUp() throws URISyntaxException {
        xmlFile = Path.of(OpenOfficeDocumentCreatorTest.class.getResource("OldOpenOfficeCalcExportFormatContentSingleEntry.xml").toURI());

        List<TemplateExporter> customFormats = new ArrayList<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        BibEntryTypesManager entryTypesManager = mock(BibEntryTypesManager.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences, BibDatabaseMode.BIBTEX, entryTypesManager);

        exporter = exporterFactory.getExporterByName(OPEN_OFFICE_EXPORTER_ID).get();

        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;

        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.ADDRESS, "New York, NY, USA");
        entry.setField(StandardField.TITLE, "Design and usability in security systems: daily life as a context of use?");
        entry.setField(StandardField.AUTHOR, "Tony Clear");
        entry.setField(StandardField.ISSN, "0097-8418");
        entry.setField(StandardField.DOI, "http://doi.acm.org/10.1145/820127.820136");
        entry.setField(StandardField.JOURNAL, "SIGCSE Bull.");
        entry.setField(StandardField.NUMBER, "4");
        entry.setField(StandardField.PAGES, "13--14");
        entry.setField(StandardField.PUBLISHER, "ACM");
        entry.setField(StandardField.VOLUME, "34");
        entry.setField(StandardField.YEAR, "2002");

        entries = Collections.singletonList(entry);
    }

    @Test
    void testPerformExportForSingleEntry(@TempDir Path testFolder) throws Exception {
        Path zipPath = testFolder.resolve("OpenOfficeRandomNamedFile");

        exporter.export(databaseContext, zipPath, charset, entries);

        Path unzipFolder = testFolder.resolve("unzipFolder");
        unzipContentXml(zipPath, testFolder.resolve(unzipFolder));
        Path contentXmlPath = unzipFolder.resolve("content.xml");

        Input.Builder control = Input.from(Files.newInputStream(xmlFile));
        Input.Builder test = Input.from(Files.newInputStream(contentXmlPath));

        assertThat(test, CompareMatcher.isSimilarTo(control)
                                       .normalizeWhitespace()
                                       .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)).throwComparisonFailure());
    }

    private static void unzipContentXml(Path zipFile, Path unzipFolder) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                boolean isContentXml = zipEntry.getName().equals("content.xml");

                Path newPath = zipSlipProtect(zipEntry, unzipFolder);

                if (isContentXml) {
                    if (newPath.getParent() != null) {
                        if (Files.notExists(newPath.getParent())) {
                            Files.createDirectories(newPath.getParent());
                        }
                    }
                    Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING);
                }

                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    // protect zip slip attack: https://snyk.io/research/zip-slip-vulnerability
    private static Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
        Path targetDirResolved = targetDir.resolve(zipEntry.getName());
        Path normalizePath = targetDirResolved.normalize();
        if (!normalizePath.startsWith(targetDir)) {
            throw new IOException("Bad zip entry: " + zipEntry.getName());
        }
        return normalizePath;
    }
}
