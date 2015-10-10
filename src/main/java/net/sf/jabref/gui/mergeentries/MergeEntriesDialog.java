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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;

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

    private static final long serialVersionUID = 5454378088546423798L;

    // private String [] preferedOrder = {"author", "title", "journal", "booktitle", "volume", "number", "pages", "year", "month"};
    private final Dimension DIM = new Dimension(800, 800);
    private final BasePanel panel;
    private final JabRefFrame frame;
    private final CellConstraints cc = new CellConstraints();
    private BibtexEntry one;
    private BibtexEntry two;
    private NamedCompound ce;
    private MergeEntries mergeEntries;


    public MergeEntriesDialog(BasePanel panel) {
        super(panel.frame(), Localization.lang("Merge entries"), true);

        this.panel = panel;
        this.frame = panel.frame();

        // Start setting up the dialog
        init(panel.getSelectedEntries());
        Util.placeDialog(this, this.frame);
    }

    /**
     * Sets up the dialog
     * 
     * @param selected Selected BibtexEntries
     */
    private void init(BibtexEntry[] selected) {

        // Check if there are two entries selected
        if (selected.length != 2) { // None selected. Inform the user to select entries first.
            // @formatter:off
            JOptionPane.showMessageDialog(frame, Localization.lang("You have to choose exactly two entries to merge."),
                    Localization.lang("Merge entries"), JOptionPane.INFORMATION_MESSAGE);
            // @formatter:on
            this.dispose();
            return;
        }

        // Store the two entries
        one = selected[0];
        two = selected[1];

        mergeEntries = new MergeEntries(one, two);

        // Create undo-compound
        ce = new NamedCompound(Localization.lang("Merge entries"));

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

        JButton replaceentries = new JButton(Localization.lang("Merge entries"));
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
        layout.appendRow(RowSpec.decode("5px"));
        layout.appendColumn(ColumnSpec.decode("5px"));
        layout.insertRow(1, RowSpec.decode("5px"));
        layout.insertColumn(1, ColumnSpec.decode("5px"));

        pack();

        if (getHeight() > DIM.height) {
            setSize(new Dimension(getWidth(), DIM.height));
        }
        if (getWidth() > DIM.width) {
            setSize(new Dimension(DIM.width, getHeight()));
        }

        // Show what we've got
        setVisible(true);

        pack();

    }

    /**
     * Act on button pressed
     * 
     * @param button Button pressed
     */
    private void buttonPressed(String button) {
        BibtexEntry mergedEntry = mergeEntries.getMergeEntry();
        if (button.equals("cancel")) {
            // Cancelled, throw it away
            panel.output(Localization.lang("Cancelled merging entries"));

            dispose();
        } else if (button.equals("replace")) {
            // Remove the other two entries and add them to the undo stack (which is not working...)
            ce.addEdit(new UndoableRemoveEntry(panel.database(), one, panel));
            panel.database().removeEntry(one.getId());
            ce.addEdit(new UndoableRemoveEntry(panel.database(), two, panel));
            panel.database().removeEntry(two.getId());
            // Create a new entry and add it to the undo stack
            panel.insertEntry(mergedEntry);
            ce.addEdit(new UndoableInsertEntry(panel.database(), mergedEntry, panel));
            ce.end();
            panel.undoManager.addEdit(ce);
            panel.output(Localization.lang("Merged entries"));
            dispose();
        }
    }
}