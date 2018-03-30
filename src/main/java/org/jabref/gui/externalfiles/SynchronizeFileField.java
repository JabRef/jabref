package org.jabref.gui.externalfiles;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.jabref.Globals;
import org.jabref.JabRefExecutorService;
import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypeEntryEditor;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.UnknownExternalFileType;
import org.jabref.gui.filelist.FileListEntry;
import org.jabref.gui.filelist.FileListEntryEditor;
import org.jabref.gui.filelist.FileListTableModel;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.gui.worker.AbstractWorker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.LinkedFile;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This action goes through all selected entries in the BasePanel, and attempts to auto set the
 * given external file (pdf, ps, ...)
 */
public class SynchronizeFileField extends AbstractWorker {

    private final BasePanel panel;
    private final Object[] brokenLinkOptions = {Localization.lang("Ignore"), Localization.lang("Assign new file"),
            Localization.lang("Remove link"), Localization.lang("Remove all broken links"),
            Localization.lang("Quit synchronization")};
    private List<BibEntry> selectedEntries;
    private SynchronizeFileField.OptionsDialog dialog;
    private int entriesChangedCount;
    private boolean goOn = true;
    private boolean autoSet = true;
    private boolean checkExisting = true;

    public SynchronizeFileField(BasePanel panel) {
        this.panel = panel;
    }

    @Override
    public void init() {
        goOn = true;
        selectedEntries = new ArrayList<>(panel.getDatabase().getEntries());

        // Ask about rules for the operation:
        if (dialog == null) {
            dialog = new SynchronizeFileField.OptionsDialog(panel.frame(), panel.getBibDatabaseContext());
        }
        dialog.setLocationRelativeTo(panel.frame());
        dialog.setVisible(true);
        if (dialog.canceled()) {
            goOn = false;
            return;
        }
        autoSet = !dialog.isAutoSetNone();
        checkExisting = dialog.isCheckLinks();

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
        int progressBarMax = (autoSet ? weightAutoSet * selectedEntries.size() : 0) + (checkExisting ? selectedEntries.size() : 0);
        panel.frame().setProgressBarMaximum(progressBarMax);
        int progress = 0;
        final NamedCompound ce = new NamedCompound(Localization.lang("Automatically set file links"));

        Set<BibEntry> changedEntries = new HashSet<>();

        // First we try to autoset fields
        if (autoSet) {
            List<BibEntry> entries = new ArrayList<>(selectedEntries);

            // Start the automatically setting process:
            Runnable runnable = AutoSetLinks.autoSetLinks(entries, ce, changedEntries, panel.getBibDatabaseContext(), null, null);
            JabRefExecutorService.INSTANCE.executeAndWait(runnable);
        }
        progress += selectedEntries.size() * weightAutoSet;
        panel.frame().setProgressBarValue(progress);
        // The following loop checks all external links that are already set.
        if (checkExisting) {
            boolean removeAllBroken = false;
            mainLoop: for (BibEntry aSel : selectedEntries) {
                panel.frame().setProgressBarValue(progress++);
                final Optional<String> old = aSel.getField(FieldName.FILE);
                // Check if a extension is set:
                if (old.isPresent() && !(old.get().isEmpty())) {
                    FileListTableModel tableModel = new FileListTableModel();
                    tableModel.setContentDontGuessTypes(old.get());

                    for (int j = 0; j < tableModel.getRowCount(); j++) {
                        FileListEntry flEntry = tableModel.getEntry(j);
                        LinkedFile field = flEntry.toParsedFileField();

                        // See if the link looks like an URL:
                        if (field.isOnlineLink()) {
                            continue; // Don't check the remote file.
                            // TODO: should there be an option to check remote links?
                        }

                        // A variable to keep track of whether this link gets deleted:
                        boolean deleted = false;

                        // Get an absolute path representation:
                        Optional<Path> file = field.findIn(panel.getBibDatabaseContext(), Globals.prefs.getFileDirectoryPreferences());
                        if ((!file.isPresent()) || !Files.exists(file.get())) {
                            int answer;
                            if (removeAllBroken) {
                                answer = 2; // We should delete this link.
                            } else {
                                answer = JOptionPane.showOptionDialog(panel.frame(),
                                        Localization.lang("<HTML>Could not find file '%0'<BR>linked from entry '%1'</HTML>",
                                                flEntry.getLink(),
                                                aSel.getCiteKeyOptional().orElse(Localization.lang("undefined"))),
                                        Localization.lang("Broken link"),
                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                        JOptionPane.QUESTION_MESSAGE, null, brokenLinkOptions, brokenLinkOptions[0]
                                        );
                            }
                            switch (answer) {
                            case 1:
                                // Assign new file.
                                FileListEntryEditor flEditor = new FileListEntryEditor(flEntry.toParsedFileField(), false, true, panel.getBibDatabaseContext());
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
                        if (!deleted && flEntry.getType().isPresent()
                                && (flEntry.getType().get() instanceof UnknownExternalFileType)) {
                            String[] options = new String[] {
                                    Localization.lang("Define '%0'", flEntry.getType().get().getName()),
                                    Localization.lang("Change file type"),
                                    Localization.lang("Cancel")};
                            String defOption = options[0];
                            int answer = JOptionPane.showOptionDialog(panel.frame(), Localization.lang("One or more file links are of the type '%0', which is undefined. What do you want to do?",
                                    flEntry.getType().get().getName()),
                                    Localization.lang("Undefined file type"), JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null, options, defOption
                                    );
                            if (answer == JOptionPane.CANCEL_OPTION) {
                                // User doesn't want to handle this unknown link type.
                            } else if (answer == JOptionPane.YES_OPTION) {
                                // User wants to define the new file type. Show the dialog:
                                ExternalFileType newType = new ExternalFileType(flEntry.getType().get().getName(), "", "",
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
                                    panel.getMainTable().repaint();
                                }
                            } else {
                                // User wants to change the type of this link.
                                // First get a model of all file links for this entry:
                                FileListEntryEditor editor = new FileListEntryEditor
                                        (flEntry.toParsedFileField(), false, true, panel.getBibDatabaseContext());
                                editor.setVisible(true, false);
                            }
                        }
                    }

                    if (!tableModel.getStringRepresentation().equals(old.orElse(null))) {
                        // The table has been modified. Store the change:
                        String toSet = tableModel.getStringRepresentation();
                        if (toSet.isEmpty()) {
                            ce.addEdit(new UndoableFieldChange(aSel, FieldName.FILE, old.orElse(null), null));
                            aSel.clearField(FieldName.FILE);
                        } else {
                            ce.addEdit(new UndoableFieldChange(aSel, FieldName.FILE, old.orElse(null), toSet));
                            aSel.setField(FieldName.FILE, toSet);
                        }
                        changedEntries.add(aSel);
                    }

                }
            }
        }

        if (!changedEntries.isEmpty()) {
            // Add the undo edit:
            ce.end();
            panel.getUndoManager().addEdit(ce);
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

    static class OptionsDialog extends JabRefDialog {
        private final JButton ok = new JButton(Localization.lang("OK"));
        private final JButton cancel = new JButton(Localization.lang("Cancel"));
        private final BibDatabaseContext databaseContext;
        private final JRadioButton autoSetUnset = new JRadioButton(Localization.lang("Automatically set file links")
                + ". " + Localization.lang("Do not overwrite existing links."), true);
        private final JRadioButton autoSetAll = new JRadioButton(Localization.lang("Automatically set file links")
                + ". " + Localization.lang("Allow overwriting existing links."), false);
        private final JRadioButton autoSetNone = new JRadioButton(Localization.lang("Do not automatically set"), false);
        private final JCheckBox checkLinks = new JCheckBox(Localization.lang("Check existing file links"), true);
        private boolean canceled = true;


        public OptionsDialog(JFrame parent, BibDatabaseContext databaseContext) {
            super(parent, Localization.lang("Synchronize file links"), true, OptionsDialog.class);
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

            FormLayout layout = new FormLayout("fill:pref",
                    "pref, 2dlu, pref, 2dlu, pref, pref, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref");
            FormBuilder builder = FormBuilder.create().layout(layout);
            JLabel description = new JLabel("<HTML>"
                    + Localization
                            .lang("Attempt to automatically set file links for your entries. Automatically setting works if "
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

            List<String> dirs = databaseContext.getFileDirectories(Globals.prefs.getFileDirectoryPreferences());
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

            ok.requestFocus();
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
