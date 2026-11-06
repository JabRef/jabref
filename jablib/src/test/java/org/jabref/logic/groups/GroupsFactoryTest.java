package org.jabref.logic.groups;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class GroupsFactoryTest {

    /// The search expressions are stored in the libraries of the users and identify the suggested groups there.
    /// Changing one makes the existing groups unrecognizable, so that "Add JabRef suggested groups" adds them a second time.
    @Test
    // [utest->req~ux.groups.suggested-marking-groups~1]
    void markingAndGradingGroupsHaveStableExpressionsIconsAndColors() {
        String expected = """
                ranking =~.* OR relevance =~.* OR qualityassured =~.* OR printed =~.* OR priority =~.* OR readstatus =~.*
                  ranking =~.* | RANKING
                    ranking == rank1 | RANK1
                    ranking == rank2 | RANK2
                    ranking == rank3 | RANK3
                    ranking == rank4 | RANK4
                    ranking == rank5 | RANK5
                  relevance =~.* | RELEVANCE
                    relevance == relevant | RELEVANCE
                    relevance !== relevant
                  qualityassured =~.* | QUALITY
                    qualityassured == qualityAssured | QUALITY_ASSURED
                    qualityassured !== qualityAssured
                  printed =~.* | PRINTED
                    printed == printed | PRINTED
                    printed !== printed
                  priority =~.* | PRIORITY
                    priority == prio1 | PRIORITY_HIGH | #ff0000
                    priority == prio2 | PRIORITY_MEDIUM | #ffa500
                    priority == prio3 | PRIORITY_LOW | #6fcc75
                  readstatus =~.* | READ_STATUS
                    readstatus == read | READ_STATUS_READ | #6fcc75
                    readstatus == skimmed | READ_STATUS_SKIMMED | #ffa500""";

        assertEquals(expected, describe(GroupsFactory.createMarkingAndGradingGroups()));
    }

    /// Including the subgroups would make "Relevance" show the whole library, because "Not relevant" also contains the entries without the field.
    @Test
    void markingAndGradingGroupsAreIndependentSearchGroups() {
        Set<String> kinds = groups(GroupsFactory.createMarkingAndGradingGroups())
                .map(group -> group.getClass().getSimpleName() + " " + group.getHierarchicalContext())
                .collect(Collectors.toSet());

        assertEquals(Set.of(SearchGroup.class.getSimpleName() + " " + GroupHierarchyType.INDEPENDENT), kinds);
    }

    /// Group names are unique within a library, see [GroupTreeNode#findGroupByName(String)].
    @Test
    void suggestedGroupNamesAreUnique() {
        List<String> names = groups(SuggestedGroups.createSuggestions()).map(AbstractGroup::getName).toList();

        assertEquals(names.size(), Set.copyOf(names).size(), () -> "Duplicate names in " + names);
    }

    private static Stream<AbstractGroup> groups(GroupTreeNode root) {
        return root.iterateOverTree().map(GroupTreeNode::getGroup);
    }

    private static String describe(GroupTreeNode root) {
        return root.iterateOverTree()
                   .map(node -> {
                       SearchGroup group = (SearchGroup) node.getGroup();
                       return "  ".repeat(node.getLevel())
                               + group.getSearchExpression()
                               + group.getIconName().map(icon -> " | " + icon).orElse("")
                               + group.getColor().map(color -> " | " + color).orElse("");
                   })
                   .collect(Collectors.joining("\n"));
    }
}
