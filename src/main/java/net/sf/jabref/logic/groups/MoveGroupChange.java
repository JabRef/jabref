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
package net.sf.jabref.logic.groups;

public class MoveGroupChange {

    private GroupTreeNode oldParent;
    private int oldChildIndex;
    private GroupTreeNode newParent;
    private int newChildIndex;

    /**
     * @param oldParent
     * @param oldChildIndex
     * @param newParent The new parent node to which the node will be moved.
     * @param newChildIndex The child index at newParent to which the node will be moved.
     */
    public MoveGroupChange(GroupTreeNode oldParent, int oldChildIndex, GroupTreeNode newParent, int newChildIndex) {
        this.oldParent = oldParent;
        this.oldChildIndex = oldChildIndex;
        this.newParent = newParent;
        this.newChildIndex = newChildIndex;
    }

    public GroupTreeNode getOldParent() {
        return oldParent;
    }

    public int getOldChildIndex() {
        return oldChildIndex;
    }

    public GroupTreeNode getNewParent() {
        return newParent;
    }

    public int getNewChildIndex() {
        return newChildIndex;
    }

}
