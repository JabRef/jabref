/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.logic.groups.GroupTreeNode;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

/**
 * @author jzieren
 */
public class UndoableChangeAssignment extends AbstractUndoableEdit {

    private final List<BibEntry> previousAssignments;
    private final List<BibEntry> newAssignments;
    /**
     * The path to the edited node
     */
    private List<Integer> pathToNode;
    /**
     * The root of the global groups tree
     */
    private GroupTreeNode root;

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
    public String getUndoPresentationName() {
        return Localization.lang("Undo") + ": " + Localization.lang("change assignment of entries");
    }

    @Override
    public String getRedoPresentationName() {
        return Localization.lang("Redo") + ": " + Localization.lang("change assignment of entries");
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
