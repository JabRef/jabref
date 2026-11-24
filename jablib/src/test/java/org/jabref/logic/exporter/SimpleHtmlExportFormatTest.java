package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@NullMarked
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("exporter")
class SimpleHtmlExportFormatTest {
    private final Exporter exporter = new TemplateExporter("Simple HTML",
            "simplehtml",
            "simplehtml",
            "",
            StandardFileType.HTML,
            mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS),
            SaveOrder.getDefaultSaveOrder());

    @Test
    void authorFirstNamesAreExportedAsInitials(@TempDir Path testFolder) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        BibEntry entry = new BibEntry()
                .withCitationKey("mykey")
                .withField(StandardField.AUTHOR, "Kolb, Stefan Anton")
                .withField(StandardField.TITLE, "my paper title");
        Path path = testFolder.resolve("export.html");

        exporter.export(new BibDatabaseContext(), path, List.of(entry));

        String content = Files.readString(path);
        assertTrue(content.contains("<dd>Kolb, S.A.</dd>"), content);
    }
}
