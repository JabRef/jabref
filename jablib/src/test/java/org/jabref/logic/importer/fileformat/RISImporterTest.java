package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RISImporterTest {

    private RisImporter importer = new RisImporter();

    @Test
    void ifNotRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(RISImporterTest.class.getResource("RisImporterCorrupted.ris").toURI());
        assertFalse(importer.isRecognizedFormat(file));
    }

    @Test
    void accessionNumberDoesNotOverwriteIssueNumber() throws IOException {
        String ris = """
                TY  - JOUR
                VL  - 28
                IS  - 1
                AN  - 26957754
                ER  -
                """;

        BibEntry entry = importer.importDatabase(new BufferedReader(new StringReader(ris)))
                                  .getDatabase().getEntries().getFirst();

        assertEquals(Optional.of("1"), entry.getField(StandardField.NUMBER));
        assertEquals(Optional.of("26957754"), entry.getField(new UnknownField("accession-number")));
    }
}
