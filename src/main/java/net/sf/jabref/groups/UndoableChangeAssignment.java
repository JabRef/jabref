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
package net.sf.jabref.groups;

import java.util.HashSet;
import java.util.Set;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.groups.structure.ExplicitGroup;
import net.sf.jabref.logic.l10n.Localization;

/**
 * @author jzieren
 */
public class UndoableChangeAssignment extends AbstractUndoableEdit {

    private final Set<BibEntry> previousAssignments;
    private final Set<BibEntry> newAssignments;
    /**
     * The path to the edited node
     */
    private int[] pathToNode;
    /**
     * The root of the global groups tree
     */
    private GroupTreeNode root;

    /**
     * @param node The node whose assignments were edited.
     */
    public UndoableChangeAssignment(GroupTreeNode node, Set<BibEntry> previousAssignments,
            Set<BibEntry> newAssignments) {
        this.previousAssignments = new HashSet<>(previousAssignments);
        this.newAssignments = new HashSet<>(newAssignments);
        this.root = (GroupTreeNode) node.getRoot();
        this.pathToNode = node.getIndexedPath();
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

        GroupTreeNode node = root.getChildAt(pathToNode);
        if (node != null) {
            ExplicitGroup group = (ExplicitGroup) node.getGroup();
            group.clearAssignments();
            for (final BibEntry entry : previousAssignments) {
                group.addEntry(entry);
            }
        }
    }

    @Override
    public void redo() {
        super.redo();

        GroupTreeNode node = root.getChildAt(pathToNode);
        if (node != null) {
            ExplicitGroup group = (ExplicitGroup) node.getGroup();
            group.clearAssignments();
            for (final BibEntry entry : newAssignments) {
                group.addEntry(entry);
            }
        }
    }
}
