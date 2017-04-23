package org.jabref.gui.mergeentries;

import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JSeparator;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableChangeType;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.util.WindowLocation;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * Dialog for merging Bibtex entry with fetched data
 */
public class MergeFetchedEntryDialog extends JabRefDialog {

    private static final String MARGIN = "5px";
    private final BasePanel panel;
    private final CellConstraints cc = new CellConstraints();
    private final BibEntry originalEntry;
    private final BibEntry fetchedEntry;
    private NamedCompound ce;
    private MergeEntries mergeEntries;
    private final String type;


    public MergeFetchedEntryDialog(BasePanel panel, BibEntry originalEntry, BibEntry fetchedEntry, String type) {
        super(panel.frame(), Localization.lang("Merge entry with %0 information", type), true, MergeFetchedEntryDialog.class);

        this.panel = panel;
        this.originalEntry = originalEntry;
        this.fetchedEntry = fetchedEntry;
        this.type = type;

        // Start setting up the dialog
        init();
    }

    /**
     * Sets up the dialog
     */
    private void init() {
        mergeEntries = new MergeEntries(this.originalEntry, this.fetchedEntry, Localization.lang("Original entry"),
                Localization.lang("Entry from %0", type), panel.getBibDatabaseContext().getMode());

        // Create undo-compound
        ce = new NamedCompound(Localization.lang("Merge entry with %0 information", type));

        FormLayout layout = new FormLayout("fill:700px:grow", "fill:400px:grow, 4px, p, 5px, p");
        this.setLayout(layout);

        this.add(mergeEntries.getMergeEntryPanel(), cc.xy(1, 1));
        this.add(new JSeparator(), cc.xy(1, 3));

        // Create buttons
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();

        JButton cancel = new JButton(new CancelAction());
        JButton replaceEntry = new JButton(new ReplaceAction());

        bb.addButton(replaceEntry, cancel);
        this.add(bb.getPanel(), cc.xy(1, 5));

        // Add some margin around the layout
        layout.appendRow(RowSpec.decode(MARGIN));
        layout.appendColumn(ColumnSpec.decode(MARGIN));
        layout.insertRow(1, RowSpec.decode(MARGIN));
        layout.insertColumn(1, ColumnSpec.decode(MARGIN));

        WindowLocation pw = new WindowLocation(this, JabRefPreferences.MERGEENTRIES_POS_X,
                JabRefPreferences.MERGEENTRIES_POS_Y, JabRefPreferences.MERGEENTRIES_SIZE_X,
                JabRefPreferences.MERGEENTRIES_SIZE_Y);
        pw.displayWindowAtStoredLocation();

    }

    private class CancelAction extends AbstractAction {
        CancelAction() {
            putValue(Action.NAME, Localization.lang("Cancel"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            panel.output(Localization.lang("Canceled merging entries"));
            dispose();
        }
    }

    private class ReplaceAction extends AbstractAction {
        ReplaceAction() {
            putValue(Action.NAME, Localization.lang("Replace original entry"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            BibEntry mergedEntry = mergeEntries.getMergeEntry();

            // Updated the original entry with the new fields
            Set<String> jointFields = new TreeSet<>(mergedEntry.getFieldNames());
            Set<String> originalFields = new TreeSet<>(originalEntry.getFieldNames());
            boolean edited = false;

            // entry type
            String oldType = originalEntry.getType();
            String newType = mergedEntry.getType();

            if (!oldType.equalsIgnoreCase(newType)) {
                originalEntry.setType(newType);
                ce.addEdit(new UndoableChangeType(originalEntry, oldType, newType));
                edited = true;
            }

            // fields
            for (String field : jointFields) {
                Optional<String> originalString = originalEntry.getField(field);
                Optional<String> mergedString = mergedEntry.getField(field);
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
                    Optional<String> originalString = originalEntry.getField(field);
                    originalEntry.clearField(field);
                    ce.addEdit(new UndoableFieldChange(originalEntry, field, originalString.get(), null)); // originalString always present
                    edited = true;
                }
            }

            if (edited) {
                ce.end();
                panel.getUndoManager().addEdit(ce);
                panel.output(Localization.lang("Updated entry with info from %0", type));
                panel.updateEntryEditorIfShowing();
                panel.markBaseChanged();
            } else {
                panel.output(Localization.lang("No information added"));
            }

            dispose();
        }
    }
}
