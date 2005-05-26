package net.sf.jabref.collab;

import javax.swing.*;

import net.sf.jabref.*;
import net.sf.jabref.groups.*;
import net.sf.jabref.undo.NamedCompound;

public class GroupChange extends Change {
    private final GroupTreeNode m_changedGroups;
    public GroupChange(GroupTreeNode changedGroups) {
        super(changedGroups != null ? 
                "Modified groups tree"
                : "Removed all groups"); // JZTODO lyrics
        m_changedGroups = changedGroups;
    }

    public void makeChange(BasePanel panel, NamedCompound undoEdit) {
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
        }
        panel.getGroupSelector().revalidateGroups();
        undoEdit.addEdit(undo);
    }

    JComponent description() {
        return new JLabel("<html>" + name + "." + (m_changedGroups != null ? " " 
                + "Accepting the change replaces the complete " +
                "groups tree with the externally modified groups tree." : "") 
                + "</html>"); 
        // JZTODO lyrics
    }
}
