/*  Copyright (C) 2012-2015 JabRef contributors.
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
package net.sf.jabref.gui.actions;

import java.util.List;
import java.util.Objects;

import javax.swing.JOptionPane;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.cleanup.CleanupPresetPanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.util.component.CheckBoxMessage;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.cleanup.CleanupPreset;
import net.sf.jabref.logic.cleanup.CleanupWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

public class CleanupAction extends AbstractWorker {

    private final BasePanel panel;
    private final JabRefFrame frame;

    /**
     * Global variable to count unsuccessful renames
     */
    private int unsuccessfulRenames;

    private boolean canceled;
    private int modifiedEntriesCount;
    private final JabRefPreferences preferences;


    public CleanupAction(BasePanel panel, JabRefPreferences preferences) {
        this.panel = panel;
        this.frame = panel.frame();
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public void init() {
        canceled = false;
        modifiedEntriesCount = 0;
        if (panel.getSelectedEntries().isEmpty()) { // None selected. Inform the user to select entries first.
            JOptionPane.showMessageDialog(frame, Localization.lang("First select entries to clean up."),
                    Localization.lang("Cleanup entry"), JOptionPane.INFORMATION_MESSAGE);
            canceled = true;
            return;
        }
        frame.block();
        panel.output(Localization.lang("Doing a cleanup for %0 entries...",
                Integer.toString(panel.getSelectedEntries().size())));
    }

    @Override
    public void run() {
        if (canceled) {
            return;
        }
        CleanupPresetPanel presetPanel = new CleanupPresetPanel(panel.getBibDatabaseContext(),
                CleanupPreset.loadFromPreferences(preferences));
        int choice = showDialog(presetPanel);
        if (choice != JOptionPane.OK_OPTION) {
            canceled = true;
            return;
        }
        CleanupPreset cleanupPreset = presetPanel.getCleanupPreset();
        cleanupPreset.storeInPreferences(preferences);

        if (cleanupPreset.isRenamePDF() && Globals.prefs.getBoolean(JabRefPreferences.AKS_AUTO_NAMING_PDFS_AGAIN)) {
            CheckBoxMessage cbm = new CheckBoxMessage(
                    Localization.lang("Auto-generating PDF-Names does not support undo. Continue?"),
                    Localization.lang("Disable this confirmation dialog"), false);
            int answer = JOptionPane.showConfirmDialog(frame, cbm, Localization.lang("Autogenerate PDF Names"),
                    JOptionPane.YES_NO_OPTION);
            if (cbm.isSelected()) {
                Globals.prefs.putBoolean(JabRefPreferences.AKS_AUTO_NAMING_PDFS_AGAIN, false);
            }
            if (answer == JOptionPane.NO_OPTION) {
                canceled = true;
                return;
            }
        }

        for (BibEntry entry : panel.getSelectedEntries()) {
            // undo granularity is on entry level
            NamedCompound ce = new NamedCompound(Localization.lang("Cleanup entry"));

            doCleanup(cleanupPreset, entry, ce);

            ce.end();
            if (ce.hasEdits()) {
                modifiedEntriesCount++;
                panel.undoManager.addEdit(ce);
            }
        }
    }

    @Override
    public void update() {
        if (canceled) {
            frame.unblock();
            return;
        }
        if (unsuccessfulRenames > 0) { //Rename failed for at least one entry
            JOptionPane.showMessageDialog(frame,
                    Localization.lang("File rename failed for %0 entries.", Integer.toString(unsuccessfulRenames)),
                    Localization.lang("Autogenerate PDF Names"), JOptionPane.INFORMATION_MESSAGE);
        }
        if (modifiedEntriesCount > 0) {
            panel.updateEntryEditorIfShowing();
            panel.markBaseChanged();
        }
        String message;
        switch (modifiedEntriesCount) {
        case 0:
            message = Localization.lang("No entry needed a clean up");
            break;
        case 1:
            message = Localization.lang("One entry needed a clean up");
            break;
        default:
            message = Localization.lang("%0 entries needed a clean up", Integer.toString(modifiedEntriesCount));
            break;
        }
        panel.output(message);
        frame.unblock();
    }

    private int showDialog(CleanupPresetPanel presetPanel) {
        String dialogTitle = Localization.lang("Cleanup entries");

        Object[] messages = {Localization.lang("What would you like to clean up?"), presetPanel.getPanel()};
        return JOptionPane.showConfirmDialog(frame, messages, dialogTitle, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Runs the cleanup on the entry and records the change.
     */
    private void doCleanup(CleanupPreset preset, BibEntry entry, NamedCompound ce) {
        // Create and run cleaner
        BibDatabaseContext bibDatabaseContext = panel.getBibDatabaseContext();
        CleanupWorker cleaner = new CleanupWorker(bibDatabaseContext,
                Globals.journalAbbreviationLoader.getRepository());
        List<FieldChange> changes = cleaner.cleanup(preset, entry);

        unsuccessfulRenames = cleaner.getUnsuccessfulRenames();

        if (changes.isEmpty()) {
            return;
        }

        // Register undo action
        for (FieldChange change : changes) {
            ce.addEdit(new UndoableFieldChange(change));
        }
    }
}
