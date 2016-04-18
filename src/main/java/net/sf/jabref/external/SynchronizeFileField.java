/*  Copyright (C) 2003-2016 JabRef contributors.
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
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListEntryEditor;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.BibEntry;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This action goes through all selected entries in the BasePanel, and attempts to autoset the
 * given external file (pdf, ps, ...) based on the same algorithm used for the "Auto" button in
 * EntryEditor.
 */
public class SynchronizeFileField extends AbstractWorker {

    private final BasePanel panel;
    private List<BibEntry> sel;
    private SynchronizeFileField.OptionsDialog optDiag;

    private int entriesChangedCount;

    private final Object[] brokenLinkOptions = {
            Localization.lang("Ignore"),
            Localization.lang("Assign new file"),
            Localization.lang("Remove link"),
            Localization.lang("Remove all broken links"),
            Localization.lang("Quit synchronization")};

    private boolean goOn = true;
    private boolean autoSet = true;
    private boolean checkExisting = true;


    public SynchronizeFileField(BasePanel panel) {
        this.panel = panel;
    }

    @Override
    public void init() {
        Collection<BibEntry> col = panel.getDatabase().getEntries();
        goOn = true;
        sel = new ArrayList<>(col);

        // Ask about rules for the operation:
        if (optDiag == null) {
            optDiag = new SynchronizeFileField.OptionsDialog(panel.frame(), panel.getBibDatabaseContext());
        }
        optDiag.setLocationRelativeTo(panel.frame());
        optDiag.setVisible(true);
        if (optDiag.canceled()) {
            goOn = false;
            return;
        }
        autoSet = !optDiag.isAutoSetNone();
        checkExisting = optDiag.isCheckLinks();

        panel.output(Localization.lang("Synchronizing file links..."));
    }

    @Override
    public void run() {
        if (!goOn) {
            panel.output(Localization.lang("This operation requires one or more entries to be selected."));
            return;
        }
        entriesChangedCount = 0;
        panel.frame().setProgressBarValue(0);
        panel.frame().setProgressBarVisible(true);
        int weightAutoSet = 10; // autoSet takes 10 (?) times longer than checkExisting
        int progressBarMax = (autoSet ? weightAutoSet * sel.size() : 0) + (checkExisting ? sel.size() : 0);
        panel.frame().setProgressBarMaximum(progressBarMax);
        int progress = 0;
        final NamedCompound ce = new NamedCompound(Localization.lang("Automatically set file links"));

        Set<BibEntry> changedEntries = new HashSet<>();

        // First we try to autoset fields
        if (autoSet) {
            Collection<BibEntry> entries = new ArrayList<>(sel);

            // Start the automatically setting process:
            Runnable r = AutoSetLinks.autoSetLinks(entries, ce, changedEntries, null, panel.getBibDatabaseContext(), null, null);
            JabRefExecutorService.INSTANCE.executeAndWait(r);
        }
        progress += sel.size() * weightAutoSet;
        panel.frame().setProgressBarValue(progress);
        // The following loop checks all external links that are already set.
        if (checkExisting) {
            boolean removeAllBroken = false;
            mainLoop: for (BibEntry aSel : sel) {
                panel.frame().setProgressBarValue(progress++);
                final String old = aSel.getField(Globals.FILE_FIELD);
                // Check if a extension is set:
                if ((old != null) && !(old.isEmpty())) {
                    FileListTableModel tableModel = new FileListTableModel();
                    tableModel.setContentDontGuessTypes(old);

                    // We need to specify which directories to search in for Util.expandFilename:
                    List<String> dirsS = panel.getBibDatabaseContext().getFileDirectory();
                    List<File> dirs = new ArrayList<>();
                    for (String dirs1 : dirsS) {
                        dirs.add(new File(dirs1));
                    }

                    for (int j = 0; j < tableModel.getRowCount(); j++) {
                        FileListEntry flEntry = tableModel.getEntry(j);
                        // See if the link looks like an URL:
                        boolean httpLink = flEntry.link.toLowerCase(Locale.ENGLISH).startsWith("http");
                        if (httpLink) {
                            continue; // Don't check the remote file.
                            // TODO: should there be an option to check remote links?
                        }

                        // A variable to keep track of whether this link gets deleted:
                        boolean deleted = false;

                        // Get an absolute path representation:
                        Optional<File> file = FileUtil.expandFilename(flEntry.link, dirsS);
                        if ((!file.isPresent()) || !file.get().exists()) {
                            int answer;
                            if (removeAllBroken) {
                                answer = 2; // We should delete this link.
                            } else {
                                answer = JOptionPane.showOptionDialog(panel.frame(),
                                        Localization.lang("<HTML>Could not find file '%0'<BR>linked from entry '%1'</HTML>",
                                                flEntry.link, aSel.getCiteKey()),
                                        Localization.lang("Broken link"),
                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null, brokenLinkOptions, brokenLinkOptions[0]
                                        );
                            }
                            switch (answer) {
                            case 1:
                                // Assign new file.
                                FileListEntryEditor flEditor = new FileListEntryEditor
                                (panel.frame(), flEntry, false, true, panel.getBibDatabaseContext());
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
                            default:
                                // Cancel
                                break mainLoop;
                            }
                        }

                        // Unless we deleted this link, see if its file type is recognized:
                        if (!deleted && flEntry.type.isPresent()
                                && (flEntry.type.get() instanceof UnknownExternalFileType)) {
                            String[] options = new String[] {
                                    Localization.lang("Define '%0'", flEntry.type.get().getName()),
                                    Localization.lang("Change file type"),
                                    Localization.lang("Cancel")};
                            String defOption = options[0];
                            int answer = JOptionPane.showOptionDialog(panel.frame(), Localization.lang("One or more file links are of the type '%0', which is undefined. What do you want to do?",
                                    flEntry.type.get().getName()),
                                    Localization.lang("Undefined file type"), JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null, options, defOption
                                    );
                            if (answer == JOptionPane.CANCEL_OPTION) {
                                // User doesn't want to handle this unknown link type.
                            } else if (answer == JOptionPane.YES_OPTION) {
                                // User wants to define the new file type. Show the dialog:
                                ExternalFileType newType = new ExternalFileType(flEntry.type.get().getName(), "", "",
                                        "", "new", IconTheme.JabRefIcon.FILE.getSmallIcon());
                                ExternalFileTypeEntryEditor editor = new ExternalFileTypeEntryEditor(panel.frame(), newType);
                                editor.setVisible(true);
                                if (editor.okPressed()) {
                                    // Get the old list of types, add this one, and update the list in prefs:
                                    List<ExternalFileType> fileTypes = new ArrayList<>(
                                            ExternalFileTypes.getInstance().getExternalFileTypeSelection());
                                    fileTypes.add(newType);
                                    Collections.sort(fileTypes);
                                    ExternalFileTypes.getInstance().setExternalFileTypes(fileTypes);
                                    panel.mainTable.repaint();
                                }
                            } else {
                                // User wants to change the type of this link.
                                // First get a model of all file links for this entry:
                                FileListEntryEditor editor = new FileListEntryEditor
                                        (panel.frame(), flEntry, false, true, panel.getBibDatabaseContext());
                                editor.setVisible(true, false);
                            }
                        }
                    }

                    if (!tableModel.getStringRepresentation().equals(old)) {
                        // The table has been modified. Store the change:
                        String toSet = tableModel.getStringRepresentation();
                        if (toSet.isEmpty()) {
                            ce.addEdit(new UndoableFieldChange(aSel, Globals.FILE_FIELD, old, null));
                            aSel.clearField(Globals.FILE_FIELD);
                        } else {
                            ce.addEdit(new UndoableFieldChange(aSel, Globals.FILE_FIELD, old, toSet));
                            aSel.setField(Globals.FILE_FIELD, toSet);
                        }
                        changedEntries.add(aSel);
                    }

                }
            }
        }

        if (!changedEntries.isEmpty()) {
            // Add the undo edit:
            ce.end();
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
            entriesChangedCount = changedEntries.size();
        }
    }

    @Override
    public void update() {
        if (!goOn) {
            return;
        }

        panel.output(Localization.lang("Finished synchronizing file links. Entries changed: %0.",
                String.valueOf(entriesChangedCount)));
        panel.frame().setProgressBarVisible(false);
        if (entriesChangedCount > 0) {
            panel.markBaseChanged();
        }
    }


    static class OptionsDialog extends JDialog {


        private final JButton ok = new JButton(Localization.lang("OK"));
        private final JButton cancel = new JButton(Localization.lang("Cancel"));
        private boolean canceled = true;
        private final BibDatabaseContext databaseContext;
        private final JRadioButton autoSetUnset = new JRadioButton(
                Localization.lang("Automatically set file links") + ". "
                        + Localization.lang("Do not overwrite existing links."),
                true);
        private final JRadioButton autoSetAll = new JRadioButton(
                Localization.lang("Automatically set file links") + ". "
                        + Localization.lang("Allow overwriting existing links."),
                false);
        private final JRadioButton autoSetNone = new JRadioButton(Localization.lang("Do not automatically set"), false);
        private final JCheckBox checkLinks = new JCheckBox(Localization.lang("Check existing file links"), true);


        public OptionsDialog(JFrame parent, BibDatabaseContext databaseContext) {
            super(parent, Localization.lang("Synchronize file links"), true);
            this.databaseContext = databaseContext;
            ok.addActionListener(e -> {
                canceled = false;
                dispose();
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
            im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
            am.put("close", closeAction);

            ButtonGroup bg = new ButtonGroup();
            bg.add(autoSetUnset);
            bg.add(autoSetNone);
            bg.add(autoSetAll);

            FormLayout layout = new FormLayout("fill:pref", "pref, 2dlu, pref, 2dlu, pref, pref, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref");
            FormBuilder builder = FormBuilder.create().layout(layout);
            JLabel description = new JLabel("<HTML>" + Localization.lang(
                    "Attempt to automatically set file links for your entries. Automatically setting works if "
                            + "a file in your file directory<BR>or a subdirectory is named identically to an entry's BibTeX key, plus extension.")
                    + "</HTML>");

            builder.addSeparator(Localization.lang("Automatically set file links")).xy(1, 1);
            builder.add(description).xy(1, 3);
            builder.add(autoSetUnset).xy(1, 5);
            builder.add(autoSetAll).xy(1, 6);
            builder.add(autoSetNone).xy(1, 7);
            builder.addSeparator(Localization.lang("Check links")).xy(1, 9);

            description = new JLabel("<HTML>"
                    + Localization
                            .lang("This makes JabRef look up each file link and check if the file exists. If not, you will be given options<BR>to resolve the problem.")
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

            List<String> dirs = databaseContext.getFileDirectory();
            if (dirs.isEmpty()) {
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

        public boolean isAutoSetNone() {
            return autoSetNone.isSelected();
        }

        public boolean isCheckLinks() {
            return checkLinks.isSelected();
        }

        public boolean canceled() {
            return canceled;
        }
    }
}
