package net.sf.jabref.gui.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.sf.jabref.gui.undo.AbstractUndoableJabRefEdit;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.groups.GroupTreeNode;

/**
 * @author jzieren
 */
public class UndoableChangeAssignment extends AbstractUndoableJabRefEdit {

    private final List<BibEntry> previousAssignments;
    private final List<BibEntry> newAssignments;
    /**
     * The path to the edited node
     */
    private final List<Integer> pathToNode;
    /**
     * The root of the global groups tree
     */
    private final GroupTreeNode root;

    /**
     * @param node The node whose assignments were edited.
     */
    public UndoableChangeAssignment(GroupTreeNodeViewModel node, Set<BibEntry> previousAssignments,
            Set<BibEntry> newAssignments) {
        this.previousAssignments = new ArrayList<>(previousAssignments);
        this.newAssignments = new ArrayList<>(newAssignments);
        this.root = node.getNode().getRoot();
        this.pathToNode = node.getNode().getIndexedPathFromRoot();
    }


    @Override
    public String getPresentationName() {
        return Localization.lang("change assignment of entries");
    }

    @Override
    public void undo() {
        super.undo();

        Optional<GroupTreeNode> node = root.getDescendant(pathToNode);
        if (node.isPresent()) {
            node.get().getGroup().add(previousAssignments);
        }
    }

    @Override
    public void redo() {
        super.redo();

        Optional<GroupTreeNode> node = root.getDescendant(pathToNode);
        if (node.isPresent()) {
            node.get().getGroup().add(newAssignments);
        }
    }
}
