package org.jabref.model.groups;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyDirectoryUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AllowedToUseLogic("because class under test relies on logic classes")
class DirectoryGroupTest {

    private MetaData metaData;
    private Path temporaryFolder;

    @BeforeEach
    void setUp(@TempDir Path temporaryFolder) {
        metaData = new MetaData();
        this.temporaryFolder = temporaryFolder;
    }

    @Test
    void containsReturnsTrueForEntryWithAFileDirectlyInTheMirroredDirectory() {
        Path directoryPath = temporaryFolder.resolve("MyDirectory").toAbsolutePath();
        Path filePath = directoryPath.resolve("MyFile.pdf").toAbsolutePath();

        DirectoryGroup group = new DirectoryGroup("LocalDirectory", GroupHierarchyType.INCLUDING, directoryPath, new DummyDirectoryUpdateMonitor(), metaData, "userandHost");
        BibEntry entry = new BibEntry();
        LinkedFile file = new LinkedFile(filePath);
        List<LinkedFile> files = Collections.singletonList(file);
        entry.setFiles(files);

        assertTrue(group.contains(entry));
    }

    @Test
    void containsReturnsFalseForEntryWithoutAFileDirectlyInTheMirroredDirectory() {
        Path directoryPath = temporaryFolder.resolve("MyDirectory").toAbsolutePath();
        Path filePathNotInTheDirectory = temporaryFolder.resolve("MyFile.pdf").toAbsolutePath();
        Path subdirectoryPath = directoryPath.resolve("MySubdirectory").toAbsolutePath();
        Path filePathNotDirectlyInTheDirectory = subdirectoryPath.resolve("MyFile.pdf").toAbsolutePath();

        DirectoryGroup group = new DirectoryGroup("LocalDirectory", GroupHierarchyType.INCLUDING, directoryPath, new DummyDirectoryUpdateMonitor(), metaData, "userandHost");
        BibEntry entryWithNoFile = new BibEntry();
        BibEntry entryWithNoFileInTheDirectory = new BibEntry();
        BibEntry entryWithNoFileDirectlyInTheDirectory = new BibEntry();
        List<LinkedFile> filesNotInTheDirectory = Collections.singletonList(new LinkedFile(filePathNotInTheDirectory));
        List<LinkedFile> filesNotDirectlyInTheDirectory = Collections.singletonList(new LinkedFile(filePathNotDirectlyInTheDirectory));
        entryWithNoFileInTheDirectory.setFiles(filesNotInTheDirectory);
        entryWithNoFileDirectlyInTheDirectory.setFiles(filesNotDirectlyInTheDirectory);

        assertFalse(group.contains(entryWithNoFile));
        assertFalse(group.contains(entryWithNoFileInTheDirectory));
        assertFalse(group.contains(entryWithNoFileDirectlyInTheDirectory));
    }
}
