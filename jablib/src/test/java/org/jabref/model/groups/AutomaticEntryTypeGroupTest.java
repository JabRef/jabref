package org.jabref.model.groups;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticEntryTypeGroupTest {

    @Test
    void createsEntryTypeSubgroupFromEntry() {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        AutomaticEntryTypeGroup byType = new AutomaticEntryTypeGroup("By Type", GroupHierarchyType.INCLUDING);

        var children = byType.createSubgroups(entry);
        assertEquals(1, children.size());
        GroupTreeNode node = children.iterator().next();
        assertEquals(StandardEntryType.Article.getName(), node.getName());
        assertTrue(node.getGroup().contains(entry));
        assertEquals(GroupHierarchyType.INDEPENDENT, node.getGroup().getHierarchicalContext());
    }

    @Test
    void mergesSameTypeAcrossEntries() {
        BibEntry article1 = new BibEntry(StandardEntryType.Article);
        BibEntry article2 = new BibEntry(StandardEntryType.Article);
        BibEntry book = new BibEntry(StandardEntryType.Book);

        AutomaticEntryTypeGroup byType = new AutomaticEntryTypeGroup("By Type", GroupHierarchyType.INCLUDING);
        var nodes = byType.createSubgroups(FXCollections.observableArrayList(List.of(article1, article2, book)));

        assertEquals(2, nodes.size());
        Set<String> names = nodes.stream().map(GroupTreeNode::getName).collect(Collectors.toSet());
        assertEquals(Set.of(StandardEntryType.Article.getName(), StandardEntryType.Book.getName()), names);

        GroupTreeNode articleNode = nodes.stream()
                                         .filter(n -> StandardEntryType.Article.getName().equals(n.getName()))
                                         .findFirst()
                                         .orElseThrow();

        var matches = articleNode.findMatches(List.of(article1, article2, book));
        assertEquals(2, matches.size());
        assertTrue(matches.contains(article1));
        assertTrue(matches.contains(article2));
        assertFalse(matches.contains(book));
    }
}
