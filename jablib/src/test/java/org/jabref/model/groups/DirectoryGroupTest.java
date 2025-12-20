package org.jabref.model.groups;

import java.nio.file.Path;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoryGroupTest {

    @TempDir
    Path tempDir;

    private DirectoryGroup group;

    @BeforeEach
    void setUp() {
        group = new DirectoryGroup("TestGroup", GroupHierarchyType.INDEPENDENT, tempDir);
    }

    @Test
    void constructorSetsNameCorrectly() {
        assertEquals("TestGroup", group.getName());
    }

    @Test
    void constructorSetsDirectoryPathCorrectly() {
        assertEquals(tempDir, group.getDirectoryPath());
    }

    @Test
    void constructorSetsContextCorrectly() {
        assertEquals(GroupHierarchyType.INDEPENDENT, group.getHierarchicalContext());
    }

    @Test
    void isDynamicReturnsTrue() {
        assertTrue(group.isDynamic());
    }

    @Test
    void containsReturnsFalseForUnmatchedEntry() {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Test Author");
        assertFalse(group.contains(entry));
    }

    @Test
    void containsReturnsTrueAfterUpdateMatches() {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Test Author");

        group.updateMatches(entry, true);
        assertTrue(group.contains(entry));
    }

    @Test
    void containsReturnsFalseAfterRemovingMatch() {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.AUTHOR, "Test Author");

        group.updateMatches(entry, true);
        assertTrue(group.contains(entry));

        group.updateMatches(entry, false);
        assertFalse(group.contains(entry));
    }

    @Test
    void deepCopyCreatesEqualGroup() {
        group.setColor("#FF0000");
        group.setDescription("Test description");
        group.setExpanded(false);

        AbstractGroup copy = group.deepCopy();

        assertNotNull(copy);
        assertTrue(copy instanceof DirectoryGroup);
        assertEquals(group.getName(), copy.getName());
        assertEquals(group.getHierarchicalContext(), copy.getHierarchicalContext());
        assertEquals(group.getColor(), copy.getColor());
        assertEquals(group.getDescription(), copy.getDescription());
        assertEquals(group.isExpanded(), copy.isExpanded());
    }

    @Test
    void equalsReturnsTrueForSameGroup() {
        DirectoryGroup sameGroup = new DirectoryGroup("TestGroup", GroupHierarchyType.INDEPENDENT, tempDir);
        assertEquals(group, sameGroup);
    }

    @Test
    void equalsReturnsFalseForDifferentName() {
        DirectoryGroup differentGroup = new DirectoryGroup("DifferentName", GroupHierarchyType.INDEPENDENT, tempDir);
        assertNotEquals(group, differentGroup);
    }

    @Test
    void equalsReturnsFalseForDifferentPath() {
        DirectoryGroup differentGroup = new DirectoryGroup("TestGroup", GroupHierarchyType.INDEPENDENT, tempDir.resolve("subdir"));
        assertNotEquals(group, differentGroup);
    }

    @Test
    void hashCodeIsConsistent() {
        DirectoryGroup sameGroup = new DirectoryGroup("TestGroup", GroupHierarchyType.INDEPENDENT, tempDir);
        assertEquals(group.hashCode(), sameGroup.hashCode());
    }

    @Test
    void toStringContainsRelevantInfo() {
        String str = group.toString();
        assertTrue(str.contains("TestGroup"));
        assertTrue(str.contains("DirectoryGroup"));
    }
}
