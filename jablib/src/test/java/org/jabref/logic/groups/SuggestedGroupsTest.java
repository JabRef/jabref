package org.jabref.logic.groups;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.SearchFlags;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class SuggestedGroupsTest {

    private GroupTreeNode allEntries;

    @BeforeEach
    void setUp() {
        allEntries = GroupTreeNode.fromGroup(GroupsFactory.createAllEntriesGroup());
    }

    @Test
    void addsAllSuggestionsToLibraryWithoutGroups() {
        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(describe(SuggestedGroups.createSuggestions()), describe(allEntries));
        assertEquals(allEntries.getChildren(), added);
        assertTrue(SuggestedGroups.areAllPresent(allEntries));
    }

    @Test
    void secondRunAddsNothing() {
        SuggestedGroups.addMissing(allEntries);

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(List.of(), added);
        assertEquals(describe(SuggestedGroups.createSuggestions()), describe(allEntries));
    }

    /// The name of a suggested group is translated when the group is created.
    /// After switching the language, the stored name no longer matches the new translation.
    @Test
    // [utest->req~ux.groups.suggested-groups-recognized-by-expression~1]
    void recognizesSuggestedGroupsCreatedInAnotherLanguage() {
        SuggestedGroups.addMissing(allEntries);
        allEntries.iterateOverTree()
                  .filter(node -> node != allEntries)
                  .forEach(node -> node.getGroup().nameProperty().set("Übersetzt: " + node.getGroup().getName()));
        long groupsBefore = allEntries.iterateOverTree().count();

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(List.of(), added);
        assertEquals(groupsBefore, allEntries.iterateOverTree().count());
    }

    @Test
    void keepsCustomizedSuggestedGroupUnchanged() {
        SearchGroup customized = GroupsFactory.createWithoutFilesGroup();
        customized.nameProperty().set("Missing PDFs");
        customized.setColor("#ff0000");
        customized.setIconName("PDF_FILE");
        allEntries.addSubgroup(customized);

        SuggestedGroups.addMissing(allEntries);

        assertEquals(SuggestedGroups.createSuggestions().getNumberOfChildren(), allEntries.getNumberOfChildren());
        AbstractGroup kept = allEntries.getChildren().getFirst().getGroup();
        assertEquals("Missing PDFs", kept.getName());
        assertEquals(Optional.of("#ff0000"), kept.getColor());
        assertEquals(Optional.of("PDF_FILE"), kept.getIconName());
    }

    @Test
    void addsSuggestionNextToUserGroupWithSameNameButOtherExpression() {
        String suggestedName = GroupsFactory.createWithoutFilesGroup().getName();
        SearchGroup userGroup = new SearchGroup(suggestedName, GroupHierarchyType.INDEPENDENT, "file = pdf", EnumSet.noneOf(SearchFlags.class));
        allEntries.addSubgroup(userGroup);

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(groupsOf(SuggestedGroups.createSuggestions().getChildren()), groupsOf(added));
        assertEquals(userGroup, allEntries.getChildren().getFirst().getGroup());
    }

    @Test
    void ignoresSuggestedGroupMovedIntoAnotherGroup() {
        GroupTreeNode folder = allEntries.addSubgroup(new ExplicitGroup("Folder", GroupHierarchyType.INDEPENDENT, ','));
        folder.addSubgroup(GroupsFactory.createWithoutFilesGroup());

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(groupsOf(SuggestedGroups.createSuggestions().getChildren()), groupsOf(added));
        assertEquals(1, folder.getNumberOfChildren());
    }

    @Test
    void insertsMissingSuggestionBeforeNextPresentSuggestion() {
        allEntries.addSubgroup(new ExplicitGroup("Books", GroupHierarchyType.INDEPENDENT, ','));
        allEntries.addSubgroup(GroupsFactory.createWithoutGroupsGroup());
        allEntries.addSubgroup(new ExplicitGroup("Articles", GroupHierarchyType.INDEPENDENT, ','));

        SuggestedGroups.addMissing(allEntries);

        assertEquals(List.of(
                        "Books",
                        GroupsFactory.createWithoutFilesGroup().getName(),
                        GroupsFactory.createWithoutGroupsGroup().getName(),
                        "Articles",
                        GroupsFactory.createMarkingAndGradingGroups().getGroup().getName()),
                namesOf(allEntries.getChildren()));
    }

    @Test
    void restoresDeletedIntermediateGroupWithItsSubgroups() {
        SuggestedGroups.addMissing(allEntries);
        GroupTreeNode marking = allEntries.getChildren().getLast();
        GroupTreeNode rank = marking.getChildren().getFirst();
        String rankDescription = describe(rank);
        rank.removeFromParent();

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(1, added.size());
        assertEquals(marking.getChildren().getFirst(), added.getFirst());
        assertEquals(rankDescription, describe(added.getFirst()));
    }

    @Test
    void restoresDeletedRankLeafAtItsPosition() {
        SuggestedGroups.addMissing(allEntries);
        GroupTreeNode rank = allEntries.getChildren().getLast().getChildren().getFirst();
        GroupTreeNode threeStars = rank.getChildren().get(2);
        AbstractGroup deleted = threeStars.getGroup();
        threeStars.removeFromParent();

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(List.of(deleted), groupsOf(added));
        assertEquals(added.getFirst(), rank.getChildren().get(2));
        assertEquals(5, rank.getNumberOfChildren());
    }

    @Test
    void areAllPresentIsFalseWhenOneLeafIsMissing() {
        SuggestedGroups.addMissing(allEntries);
        allEntries.getChildren().getLast().getChildren().getLast().getChildren().getLast().removeFromParent();

        assertFalse(SuggestedGroups.areAllPresent(allEntries));
    }

    @Test
    void areAllPresentDoesNotChangeTheTree() {
        SuggestedGroups.areAllPresent(allEntries);

        assertEquals(0, allEntries.getNumberOfChildren());
    }

    private static List<AbstractGroup> groupsOf(List<GroupTreeNode> nodes) {
        return nodes.stream().map(GroupTreeNode::getGroup).toList();
    }

    private static List<String> namesOf(List<GroupTreeNode> nodes) {
        return nodes.stream().map(node -> node.getGroup().getName()).toList();
    }

    /// Describes a subtree by what identifies a suggested group: its position and its search expression
    private static String describe(GroupTreeNode root) {
        return String.join("\n", root.iterateOverTree()
                                     .map(node -> "  ".repeat(node.getLevel() - root.getLevel())
                                             + (node.getGroup() instanceof SearchGroup searchGroup ? searchGroup.getSearchExpression() : node.getGroup().getClass().getSimpleName()))
                                     .toList());
    }
}
