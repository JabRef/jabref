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
package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JLabel;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.groups.structure.AllEntriesGroup;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.UndoableModifySubtree;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;

class GroupChange extends Change {

    private final GroupTreeNode changedGroups;
    private final GroupTreeNode tmpGroupRoot;


    public GroupChange(GroupTreeNode changedGroups, GroupTreeNode tmpGroupRoot) {
        super(changedGroups == null ? Localization.lang("Removed all groups") : Localization
                .lang("Modified groups tree"));
        this.changedGroups = changedGroups;
        this.tmpGroupRoot = tmpGroupRoot;
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        final GroupTreeNode root = panel.getBibDatabaseContext().getMetaData().getGroups();
        final UndoableModifySubtree undo = new UndoableModifySubtree(
                panel.getGroupSelector(), panel.getBibDatabaseContext().getMetaData().getGroups(),
                root, Localization.lang("Modified groups"));
        root.removeAllChildren();
        if (changedGroups == null) {
            // I think setting root to null is not possible
            root.setGroup(new AllEntriesGroup());
        } else {
            // change root group, even though it'll be AllEntries anyway
            root.setGroup(changedGroups.getGroup());
            for (int i = 0; i < changedGroups.getChildCount(); ++i) {
                root.add(((GroupTreeNode) changedGroups.getChildAt(i)).deepCopy());
            }
            // the group tree is now appled to a different BibDatabase than it was created
            // for, which affects groups such as ExplicitGroup (which links to BibEntry objects).
            // We must traverse the tree and refresh all groups:
            root.refreshGroupsForNewDatabase(panel.getDatabase());
        }

        if (panel.getGroupSelector().getGroupTreeRoot() == root) {
            panel.getGroupSelector().revalidateGroups();
        }
        undoEdit.addEdit(undo);

        // Update tmp database:
        tmpGroupRoot.removeAllChildren();
        if (changedGroups != null) {
            GroupTreeNode copied = changedGroups.deepCopy();
            tmpGroupRoot.setGroup(copied.getGroup());
            for (int i = 0; i < copied.getChildCount(); ++i) {
                tmpGroupRoot.add(((GroupTreeNode) copied.getChildAt(i)).deepCopy());
            }
        }
        tmpGroupRoot.refreshGroupsForNewDatabase(secondary);
        return true;
    }

    @Override
    public JComponent description() {
        return new JLabel("<html>" + toString() + '.'
                + (changedGroups == null ? "" : ' ' + Localization
                        .lang("Accepting the change replaces the complete groups tree with the externally modified groups tree."))
                + "</html>");

    }
}
