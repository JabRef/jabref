package org.jabref.model.groups;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@AllowedToUseLogic("because class under test relies on logic classes")
class TexGroupTest {

    private static final String USER_AND_HOST = "Darwin";

    private MetaData metaData;

    @BeforeEach
    void setUp() {
        metaData = new MetaData();
    }

    @Test
    void containsReturnsTrueForEntryInAux() throws Exception {
        Path auxFile = paperAuxSource();
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, auxFile, new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData, USER_AND_HOST);
        BibEntry inAux = new BibEntry().withCitationKey("Darwin1888");

        assertTrue(group.contains(inAux));
    }

    @Test
    void containsReturnsFalseForEntryNotInAux() throws Exception {
        Path auxFile = paperAuxSource();
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, auxFile, new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData, USER_AND_HOST);
        BibEntry notInAux = new BibEntry().withCitationKey("NotInAux2017");

        assertFalse(group.contains(notInAux));
    }

    @Test
    void getFilePathReturnsRelativePath() throws Exception {
        Path auxFile = paperAuxSource();
        metaData.setLatexFileDirectory(USER_AND_HOST, auxFile.getParent().toString());
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, auxFile, new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData, USER_AND_HOST);

        assertEquals("paper.aux", group.getFilePath().toString());
    }

    @Test
    void getFilePathReturnsPathRelativeToLibrary(@TempDir Path tempDir) throws Exception {
        Path auxFile = copyPaperAux(tempDir);
        metaData.setLibraryPath(tempDir.resolve("library.bib"));
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, auxFile, new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData, USER_AND_HOST);

        assertEquals("paper.aux", group.getFilePath().toString());
    }

    /// The location of the `.bib` file is unknown while the library is parsed. The group must
    /// therefore resolve the aux file after the location becomes known.
    @Test
    void relativePathIsResolvedWhenLibraryPathBecomesKnown(@TempDir Path tempDir) throws Exception {
        Path auxFile = copyPaperAux(tempDir);
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, Path.of("paper.aux"), new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData, USER_AND_HOST);

        metaData.setLibraryPath(tempDir.resolve("library.bib"));

        BibEntry inAux = new BibEntry().withCitationKey("Darwin1888");
        assertEquals(auxFile, group.getFilePathResolved());
        assertTrue(group.contains(inAux));
    }

    @Test
    void latexDirectoryTakesPrecedenceOverLibraryPath(@TempDir Path tempDir) throws Exception {
        Path libraryDirectory = Files.createDirectory(tempDir.resolve("library"));
        Path latexDirectory = Files.createDirectory(tempDir.resolve("latex"));
        copyPaperAux(libraryDirectory);
        Path latexAuxFile = copyPaperAux(latexDirectory);

        metaData.setLibraryPath(libraryDirectory.resolve("library.bib"));
        metaData.setLatexFileDirectory(USER_AND_HOST, latexDirectory.toString());
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, Path.of("paper.aux"), new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData, USER_AND_HOST);

        assertEquals(latexAuxFile, group.getFilePathResolved());
    }

    @Test
    void libraryPathIsUsedWhenLatexDirectoryDoesNotContainTheAuxFile(@TempDir Path tempDir) throws Exception {
        Path libraryDirectory = Files.createDirectory(tempDir.resolve("library"));
        Path latexDirectory = Files.createDirectory(tempDir.resolve("latex"));
        Path libraryAuxFile = copyPaperAux(libraryDirectory);

        metaData.setLibraryPath(libraryDirectory.resolve("library.bib"));
        metaData.setLatexFileDirectory(USER_AND_HOST, latexDirectory.toString());
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, Path.of("paper.aux"), new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData, USER_AND_HOST);

        assertEquals(libraryAuxFile, group.getFilePathResolved());
    }

    /// "Save as" moves the library. The aux file then resolves to another directory.
    @Test
    void resolvedPathFollowsTheLibrary(@TempDir Path tempDir) throws Exception {
        Path firstDir = Files.createDirectory(tempDir.resolve("first"));
        Path secondDir = Files.createDirectory(tempDir.resolve("second"));
        Path firstAuxFile = copyPaperAux(firstDir);
        Path secondAuxFile = copyPaperAux(secondDir);

        metaData.setLibraryPath(firstDir.resolve("library.bib"));
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, Path.of("paper.aux"), new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData, USER_AND_HOST);
        assertEquals(firstAuxFile, group.getFilePathResolved());

        metaData.setLibraryPath(secondDir.resolve("library.bib"));

        assertEquals(secondAuxFile, group.getFilePathResolved());
    }

    @Test
    void rebindingToNewMetaDataUpdatesResolution(@TempDir Path tempDir) throws Exception {
        Path firstDir = Files.createDirectory(tempDir.resolve("first"));
        Path secondDir = Files.createDirectory(tempDir.resolve("second"));
        copyPaperAux(firstDir);
        Path secondAuxFile = copyPaperAux(secondDir);

        metaData.setLibraryPath(firstDir.resolve("library.bib"));
        TexGroup group = new TexGroup("paper", GroupHierarchyType.INDEPENDENT, Path.of("paper.aux"), new DefaultAuxParser(new BibDatabase()), new DummyFileUpdateMonitor(), metaData, USER_AND_HOST);
        assertEquals(firstDir.resolve("paper.aux"), group.getFilePathResolved());

        MetaData newMetaData = new MetaData();
        newMetaData.setLibraryPath(secondDir.resolve("library.bib"));
        newMetaData.setGroups(GroupTreeNode.fromGroup(group));

        assertEquals(secondAuxFile, group.getFilePathResolved());
    }

    @Test
    void fileMonitorIsArmedWhenLibraryPathBecomesKnown(@TempDir Path tempDir) throws Exception {
        Path auxFile = copyPaperAux(tempDir);
        FileUpdateMonitor fileMonitor = mock(FileUpdateMonitor.class);

        TexGroup group = TexGroup.create("paper", GroupHierarchyType.INDEPENDENT, Path.of("paper.aux"), new DefaultAuxParser(new BibDatabase()), fileMonitor, metaData, USER_AND_HOST);
        verifyNoInteractions(fileMonitor);

        metaData.setLibraryPath(tempDir.resolve("library.bib"));

        verify(fileMonitor).addListenerForFile(auxFile, group);
    }

    @Test
    void fileMonitorMovesWhenResolutionChanges(@TempDir Path tempDir) throws Exception {
        Path firstDir = Files.createDirectory(tempDir.resolve("first"));
        Path secondDir = Files.createDirectory(tempDir.resolve("second"));
        Path firstAuxFile = copyPaperAux(firstDir);
        Path secondAuxFile = copyPaperAux(secondDir);
        FileUpdateMonitor fileMonitor = mock(FileUpdateMonitor.class);

        metaData.setLibraryPath(firstDir.resolve("library.bib"));
        TexGroup group = TexGroup.create("paper", GroupHierarchyType.INDEPENDENT, Path.of("paper.aux"), new DefaultAuxParser(new BibDatabase()), fileMonitor, metaData, USER_AND_HOST);

        verify(fileMonitor).addListenerForFile(firstAuxFile, group);

        metaData.setLibraryPath(secondDir.resolve("library.bib"));

        verify(fileMonitor).removeListener(firstAuxFile, group);
        verify(fileMonitor).addListenerForFile(secondAuxFile, group);
    }

    private static Path paperAuxSource() throws Exception {
        return Path.of(TexGroupTest.class.getResource("paper.aux").toURI());
    }

    private static Path copyPaperAux(Path directory) throws Exception {
        Path auxFile = directory.resolve("paper.aux");
        Files.copy(paperAuxSource(), auxFile);
        return auxFile;
    }
}
