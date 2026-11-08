package org.jabref.gui.groups;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.RegexKeywordGroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// [utest->feat~groups.assign-entries-by-keyboard~1]
class AddToGroupDialogViewModelTest {

    private final BibEntry entry = new BibEntry().withCitationKey("Smith2020");
    private final HeadlessGuiUndoManager undoManager = new HeadlessGuiUndoManager();

    private GroupTreeNode root;
    private GroupTreeNode review;
    private GroupTreeNode included;
    private GroupTreeNode excluded;

    @BeforeEach
    void setUp() {
        root = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
        review = root.addSubgroup(new ExplicitGroup("Review", GroupHierarchyType.INDEPENDENT, ','));
        included = review.addSubgroup(new ExplicitGroup("Included", GroupHierarchyType.INDEPENDENT, ','));
        excluded = review.addSubgroup(new ExplicitGroup("Excluded", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new AutomaticKeywordGroup("Keywords", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '>'));
    }

    private AddToGroupDialogViewModel createViewModel(Optional<GroupTreeNode> lastUsedGroup) {
        return new AddToGroupDialogViewModel(root, List.of(entry), undoManager, lastUsedGroup);
    }

    @Test
    void listsOnlyGroupsThatAcceptEntries() {
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.empty());

        assertEquals(List.of(review, included, excluded), viewModel.getGroups());
    }

    @Test
    void searchMatchesGroupPathIgnoringCase() {
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.empty());

        viewModel.searchTextProperty().set("incl");
        assertEquals(List.of(included), viewModel.getGroups());

        viewModel.searchTextProperty().set("REVIEW");
        assertEquals(List.of(review, included, excluded), viewModel.getGroups());
    }

    @Test
    void preselectsFirstGroupWithoutLastUsedGroup() {
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.empty());

        assertEquals(review, viewModel.selectedGroupProperty().get());
    }

    @Test
    void preselectsLastUsedGroup() {
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.of(excluded));

        assertEquals(excluded, viewModel.selectedGroupProperty().get());
    }

    @Test
    void ignoresLastUsedGroupRemovedFromLibrary() {
        excluded.removeFromParent();

        AddToGroupDialogViewModel viewModel = createViewModel(Optional.of(excluded));

        assertEquals(review, viewModel.selectedGroupProperty().get());
    }

    @Test
    void searchSelectsFirstMatchWhenSelectedGroupIsFilteredOut() {
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.of(included));

        viewModel.searchTextProperty().set("excl");

        assertEquals(excluded, viewModel.selectedGroupProperty().get());
    }

    @Test
    void searchKeepsSelectedGroupWhileItMatches() {
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.of(included));

        viewModel.searchTextProperty().set("review");

        assertEquals(included, viewModel.selectedGroupProperty().get());
    }

    @Test
    void searchWithoutMatchClearsSelection() {
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.empty());

        viewModel.searchTextProperty().set("no such group");

        assertEquals(Optional.empty(), viewModel.assignEntriesToSelectedGroup());
        assertEquals(List.of(), included.getEntriesInGroup(List.of(entry)));
    }

    @Test
    void selectNextAndPreviousStayWithinList() {
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.empty());

        viewModel.selectNext();
        viewModel.selectNext();
        viewModel.selectNext();
        assertEquals(excluded, viewModel.selectedGroupProperty().get());

        viewModel.selectPrevious();
        assertEquals(included, viewModel.selectedGroupProperty().get());
    }

    @Test
    void assignAddsEntriesToSelectedGroup() {
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.of(included));

        assertEquals(Optional.of(included), viewModel.assignEntriesToSelectedGroup());
        assertEquals(List.of(entry), included.getEntriesInGroup(List.of(entry)));
    }

    @Test
    void assignIsUndoneInOneStep() {
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.of(included));
        viewModel.assignEntriesToSelectedGroup();

        undoManager.undo();

        assertEquals(List.of(), included.getEntriesInGroup(List.of(entry)));
    }

    @Test
    void leavesOutGroupsThatCannotTakeEntries() {
        GroupTreeNode automatic = root.getChildren().getLast();
        automatic.addSubgroup(new RegexKeywordGroup("Regex", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "a.*", false));

        AddToGroupDialogViewModel viewModel = createViewModel(Optional.empty());

        assertEquals(List.of(review, included, excluded), viewModel.getGroups());
    }

    @Test
    void preselectsLastUsedGroupAmongEqualGroups() {
        GroupTreeNode otherIncluded = addSecondReviewWithIncluded();

        AddToGroupDialogViewModel viewModel = createViewModel(Optional.of(otherIncluded));

        assertSame(otherIncluded, viewModel.selectedGroupProperty().get());
    }

    @Test
    void selectPreviousStartsFromSelectedAmongEqualGroups() {
        GroupTreeNode otherIncluded = addSecondReviewWithIncluded();
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.of(otherIncluded));

        viewModel.selectPrevious();

        assertSame(otherIncluded.getParent().orElseThrow(), viewModel.selectedGroupProperty().get());
    }

    @Test
    void searchKeepsSelectedGroupAmongEqualGroups() {
        GroupTreeNode otherIncluded = addSecondReviewWithIncluded();
        AddToGroupDialogViewModel viewModel = createViewModel(Optional.of(otherIncluded));

        viewModel.searchTextProperty().set("included");

        assertSame(otherIncluded, viewModel.selectedGroupProperty().get());
    }

    /// Adds `Second review > Included`, which equals `Review > Included`: same group definition, no children.
    private GroupTreeNode addSecondReviewWithIncluded() {
        GroupTreeNode secondReview = root.addSubgroup(new ExplicitGroup("Second review", GroupHierarchyType.INDEPENDENT, ','));
        GroupTreeNode otherIncluded = secondReview.addSubgroup(new ExplicitGroup("Included", GroupHierarchyType.INDEPENDENT, ','));
        assertEquals(included, otherIncluded);
        return otherIncluded;
    }
}
