package org.jabref.gui.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.GUITest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@GUITest
public class EntryFromFileCreatorManagerTest {

    private static final File UNLINKED_FILES_TEST_BIB = Paths
            .get("src/test/resources/org/jabref/gui/importer/unlinkedFilesTestBib.bib").toFile();
    private final ImportFormatPreferences prefs = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private ExternalFileTypes externalFileTypes;

    @BeforeEach
    public void setUp() {
        externalFileTypes = mock(ExternalFileTypes.class, Answers.RETURNS_DEEP_STUBS);
        when(externalFileTypes.getExternalFileTypeByExt("pdf")).thenReturn(Optional.empty());
    }

    /**
     * Tests the prerequisites of this test-class itself.
     */
    @Test
    public void testTestDatabase() throws IOException {
        BibDatabase database;
        Collection<BibEntry> entries;
        BibEntry entry1;
        BibEntry entry2;
        try (FileInputStream stream = new FileInputStream(UNLINKED_FILES_TEST_BIB);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor()).parse(reader);
            database = result.getDatabase();
            entries = database.getEntries();

            entry1 = database.getEntryByKey("entry1").get();
            entry2 = database.getEntryByKey("entry2").get();
        }

        assertEquals(2, database.getEntryCount());
        assertEquals(2, entries.size());
        assertNotNull(entry1);
        assertNotNull(entry2);
    }

    @Test
    public void testGetCreator() {
        EntryFromFileCreatorManager manager = new EntryFromFileCreatorManager(externalFileTypes);
        EntryFromFileCreator creator = manager.getEntryCreator(ImportDataTest.NOT_EXISTING_PDF);
        assertNull(creator);

        creator = manager.getEntryCreator(ImportDataTest.FILE_IN_DATABASE);
        assertNotNull(creator);
        assertTrue(creator.accept(ImportDataTest.FILE_IN_DATABASE));
    }

    @Test
    public void testAddEntrysFromFiles() throws IOException {
        try (FileInputStream stream = new FileInputStream(UNLINKED_FILES_TEST_BIB);
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ParserResult result = new BibtexParser(prefs, new DummyFileUpdateMonitor()).parse(reader);
            BibDatabase database = result.getDatabase();

            List<File> files = new ArrayList<>();

            files.add(ImportDataTest.FILE_NOT_IN_DATABASE);
            files.add(ImportDataTest.NOT_EXISTING_PDF);

            EntryFromFileCreatorManager manager = new EntryFromFileCreatorManager(externalFileTypes);
            List<String> errors = manager.addEntrysFromFiles(files, database, null, true);

            /**
             * One file doesn't exist, so adding it as an entry should lead to an error message.
             */
            assertEquals(1, errors.size());

            boolean file1Found = false;
            boolean file2Found = false;
            for (BibEntry entry : database.getEntries()) {
                String filesInfo = entry.getField("file").get();
                if (filesInfo.contains(files.get(0).getName())) {
                    file1Found = true;
                }
                if (filesInfo.contains(files.get(1).getName())) {
                    file2Found = true;
                }
            }

            assertTrue(file1Found);
            assertFalse(file2Found);
        }
    }
}
