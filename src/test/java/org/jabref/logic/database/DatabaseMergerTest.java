package org.jabref.logic.database;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.bibtex.comparator.BibtexStringComparator;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseMergerTest {
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
    }

    @Test
    void mergeAddsNonDuplicateEntries() {
        // Entries 1 and 2 are identical
        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Phillip Kaye and Michele Mosca")
                .withField(StandardField.TITLE, "Quantum Networks for Generating Arbitrary Quantum States");
        BibEntry entry2 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Phillip Kaye and Michele Mosca")
                .withField(StandardField.TITLE, "Quantum Networks for Generating Arbitrary Quantum States");
        BibEntry entry3 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Stephen Blaha")
                .withField(StandardField.TITLE, "Quantum Computers and Quantum Computer Languages: Quantum Assembly Language and Quantum C Language");

        BibDatabase database = new BibDatabase(List.of(entry1));
        BibDatabase other = new BibDatabase(List.of(entry2, entry3));
        new DatabaseMerger(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).merge(database, other);

        assertEquals(List.of(entry1, entry3), database.getEntries());
    }

    @Test
    void mergeAddsWithDuplicateEntries() {
        // Entries 1 and 2 are identical,  Entries 3 and 4 are identical
        BibEntry entry1 = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Joshua Bloch")
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.ISBN, "0-201-31005-8");
        BibEntry entry2 = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Joshua J. Bloch")
                .withField(StandardField.TITLE, "Effective Java:Programming Language Guide")
                .withField(StandardField.ISBN, "0-201-31005-8")
                .withField(StandardField.YEAR, "2001")
                .withField(StandardField.EDITION, "1");
        BibEntry entry3 = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Joshua J. Bloch")
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.ISBN, "978-0-321-35668-0")
                .withField(StandardField.YEAR, "2008")
                .withField(StandardField.EDITION, "2");
        BibEntry entry4 = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Joshua Bloch")
                .withField(StandardField.TITLE, "Effective Java:Programming Language Guide")
                .withField(StandardField.ISBN, "978-0-321-35668-0");

        BibDatabase database = new BibDatabase(List.of(entry1, entry4));
        BibDatabase other = new BibDatabase(List.of(entry2, entry3));
        new DatabaseMerger(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).merge(database, other);
        assertEquals(List.of(entry1, entry4), database.getEntries());
    }

    @Test
    void mergeBibTexStringsWithSameNameAreImportedWithModifiedName() {
        BibtexString targetString = new BibtexString("name", "content1");

        // BibTeXStrings that are imported from two sources (same name different content)
        BibtexString sourceString1 = new BibtexString("name", "content2");
        BibtexString sourceString2 = new BibtexString("name", "content3");

        // The expected source BibTeXStrings after import (different name, different content)
        BibtexString importedBibTeXString1 = new BibtexString("name_1", "content2");
        BibtexString importedBibTeXString2 = new BibtexString("name_2", "content3");

        BibDatabase target = new BibDatabase();
        BibDatabase source1 = new BibDatabase();
        BibDatabase source2 = new BibDatabase();
        target.addString(targetString);
        source1.addString(sourceString1);
        source2.addString(sourceString2);

        new DatabaseMerger(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).mergeStrings(target, source1);
        new DatabaseMerger(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).mergeStrings(target, source2);
        // Use string representation to compare since the id will not match
        List<String> resultStringsSorted = target.getStringValues()
                .stream()
                .map(BibtexString::toString)
                .sorted()
                .collect(Collectors.toList());

        assertEquals(List.of(targetString.toString(), importedBibTeXString1.toString(),
                importedBibTeXString2.toString()), resultStringsSorted);
    }

    @Test
    void mergeBibTexStringsWithSameNameAndContentAreIgnored() {
        BibtexString targetString1 = new BibtexString("name1", "content1");
        BibtexString targetString2 = new BibtexString("name2", "content2");

        // BibTeXStrings that are imported (equivalent to target strings)
        BibtexString sourceString1 = new BibtexString("name1", "content1");
        BibtexString sourceString2 = new BibtexString("name2", "content2");

        BibDatabase target = new BibDatabase();
        BibDatabase source = new BibDatabase();
        target.addString(targetString1);
        target.addString(targetString2);
        source.addString(sourceString1);
        source.addString(sourceString2);

        new DatabaseMerger(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).mergeStrings(target, source);
        List<BibtexString> resultStringsSorted = target.getStringValues()
                .stream()
                .sorted(new BibtexStringComparator(false)::compare)
                .collect(Collectors.toList());

        assertEquals(List.of(targetString1, targetString2), resultStringsSorted);
    }

    @Test
    void mergeMetaDataWithoutAllEntriesGroup() {
        MetaData target = new MetaData();
        target.addContentSelector(new ContentSelector(StandardField.AUTHOR, List.of("Test Author")));
        GroupTreeNode targetRootGroup = new GroupTreeNode(new ExplicitGroup("targetGroup", GroupHierarchyType.INDEPENDENT, ','));
        target.setGroups(targetRootGroup);
        MetaData other = new MetaData();
        GroupTreeNode otherRootGroup = new GroupTreeNode(new ExplicitGroup("otherGroup", GroupHierarchyType.INCLUDING, ','));
        other.setGroups(otherRootGroup);
        other.addContentSelector(new ContentSelector(StandardField.TITLE, List.of("Test Title")));
        List<ContentSelector> expectedContentSelectors =
                List.of(new ContentSelector(StandardField.AUTHOR, List.of("Test Author")),
                        new ContentSelector(StandardField.TITLE, List.of("Test Title")));

        new DatabaseMerger(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).mergeMetaData(target, other, "unknown", List.of());

        // Assert that content selectors are all merged
        assertEquals(expectedContentSelectors, target.getContentSelectorList());

        // Assert that groups of other are children of root node of target
        assertEquals(targetRootGroup, target.getGroups().get());
        assertEquals(target.getGroups().get().getChildren().size(), 1);
        assertEquals(otherRootGroup, target.getGroups().get().getChildren().get(0));
    }

    @Test
    void mergeMetaDataWithAllEntriesGroup() {
        MetaData target = new MetaData();
        target.addContentSelector(new ContentSelector(StandardField.AUTHOR, List.of("Test Author")));
        GroupTreeNode targetRootGroup = new GroupTreeNode(new AllEntriesGroup("targetGroup"));
        target.setGroups(targetRootGroup);
        MetaData other = new MetaData();
        GroupTreeNode otherRootGroup = new GroupTreeNode(new AllEntriesGroup("otherGroup"));
        other.setGroups(otherRootGroup);
        other.addContentSelector(new ContentSelector(StandardField.TITLE, List.of("Test Title")));
        List<ContentSelector> expectedContentSelectors =
                List.of(new ContentSelector(StandardField.AUTHOR, List.of("Test Author")),
                        new ContentSelector(StandardField.TITLE, List.of("Test Title")));
        GroupTreeNode expectedImportedGroupNode = new GroupTreeNode(new ExplicitGroup("Imported unknown", GroupHierarchyType.INDEPENDENT, ';'));

        new DatabaseMerger(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).mergeMetaData(target, other, "unknown", List.of());

        // Assert that groups of other are children of root node of target
        assertEquals(targetRootGroup, target.getGroups().get());
        assertEquals(target.getGroups().get().getChildren().size(), 1);
        assertEquals(expectedImportedGroupNode, target.getGroups().get().getChildren().get(0));
        assertEquals(expectedContentSelectors, target.getContentSelectorList());
    }
}
