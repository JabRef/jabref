package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    void serialNumberOfABookIsImportedAsIsbn() throws IOException {
        BibEntry entry = importOne("""
                TY  - BOOK
                ED  - Kempf, Ute
                SN  - ISBN 978-3-932869-19-8
                TI  - Gender-Effekte. Wie Frauen die Technik von morgen gestalten
                VL  - Band 19
                ER  -
                """);

        assertEquals(StandardEntryType.Book, entry.getType());
        assertEquals(Optional.of("ISBN 978-3-932869-19-8"), entry.getField(StandardField.ISBN));
        assertEquals(Optional.empty(), entry.getField(StandardField.ISSN));
    }

    @Test
    void serialNumberOfABookChapterIsImportedAsIsbn() throws IOException {
        BibEntry entry = importOne("""
                TY  - CHAP
                SN  - 978-1-4822-5326-9
                TI  - A chapter
                ER  -
                """);

        assertEquals(StandardEntryType.InCollection, entry.getType());
        assertEquals(Optional.of("978-1-4822-5326-9"), entry.getField(StandardField.ISBN));
        assertEquals(Optional.empty(), entry.getField(StandardField.ISSN));
    }

    @Test
    void serialNumberIsImportedAsIsbnWhenItComesBeforeTheType() throws IOException {
        BibEntry entry = importOne("""
                SN  - 978-3-932869-19-8
                TY  - BOOK
                TI  - A book
                ER  -
                """);

        assertEquals(Optional.of("978-3-932869-19-8"), entry.getField(StandardField.ISBN));
        assertEquals(Optional.empty(), entry.getField(StandardField.ISSN));
    }

    @ParameterizedTest
    @CsvSource({"JOUR", "MGZN", "CONF", "THES", "RPRT", "GEN"})
    void serialNumberOfOtherEntryTypesStaysIssn(String risType) throws IOException {
        BibEntry entry = importOne("""
                TY  - %s
                SN  - 1537-744X
                TI  - Something
                ER  -
                """.formatted(risType));

        assertEquals(Optional.of("1537-744X"), entry.getField(StandardField.ISSN));
        assertEquals(Optional.empty(), entry.getField(StandardField.ISBN));
    }

    private BibEntry importOne(String ris) throws IOException {
        var entries = importer.importDatabase(ris).getDatabase().getEntries();
        assertEquals(1, entries.size());
        return entries.getFirst();
    }
}
