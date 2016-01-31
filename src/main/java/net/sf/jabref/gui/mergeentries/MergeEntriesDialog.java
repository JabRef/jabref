/*  Copyright (C) 2012-2105 JabRef contributors.
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
package net.sf.jabref.gui.mergeentries;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.*;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.gui.util.PositionWindow;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.ColumnSpec;

/**
 * @author Oscar
 *
 *         Dialog for merging two Bibtex entries
 */
public class MergeEntriesDialog extends JDialog {

    private final BasePanel panel;
    private final CellConstraints cc = new CellConstraints();
    private BibEntry one;
    private BibEntry two;
    private NamedCompound ce;
    private MergeEntries mergeEntries;

    private PositionWindow pw;


    private static final String MERGE_ENTRIES = Localization.lang("Merge entries");
    private static final String MARGIN = "5px";

    public MergeEntriesDialog(BasePanel panel) {
        super(panel.frame(), MERGE_ENTRIES, true);

        this.panel = panel;

        // Start setting up the dialog
        init(panel.getSelectedEntries());
    }

    /**
     * Sets up the dialog
     *
     * @param selected Selected BibtexEntries
     */
    private void init(BibEntry[] selected) {

        // Check if there are two entries selected
        if (selected.length != 2) { // None selected. Inform the user to select entries first.
            JOptionPane.showMessageDialog(panel.frame(),
                    Localization.lang("You have to choose exactly two entries to merge."),
                    MERGE_ENTRIES, JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
            return;
        }

        // Store the two entries
        one = selected[0];
        two = selected[1];

        mergeEntries = new MergeEntries(one, two, panel.getBibDatabaseContext().getMode());

        // Create undo-compound
        ce = new NamedCompound(MERGE_ENTRIES);

        FormLayout layout = new FormLayout("fill:700px:grow", "fill:400px:grow, 4px, p, 5px, p");
        // layout.setColumnGroups(new int[][] {{3, 11}});
        this.setLayout(layout);

        this.add(mergeEntries.getMergeEntryPanel(), cc.xy(1, 1));
        this.add(new JSeparator(), cc.xy(1, 3));

        // Create buttons
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        JButton cancel = new JButton(Localization.lang("Cancel"));
        cancel.setActionCommand("cancel");
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                buttonPressed(e.getActionCommand());
            }
        });

        JButton replaceentries = new JButton(MERGE_ENTRIES);
        replaceentries.setActionCommand("replace");
        replaceentries.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                buttonPressed(e.getActionCommand());
            }
        });

        bb.addButton(new JButton[] {replaceentries, cancel});
        this.add(bb.getPanel(), cc.xy(1, 5));

        // Add some margin around the layout
        layout.appendRow(RowSpec.decode(MARGIN));
        layout.appendColumn(ColumnSpec.decode(MARGIN));
        layout.insertRow(1, RowSpec.decode(MARGIN));
        layout.insertColumn(1, ColumnSpec.decode(MARGIN));

        // Set up a ComponentListener that saves the last size and position of the dialog
        this.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                // Save dialog position
                pw.storeWindowPosition();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                // Save dialog position
                pw.storeWindowPosition();
            }
        });

        pw = new PositionWindow(this, JabRefPreferences.MERGEENTRIES_POS_X, JabRefPreferences.MERGEENTRIES_POS_Y,
                JabRefPreferences.MERGEENTRIES_SIZE_X, JabRefPreferences.MERGEENTRIES_SIZE_Y);
        pw.setWindowPosition();

        // Show what we've got
        setVisible(true);
    }

    /**
     * Act on button pressed
     *
     * @param button Button pressed
     */
    private void buttonPressed(String button) {
        BibEntry mergedEntry = mergeEntries.getMergeEntry();
        if ("cancel".equals(button)) {
            // Cancelled, throw it away
            panel.output(Localization.lang("Cancelled merging entries"));
        } else if ("replace".equals(button)) {
            // Create a new entry and add it to the undo stack
            // Remove the other two entries and add them to the undo stack (which is not working...)
            panel.insertEntry(mergedEntry);
            ce.addEdit(new UndoableInsertEntry(panel.database(), mergedEntry, panel));
            ce.addEdit(new UndoableRemoveEntry(panel.database(), one, panel));
            panel.database().removeEntry(one);
            ce.addEdit(new UndoableRemoveEntry(panel.database(), two, panel));
            panel.database().removeEntry(two);
            ce.end();
            panel.undoManager.addEdit(ce);
            panel.output(Localization.lang("Merged entries"));
        }
        dispose();
    }
}
