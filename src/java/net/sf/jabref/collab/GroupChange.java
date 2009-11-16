package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JLabel;

import net.sf.jabref.BasePanel;
import net.sf.jabref.Globals;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.groups.AllEntriesGroup;
import net.sf.jabref.groups.GroupTreeNode;
import net.sf.jabref.groups.UndoableModifySubtree;
import net.sf.jabref.undo.NamedCompound;

public class GroupChange extends Change {
    private final GroupTreeNode m_changedGroups;
    private GroupTreeNode tmpGroupRoot;

    public GroupChange(GroupTreeNode changedGroups, GroupTreeNode tmpGroupRoot) {
        super(changedGroups != null ? 
                "Modified groups tree"
                : "Removed all groups"); // JZTODO lyrics
        m_changedGroups = changedGroups;
        this.tmpGroupRoot = tmpGroupRoot;
    }

    public boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit) {
        final GroupTreeNode root = panel.getGroupSelector().getGroupTreeRoot();
        final UndoableModifySubtree undo = new UndoableModifySubtree(
                panel.getGroupSelector(), root, Globals.lang("Modified groups")); // JZTODO lyrics
        root.removeAllChildren();
        if (m_changedGroups == null) {
            // I think setting root to null is not possible
            root.setGroup(new AllEntriesGroup());
        } else {
            // change root group, even though it'll be AllEntries anyway
            root.setGroup(m_changedGroups.getGroup());
            for (int i = 0; i < m_changedGroups.getChildCount(); ++i)        
                root.add(((GroupTreeNode) m_changedGroups.getChildAt(i)).deepCopy());
            // the group tree is now appled to a different BibtexDatabase than it was created
            // for, which affects groups such as ExplicitGroup (which links to BibtexEntry objects).
            // We must traverse the tree and refresh all groups:
            root.refreshGroupsForNewDatabase(panel.database());
        }
        panel.getGroupSelector().revalidateGroups();
        undoEdit.addEdit(undo);
        
        // Update tmp database:
        GroupTreeNode copied = m_changedGroups.deepCopy();
        tmpGroupRoot.removeAllChildren();
        tmpGroupRoot.setGroup(copied.getGroup());
        for (int i = 0; i < copied.getChildCount(); ++i)
            tmpGroupRoot.add(((GroupTreeNode) copied.getChildAt(i)).deepCopy());
        tmpGroupRoot.refreshGroupsForNewDatabase(secondary);
        return true;
    }

    JComponent description() {
        return new JLabel("<html>" + name + "." + (m_changedGroups != null ? " " 
                + "Accepting the change replaces the complete " +
                "groups tree with the externally modified groups tree." : "") 
                + "</html>"); 
        // JZTODO lyrics
    }
}
