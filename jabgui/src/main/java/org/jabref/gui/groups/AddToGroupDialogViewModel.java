package org.jabref.gui.groups;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Lets the user pick a group by typing part of its path and assigns the selected entries to it.
///
/// Only groups listed in the library's group tree are offered, in tree order. Subgroups that
/// automatic groups create on the fly are left out, as they are not part of that tree.
@NullMarked
public class AddToGroupDialogViewModel extends AbstractViewModel {

    private final List<BibEntry> entries;
    private final UndoManager undoManager;
    private final StringProperty searchText = new SimpleStringProperty("");
    private final FilteredList<GroupTreeNode> groups;
    private final ObjectProperty<@Nullable GroupTreeNode> selectedGroup = new SimpleObjectProperty<>();

    public AddToGroupDialogViewModel(GroupTreeNode root,
                                     List<BibEntry> entries,
                                     UndoManager undoManager,
                                     Optional<GroupTreeNode> lastUsedGroup) {
        this.entries = entries;
        this.undoManager = undoManager;

        List<GroupTreeNode> assignableGroups = root.iterateOverTree()
                                                   .filter(GroupNodeViewModel::canAddEntriesIn)
                                                   .toList();
        groups = new FilteredList<>(FXCollections.observableArrayList(assignableGroups));

        selectedGroup.set(lastUsedGroup.filter(assignableGroups::contains)
                                       .orElse(groups.isEmpty() ? null : groups.getFirst()));

        searchText.addListener((_, _, newText) -> filterGroups(newText));
    }

    public static boolean hasAssignableGroups(GroupTreeNode root) {
        return root.iterateOverTree().anyMatch(GroupNodeViewModel::canAddEntriesIn);
    }

    public StringProperty searchTextProperty() {
        return searchText;
    }

    public ObservableList<GroupTreeNode> getGroups() {
        return groups;
    }

    public ObjectProperty<@Nullable GroupTreeNode> selectedGroupProperty() {
        return selectedGroup;
    }

    public void selectNext() {
        moveSelection(1);
    }

    public void selectPrevious() {
        moveSelection(-1);
    }

    /// @return the group the entries were assigned to, empty if no group is selected
    public Optional<GroupTreeNode> assignEntriesToSelectedGroup() {
        Optional<GroupTreeNode> group = Optional.ofNullable(selectedGroup.get());
        group.ifPresent(target -> undoManager.addEdit(
                Localization.lang("Assign entries to group"),
                edit -> edit.addAll(target.addEntriesToGroup(entries))));
        return group;
    }

    private void filterGroups(String text) {
        groups.setPredicate(group -> StringUtil.containsIgnoreCase(group.getPath(), text));
        if (!groups.contains(selectedGroup.get())) {
            selectedGroup.set(groups.isEmpty() ? null : groups.getFirst());
        }
    }

    private void moveSelection(int offset) {
        if (groups.isEmpty()) {
            return;
        }
        int newIndex = Math.clamp(groups.indexOf(selectedGroup.get()) + offset, 0, groups.size() - 1);
        selectedGroup.set(groups.get(newIndex));
    }
}
