package org.jabref.model.groups;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomaticKeywordGroupTest {

    @Test
    void createSubgroupsForTwoKeywords() throws Exception {
        AutomaticKeywordGroup keywordsGroup = new AutomaticKeywordGroup("Keywords", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '>');
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "A, B");

        Set<GroupTreeNode> expected = createIncludingKeywordsSubgroup();

        assertEquals(
                expected.stream().map(GroupTreeNode::getGroup).collect(Collectors.toSet()),
                keywordsGroup.createSubgroups(entry).stream().map(GroupTreeNode::getGroup).collect(Collectors.toSet())
        );
    }

    @Test
    void createSubgroupsIgnoresEmptyKeyword() throws Exception {
        AutomaticKeywordGroup keywordsGroup = new AutomaticKeywordGroup("Keywords", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '>');
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "A, ,B");

        Set<GroupTreeNode> expected = createIncludingKeywordsSubgroup();
        Set<GroupTreeNode> actual = keywordsGroup.createSubgroups(entry);

        assertEquals(actual, expected);
        assertTrue(actual.containsAll(expected));
    }

    private Set<GroupTreeNode> createIncludingKeywordsSubgroup() {
        Set<GroupTreeNode> expectedKeywordsSubgroup = new HashSet<>();
        expectedKeywordsSubgroup.add(GroupTreeNode.fromGroup(new WordKeywordGroup("A", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "A", true, ',', true)));
        expectedKeywordsSubgroup.add(GroupTreeNode.fromGroup(new WordKeywordGroup("B", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "B", true, ',', true)));

        return expectedKeywordsSubgroup;
    }
}
