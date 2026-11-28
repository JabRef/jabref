package org.jabref.logic.groups;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.exporter.GroupSerializer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.util.GroupsParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.util.DummyFileUpdateMonitor;

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

    /// Dragging a group finds it by its path, so the suggestion must not take the name of the user's group
    @Test
    void addsSuggestionUnderFreeNameNextToUserGroupWithSameNameButOtherExpression() {
        SearchGroup suggestion = GroupsFactory.createWithoutFilesGroup();
        SearchGroup userGroup = new SearchGroup(suggestion.getName(), GroupHierarchyType.INDEPENDENT, "file = pdf", EnumSet.noneOf(SearchFlags.class));
        allEntries.addSubgroup(userGroup);

        GroupTreeNode added = SuggestedGroups.addMissing(allEntries).getFirst();

        assertEquals(suggestion.getName() + " (2)", added.getName());
        assertEquals(suggestion.getSearchExpression(), ((SearchGroup) added.getGroup()).getSearchExpression());
        assertEquals(Optional.of(added), allEntries.getChildByPath(added.getPath()));
        assertEquals(userGroup, allEntries.getChildren().getFirst().getGroup());
    }

    /// Group names are unique within a library, see [GroupTreeNode#findGroupByName(String)]
    @Test
    void restoredSuggestionAvoidsNameUsedElsewhereInTheLibrary() {
        SuggestedGroups.addMissing(allEntries);
        GroupTreeNode rank = allEntries.getChildren().getLast().getChildren().getFirst();
        GroupTreeNode threeStars = rank.getChildren().get(2);
        String suggestedName = threeStars.getName();
        threeStars.removeFromParent();
        GroupTreeNode userGroup = allEntries.addSubgroup(new ExplicitGroup(suggestedName, GroupHierarchyType.INDEPENDENT, ','));

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(List.of(suggestedName + " (2)"), namesOf(added));
        assertEquals(suggestedName, userGroup.getName());
    }

    /// The moved group keeps its name, which is unique within the library, so the suggestion added again gets a free one
    @Test
    void ignoresSuggestedGroupMovedIntoAnotherGroup() {
        GroupTreeNode folder = allEntries.addSubgroup(new ExplicitGroup("Folder", GroupHierarchyType.INDEPENDENT, ','));
        SearchGroup moved = GroupsFactory.createWithoutFilesGroup();
        folder.addSubgroup(moved);

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(SuggestedGroups.createSuggestions().getNumberOfChildren(), added.size());
        assertEquals(moved.getName() + " (2)", added.getFirst().getName());
        assertEquals(List.of(moved), groupsOf(folder.getChildren()));
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

    /// The name of a suggested group is translated when the group is created and the user may rename it.
    /// Neither stops the group from being recognized, also after saving and reopening the library.
    @Test
    // [utest->req~ux.groups.suggested-groups-recognized-by-expression~1]
    void recognizesCustomizedSuggestionsAfterSerialization() throws ParseException {
        SuggestedGroups.addMissing(allEntries);
        allEntries.iterateOverTree().filter(node -> node != allEntries).forEach(node -> {
            node.getGroup().nameProperty().set("Personalizado: " + node.getName());
            node.getGroup().setDescription("My description");
        });
        GroupSerializer serializer = new GroupSerializer();
        List<String> saved = serializer.serializeTree(allEntries);
        GroupTreeNode restored = GroupsParser.importGroups(saved, ',', new DummyFileUpdateMonitor(), new MetaData(), "userAndHost");

        assertTrue(SuggestedGroups.areAllPresent(restored));
        assertEquals(List.of(), SuggestedGroups.addMissing(restored));
        assertEquals(saved, serializer.serializeTree(restored));
    }

    /// The suggested groups use the current search syntax, wherever the user moved them
    @Test
    void reportsSearchGroupsOtherThanSuggestions() {
        SuggestedGroups.addMissing(allEntries);
        SearchGroup moved = GroupsFactory.createWithoutFilesGroup();
        moved.nameProperty().set("Moved suggestion");
        allEntries.addSubgroup(new ExplicitGroup("Folder", GroupHierarchyType.INDEPENDENT, ',')).addSubgroup(moved);

        assertFalse(SuggestedGroups.containsOtherSearchGroups(allEntries));

        allEntries.addSubgroup(new SearchGroup("Old syntax", GroupHierarchyType.INDEPENDENT, "title = test", EnumSet.of(SearchFlags.CASE_SENSITIVE)));

        assertTrue(SuggestedGroups.containsOtherSearchGroups(allEntries));
    }

    @Test
    void droppingEntriesOnMarkingGroupsPreservesEntryFields() {
        BibEntry entry = new BibEntry();
        GroupsFactory.createMarkingAndGradingGroups().iterateOverTree().forEach(node ->
                assertEquals(List.of(), node.addEntriesToGroup(List.of(entry))));

        assertEquals(Map.of(), entry.getFieldMap());
    }

    @Test
    void preservesGroupWithDifferentHierarchyAndAddsIndependentSuggestion() {
        SearchGroup userGroup = new SearchGroup("Custom relevance", GroupHierarchyType.INCLUDING,
                "relevance =~.*", EnumSet.noneOf(SearchFlags.class));
        GroupTreeNode marking = allEntries.addChild(GroupsFactory.createMarkingAndGradingGroups());
        marking.getChildren().get(1).removeFromParent();
        marking.addSubgroup(userGroup);

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(List.of("Entries without linked files", "Entries without groups", "Relevance"), namesOf(added));
        assertEquals(userGroup, marking.getChildren().getLast().getGroup());
        assertEquals(GroupHierarchyType.INDEPENDENT, marking.getChildren().get(1).getGroup().getHierarchicalContext());
    }

    /// Searching also the linked files changes what the group shows, so the group is the user's own
    @Test
    void preservesGroupWithDifferentSearchFlagsAndAddsSuggestion() {
        GroupTreeNode relevance = allEntries.addChild(GroupsFactory.createMarkingAndGradingGroups()).getChildren().get(1);
        GroupTreeNode notRelevant = relevance.getChildren().getLast();
        SearchGroup suggestion = (SearchGroup) notRelevant.getGroup();
        notRelevant.removeFromParent();
        SearchGroup userGroup = new SearchGroup("Not relevant, also in files", GroupHierarchyType.INDEPENDENT,
                suggestion.getSearchExpression(), EnumSet.of(SearchFlags.FULLTEXT));
        relevance.addSubgroup(userGroup);

        SuggestedGroups.addMissing(allEntries);

        assertEquals(List.of(userGroup, suggestion), groupsOf(relevance.getChildren().subList(1, 3)));
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
