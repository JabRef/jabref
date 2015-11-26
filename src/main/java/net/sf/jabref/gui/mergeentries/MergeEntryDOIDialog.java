/*  Copyright (C) 2012-2015 JabRef contributors.
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
import java.util.Set;
import java.util.TreeSet;

import javax.swing.*;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.importer.fetcher.DOItoBibTeXFetcher;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.ColumnSpec;

/**
 * @author Oscar
 *
 *         Dialog for merging Bibtex entry with data fetched from DOI
 */
public class MergeEntryDOIDialog extends JDialog {

    private final BasePanel panel;
    private final JabRefFrame frame;
    private final CellConstraints cc = new CellConstraints();
    private BibtexEntry originalEntry;
    private BibtexEntry doiEntry;
    private NamedCompound ce;
    private MergeEntries mergeEntries;
    private PositionWindow pw;

    private final DOItoBibTeXFetcher doiFetcher = new DOItoBibTeXFetcher();


    public MergeEntryDOIDialog(BasePanel panel) {
        super(panel.frame(), Localization.lang("Merge entry with DOI information"), true);

        this.panel = panel;
        this.frame = panel.frame();

        if (panel.getSelectedEntries().length != 1) {
            // @formatter:off
            JOptionPane.showMessageDialog(frame, Localization.lang("Select one entry."),
                    Localization.lang("Merge entry from DOI"), JOptionPane.INFORMATION_MESSAGE);
            // @formatter:on
            this.dispose();
            return;
        }

        this.originalEntry = panel.getSelectedEntries()[0];
        panel.output(Localization.lang("Fetching info based on DOI"));
        this.doiEntry = doiFetcher.getEntryFromDOI(this.originalEntry.getField("doi"), null);

        if (this.doiEntry == null) {
            panel.output("");
            // @formatter:off
            JOptionPane.showMessageDialog(frame, Localization.lang("Cannot get info based on given DOI: %0", this.originalEntry.getField("doi")),
                    Localization.lang("Merge entry from DOI"), JOptionPane.INFORMATION_MESSAGE);
            // @formatter:on
            this.dispose();
            return;
        }

        panel.output(Localization.lang("Opening dialog"));
        // Start setting up the dialog
        init();
    }

    /**
     * Sets up the dialog
     *
     * @param selected Selected BibtexEntries
     */
    private void init() {
        // @formatter:off
        mergeEntries = new MergeEntries(this.originalEntry, this.doiEntry, Localization.lang("Original entry"),
                Localization.lang("Entry from DOI"));
        // @formatter:on

        // Create undo-compound
        ce = new NamedCompound(Localization.lang("Merge from DOI"));

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

        JButton replaceentry = new JButton(Localization.lang("Replace original entry"));
        replaceentry.setActionCommand("done");
        replaceentry.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                buttonPressed(e.getActionCommand());
            }
        });

        bb.addButton(new JButton[] {replaceentry, cancel});
        this.add(bb.getPanel(), cc.xy(1, 5));

        // Add some margin around the layout
        layout.appendRow(RowSpec.decode("5px"));
        layout.appendColumn(ColumnSpec.decode("5px"));
        layout.insertRow(1, RowSpec.decode("5px"));
        layout.insertColumn(1, ColumnSpec.decode("5px"));

        pw = new PositionWindow(this, JabRefPreferences.MERGEENTRIES_POS_X,
                JabRefPreferences.MERGEENTRIES_POS_Y, JabRefPreferences.MERGEENTRIES_SIZE_X,
                JabRefPreferences.MERGEENTRIES_SIZE_Y);
        pw.setWindowPosition();

        // Set up a ComponentListener that saves the last size and position of the dialog
        addComponentListener(new ComponentAdapter() {

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

        // Show what we've got
        setVisible(true);

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
        } else if (button.equals("done")) {
            // Create a new entry and add it to the undo stack
            // Remove the old entry and add it to the undo stack (which is not working...)
            Set<String> joint = new TreeSet<>(mergedEntry.getFieldNames());
            Boolean edited = false;

            for (String field : joint) {
                String originalString = originalEntry.getField(field);
                String mergedString = mergedEntry.getField(field);
                if ((originalString == null) || !originalString.equals(mergedEntry.getField(field))) {
                    originalEntry.setField(field, mergedString);
                    ce.addEdit(new UndoableFieldChange(originalEntry, field, originalString, mergedString));
                    edited = true;
                }
            }

            if (edited) {
                ce.end();
                panel.undoManager.addEdit(ce);
                panel.output(Localization.lang("Updated entry with info from DOI"));
                panel.markBaseChanged();
            } else {
                panel.output(Localization.lang("No information added"));
            }
        }
        dispose();
    }
}