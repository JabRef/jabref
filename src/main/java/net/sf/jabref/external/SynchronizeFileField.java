/*  Copyright (C) 2003-2015 JabRef contributors.
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

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.*;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.keyboard.KeyBinds;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.util.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * This action goes through all selected entries in the BasePanel, and attempts to autoset the
 * given external file (pdf, ps, ...) based on the same algorithm used for the "Auto" button in
 * EntryEditor.
 */
public class SynchronizeFileField extends AbstractWorker {

    private final String fieldName = Globals.FILE_FIELD;
    private final BasePanel panel;
    private BibtexEntry[] sel;
    private SynchronizeFileField.OptionsDialog optDiag;

    private final Object[] brokenLinkOptions = {
            // @formatter:off
            Localization.lang("Ignore"),
            Localization.lang("Assign new file"),
            Localization.lang("Remove link"),
            Localization.lang("Remove all broken links"),
            Localization.lang("Quit synchronization")};
    // @formatter:on

    private boolean goOn = true;
    private boolean autoSet = true;
    private boolean checkExisting = true;


    public SynchronizeFileField(BasePanel panel) {
        this.panel = panel;
    }

    @Override
    public void init() {
        Collection<BibtexEntry> col = panel.database().getEntries();
        goOn = true;
        sel = new BibtexEntry[col.size()];
        sel = col.toArray(sel);

        // Ask about rules for the operation:
        if (optDiag == null) {
            optDiag = new SynchronizeFileField.OptionsDialog(panel.frame(), panel.metaData(), fieldName);
        }
        PositionWindow.placeDialog(optDiag, panel.frame());
        optDiag.setVisible(true);
        if (optDiag.canceled()) {
            goOn = false;
            return;
        }
        autoSet = !optDiag.autoSetNone.isSelected();
        checkExisting = optDiag.checkLinks.isSelected();

        panel.output(Localization.lang("Synchronizing %0 links...", fieldName.toUpperCase()));
    }

    @Override
    public void run() {
        if (!goOn) {
            panel.output(Localization.lang("No entries selected."));
            return;
        }
        panel.frame().setProgressBarValue(0);
        panel.frame().setProgressBarVisible(true);
        int weightAutoSet = 10; // autoSet takes 10 (?) times longer than checkExisting
        int progressBarMax = (autoSet ? weightAutoSet * sel.length : 0)
                + (checkExisting ? sel.length : 0);
        panel.frame().setProgressBarMaximum(progressBarMax);
        int progress = 0;
        final NamedCompound ce = new NamedCompound(Localization.lang("Autoset %0 field", fieldName));

        //final OpenFileFilter off = Util.getFileFilterForField(fieldName);

        //ExternalFilePanel extPan = new ExternalFilePanel(fieldName, panel.metaData(), null, null, off);
        //TextField editor = new TextField(fieldName, "", false);

        Set<BibtexEntry> changedEntries = new HashSet<>();

        // First we try to autoset fields
        if (autoSet) {
            Collection<BibtexEntry> entries = new ArrayList<>();
            Collections.addAll(entries, sel);

            // Start the autosetting process:
            Runnable r = Util.autoSetLinks(entries, ce, changedEntries, null, panel.metaData(), null, null);
            JabRefExecutorService.INSTANCE.executeAndWait(r);
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
        progress += sel.length * weightAutoSet;
        panel.frame().setProgressBarValue(progress);
        //System.out.println("Done setting");
        // The following loop checks all external links that are already set.
        if (checkExisting) {
            boolean removeAllBroken = false;
            mainLoop: for (BibtexEntry aSel : sel) {
                panel.frame().setProgressBarValue(progress++);
                final String old = aSel.getField(fieldName);
                // Check if a extension is set:
                if ((old != null) && !old.equals("")) {
                    FileListTableModel tableModel = new FileListTableModel();
                    tableModel.setContentDontGuessTypes(old);

                    // We need to specify which directories to search in for Util.expandFilename:
                    String[] dirsS = panel.metaData().getFileDirectory(Globals.FILE_FIELD);
                    ArrayList<File> dirs = new ArrayList<>();
                    for (String dirs1 : dirsS) {
                        dirs.add(new File(dirs1));
                    }

                    for (int j = 0; j < tableModel.getRowCount(); j++) {
                        FileListEntry flEntry = tableModel.getEntry(j);
                        // See if the link looks like an URL:
                        boolean httpLink = flEntry.getLink().toLowerCase().startsWith("http");
                        if (httpLink)
                        {
                            continue; // Don't check the remote file.
                            // TODO: should there be an option to check remote links?
                        }

                        // A variable to keep track of whether this link gets deleted:
                        boolean deleted = false;

                        // Get an absolute path representation:
                        File file = FileUtil.expandFilename(flEntry.getLink(), dirsS);
                        if ((file == null) || !file.exists()) {
                            int answer;
                            if (!removeAllBroken) {
                                answer = JOptionPane.showOptionDialog(panel.frame(),
                                        Localization.lang("<HTML>Could not find file '%0'<BR>linked from entry '%1'</HTML>",
                                                new String[]{flEntry.getLink(), aSel.getCiteKey()}),
                                        Localization.lang("Broken link"),
                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null, brokenLinkOptions, brokenLinkOptions[0]
                                        );
                            } else {
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
                        }

                        // Unless we deleted this link, see if its file type is recognized:
                        if (!deleted && (flEntry.getType() instanceof UnknownExternalFileType)) {
                            // @formatter:off
                            String[] options = new String[] {
                                    Localization.lang("Define '%0'", flEntry.getType().getName()),
                                    Localization.lang("Change file type"),
                                    Localization.lang("Cancel")};
                            // @formatter:on
                            String defOption = options[0];
                            int answer = JOptionPane.showOptionDialog(panel.frame(), Localization.lang("One or more file links are of the type '%0', which is undefined. What do you want to do?",
                                    flEntry.getType().getName()),
                                    Localization.lang("Undefined file type"), JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null, options, defOption
                                    );
                            if (answer == JOptionPane.CANCEL_OPTION) {
                                // User doesn't want to handle this unknown link type.
                            } else if (answer == JOptionPane.YES_OPTION) {
                                // User wants to define the new file type. Show the dialog:
                                ExternalFileType newType = new ExternalFileType(flEntry.getType().getName(), "", "", "", "new", IconTheme.getImage("new"));
                                ExternalFileTypeEntryEditor editor = new ExternalFileTypeEntryEditor(panel.frame(), newType);
                                editor.setVisible(true);
                                if (editor.okPressed()) {
                                    // Get the old list of types, add this one, and update the list in prefs:
                                    List<ExternalFileType> fileTypes = new ArrayList<>();
                                    ExternalFileType[] oldTypes = Globals.prefs.getExternalFileTypeSelection();
                                    Collections.addAll(fileTypes, oldTypes);
                                    fileTypes.add(newType);
                                    Collections.sort(fileTypes);
                                    Globals.prefs.setExternalFileTypes(fileTypes);
                                    panel.mainTable.repaint();
                                }
                            } else {
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
                        if (toSet.isEmpty()) {
                            toSet = null;
                        }
                        ce.addEdit(new UndoableFieldChange(aSel, fieldName, old,
                                toSet));
                        aSel.setField(fieldName, toSet);
                        changedEntries.add(aSel);
                        //System.out.println("Changed to: "+tableModel.getStringRepresentation());
                    }

                }
            }
        }

        if (!changedEntries.isEmpty()) {
            // Add the undo edit:
            ce.end();
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
        }
    }

    @Override
    public void update() {
        if (!goOn) {
            return;
        }

        int entriesChangedCount = 0;
        panel.output(Localization.lang("Finished synchronizing %0 links. Entries changed%c %1.",
                new String[]{fieldName.toUpperCase(), String.valueOf(entriesChangedCount)}));
        panel.frame().setProgressBarVisible(false);
        if (entriesChangedCount > 0) {
            panel.markBaseChanged();
        }
    }


    static class OptionsDialog extends JDialog {

        final JRadioButton autoSetUnset;
        final JRadioButton autoSetAll;
        final JRadioButton autoSetNone;
        final JCheckBox checkLinks;
        final JButton ok = new JButton(Localization.lang("Ok"));
        final JButton cancel = new JButton(Localization.lang("Cancel"));
        JLabel description;
        private boolean canceled = true;
        private final MetaData metaData;


        public OptionsDialog(JFrame parent, MetaData metaData, String fieldName) {
            super(parent, Localization.lang("Synchronize %0 links", fieldName.toUpperCase()), true);
            this.metaData = metaData;
            final String fn = Localization.lang("file");
            ok.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    canceled = false;
                    dispose();
                }
            });

            Action closeAction = new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            };

            cancel.addActionListener(closeAction);

            InputMap im = cancel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = cancel.getActionMap();
            im.put(Globals.prefs.getKey(KeyBinds.CLOSE_DIALOG), "close");
            am.put("close", closeAction);

            autoSetUnset = new JRadioButton(Localization.lang("Autoset %0 links. Do not overwrite existing links.", fn), true);
            autoSetAll = new JRadioButton(Localization.lang("Autoset %0 links. Allow overwriting existing links.", fn), false);
            autoSetNone = new JRadioButton(Localization.lang("Do not autoset"), false);
            checkLinks = new JCheckBox(Localization.lang("Check existing %0 links", fn), true);
            ButtonGroup bg = new ButtonGroup();
            bg.add(autoSetUnset);
            bg.add(autoSetNone);
            bg.add(autoSetAll);

            FormLayout layout = new FormLayout("fill:pref", "pref, 2dlu, pref, 2dlu, pref, pref, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref");
            FormBuilder builder = FormBuilder.create().layout(layout);
            description = new JLabel("<HTML>" + Localization.lang(
                    "Attempt to autoset %0 links for your entries. Autoset works if "
                            + "a %0 file in your %0 directory or a subdirectory<BR>is named identically to an entry's BibTeX key, plus extension.",
                    fn) + "</HTML>");

            builder.addSeparator(Localization.lang("Autoset")).xy(1, 1);
            builder.add(description).xy(1, 3);
            builder.add(autoSetUnset).xy(1, 5);
            builder.add(autoSetAll).xy(1, 6);
            builder.add(autoSetNone).xy(1, 7);
            builder.addSeparator(Localization.lang("Check links")).xy(1, 9);

            description = new JLabel("<HTML>" +
                    Localization.lang("This makes JabRef look up each %0 link and check if the file exists. If not, you will be given options<BR>to resolve the problem.", fn)
            + "</HTML>");
            builder.add(description).xy(1, 11);
            builder.add(checkLinks).xy(1, 13);
            builder.addSeparator("").xy(1, 15);

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

            String[] dirs = metaData.getFileDirectory(Globals.FILE_FIELD);
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
