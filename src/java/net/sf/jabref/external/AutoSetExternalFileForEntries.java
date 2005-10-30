package net.sf.jabref.external;

import net.sf.jabref.*;
import net.sf.jabref.gui.AttachFileDialog;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

import javax.swing.*;
import java.io.File;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;

/**
 * This action goes through all selected entries in the BasePanel, and attempts to autoset the
 * given external file (pdf, ps, ...) based on the same algorithm used for the "Auto" button in
 * EntryEditor.
 */
public class AutoSetExternalFileForEntries extends AbstractWorker {

    private String fieldName;
    private BasePanel panel;
    private BibtexEntry[] sel = null;
    private OptionsDialog optDiag = null;

    Object[] brokenLinkOptions =
        { Globals.lang("Ignore"), Globals.lang("Assign new file"), Globals.lang("Clear field"),
            Globals.lang("Quit synchronization")};

    private boolean goOn = true, autoSet = true, overWriteAllowed = true, checkExisting = true;

    private int skipped=0, entriesChanged=0, brokenLinks=0;

    public AutoSetExternalFileForEntries(BasePanel panel, String fieldName) {
        this.fieldName = fieldName;
        this.panel = panel;
    }

    public void init() {

        // Get all entries, and make sure there are selected entries:
        panel.database().getEntries();
        sel = (BibtexEntry[])panel.database().getEntries().toArray(new BibtexEntry[0]);
        if (sel.length < 1) {
            goOn = false;
            return;
        }

        // Ask about rules for the operation:
        if (optDiag == null)
            optDiag = new OptionsDialog(panel.frame(), fieldName);
        Util.placeDialog(optDiag, panel.frame());
        optDiag.setVisible(true);
        if (optDiag.canceled()) {
            goOn = false;
            return;
        }
        autoSet = !optDiag.autoSetNone.isSelected();
        overWriteAllowed = optDiag.autoSetAll.isSelected();
        checkExisting = optDiag.checkLinks.isSelected();

        panel.output(Globals.lang("Synchronizing %0 links...", fieldName.toUpperCase()));
    }

    public void run() {

        if (!goOn)
            return;

        skipped=0;
        entriesChanged=0;
        brokenLinks=0;
        NamedCompound ce = new NamedCompound(Globals.lang("Autoset %0 field", fieldName));

        final OpenFileFilter off = Util.getFileFilterForField(fieldName);

        ExternalFilePanel extPan = new ExternalFilePanel(fieldName, null, off);
        FieldTextField editor = new FieldTextField(fieldName, "", false);
        String dir = Globals.prefs.get(fieldName+"Directory");
        // First we try to autoset fields
        if (autoSet) {
            for (int i=0; i<sel.length; i++) {
                final Object old = sel[i].getField(fieldName);
                // Check if a link is already set, and if so, if we are allowed to overwrite it:
                if ((old != null) && !old.equals("") && !overWriteAllowed)
                    continue;
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
        //System.out.println("Done setting");
        // The following loop checks all external links that are already set.
        if (checkExisting) {
            mainLoop: for (int i=0; i<sel.length; i++) {
                final Object old = sel[i].getField(fieldName);
                // Check if a link is set:
                if ((old != null) && !((String)old).equals("")) {
                    // Get an absolute path representation:
                    File file = Util.expandFilename((String)old, dir);

                    if ((file == null) || !file.exists()) {

                        int answer =
                            JOptionPane.showOptionDialog(panel.frame(),
                            Globals.lang("<HTML>Could not find file '%0'<BR>linked from entry '%1'</HTML>",
                                new String[] {(String)old, sel[i].getCiteKey()}),
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

        panel.output(Globals.lang("Finished synchronizing %0 links. Entries changed%c %1.",
                new String[] {fieldName.toUpperCase(), String.valueOf(entriesChanged)}));
        if (entriesChanged > 0) {
            panel.markBaseChanged();
        }
    }

    static class OptionsDialog extends JDialog {
        JRadioButton autoSetUnset, autoSetAll, autoSetNone;
        JCheckBox checkLinks;
        JButton ok = new JButton(Globals.lang("Ok")),
            cancel = new JButton(Globals.lang("Cancel"));
        JLabel description;
        private boolean canceled = true;
        private String fieldName;

        public OptionsDialog(JFrame parent, String fieldName) {
            super(parent, Globals.lang("Synchronize %0 links", fieldName.toUpperCase()), true);
            final String fn = fieldName.toUpperCase();
            this.fieldName = fieldName;
            ok.addActionListener(new ActionListener () {
                public void actionPerformed(ActionEvent e) {
                    canceled = false;
                    dispose();
                }
            });

            Action closeAction = new AbstractAction () {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            };


            cancel.addActionListener(closeAction);

            InputMap im = cancel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	        ActionMap am = cancel.getActionMap();
            im.put(Globals.prefs.getKey("Close dialog"), "close");
	        am.put("close", closeAction);

            fieldName = fieldName.toUpperCase();
            autoSetUnset =  new JRadioButton(Globals.lang("Autoset %0 links. Do not overwrite existing links.", fn), true);
            autoSetAll =  new JRadioButton(Globals.lang("Autoset %0 links. Allow overwriting existing links.", fn), false);
            autoSetNone =  new JRadioButton(Globals.lang("Do not autoset"), false);
            checkLinks = new JCheckBox(Globals.lang("Check existing %0 links", fn), true);
            ButtonGroup bg = new ButtonGroup();
            bg.add(autoSetUnset);
            bg.add(autoSetNone);
            bg.add(autoSetAll);
            FormLayout layout = new FormLayout("fill:pref","");
	        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            description = new JLabel("<HTML>"+
                    Globals.lang(//"This function helps you keep your external %0 links up-to-date." +
                            "Attempt to autoset %0 links for your entries. Autoset works if "
                            +"a %0 file in your %0 directory or a subdirectory<BR>is named identically to an entry's BibTeX key, plus extension.", fn)
                    +"</HTML>");
            //            description.setVerticalAlignment(JLabel.TOP);
            builder.appendSeparator(Globals.lang("Autoset"));
            builder.append(description);
            builder.nextLine();
            builder.append(autoSetUnset);
            builder.nextLine();
            builder.append(autoSetAll);
            builder.nextLine();
            builder.append(autoSetNone);
            builder.nextLine();
            builder.appendSeparator(Globals.lang("Check links"));

            description = new JLabel("<HTML>"+
                    Globals.lang("This makes JabRef look up each %0 link and check if the file exists. If not, you will "
                                    +"be given options<BR>to resolve the problem.", fn)
                +"</HTML>");
            builder.append(description);
            builder.nextLine();
            builder.append(checkLinks);
            builder.nextLine();
            builder.appendSeparator();



            JPanel main = builder.getPanel();
            main.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

            ButtonBarBuilder bb = new ButtonBarBuilder();
            bb.addGlue();
            bb.addGridded(ok);
            bb.addGridded(cancel);
            bb.addGlue();
            getContentPane().add(main, BorderLayout.CENTER);
            getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

            pack();
        }

        public void setVisible(boolean visible) {
            if (visible)
                canceled = true;

            String dir = Globals.prefs.get(fieldName+"Directory");
            if ((dir == null) || (dir.trim().length() == 0)) {

                autoSetNone.setSelected(true);
                autoSetNone.setEnabled(false);
                autoSetAll.setEnabled(false);
                autoSetUnset.setEnabled(false);
            }
            else {
                autoSetNone.setEnabled(true);
                autoSetAll.setEnabled(true);
                autoSetUnset.setEnabled(true);
            }

            new FocusRequester(ok);
            super.setVisible(visible);

        }

        public boolean canceled() {
            return canceled;
        }
    }
}
