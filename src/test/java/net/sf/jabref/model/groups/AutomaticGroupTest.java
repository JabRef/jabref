package net.sf.jabref.model.groups;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.*;

public class AutomaticGroupTest {

    @Test
    public void createSubgroupsForTwoKeywords() throws Exception {
        AutomaticGroup keywordsGroup = new AutomaticGroup("Keywords", GroupHierarchyType.INDEPENDENT, "keywords", ',');
        BibEntry entry = new BibEntry().withField("keywords", "A, B");

        Set<GroupTreeNode> expected = new HashSet<>();
        expected.add(GroupTreeNode.fromGroup(new WordKeywordGroup("A", GroupHierarchyType.INDEPENDENT, "keywords", "A", true, ',', true)));
        expected.add(GroupTreeNode.fromGroup(new WordKeywordGroup("B", GroupHierarchyType.INDEPENDENT, "keywords", "B", true, ',', true)));
        assertEquals(expected, keywordsGroup.createSubgroups(entry));
    }
}
