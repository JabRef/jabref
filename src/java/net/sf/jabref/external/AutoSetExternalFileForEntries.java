package net.sf.jabref.external;

import net.sf.jabref.*;
import net.sf.jabref.gui.AttachFileDialog;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

import javax.swing.*;
import java.io.File;

/**
 * This action goes through all selected entries in the BasePanel, and attempts to autoset the
 * given external file (pdf, ps, ...) based on the same algorithm used for the "Auto" button in
 * EntryEditor.
 */
public class AutoSetExternalFileForEntries extends AbstractWorker {

    private String fieldName;
    private BasePanel panel;
    private BibtexEntry[] sel = null;

    Object[] brokenLinkOptions =
        { Globals.lang("Ignore"), Globals.lang("Assign new file"), Globals.lang("Clear field"),
            Globals.lang("Cancel")};

    private boolean goOn = true, overWriteAllowed = true, checkExisting = true;

    private int skipped=0, entriesChanged=0, brokenLinks=0;

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
        entriesChanged=0;
        brokenLinks=0;
        NamedCompound ce = new NamedCompound(Globals.lang("Autoset %0 field", fieldName));
        ExternalFilePanel extPan = new ExternalFilePanel(fieldName, null);
        FieldTextField editor = new FieldTextField(fieldName, "", false);
        mainLoop: for (int i=0; i<sel.length; i++) {
            //System.out.println("Checking: "+sel[i].getField("author"));
            final Object old = sel[i].getField(fieldName);
            // Check if a key is already entriesChanged:
            if ((old != null) && !((String)old).equals("")) {
                // Should we check if the file exists?
                if (checkExisting) {
                    File file = new File((String)old);
                    if (!file.exists()) {
                        System.out.println("Broken link: "+file.getPath());

                        int answer =
                            JOptionPane.showOptionDialog(panel.frame(),
                            Globals.lang("<HTML>Could not find file '%0'<BR>linked from entry '%1'</HTML>",
                                new String[] {file.getPath(), sel[i].getCiteKey()}),
                                    Globals.lang("Broken link"),
                                    JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null, brokenLinkOptions, brokenLinkOptions[0]);
                        switch (answer) {
                            case 1:
                                // Assign new file.
                                AttachFileDialog afd = new AttachFileDialog(panel.frame(), sel[i], fieldName);
                                Util.placeDialog(afd, panel.frame());
                                afd.setVisible(true);
                                if (!afd.cancelled()) {
                                    ce.addEdit(new UndoableFieldChange(sel[i], fieldName, old, afd.getValue()));
                                    sel[i].setField(fieldName, afd.getValue());
                                    entriesChanged++;
                                }
                                break;
                            case 2:
                                 // Clear field
                                ce.addEdit(new UndoableFieldChange(sel[i], fieldName, old, null));
                                sel[i].setField(fieldName, null);
                                entriesChanged++;
                                break;
                            case 3:
                                // Cancel
                                break mainLoop;
                        }
                        brokenLinks++;
                    }

                    continue;
                }
                // Otherwise, should we be allowed to overwrite the old link?
                else if (!overWriteAllowed) {
                    skipped++;
                    continue;
                }
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
                // If something was found, entriesChanged it:
                if (!editor.getText().equals("") && !editor.getText().equals(old)) {
                    // Store an undo edit:
                    //System.out.println("Setting: "+sel[i].getCiteKey()+" "+editor.getText());
                    ce.addEdit(new UndoableFieldChange(sel[i], fieldName, old, editor.getText()));
                    sel[i].setField(fieldName, editor.getText());
                    entriesChanged++;
                }

            }
        }

        if (entriesChanged > 0) {
            // Add the undo edit:
            ce.end();
            panel.undoManager.addEdit(ce);
        }
    }

    public void update() {
        if (!goOn)
            return;

        panel.output(Globals.lang("Finished autosetting %0 field. Entries changed: %1.",
                new String[] {fieldName, String.valueOf(entriesChanged)}));
        if (entriesChanged > 0) {
            panel.markBaseChanged();
            panel.refreshTable();
        }
    }
}
