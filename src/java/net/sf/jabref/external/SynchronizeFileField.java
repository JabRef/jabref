package net.sf.jabref.external;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.*;
import net.sf.jabref.gui.FileListEditor;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;

/**
 * This action goes through all selected entries in the BasePanel, and attempts to autoset the
 * given external file (pdf, ps, ...) based on the same algorithm used for the "Auto" button in
 * EntryEditor.
 */
public class SynchronizeFileField extends AbstractWorker {

    private String fieldName = GUIGlobals.FILE_FIELD;
    private BasePanel panel;
    private BibtexEntry[] sel = null;
    private SynchronizeFileField.OptionsDialog optDiag = null;

    Object[] brokenLinkOptions =
            {Globals.lang("Ignore"), Globals.lang("Assign new file"), Globals.lang("Remove link"),
                    Globals.lang("Remove all broken links"),
                    Globals.lang("Quit synchronization")};

    private boolean goOn = true, autoSet = true, checkExisting = true;

    private int brokenLinks = 0, entriesChangedCount = 0;

    public SynchronizeFileField(BasePanel panel) {
        this.panel = panel;
    }

    public void init() {
        Collection<BibtexEntry> col = panel.database().getEntries();
        goOn = true;
        sel = new BibtexEntry[col.size()];
        sel = col.toArray(sel);

        // Ask about rules for the operation:
        if (optDiag == null)
            optDiag = new SynchronizeFileField.OptionsDialog(panel.frame(), panel.metaData(), fieldName);
        Util.placeDialog(optDiag, panel.frame());
        optDiag.setVisible(true);
        if (optDiag.canceled()) {
            goOn = false;
            return;
        }
        autoSet = !optDiag.autoSetNone.isSelected();
        checkExisting = optDiag.checkLinks.isSelected();
        
        panel.output(Globals.lang("Synchronizing %0 links...", fieldName.toUpperCase()));
    }

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
        brokenLinks = 0;
        final NamedCompound ce = new NamedCompound(Globals.lang("Autoset %0 field", fieldName));

        //final OpenFileFilter off = Util.getFileFilterForField(fieldName);

        //ExternalFilePanel extPan = new ExternalFilePanel(fieldName, panel.metaData(), null, null, off);
        //FieldTextField editor = new FieldTextField(fieldName, "", false);

        // Find the default directory for this field type:
        String dir = panel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD);
        Set<BibtexEntry> changedEntries = new HashSet<BibtexEntry>();

        // First we try to autoset fields
        if (autoSet) {
            Collection<BibtexEntry> entries = new ArrayList<BibtexEntry>();
            for (int i = 0; i < sel.length; i++) {
                entries.add(sel[i]);
            }

            // We need to specify which directories to search in:
            ArrayList<File> dirs = new ArrayList<File>();
            String dr = panel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD);
            if (dr != null)
                dirs.add(new File(dr));

            // Start the autosetting process:                
            Thread t = FileListEditor.autoSetLinks(entries, ce, changedEntries, dirs);
            // Wait for the autosetting to finish:
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            /*
                progress += weightAutoSet;
                panel.frame().setProgressBarValue(progress);

                Object old = sel[i].getField(fieldName);
                FileListTableModel tableModel = new FileListTableModel();
                if (old != null)
                    tableModel.setContent((String)old);
                Thread t = FileListEditor.autoSetLinks(sel[i], tableModel, null, null);

                if (!tableModel.getStringRepresentation().equals(old)) {
                    String toSet = tableModel.getStringRepresentation();
                    if (toSet.length() == 0)
                        toSet = null;
                    ce.addEdit(new UndoableFieldChange(sel[i], fieldName, old, toSet));
                    sel[i].setField(fieldName, toSet);
                    entriesChanged++;
                }
            }    */

        }
        progress += sel.length*weightAutoSet;
        panel.frame().setProgressBarValue(progress);
        //System.out.println("Done setting");
        // The following loop checks all external links that are already set.
        if (checkExisting) {
            boolean removeAllBroken = false;
            mainLoop:
            for (int i = 0; i < sel.length; i++) {
                panel.frame().setProgressBarValue(progress++);
                final String old = sel[i].getField(fieldName);
                // Check if a extension is set:
                if ((old != null) && !old.equals("")) {
                    FileListTableModel tableModel = new FileListTableModel();
                    tableModel.setContentDontGuessTypes(old);
                    for (int j=0; j<tableModel.getRowCount(); j++) {
                        FileListEntry flEntry = tableModel.getEntry(j);
                        // See if the link looks like an URL:
                        boolean httpLink = flEntry.getLink().toLowerCase().startsWith("http");
                        if (httpLink)
                            continue; // Don't check the remote file.
                        // TODO: should there be an option to check remote links?

                        // A variable to keep track of whether this link gets deleted:
                        boolean deleted = false;

                        // Get an absolute path representation:
                        File file = Util.expandFilename(flEntry.getLink(), new String[]{dir, "."});
                        if ((file == null) || !file.exists()) {
                            int answer;
                            if (!removeAllBroken) {
                                answer = JOptionPane.showOptionDialog(panel.frame(),
                                    Globals.lang("<HTML>Could not find file '%0'<BR>linked from entry '%1'</HTML>",
                                            new String[]{flEntry.getLink(), sel[i].getCiteKey()}),
                                    Globals.lang("Broken link"),
                                    JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null, brokenLinkOptions, brokenLinkOptions[0]);
                            }
                            else {
                                answer = 2; // We should delete this link.
                            }
                            switch (answer) {
                                case 1:
                                    // Assign new file.
                                    FileListEntryEditor flEditor = new FileListEntryEditor
                                            (panel.frame(), flEntry, false, true, panel.metaData());
                                    flEditor.setVisible(true, true);
                                    break;
                                case 2:
                                    // Clear field:
                                    tableModel.removeEntry(j);
                                    deleted = true; // Make sure we don't investigate this link further.
                                    j--; // Step back in the iteration, because we removed an entry.
                                    break;
                                case 3:
                                    // Clear field:
                                    tableModel.removeEntry(j);
                                    deleted = true; // Make sure we don't investigate this link further.
                                    j--; // Step back in the iteration, because we removed an entry.
                                    removeAllBroken = true; // Notify for further cases.
                                    break;
                                case 4:
                                    // Cancel
                                    break mainLoop;
                            }
                            brokenLinks++;
                        }

                        // Unless we deleted this link, see if its file type is recognized:
                        if (!deleted && (flEntry.getType() instanceof UnknownExternalFileType)) {
                            String[] options = new String[]
                                    {Globals.lang("Define '%0'", flEntry.getType().getName()),
                                    Globals.lang("Change file type"), Globals.lang("Cancel")};
                            String defOption = options[0];
                            int answer = JOptionPane.showOptionDialog(panel.frame(), Globals.lang("One or more file links are of the type '%0', which is undefined. What do you want to do?",
                                    flEntry.getType().getName()),
                                    Globals.lang("Undefined file type"), JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null, options, defOption);
                            if (answer == JOptionPane.CANCEL_OPTION) {
                                // User doesn't want to handle this unknown link type.
                            }
                            else if (answer == JOptionPane.YES_OPTION) {
                                // User wants to define the new file type. Show the dialog:
                                ExternalFileType newType = new ExternalFileType(flEntry.getType().getName(), "", "", "", "new");
                                ExternalFileTypeEntryEditor editor = new ExternalFileTypeEntryEditor(panel.frame(), newType);
                                editor.setVisible(true);
                                if (editor.okPressed()) {
                                    // Get the old list of types, add this one, and update the list in prefs:
                                    java.util.List<ExternalFileType> fileTypes = new ArrayList<ExternalFileType>();
                                    ExternalFileType[] oldTypes = Globals.prefs.getExternalFileTypeSelection();
                                    for (int k = 0; k < oldTypes.length; k++) {
                                        fileTypes.add(oldTypes[k]);
                                    }
                                    fileTypes.add(newType);
                                    Collections.sort(fileTypes);
                                    Globals.prefs.setExternalFileTypes(fileTypes);
                                    panel.mainTable.repaint();
                                }
                            }
                            else {
                                // User wants to change the type of this link.
                                // First get a model of all file links for this entry:
                                FileListEntryEditor editor = new FileListEntryEditor
                                        (panel.frame(), flEntry, false, true, panel.metaData());
                                editor.setVisible(true, false);
                            }
                        }
                    }

                    if (!tableModel.getStringRepresentation().equals(old)) {
                        // The table has been modified. Store the change:
                        String toSet = tableModel.getStringRepresentation();
                        if (toSet.length() == 0)
                            toSet = null;
                        ce.addEdit(new UndoableFieldChange(sel[i], fieldName, old,
                                toSet));
                        sel[i].setField(fieldName, toSet);
                        changedEntries.add(sel[i]);
                        //System.out.println("Changed to: "+tableModel.getStringRepresentation());
                    }


                }
            }
        }

        entriesChangedCount = changedEntries.size();
	//for (BibtexEntry entr : changedEntries)
	//    System.out.println(entr.getCiteKey());
        if (entriesChangedCount > 0) {
            // Add the undo edit:
            ce.end();
            panel.undoManager.addEdit(ce);
            
        }
    }


    public void update() {
        if (!goOn)
            return;

        panel.output(Globals.lang("Finished synchronizing %0 links. Entries changed%c %1.",
                new String[]{fieldName.toUpperCase(), String.valueOf(entriesChangedCount)}));
        panel.frame().setProgressBarVisible(false);
        if (entriesChangedCount > 0) {
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
        private MetaData metaData;

        public OptionsDialog(JFrame parent, MetaData metaData, String fieldName) {
            super(parent, Globals.lang("Synchronize %0 links", fieldName.toUpperCase()), true);
            this.metaData = metaData;
            final String fn = Globals.lang("file");
            ok.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    canceled = false;
                    dispose();
                }
            });

            Action closeAction = new AbstractAction() {
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
                    Globals.lang("This makes JabRef look up each %0 extension and check if the file exists. If not, you will "
                            + "be given options<BR>to resolve the problem.", fn)
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

            String dir = metaData.getFileDirectory(GUIGlobals.FILE_FIELD);
            if ((dir == null) || (dir.trim().length() == 0)) {

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
