/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.external;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.gui.AttachFileDialog;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import net.sf.jabref.util.FileUtil;
import net.sf.jabref.util.Util;

/**
 * This action goes through all selected entries in the BasePanel, and attempts to autoset the given external file (pdf,
 * ps, ...) based on the same algorithm used for the "Auto" button in EntryEditor.
 */
public class AutoSetExternalFileForEntries extends AbstractWorker {

    private final String fieldName;
    private final BasePanel panel;
    private BibtexEntry[] sel = null;
    private OptionsDialog optDiag = null;

    private static final Log log = LogFactory.getLog(AutoSetExternalFileForEntries.class);

    private final Object[] brokenLinkOptions =
    {Globals.lang("Ignore"), Globals.lang("Assign new file"), Globals.lang("Clear field"),
            Globals.lang("Quit synchronization")};

    private boolean goOn = true, autoSet = true, overWriteAllowed = true, checkExisting = true;

    private int entriesChanged = 0;


    public AutoSetExternalFileForEntries(BasePanel panel, String fieldName) {
        this.fieldName = fieldName;
        this.panel = panel;
    }

    @Override
    public void init() {

        Collection<BibtexEntry> col = panel.database().getEntries();
        sel = col.toArray(new BibtexEntry[col.size()]);

        // Ask about rules for the operation:
        if (optDiag == null) {
            optDiag = new OptionsDialog(panel.frame(), fieldName);
        }
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

    @Override
    public void run() {
        if (!goOn) {
            panel.output(Globals.lang("No entries selected."));
            return;
        }
        panel.frame().setProgressBarValue(0);
        panel.frame().setProgressBarVisible(true);
        int weightAutoSet = 10; // autoSet takes 10 (?) times longer than checkExisting
        int progressBarMax = (autoSet ? weightAutoSet * sel.length : 0)
                + (checkExisting ? sel.length : 0);
        panel.frame().setProgressBarMaximum(progressBarMax);
        int progress = 0;
        entriesChanged = 0;
        int brokenLinks = 0;
        NamedCompound ce = new NamedCompound(Globals.lang("Autoset %0 field", fieldName));

        final OpenFileFilter off = Util.getFileFilterForField(fieldName);

        ExternalFilePanel extPan = new ExternalFilePanel(fieldName, panel.metaData(), null, null, off);
        FieldTextField editor = new FieldTextField(fieldName, "", false);

        // Find the default directory for this field type:
        String[] dirs = panel.metaData().getFileDirectory(fieldName);

        // First we try to autoset fields
        if (autoSet) {
            for (BibtexEntry aSel : sel) {
                progress += weightAutoSet;
                panel.frame().setProgressBarValue(progress);

                final String old = aSel.getField(fieldName);
                // Check if a extension is already set, and if so, if we are allowed to overwrite it:
                if ((old != null) && !old.equals("") && !overWriteAllowed) {
                    continue;
                }
                extPan.setEntry(aSel, panel.getDatabase());
                editor.setText((old != null) ? old : "");
                JabRefExecutorService.INSTANCE.executeAndWait(extPan.autoSetFile(fieldName, editor));
                // If something was found, entriesChanged it:
                if (!editor.getText().equals("") && !editor.getText().equals(old)) {
                    // Store an undo edit:
                    //System.out.println("Setting: "+sel[i].getCiteKey()+" "+editor.getText());
                    ce.addEdit(new UndoableFieldChange(aSel, fieldName, old, editor.getText()));
                    aSel.setField(fieldName, editor.getText());
                    entriesChanged++;
                }
            }
        }
        //System.out.println("Done setting");
        // The following loop checks all external links that are already set.
        if (checkExisting) {
            mainLoop: for (BibtexEntry aSel : sel) {
                panel.frame().setProgressBarValue(progress++);
                final String old = aSel.getField(fieldName);
                // Check if a extension is set:
                if ((old != null) && !old.equals("")) {
                    // Get an absolute path representation:
                    File file = FileUtil.expandFilename(old, dirs);

                    if ((file == null) || !file.exists()) {

                        int answer =
                                JOptionPane.showOptionDialog(panel.frame(),
                                        Globals.lang("<HTML>Could not find file '%0'<BR>linked from entry '%1'</HTML>",
                                                new String[] {old, aSel.getCiteKey()}),
                                        Globals.lang("Broken link"),
                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null, brokenLinkOptions, brokenLinkOptions[0]
                                        );
                        switch (answer) {
                        case 1:
                            // Assign new file.
                            AttachFileDialog afd = new AttachFileDialog(panel.frame(),
                                    panel.metaData(), aSel, fieldName);
                            Util.placeDialog(afd, panel.frame());
                            afd.setVisible(true);
                            if (!afd.cancelled()) {
                                ce.addEdit(new UndoableFieldChange(aSel, fieldName, old, afd.getValue()));
                                aSel.setField(fieldName, afd.getValue());
                                entriesChanged++;
                            }
                            break;
                        case 2:
                            // Clear field
                            ce.addEdit(new UndoableFieldChange(aSel, fieldName, old, null));
                            aSel.setField(fieldName, null);
                            entriesChanged++;
                            break;
                        case 3:
                            // Cancel
                            break mainLoop;
                        }
                        brokenLinks++;
                    }

                }
            }
        }

        //log brokenLinks if some were found
        if (brokenLinks > 0) {
            log.warn(Globals.lang("Found %0 broken links", brokenLinks + ""));
        }

        if (entriesChanged > 0) {
            // Add the undo edit:
            ce.end();
            panel.undoManager.addEdit(ce);
        }
    }

    @Override
    public void update() {
        if (!goOn) {
            return;
        }

        panel.output(Globals.lang("Finished synchronizing %0 links. Entries changed%c %1.",
                new String[] {fieldName.toUpperCase(), String.valueOf(entriesChanged)}));
        panel.frame().setProgressBarVisible(false);
        if (entriesChanged > 0) {
            panel.markBaseChanged();
        }
    }


    class OptionsDialog extends JDialog {

        private static final long serialVersionUID = 1L;
        final JRadioButton autoSetUnset;
        final JRadioButton autoSetAll;
        final JRadioButton autoSetNone;
        final JCheckBox checkLinks;
        final JButton ok = new JButton(Globals.lang("Ok"));
        final JButton cancel = new JButton(Globals.lang("Cancel"));
        JLabel description;
        private boolean canceled = true;
        private final String fieldName;


        public OptionsDialog(JFrame parent, String fieldName) {
            super(parent, Globals.lang("Synchronize %0 links", fieldName.toUpperCase()), true);
            final String fn = fieldName.toUpperCase();
            this.fieldName = fieldName;
            ok.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    canceled = false;
                    dispose();
                }
            });

            Action closeAction = new AbstractAction() {

                private static final long serialVersionUID = 1L;


                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            };

            cancel.addActionListener(closeAction);

            InputMap im = cancel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = cancel.getActionMap();
            im.put(Globals.prefs.getKey("Close dialog"), "close");
            am.put("close", closeAction);

            autoSetUnset = new JRadioButton(Globals.lang("Autoset %0 links. Do not overwrite existing links.", fn), true);
            autoSetAll = new JRadioButton(Globals.lang("Autoset %0 links. Allow overwriting existing links.", fn), false);
            autoSetNone = new JRadioButton(Globals.lang("Do not autoset"), false);
            checkLinks = new JCheckBox(Globals.lang("Check existing %0 links", fn), true);
            ButtonGroup bg = new ButtonGroup();
            bg.add(autoSetUnset);
            bg.add(autoSetNone);
            bg.add(autoSetAll);
            FormLayout layout = new FormLayout("fill:pref", "");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            description = new JLabel("<HTML>" +
                    Globals.lang(//"This function helps you keep your external %0 links up-to-date." +
                            "Attempt to autoset %0 links for your entries. Autoset works if "
                                    + "a %0 file in your %0 directory or a subdirectory<BR>is named identically to an entry's BibTeX key, plus extension.", fn)
                    + "</HTML>");
            //            name.setVerticalAlignment(JLabel.TOP);
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

            description = new JLabel("<HTML>" +
                    Globals.lang("This makes JabRef look up each %0 link and check if the file exists. If not, you will be given options<BR>to resolve the problem.", fn)
                    + "</HTML>");
            builder.append(description);
            builder.nextLine();
            builder.append(checkLinks);
            builder.nextLine();
            builder.appendSeparator();

            JPanel main = builder.getPanel();
            main.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            ButtonBarBuilder bb = new ButtonBarBuilder();
            bb.addGlue();
            bb.addButton(ok);
            bb.addButton(cancel);
            bb.addGlue();
            getContentPane().add(main, BorderLayout.CENTER);
            getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

            pack();
        }

        @Override
        public void setVisible(boolean visible) {
            if (visible) {
                canceled = true;
            }

            String[] dirs = panel.metaData().getFileDirectory(fieldName);

            if (dirs.length == 0) {

                autoSetNone.setSelected(true);
                autoSetNone.setEnabled(false);
                autoSetAll.setEnabled(false);
                autoSetUnset.setEnabled(false);
            } else {
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
