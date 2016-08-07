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

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableChangeType;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.InternalBibtexFields;
import net.sf.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * Dialog for merging Bibtex entry with data fetched from DOI
 */
public class MergeFetchedEntryDialog extends JDialog {

    private final BasePanel panel;
    private final CellConstraints cc = new CellConstraints();
    private BibEntry originalEntry;
    private BibEntry fetchedEntry;
    private NamedCompound ce;
    private MergeEntries mergeEntries;

    private static final String MARGIN = "5px";

    public MergeFetchedEntryDialog(BasePanel panel, BibEntry originalEntry, BibEntry fetchedEntry) {
        super(panel.frame(), Localization.lang("Merge entry with fetched information"), true);

        this.panel = panel;
        this.originalEntry = originalEntry;
        this.fetchedEntry = fetchedEntry;

        if (panel.getSelectedEntries().size() != 1) {
            JOptionPane.showMessageDialog(panel.frame(),
                    Localization.lang("This operation requires exactly one item to be selected."),
                    Localization.lang("Merge entry with fetched information"), JOptionPane.INFORMATION_MESSAGE);
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
        mergeEntries = new MergeEntries(this.originalEntry, this.fetchedEntry, Localization.lang("Original entry"),
                Localization.lang("Fetched entry"), panel.getBibDatabaseContext().getMode());

        // Create undo-compound
        ce = new NamedCompound(Localization.lang("Merge entry with fetched information"));

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
                Optional<String> originalString = originalEntry.getFieldOptional(field);
                Optional<String> mergedString = mergedEntry.getFieldOptional(field);
                if (!originalString.isPresent() || !originalString.equals(mergedString)) {
                    originalEntry.setField(field, mergedString.get()); // mergedString always present
                    ce.addEdit(new UndoableFieldChange(originalEntry, field, originalString.orElse(null),
                            mergedString.get()));
                    edited = true;
                }
            }

            // Remove fields which are not in the merged entry, unless they are internal fields
            for (String field : originalFields) {
                if (!jointFields.contains(field) && !InternalBibtexFields.isInternalField(field)) {
                    Optional<String> originalString = originalEntry.getFieldOptional(field);
                    originalEntry.clearField(field);
                    ce.addEdit(new UndoableFieldChange(originalEntry, field, originalString.get(), null)); // originalString always present
                    edited = true;
                }
            }

            if (edited) {
                ce.end();
                panel.getUndoManager().addEdit(ce);
                panel.output(Localization.lang("Updated entry with fetched information"));
                panel.updateEntryEditorIfShowing();
                panel.markBaseChanged();
            } else {
                panel.output(Localization.lang("No information added"));
            }
        }
        dispose();
    }
}
