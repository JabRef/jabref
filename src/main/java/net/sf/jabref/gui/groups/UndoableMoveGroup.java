/*  Copyright (C) 2003-2011 JabRef contributors.
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

import java.util.List;
import java.util.Objects;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.logic.groups.GroupTreeNode;
import net.sf.jabref.logic.groups.MoveGroupChange;
import net.sf.jabref.logic.l10n.Localization;

/**
 * @author jzieren
 *
 */
class UndoableMoveGroup extends AbstractUndoableEdit {

    private final GroupTreeNodeViewModel root;
    private final List<Integer> pathToNewParent;
    private final int newChildIndex;
    private final List<Integer> pathToOldParent;
    private final int oldChildIndex;


    public UndoableMoveGroup(GroupTreeNodeViewModel root, MoveGroupChange moveChange) {
        this.root = Objects.requireNonNull(root);
        Objects.requireNonNull(moveChange);
        pathToOldParent = moveChange.getOldParent().getIndexedPathFromRoot();
        pathToNewParent = moveChange.getNewParent().getIndexedPathFromRoot();
        oldChildIndex = moveChange.getOldChildIndex();
        newChildIndex = moveChange.getNewChildIndex();
    }

    @Override
    public String getUndoPresentationName() {
        return Localization.lang("Undo") + ": " + Localization.lang("move group");
    }

    @Override
    public String getRedoPresentationName() {
        return Localization.lang("Redo") + ": " + Localization.lang("move group");
    }

    @Override
    public void undo() {
        super.undo();

        GroupTreeNode newParent = root.getNode().getDescendant(pathToNewParent).get(); //TODO: NULL
        GroupTreeNode node = newParent.getChildAt(newChildIndex).get(); //TODO: Null
        //TODO: NULL
        node.moveTo(root.getNode().getDescendant(pathToOldParent).get(), oldChildIndex);
    }

    @Override
    public void redo() {
        super.redo();

        GroupTreeNode oldParent = root.getNode().getDescendant(pathToOldParent).get(); //TODO: NULL
        GroupTreeNode node = oldParent.getChildAt(oldChildIndex).get(); //TODO:Null
        //TODO: NULL
        node.moveTo(root.getNode().getDescendant(pathToNewParent).get(), newChildIndex);
    }
}
