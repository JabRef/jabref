package org.jabref.model.groups;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.collections.FXCollections;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutomaticKeywordGroupTest {

    @Test
    void createSubgroupsForTwoKeywords() {
        AutomaticKeywordGroup keywordsGroup = new AutomaticKeywordGroup("Keywords", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '>');
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "A, B");

        Set<GroupTreeNode> expected = createIncludingKeywordsSubgroup();

        assertEquals(expected, keywordsGroup.createSubgroups(entry));
    }

    @Test
    void createSubgroupsIgnoresEmptyKeyword() {
        AutomaticKeywordGroup keywordsGroup = new AutomaticKeywordGroup("Keywords", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '>');
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "A, ,B");

        Set<GroupTreeNode> expected = createIncludingKeywordsSubgroup();

        assertEquals(expected, keywordsGroup.createSubgroups(entry));
    }

    // [utest->req~groups.automatic-keyword.missing-value-subgroup~1]
    @Test
    void createSubgroupsForRankingAddsMissingKeywordSubgroupNextToRanks() {
        AutomaticKeywordGroup rankingGroup = new AutomaticKeywordGroup("Ranking", GroupHierarchyType.INDEPENDENT, SpecialField.RANKING, ',', '>');
        List<BibEntry> entries = List.of(
                new BibEntry().withField(SpecialField.RANKING, "rank1"),
                new BibEntry(),
                new BibEntry().withField(SpecialField.RANKING, ","));

        List<AbstractGroup> expected = List.of(
                new WordKeywordGroup("rank1", GroupHierarchyType.INCLUDING, SpecialField.RANKING, "rank1", true, ',', true),
                new MissingKeywordGroup(GroupHierarchyType.INDEPENDENT, SpecialField.RANKING, ','));

        assertEquals(expected, rankingGroup.createSubgroups(FXCollections.observableList(entries))
                                           .stream()
                                           .map(GroupTreeNode::getGroup)
                                           .toList());
    }

    private Set<GroupTreeNode> createIncludingKeywordsSubgroup() {
        Set<GroupTreeNode> expectedKeywordsSubgroup = new HashSet<>();
        expectedKeywordsSubgroup.add(GroupTreeNode.fromGroup(new WordKeywordGroup("A", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "A", true, ',', true)));
        expectedKeywordsSubgroup.add(GroupTreeNode.fromGroup(new WordKeywordGroup("B", GroupHierarchyType.INCLUDING, StandardField.KEYWORDS, "B", true, ',', true)));

        return expectedKeywordsSubgroup;
    }
}
