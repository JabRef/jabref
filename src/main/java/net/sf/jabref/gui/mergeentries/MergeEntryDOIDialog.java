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

import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableChangeType;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.importer.fetcher.DOItoBibTeXFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * @author Oscar
 *
 *         Dialog for merging Bibtex entry with data fetched from DOI
 */
public class MergeEntryDOIDialog extends JDialog {

    private final BasePanel panel;
    private final CellConstraints cc = new CellConstraints();
    private BibEntry originalEntry;
    private BibEntry doiEntry;
    private NamedCompound ce;
    private MergeEntries mergeEntries;

    private final DOItoBibTeXFetcher doiFetcher = new DOItoBibTeXFetcher();

    private static final String MARGIN = "5px";


    public MergeEntryDOIDialog(BasePanel panel) {
        super(panel.frame(), Localization.lang("Merge entry with DOI information"), true);

        this.panel = panel;

        if (panel.getSelectedEntries().size() != 1) {
            JOptionPane.showMessageDialog(panel.frame(), Localization.lang("Select one entry."),
                    Localization.lang("Merge entry with DOI information"), JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
            return;
        }

        this.originalEntry = panel.getSelectedEntries().get(0);
        panel.output(Localization.lang("Fetching info based on DOI"));
        this.doiEntry = doiFetcher.getEntryFromDOI(this.originalEntry.getField("doi"), null);

        if (this.doiEntry == null) {
            panel.output("");
            JOptionPane.showMessageDialog(panel.frame(),
                    Localization.lang("Cannot get info based on given DOI: %0", this.originalEntry.getField("doi")),
                    Localization.lang("Merge entry with DOI information"), JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
            return;
        }

        panel.output(Localization.lang("Opening dialog"));
        // Start setting up the dialog
        init();
    }

    /**
     * Sets up the dialog
     */
    private void init() {
        mergeEntries = new MergeEntries(this.originalEntry, this.doiEntry, Localization.lang("Original entry"),
                Localization.lang("Entry from DOI"), panel.getBibDatabaseContext().getMode());

        // Create undo-compound
        ce = new NamedCompound(Localization.lang("Merge entry with DOI information"));

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
        cancel.addActionListener(e -> buttonPressed(e.getActionCommand()));

        JButton replaceentry = new JButton(Localization.lang("Replace original entry"));
        replaceentry.setActionCommand("done");
        replaceentry.addActionListener(e -> buttonPressed(e.getActionCommand()));

        bb.addButton(new JButton[] {replaceentry, cancel});
        this.add(bb.getPanel(), cc.xy(1, 5));

        // Add some margin around the layout
        layout.appendRow(RowSpec.decode(MARGIN));
        layout.appendColumn(ColumnSpec.decode(MARGIN));
        layout.insertRow(1, RowSpec.decode(MARGIN));
        layout.insertColumn(1, ColumnSpec.decode(MARGIN));

        PositionWindow pw = new PositionWindow(this, JabRefPreferences.MERGEENTRIES_POS_X,
                JabRefPreferences.MERGEENTRIES_POS_Y, JabRefPreferences.MERGEENTRIES_SIZE_X,
                JabRefPreferences.MERGEENTRIES_SIZE_Y);
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
            // Canceled, throw it away
            panel.output(Localization.lang("Canceled merging entries"));
        } else if ("done".equals(button)) {
            // Updated the original entry with the new fields
            Set<String> jointFields = new TreeSet<>(mergedEntry.getFieldNames());
            Set<String> originalFields = new TreeSet<>(originalEntry.getFieldNames());
            boolean edited = false;

            // entry type
            String oldType = originalEntry.getType();
            String newType = mergedEntry.getType();

            if(!oldType.equalsIgnoreCase(newType)) {
                originalEntry.setType(newType);
                ce.addEdit(new UndoableChangeType(originalEntry, oldType, newType));
                edited = true;
            }

            // fields
            for (String field : jointFields) {
                String originalString = originalEntry.getField(field);
                String mergedString = mergedEntry.getField(field);
                if ((originalString == null) || !originalString.equals(mergedEntry.getField(field))) {
                    originalEntry.setField(field, mergedString);
                    ce.addEdit(new UndoableFieldChange(originalEntry, field, originalString, mergedString));
                    edited = true;
                }
            }

            // Remove fields which are not in the merged entry
            for (String field : originalFields) {
                if (!jointFields.contains(field)) {
                    String originalString = originalEntry.getField(field);
                    originalEntry.clearField(field);
                    ce.addEdit(new UndoableFieldChange(originalEntry, field, originalString, null));
                    edited = true;
                }
            }

            if (edited) {
                ce.end();
                panel.undoManager.addEdit(ce);
                panel.output(Localization.lang("Updated entry with info from DOI"));
                panel.updateEntryEditorIfShowing();
                panel.markBaseChanged();
            } else {
                panel.output(Localization.lang("No information added"));
            }
        }
        dispose();
    }
}
