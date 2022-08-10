package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.GeneralPreferences;

import com.google.common.base.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AtomicFileWriterTest {

    @Test
    void encodingIssueDoesNotLeadToCrash(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("test.txt");
        AtomicFileWriter atomicFileWriter = new AtomicFileWriter(target, StandardCharsets.US_ASCII);
        atomicFileWriter.write("ñ");
        atomicFileWriter.close();
        assertTrue(atomicFileWriter.hasEncodingProblems());
        assertEquals(Set.of('ñ'), atomicFileWriter.getEncodingProblems());
    }

    @Test
    void bibFileIsKeptAtError(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("test.bib");

        String fiveThousandChars = Strings.repeat("A", 5_000);
        Files.writeString(target, fiveThousandChars);

        AtomicFileWriter fileWriter = new AtomicFileWriter(target, StandardCharsets.UTF_8);
        BibDatabase database = new BibDatabase();
        MetaData metaData = new MetaData();
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(database, metaData);
        BibWriter bibWriter = new BibWriter(fileWriter, "\n");

        GeneralPreferences generalPreferences = mock(GeneralPreferences.class);
        SavePreferences savePreferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(savePreferences.getSaveOrder()).thenReturn(new SaveOrderConfig());
        when(savePreferences.takeMetadataSaveOrderInAccount()).thenReturn(true);
        BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();
        BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(bibWriter, generalPreferences, savePreferences, entryTypesManager);

        BibEntry bibEntry = new BibEntry().withField(StandardField.NOTE, "{");
        database.insertEntry(bibEntry);

        assertThrows(Exception.class, () -> databaseWriter.saveDatabase(bibDatabaseContext));
        fileWriter.close();
    }
}
