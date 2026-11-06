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

        assertEquals(suggestedGroups(), groupsOf(allEntries.getChildren()));
        assertEquals(allEntries.getChildren(), added);
        assertTrue(SuggestedGroups.areAllPresent(allEntries));
    }

    @Test
    void secondRunAddsNothing() {
        SuggestedGroups.addMissing(allEntries);

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(List.of(), added);
        assertEquals(suggestedGroups(), groupsOf(allEntries.getChildren()));
    }

    /// The name of a suggested group is translated when the group is created.
    /// After switching the language, the stored name no longer matches the new translation.
    @Test
    // [utest->req~ux.groups.suggested-groups-recognized-by-expression~1]
    void recognizesSuggestedGroupsCreatedInAnotherLanguage() {
        SearchGroup withoutFiles = GroupsFactory.createWithoutFilesGroup();
        withoutFiles.nameProperty().set("Einträge ohne verknüpfte Dateien");
        allEntries.addSubgroup(withoutFiles);
        SearchGroup withoutGroups = GroupsFactory.createWithoutGroupsGroup();
        withoutGroups.nameProperty().set("Einträge ohne Gruppen");
        allEntries.addSubgroup(withoutGroups);

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(List.of(), added);
        assertEquals(List.of("Einträge ohne verknüpfte Dateien", "Einträge ohne Gruppen"), namesOf(allEntries.getChildren()));
    }

    @Test
    void keepsCustomizedSuggestedGroupUnchanged() {
        SearchGroup customized = GroupsFactory.createWithoutFilesGroup();
        customized.nameProperty().set("Missing PDFs");
        customized.setColor("#ff0000");
        customized.setIconName("PDF_FILE");
        allEntries.addSubgroup(customized);

        SuggestedGroups.addMissing(allEntries);

        assertEquals(List.of("Missing PDFs", GroupsFactory.createWithoutGroupsGroup().getName()), namesOf(allEntries.getChildren()));
        AbstractGroup kept = allEntries.getChildren().getFirst().getGroup();
        assertEquals(Optional.of("#ff0000"), kept.getColor());
        assertEquals(Optional.of("PDF_FILE"), kept.getIconName());
    }

    @Test
    void addsSuggestionNextToUserGroupWithSameNameButOtherExpression() {
        String suggestedName = GroupsFactory.createWithoutFilesGroup().getName();
        SearchGroup userGroup = new SearchGroup(suggestedName, GroupHierarchyType.INDEPENDENT, "file = pdf", EnumSet.noneOf(SearchFlags.class));
        allEntries.addSubgroup(userGroup);

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(suggestedGroups(), groupsOf(added));
        assertTrue(groupsOf(allEntries.getChildren()).contains(userGroup));
    }

    @Test
    void ignoresSuggestedGroupMovedIntoAnotherGroup() {
        GroupTreeNode folder = allEntries.addSubgroup(new ExplicitGroup("Folder", GroupHierarchyType.INDEPENDENT, ','));
        folder.addSubgroup(GroupsFactory.createWithoutFilesGroup());

        List<GroupTreeNode> added = SuggestedGroups.addMissing(allEntries);

        assertEquals(suggestedGroups(), groupsOf(added));
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
                        "Articles"),
                namesOf(allEntries.getChildren()));
    }

    @Test
    void areAllPresentIsFalseWhenOneSuggestionIsMissing() {
        allEntries.addSubgroup(GroupsFactory.createWithoutFilesGroup());

        assertFalse(SuggestedGroups.areAllPresent(allEntries));
    }

    @Test
    void areAllPresentDoesNotChangeTheTree() {
        SuggestedGroups.areAllPresent(allEntries);

        assertEquals(0, allEntries.getNumberOfChildren());
    }

    private static List<AbstractGroup> suggestedGroups() {
        return groupsOf(SuggestedGroups.createSuggestions().getChildren());
    }

    private static List<AbstractGroup> groupsOf(List<GroupTreeNode> nodes) {
        return nodes.stream().map(GroupTreeNode::getGroup).toList();
    }

    private static List<String> namesOf(List<GroupTreeNode> nodes) {
        return nodes.stream().map(node -> node.getGroup().getName()).toList();
    }
}
