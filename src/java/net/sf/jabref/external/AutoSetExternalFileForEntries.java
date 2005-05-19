package net.sf.jabref.external;

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

/**
 * This action goes through all selected entries in the BasePanel, and attempts to autoset the
 * given external file (pdf, ps, ...) based on the same algorithm used for the "Auto" button in
 * EntryEditor.
 */
public class AutoSetExternalFileForEntries extends AbstractWorker {

    private String fieldName;
    private BasePanel panel;
    private BibtexEntry[] sel = null;
    private boolean goOn = true, overWriteAllowed = true;
    private int skipped=0, set=0;

    public AutoSetExternalFileForEntries(BasePanel panel, String fieldName) {
        this.fieldName = fieldName;
        this.panel = panel;
    }

    public void init() {

        // Get selected entries, and make sure there are selected entries:
        sel = panel.getSelectedEntries();
        if (sel.length < 1) {
            goOn = false;
            return;
        }

        // Ask about rules for the operation:


        panel.output(Globals.lang("Autosetting %0 field...", fieldName));
    }

    public void run() {

        if (!goOn)
            return;

        skipped=0;
        set=0;
        NamedCompound ce = new NamedCompound(Globals.lang("Autoset %0 field", fieldName));
        ExternalFilePanel extPan = new ExternalFilePanel(fieldName, null);
        FieldTextField editor = new FieldTextField(fieldName, "", false);
        for (int i=0; i<sel.length; i++) {
            //System.out.println("Checking: "+sel[i].getField("author"));
            final Object old = sel[i].getField(fieldName);
            // If we are not allowed to overwrite the old field values, check that none is set:
            if (!overWriteAllowed && (old != null) && !((String)old).equals("")) {
                skipped++;
                continue;
            }
            else {
                // Ok, do it:
                extPan.setEntry(sel[i]);
                editor.setText((old != null) ? (String)old : "");
                Thread t = extPan.autoSetFile(fieldName, editor);
                // Wait for the autoset process to finish:
                if (t != null)
                    try {
                        t.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                // If something was found, set it:
                if (!editor.getText().equals("") && !editor.getText().equals(old)) {
                    // Store an undo edit:
                    //System.out.println("Setting: "+sel[i].getCiteKey()+" "+editor.getText());
                    ce.addEdit(new UndoableFieldChange(sel[i], fieldName, old, editor.getText()));
                    sel[i].setField(fieldName, editor.getText());
                    set++;
                }

            }
        }

        if (set > 0) {
            // Add the undo edit:
            ce.end();
            panel.undoManager.addEdit(ce);
        }
    }

    public void update() {
        if (!goOn)
            return;

        panel.output(Globals.lang("Finished autosetting %0 field. Entries changed: %1.",
                new String[] {fieldName, String.valueOf(set)}));
        if (set > 0) {
            panel.markBaseChanged();
            panel.refreshTable();
        }
    }
}
