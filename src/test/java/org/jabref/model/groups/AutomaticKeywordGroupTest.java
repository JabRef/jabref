package org.jabref.model.groups;

import java.util.HashSet;
import java.util.Set;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AutomaticKeywordGroupTest {

    @Test
    public void createSubgroupsForTwoKeywords() throws Exception {
        AutomaticKeywordGroup keywordsGroup = new AutomaticKeywordGroup("Keywords", GroupHierarchyType.INDEPENDENT, "keywords", ',', '>');
        BibEntry entry = new BibEntry().withField("keywords", "A, B");

        Set<GroupTreeNode> expected = new HashSet<>();
        expected.add(GroupTreeNode.fromGroup(new WordKeywordGroup("A", GroupHierarchyType.INCLUDING, "keywords", "A", true, ',', true)));
        expected.add(GroupTreeNode.fromGroup(new WordKeywordGroup("B", GroupHierarchyType.INCLUDING, "keywords", "B", true, ',', true)));
        assertEquals(expected, keywordsGroup.createSubgroups(entry));
    }
}
