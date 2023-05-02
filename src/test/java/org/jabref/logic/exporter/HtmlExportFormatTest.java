package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HtmlExportFormatTest {
    public BibDatabaseContext databaseContext;
    public Charset charset;
    public List<BibEntry> entries;
    private Exporter exportFormat;

    @BeforeEach
    public void setUp() {
        SaveConfiguration saveConfiguration = mock(SaveConfiguration.class);
        when(saveConfiguration.getSaveOrder()).thenReturn(SaveOrder.getDefaultSaveOrder());

        ExporterFactory exporterFactory = ExporterFactory.create(
                new ArrayList<>(),
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS),
                saveConfiguration,
                mock(XmpPreferences.class),
                mock(FieldPreferences.class),
                BibDatabaseMode.BIBTEX,
                mock(BibEntryTypesManager.class));

        exportFormat = exporterFactory.getExporterByName("html").get();

        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "my paper title");
        entry.setField(StandardField.AUTHOR, "Stefan Kolb");
        entry.setCitationKey("mykey");
        entries = Arrays.asList(entry);
    }

    @AfterEach
    public void tearDown() {
        exportFormat = null;
    }

    @Test
    public void emitWellFormedHtml(@TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");
        exportFormat.export(databaseContext, path, entries);
        List<String> lines = Files.readAllLines(path);
        assertEquals("</html>", lines.get(lines.size() - 1));
    }
}
