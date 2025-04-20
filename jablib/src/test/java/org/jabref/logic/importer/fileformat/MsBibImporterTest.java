package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.Importer;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MsBibImporterTest {

    Importer importer = new MsBibImporter();

    @Test
    final void isNotRecognizedFormat() throws Exception {
        List<String> notAccepted = Arrays.asList("CopacImporterTest1.txt", "IsiImporterTest1.isi",
                "IsiImporterTestInspec.isi", "emptyFile.xml", "IsiImporterTestWOS.isi");
        for (String s : notAccepted) {
            Path file = Path.of(MsBibImporter.class.getResource(s).toURI());
            assertFalse(importer.isRecognizedFormat(file));
        }
    }

    @Test
    final void importEntriesEmpty() throws IOException, URISyntaxException {
        Path file = Path.of(MsBibImporter.class.getResource("EmptyMsBib_Test.xml").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(Collections.emptyList(), entries);
    }

    @Test
    final void importEntriesNotRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(MsBibImporter.class.getResource("CopacImporterTest1.txt").toURI());
        List<BibEntry> entries = importer.importDatabase(file).getDatabase().getEntries();
        assertEquals(0, entries.size());
    }

    @Test
    final void getCommandLineId() {
        assertEquals("msbib", importer.getId());
    }
}
