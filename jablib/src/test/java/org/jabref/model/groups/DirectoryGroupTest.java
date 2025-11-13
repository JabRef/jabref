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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AllowedToUseLogic("because class under test relies on logic classes")
class DirectoryGroupTest {

    private MetaData metaData;

    @BeforeEach
    void setUp() {
        metaData = new MetaData();
    }

    @Test
    void containsReturnsTrueForEntryWithAFileDirectlyInTheMirroredDirectory() {
        DirectoryGroup group = new DirectoryGroup("LocalDirectory", GroupHierarchyType.INCLUDING, Path.of("C:\\Users\\Me\\MyDirectory").toAbsolutePath(), new DummyDirectoryUpdateMonitor(), metaData, "userandHost");
        BibEntry entry = new BibEntry();
        LinkedFile file = new LinkedFile(Path.of("C:\\Users\\Me\\MyDirectory\\MyFile.pdf").toAbsolutePath());
        List<LinkedFile> files = Collections.singletonList(file);
        entry.setFiles(files);

        assertTrue(group.contains(entry));
    }

    @Test
    void containsReturnsFalseForEntryWithoutAFileDirectlyInTheMirroredDirectory() {
        DirectoryGroup group = new DirectoryGroup("LocalDirectory", GroupHierarchyType.INCLUDING, Path.of("C:\\Users\\Me\\MyDirectory").toAbsolutePath(), new DummyDirectoryUpdateMonitor(), metaData, "userandHost");
        BibEntry entryWithNoFile = new BibEntry();
        BibEntry entryWithNoFileInTheDirectory = new BibEntry();
        BibEntry entryWithNoFileDirectlyInTheDirectory = new BibEntry();
        List<LinkedFile> filesNotInTheDirectory = Collections.singletonList(new LinkedFile(Path.of("C:\\Users\\Me\\AnotherDirectory\\MyFile.pdf").toAbsolutePath()));
        List<LinkedFile> filesNotDirectlyInTheDirectory = Collections.singletonList(new LinkedFile(Path.of("C:\\Users\\Me\\MyDirectory\\MySubdirectory\\MyFile.pdf").toAbsolutePath()));
        entryWithNoFileInTheDirectory.setFiles(filesNotInTheDirectory);
        entryWithNoFileDirectlyInTheDirectory.setFiles(filesNotDirectlyInTheDirectory);

        assertFalse(group.contains(entryWithNoFile));
        assertFalse(group.contains(entryWithNoFileInTheDirectory));
        assertFalse(group.contains(entryWithNoFileDirectlyInTheDirectory));
    }
}
