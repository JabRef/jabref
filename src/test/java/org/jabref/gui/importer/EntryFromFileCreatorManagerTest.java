package org.jabref.gui.importer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.importer.ImportDataTest;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.FileFieldParser;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@GUITest
class EntryFromFileCreatorManagerTest {

    private final ImportFormatPreferences prefs = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private EntryFromFileCreatorManager manager;

    @BeforeEach
    void setUp() {
        ExternalFileTypes externalFileTypes = mock(ExternalFileTypes.class, Answers.RETURNS_DEEP_STUBS);
        when(externalFileTypes.getExternalFileTypeByExt("pdf")).thenReturn(Optional.empty());
        manager = new EntryFromFileCreatorManager(externalFileTypes);
    }

    @Test
    void getEntryCreatorForNotExistingPDFReturnsEmptyOptional() {
        assertEquals(Optional.empty(), manager.getEntryCreator(ImportDataTest.NOT_EXISTING_PDF));
    }

    @Test
    void getEntryCreatorForExistingPDFReturnsCreatorAcceptingThatFile() {
        Optional<EntryFromFileCreator> creator = manager.getEntryCreator(ImportDataTest.FILE_IN_DATABASE);
        assertNotEquals(Optional.empty(), creator);
        assertTrue(creator.get().accept(ImportDataTest.FILE_IN_DATABASE.toFile()));
    }

    @Test
    void testAddEntriesFromFiles() throws IOException {
        ParserResult result = new BibtexImporter(prefs, new DummyFileUpdateMonitor())
                .importDatabase(ImportDataTest.UNLINKED_FILES_TEST_BIB, StandardCharsets.UTF_8);
        BibDatabase database = result.getDatabase();

        List<Path> files = new ArrayList<>();
        files.add(ImportDataTest.FILE_NOT_IN_DATABASE);
        files.add(ImportDataTest.NOT_EXISTING_PDF);

        List<String> errors = manager.addEntrysFromFiles(files, database, null, true);

        // One file doesn't exist, so adding it as an entry should lead to an error message.
        assertEquals(1, errors.size());

        Stream<LinkedFile> linkedFileStream = database.getEntries().stream()
                                                      .flatMap(entry -> FileFieldParser.parse(entry.getField("file").get()).stream());

        boolean file1Found = linkedFileStream
                .anyMatch(file -> file.getLink().equalsIgnoreCase(ImportDataTest.FILE_NOT_IN_DATABASE.toString()));

        boolean file2Found = linkedFileStream
                .anyMatch(file -> file.getLink().equalsIgnoreCase(ImportDataTest.NOT_EXISTING_PDF.toString()));

        assertTrue(file1Found);
        assertFalse(file2Found);
    }
}
