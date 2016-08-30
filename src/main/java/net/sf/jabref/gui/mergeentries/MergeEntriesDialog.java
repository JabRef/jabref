package net.sf.jabref.gui.mergeentries;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.gui.util.WindowLocation;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * @author Oscar
 *
 *         Dialog for merging two Bibtex entries
 */
public class MergeEntriesDialog extends JDialog {

    private final BasePanel panel;
    private final CellConstraints cc = new CellConstraints();

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
    private void init(List<BibEntry> selected) {

        // Check if there are two entries selected
        if (selected.size() != 2) { // None selected. Inform the user to select entries first.
            JOptionPane.showMessageDialog(panel.frame(),
                    Localization.lang("You have to choose exactly two entries to merge."),
                    MERGE_ENTRIES, JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
            return;
        }

        // Store the two entries
        BibEntry one = selected.get(0);
        BibEntry two = selected.get(1);

        MergeEntries mergeEntries = new MergeEntries(one, two, panel.getBibDatabaseContext().getMode());

        // Create undo-compound
        NamedCompound ce = new NamedCompound(MERGE_ENTRIES);

        FormLayout layout = new FormLayout("fill:700px:grow", "fill:400px:grow, 4px, p, 5px, p");
        this.setLayout(layout);

        this.add(mergeEntries.getMergeEntryPanel(), cc.xy(1, 1));
        this.add(new JSeparator(), cc.xy(1, 3));

        // Create buttons
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        JButton cancel = new JButton(Localization.lang("Cancel"));
        cancel.setActionCommand("cancel");
        cancel.addActionListener(e -> {
            panel.output(Localization.lang("Canceled merging entries"));
            dispose();
        });

        JButton replaceentries = new JButton(MERGE_ENTRIES);
        replaceentries.setActionCommand("replace");
        replaceentries.addActionListener(e -> {
            // Create a new entry and add it to the undo stack
            // Remove the other two entries and add them to the undo stack (which is not working...)
            BibEntry mergedEntry = mergeEntries.getMergeEntry();
            panel.insertEntry(mergedEntry);
            ce.addEdit(new UndoableInsertEntry(panel.getDatabase(), mergedEntry, panel));
            ce.addEdit(new UndoableRemoveEntry(panel.getDatabase(), one, panel));
            panel.getDatabase().removeEntry(one);
            ce.addEdit(new UndoableRemoveEntry(panel.getDatabase(), two, panel));
            panel.getDatabase().removeEntry(two);
            ce.end();
            panel.getUndoManager().addEdit(ce);
            panel.output(Localization.lang("Merged entries"));
            dispose();
        });

        bb.addButton(new JButton[] {replaceentries, cancel});
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

        // Show what we've got
        setVisible(true);
    }
}
