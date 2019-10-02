package org.jabref.model.groups;

import java.util.HashSet;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutomaticKeywordGroupTest {

    @Test
    void createSubgroupsForTwoKeywords() throws Exception {
        AutomaticKeywordGroup keywordsGroup = new AutomaticKeywordGroup("Keywords", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '>');
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "A, B");

        Set<GroupTreeNode> expected = new HashSet<>();
        expected.add(GroupTreeNode.fromGroup(new WordKeywordGroup("A", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "A", true, ',', true)));
        expected.add(GroupTreeNode.fromGroup(new WordKeywordGroup("B", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "B", true, ',', true)));
        assertEquals(expected, keywordsGroup.createSubgroups(entry));
    }

    @Test
    void createSubgroupsIgnoresEmptyKeyword() throws Exception {
        AutomaticKeywordGroup keywordsGroup = new AutomaticKeywordGroup("Keywords", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '>');
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "A, ,B");

        Set<GroupTreeNode> expected = new HashSet<>();
        expected.add(GroupTreeNode.fromGroup(new WordKeywordGroup("A", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "A", true, ',', true)));
        expected.add(GroupTreeNode.fromGroup(new WordKeywordGroup("B", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "B", true, ',', true)));
        assertEquals(expected, keywordsGroup.createSubgroups(entry));
    }
}
